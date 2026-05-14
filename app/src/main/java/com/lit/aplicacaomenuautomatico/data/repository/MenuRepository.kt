package com.lit.aplicacaomenuautomatico.data.repository

import android.content.SharedPreferences
import android.util.Base64
import com.lit.aplicacaomenuautomatico.data.local.MenuAppDao
import com.lit.aplicacaomenuautomatico.data.local.MenuAppEntity
import com.lit.aplicacaomenuautomatico.data.local.SyncLogDao
import com.lit.aplicacaomenuautomatico.data.local.SyncLogEntity
import com.lit.aplicacaomenuautomatico.data.remote.MenuAppDto
import com.lit.aplicacaomenuautomatico.data.remote.ODataService
import com.lit.aplicacaomenuautomatico.domain.model.MenuApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Chaves usadas no EncryptedSharedPreferences */
private const val PREF_USERNAME = "username"
private const val PREF_PASSWORD = "password"

/**
 * Repositório central de dados do app.
 * Orquestra as operações entre o banco local (Room) e o servidor remoto (SAP OData).
 * É a única fonte de verdade para os dados de menu — a UI nunca acessa DAOs diretamente.
 *
 * Injetado como Singleton pelo Hilt para que DAOs e SharedPreferences sejam reutilizados.
 */
@Singleton
class MenuRepository @Inject constructor(
    private val menuAppDao: MenuAppDao,
    private val syncLogDao: SyncLogDao,
    private val odataService: ODataService,
    /** EncryptedSharedPreferences — injetado pelo NetworkModule para armazenamento seguro */
    private val securePrefs: SharedPreferences
) {

    /**
     * Sincroniza os dados do menu com o servidor SAP.
     * Fluxo:
     *  1. Chama o endpoint OData com Basic Auth
     *  2. Deleta todos os registros atuais do banco local
     *  3. Insere os novos dados convertidos
     *  4. Grava um registro em sync_log com o resultado
     *
     * Deve ser chamado em Dispatchers.IO (nunca na Main thread).
     *
     * @param username Usuário SAP para autenticação Basic Auth
     * @param password Senha SAP (nunca armazenada em texto claro)
     * @return [Result.success] com a quantidade de registros inseridos, ou [Result.failure] com a exceção
     */
    suspend fun sincronizarDoServidor(username: String, password: String): Result<Int> {
        return try {
            // Monta o header de autenticação Basic Auth no formato esperado pelo SAP
            val authHeader = montarBasicAuth(username, password)

            // Chama o OData — o SAP já filtra pelos menus que o usuário tem permissão
            val resposta = odataService.getMenuApp(authorization = authHeader)
            val dtos = resposta.d.results

            // Converte DTOs para entidades Room (aqui ocorre a conversão Sequence: String → Int)
            val entidades = dtos.map { it.toEntity() }

            // Substitui todos os dados do banco local pelos dados frescos do SAP
            menuAppDao.deletarTodos()
            menuAppDao.inserirTodos(entidades)

            // Grava log de sucesso com timestamp atual
            gravarLog(status = "SUCCESS", registros = entidades.size, erro = null)

            Result.success(entidades.size)
        } catch (e: Exception) {
            gravarLog(status = "ERROR", registros = 0, erro = e.message ?: "Erro desconhecido")
            Result.failure(e)
        }
    }

    /**
     * Retorna um Flow com os itens de um grupo de menu específico, ordenados por sequence.
     * A UI observa esse Flow e se atualiza automaticamente após cada sincronização.
     *
     * @param mmenu Código do grupo de menu (ex.: "MAIN", "INB00", "GR00")
     */
    fun getItensPorMenu(mmenu: String): Flow<List<MenuApp>> {
        return menuAppDao.getItensPorMenu(mmenu).map { entidades ->
            // Mapeia entidades Room para modelos de domínio (sem dependências de framework na UI)
            entidades.map { it.toDomain() }
        }
    }

    /**
     * Verifica se o banco local está vazio (nunca foi sincronizado).
     * Usado para decidir se o primeiro acesso exige rede obrigatoriamente.
     *
     * @return true se não há nenhum item de menu no banco local
     */
    suspend fun isBancoVazio(): Boolean = menuAppDao.contarItens() == 0

    /**
     * Persiste as credenciais SAP de forma segura no EncryptedSharedPreferences.
     * A senha é armazenada criptografada — nunca em texto claro.
     * Chamado após login bem-sucedido para uso pelo WorkManager nas sincronizações em background.
     *
     * @param username Usuário SAP
     * @param password Senha SAP
     */
    fun salvarCredenciais(username: String, password: String) {
        securePrefs.edit()
            .putString(PREF_USERNAME, username)
            .putString(PREF_PASSWORD, password)
            .apply()
    }

    /**
     * Recupera as credenciais SAP salvas anteriormente.
     *
     * @return Par (username, password) se existirem, null se nunca houve login
     */
    fun getCredenciais(): Pair<String, String>? {
        val username = securePrefs.getString(PREF_USERNAME, null)
        val password = securePrefs.getString(PREF_PASSWORD, null)
        return if (username != null && password != null) Pair(username, password) else null
    }

    // ─── Funções auxiliares privadas ──────────────────────────────────────────

    /**
     * Monta o header de autenticação Basic Auth no formato "Basic <base64(user:pass)>".
     * O SAP espera exatamente esse formato no header Authorization.
     */
    private fun montarBasicAuth(username: String, password: String): String {
        val credencial = "$username:$password"
        return "Basic " + Base64.encodeToString(credencial.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Grava um registro de log de sincronização na tabela sync_log.
     *
     * @param status "SUCCESS" ou "ERROR"
     * @param registros Quantidade de registros inseridos (0 em caso de erro)
     * @param erro Mensagem de erro (null em caso de sucesso)
     */
    private suspend fun gravarLog(status: String, registros: Int, erro: String?) {
        // SimpleDateFormat usado no lugar de LocalDateTime (requer API 26, minSdk é 24)
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        syncLogDao.inserir(
            SyncLogEntity(
                timestamp = timestamp,
                status = status,
                registrosAtualizados = registros,
                mensagemErro = erro
            )
        )
    }
}

// ─── Extension functions de mapeamento ───────────────────────────────────────

/**
 * Converte um DTO da API OData para uma entidade Room.
 * A conversão de [sequence] de String para Int ocorre aqui com fallback 0
 * para evitar crash em valores inválidos vindos do SAP.
 */
private fun MenuAppDto.toEntity() = MenuAppEntity(
    lgnum = lgnum,
    mmenu = mmenu,
    sequence = sequence.toIntOrNull() ?: 0,
    componente = componente,
    type = type,
    transacao = transacao,
    text = text,
    sText = sText
)

/**
 * Converte uma entidade Room para o modelo de domínio usado pela UI.
 * Mantém a UI desacoplada do Room — a camada de UI nunca importa classes do Room.
 */
private fun MenuAppEntity.toDomain() = MenuApp(
    lgnum = lgnum,
    mmenu = mmenu,
    sequence = sequence,
    componente = componente,
    type = type,
    transacao = transacao,
    text = text,
    sText = sText
)
