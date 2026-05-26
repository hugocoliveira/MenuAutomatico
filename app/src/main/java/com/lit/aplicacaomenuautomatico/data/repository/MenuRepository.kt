package com.lit.aplicacaomenuautomatico.data.repository

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / segurança
// ─────────────────────────────────────────────────────────────────────────────
import android.content.SharedPreferences // leitura e escrita das credenciais criptografadas
import android.util.Base64               // codifica username:password em Base64 para Basic Auth

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — camada local (Room)
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.data.local.AplicativoDao    // DAO da tabela aplicativos
import com.lit.aplicacaomenuautomatico.data.local.AplicativoEntity // entidade Room de app externo
import com.lit.aplicacaomenuautomatico.data.local.MenuAppDao       // DAO da tabela menu_app
import com.lit.aplicacaomenuautomatico.data.local.MenuAppEntity    // entidade Room de item de menu
import com.lit.aplicacaomenuautomatico.data.local.SyncLogDao       // DAO da tabela sync_log
import com.lit.aplicacaomenuautomatico.data.local.SyncLogEntity    // entidade Room de log de sync

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — camada remota (Retrofit / OData)
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.data.remote.MenuAppDto  // DTO recebido do SAP OData
import com.lit.aplicacaomenuautomatico.data.remote.ODataService // interface Retrofit do endpoint SAP

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — modelo de domínio
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.domain.model.MenuApp // modelo de domínio exposto à UI

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Corrotinas / Flow
// ─────────────────────────────────────────────────────────────────────────────
import kotlinx.coroutines.flow.Flow // stream reativo emitido pelo DAO
import kotlinx.coroutines.flow.map  // transforma cada emissão do Flow (Entity → Domain)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Java utilitários
// ─────────────────────────────────────────────────────────────────────────────
import java.text.SimpleDateFormat // formata timestamp ISO 8601 (compatível com API 24)
import java.util.Date             // data/hora atual para gravar no log de sincronização
import java.util.Locale           // locale do dispositivo para formatação da data

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt / injeção de dependência
// ─────────────────────────────────────────────────────────────────────────────
import javax.inject.Inject    // anotação padrão JSR-330 para injeção de construtor
import javax.inject.Singleton // garante uma única instância do repositório no contêiner Hilt

// ═════════════════════════════════════════════════════════════════════════════
// CONSTANTES PRIVADAS
// ═════════════════════════════════════════════════════════════════════════════

/** Chave do username no EncryptedSharedPreferences */
private const val PREF_USERNAME = "username"

/** Chave da senha no EncryptedSharedPreferences */
private const val PREF_PASSWORD = "password"

// ═════════════════════════════════════════════════════════════════════════════
// REPOSITÓRIO: MenuRepository
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Repositório central de dados do app.
 *
 * Orquestra todas as operações entre o banco local (Room) e o servidor remoto (SAP OData).
 * É a única fonte de verdade para os dados de menu — a UI nunca acessa DAOs diretamente.
 *
 * Responsabilidades:
 *  - Sincronizar dados do SAP OData e persistir no SQLite
 *  - Fornecer dados do SQLite à UI via Flow reativo
 *  - Converter DTOs → Entities → Modelos de domínio (mappers ao final do arquivo)
 *  - Gerenciar credenciais SAP no EncryptedSharedPreferences
 *  - Gravar log de cada sincronização na tabela sync_log
 *
 * Injetado como Singleton pelo Hilt — um repositório para todo o ciclo de vida do app.
 */
@Singleton
class MenuRepository @Inject constructor(
    private val menuAppDao: MenuAppDao,      // acessa a tabela menu_app (itens de menu)
    private val syncLogDao: SyncLogDao,      // acessa a tabela sync_log (histórico de sincronizações)
    private val aplicativoDao: AplicativoDao, // acessa a tabela aplicativos (apps externos do menu)
    private val odataService: ODataService,  // interface Retrofit para chamar o SAP OData
    /** EncryptedSharedPreferences — provido pelo NetworkModule com AES256-GCM */
    private val securePrefs: SharedPreferences
) {

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: sincronizarDoServidor
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Sincroniza os dados do menu com o servidor SAP OData e persiste no SQLite.
     *
     * Fluxo:
     *  1. Monta o header Basic Auth com as credenciais fornecidas
     *  2. Chama o endpoint OData (o SAP já filtra pelas permissões do usuário)
     *  3. Deleta todos os dados atuais do menu_app para garantir dados frescos
     *  4. Converte os DTOs para entidades Room e insere no banco
     *  5. Extrai package IDs dos apps externos (Type=2) e atualiza a tabela aplicativos
     *  6. Grava um log de sucesso com a quantidade de registros atualizados
     *
     * Em caso de exceção em qualquer etapa: grava log de erro e retorna [Result.failure].
     * Deve ser chamado em Dispatchers.IO — nunca na Main thread.
     *
     * @param username Usuário SAP para Basic Auth (ex.: "ANDROID_API")
     * @param password Senha SAP — nunca armazenada em texto claro
     * @return [Result.success] com a quantidade de registros inseridos, ou [Result.failure] com a exceção
     */
    suspend fun sincronizarDoServidor(username: String, password: String): Result<Int> {
        return try {
            // Monta "Basic <base64>" no formato exigido pelo SAP
            val authHeader = montarBasicAuth(username, password)

            // Chama o OData — o SAP filtra automaticamente pelos menus do usuário autenticado
            val resposta = odataService.getMenuApp(authorization = authHeader)
            val dtos = resposta.d.results // lista de MenuAppDto recebida do SAP

            // Converte DTOs para entidades Room (inclui conversão Sequence: String → Int)
            val entidades = dtos.map { it.toEntity() }

            // Substitui todos os dados do banco local pelos dados frescos do SAP
            menuAppDao.deletarTodos()       // remove dados antigos (inclusive itens removidos no SAP)
            menuAppDao.inserirTodos(entidades) // insere dados novos

            // ─── ATUALIZAÇÃO DA TABELA APLICATIVOS ────────────────────────────
            // Extrai package IDs únicos dos itens Type="2" (ações que lançam apps externos).
            // Itens Type="1" são submenus — o campo Componente não é um package ID instalável.
            val componentes = dtos
                .filter { it.type == "2" && it.componente.isNotBlank() } // apenas Type=2 com package
                .map { it.componente }       // extrai apenas o campo Componente
                .distinct()                  // remove duplicatas (mesmo app em múltiplos itens)
                .map { AplicativoEntity(componente = it) } // converte para entidade Room

            aplicativoDao.deletarTodos()            // limpa registros obsoletos
            aplicativoDao.inserirTodos(componentes) // insere lista atualizada

            // Grava log de sucesso com timestamp e quantidade de registros
            gravarLog(status = "SUCCESS", registros = entidades.size, erro = null)

            Result.success(entidades.size) // retorna quantidade de itens inseridos
        } catch (e: Exception) {
            // Grava log de falha com a mensagem da exceção para diagnóstico
            gravarLog(status = "ERROR", registros = 0, erro = e.message ?: "Erro desconhecido")
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: getItensPorMenu
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Retorna um Flow com os itens de um grupo de menu do banco local, ordenados por sequence.
     *
     * O Flow emite uma nova lista automaticamente sempre que os dados mudarem no banco
     * (ex.: após sincronização em background pelo WorkManager).
     * Os dados são convertidos de entidades Room para modelos de domínio antes de emitir.
     *
     * @param mmenu Código do grupo de menu (ex.: "MAIN", "INB00", "GR00")
     * @return Flow<List<MenuApp>> — stream reativo de itens do menu
     */
    fun getItensPorMenu(mmenu: String): Flow<List<MenuApp>> {
        return menuAppDao.getItensPorMenu(mmenu).map { entidades ->
            // map transforma cada List<MenuAppEntity> em List<MenuApp> sem anotações de framework
            entidades.map { it.toDomain() }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: isBancoVazio
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verifica se o banco local nunca foi sincronizado (tabela menu_app vazia).
     *
     * Usado pela [MainActivity] para decidir a rota inicial:
     *  - true  → banco vazio → [Rota.Login] (rede obrigatória para o primeiro sync)
     *  - false → há dados   → pode entrar no menu offline se não houver rede
     *
     * @return true se não há nenhum item de menu no banco local
     */
    suspend fun isBancoVazio(): Boolean = menuAppDao.contarItens() == 0

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: getAplicativos
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Retorna os package IDs de todos os apps externos cadastrados na tabela aplicativos.
     *
     * Usado pelo [LoginViewModel] para obter a lista de apps a verificar
     * (instalação/atualização) após o login bem-sucedido.
     *
     * @return Lista de package IDs (ex.: ["com.entrada.fornecimento", "com.entrada.transporte"])
     */
    suspend fun getAplicativos(): List<String> =
        aplicativoDao.getAll().map { it.componente } // extrai apenas o campo componente de cada entidade

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: salvarCredenciais
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Persiste as credenciais SAP de forma segura no [EncryptedSharedPreferences].
     *
     * A senha é armazenada criptografada com AES256-GCM — nunca em texto claro.
     * Chamado após login bem-sucedido para que o [SyncWorker] possa
     * autenticar nas sincronizações periódicas em background.
     *
     * @param username Usuário SAP (ex.: "ANDROID_API")
     * @param password Senha SAP — criptografada antes de persistir
     */
    fun salvarCredenciais(username: String, password: String) {
        securePrefs.edit()
            .putString(PREF_USERNAME, username) // grava username criptografado
            .putString(PREF_PASSWORD, password) // grava password criptografado (nunca texto claro)
            .apply()                            // apply() é assíncrono — sem bloquear a thread
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: getCredenciais
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Recupera as credenciais SAP salvas anteriormente.
     *
     * Usada pelo [SyncWorker] para autenticar nas sincronizações em background.
     *
     * @return Par (username, password) se credenciais existirem, null se nunca houve login
     */
    fun getCredenciais(): Pair<String, String>? {
        val username = securePrefs.getString(PREF_USERNAME, null) // null se não existe
        val password = securePrefs.getString(PREF_PASSWORD, null) // null se não existe
        // Retorna par apenas se ambas existem — se qualquer uma for null, retorna null
        return if (username != null && password != null) Pair(username, password) else null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÕES PRIVADAS AUXILIARES
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Monta o cabeçalho de autenticação Basic Auth no formato exigido pelo SAP.
     * Formato: "Basic " + Base64("username:password")
     * O SAP valida este header em cada requisição OData.
     *
     * @param username Usuário SAP
     * @param password Senha SAP
     * @return String no formato "Basic <base64>" pronta para o header Authorization
     */
    private fun montarBasicAuth(username: String, password: String): String {
        val credencial = "$username:$password" // formato padrão Basic Auth (RFC 7617)
        // Base64.NO_WRAP garante que a string não tenha quebras de linha (SAP rejeita com \n)
        return "Basic " + Base64.encodeToString(credencial.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Grava um registro de log na tabela sync_log.
     *
     * Chamada após cada tentativa de sincronização — tanto em caso de sucesso
     * quanto em caso de falha — para rastreamento histórico.
     *
     * Nota: usa SimpleDateFormat em vez de LocalDateTime porque LocalDateTime
     * requer API 26 (minSdk do app é 24).
     *
     * @param status   "SUCCESS" ou "ERROR"
     * @param registros Quantidade de registros atualizados (0 em caso de erro)
     * @param erro      Mensagem de erro (null em caso de sucesso)
     */
    private suspend fun gravarLog(status: String, registros: Int, erro: String?) {
        // Formata timestamp no padrão ISO 8601 (ex.: "2026-05-07T14:32:00")
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        syncLogDao.inserir(
            SyncLogEntity(
                timestamp            = timestamp,
                status               = status,
                registrosAtualizados = registros,
                mensagemErro         = erro
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS DE MAPEAMENTO (privadas ao arquivo)
// Convertem entre as camadas: DTO (API) → Entity (Room) → Domain (UI)
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Converte um [MenuAppDto] (recebido do SAP OData) para uma [MenuAppEntity] (armazenada no Room).
 *
 * A conversão crítica é [sequence]: String no SAP → Int no banco local.
 * [toIntOrNull] com fallback 0 evita crash em valores inválidos vindos do SAP.
 */
private fun MenuAppDto.toEntity() = MenuAppEntity(
    lgnum      = lgnum,
    mmenu      = mmenu,
    sequence   = sequence.toIntOrNull() ?: 0, // converte "1","4","10" para Int; fallback 0 se inválido
    componente = componente,
    type       = type,
    transacao  = transacao,
    text       = text,
    sText      = sText
)

/**
 * Converte uma [MenuAppEntity] (Room) para um [MenuApp] (modelo de domínio da UI).
 *
 * Mantém a UI completamente desacoplada do Room:
 * a camada de UI nunca importa classes do pacote data.local.
 */
private fun MenuAppEntity.toDomain() = MenuApp(
    lgnum      = lgnum,
    mmenu      = mmenu,
    sequence   = sequence,
    componente = componente,
    type       = type,
    transacao  = transacao,
    text       = text,
    sText      = sText
)
