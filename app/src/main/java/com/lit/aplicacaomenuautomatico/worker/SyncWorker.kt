package com.lit.aplicacaomenuautomatico.worker

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / conectividade
// ─────────────────────────────────────────────────────────────────────────────
import android.content.Context             // acesso aos serviços do sistema (ConnectivityManager)
import android.net.ConnectivityManager     // verifica o estado da conexão de rede
import android.net.NetworkCapabilities     // consulta capacidades da rede ativa (internet, Wi-Fi, etc.)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — WorkManager / Hilt
// ─────────────────────────────────────────────────────────────────────────────
import androidx.hilt.work.HiltWorker        // habilita injeção de dependências neste Worker via Hilt
import androidx.work.CoroutineWorker        // base de Worker que executa em corrotina (Dispatchers.IO)
import androidx.work.WorkerParameters       // parâmetros fornecidos pelo WorkManager (não pelo Hilt)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — recursos do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository // orquestra SAP OData + SQLite

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt / AssistedInject
// ─────────────────────────────────────────────────────────────────────────────
import dagger.assisted.Assisted       // marca parâmetros fornecidos externamente (Context e WorkerParams)
import dagger.assisted.AssistedInject // permite injeção mista: Hilt + parâmetros externos

// ═════════════════════════════════════════════════════════════════════════════
// WORKER: SyncWorker
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Worker responsável pela sincronização periódica em background dos dados do SAP.
 *
 * Agendado pelo WorkManager a cada 60 minutos após o login bem-sucedido
 * (configurado em LoginViewModel.agendarSyncPeriodico).
 * Executa silenciosamente — sem notificação ou indicação visual ao usuário.
 * Grava um registro em sync_log após cada execução (sucesso ou falha).
 *
 * Por que @HiltWorker + @AssistedInject:
 *  - [Context] e [WorkerParameters] são fornecidos pelo WorkManager (não pelo Hilt)
 *  - [MenuRepository] é fornecido pelo Hilt via injeção
 *  - @AssistedInject permite misturar os dois tipos de parâmetros no mesmo construtor
 *  - O [HiltWorkerFactory] em [MenuAutoApp] garante que o WorkManager use esta factory
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,         // contexto fornecido pelo WorkManager
    @Assisted workerParams: WorkerParameters,        // parâmetros do work (ID, tentativas, dados de input)
    /** Repositório injetado pelo Hilt — orquestra a sincronização com o SAP e a gravação local */
    private val menuRepository: MenuRepository
) : CoroutineWorker(context, workerParams) {
    // CoroutineWorker executa doWork() automaticamente em Dispatchers.IO — sem bloquear a Main thread

    // ═════════════════════════════════════════════════════════════════════════
    // LÓGICA PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Ponto de entrada do Worker — chamado pelo WorkManager quando as constraints são satisfeitas.
     * Executado automaticamente em Dispatchers.IO pelo CoroutineWorker.
     *
     * Fluxo:
     *  1. Verifica conectividade — sem rede, retorna [Result.retry()] para reagendar
     *  2. Recupera credenciais salvas no EncryptedSharedPreferences
     *  3. Sincroniza dados do SAP via repositório
     *  4. Retorna [Result.success()] ou [Result.retry()] conforme o resultado
     *  O log é gravado pelo próprio repositório em ambos os casos (sync_log).
     *
     * @return [Result.success] = sincronizou OK | [Result.retry] = falha temporária |
     *         [Result.failure] = falha permanente (sem credenciais)
     */
    override suspend fun doWork(): Result {
        // Sem rede: não há como sincronizar — WorkManager reagendará automaticamente
        if (!verificarConectividade()) {
            return Result.retry() // WorkManager tentará novamente quando a constraint de rede for satisfeita
        }

        // Sem credenciais: nunca houve login — não é possível autenticar no SAP
        val credenciais = menuRepository.getCredenciais()
            ?: return Result.failure() // falha permanente — Worker não será reagendado

        // Desestrutura o par (username, password) retornado pelo repositório
        val (username, password) = credenciais

        // Sincroniza com o SAP — o repositório grava o log independente do resultado
        return menuRepository.sincronizarDoServidor(username, password).fold(
            onSuccess = { Result.success() }, // dados atualizados com sucesso no SQLite
            onFailure = { Result.retry() }    // falha de rede ou SAP — WorkManager reagendará
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO AUXILIAR: verificarConectividade
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verifica se o dispositivo tem conexão de rede ativa com capacidade de internet.
     *
     * Usa [NetworkCapabilities] (API 23+) — compatível com o minSdk 24 do app.
     * [NET_CAPABILITY_INTERNET] garante que a rede tem rota para a internet
     * (não apenas conexão Wi-Fi local sem internet).
     *
     * @return true se há rede disponível com acesso à internet, false caso contrário
     */
    private fun verificarConectividade(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // activeNetwork é null quando não há nenhuma rede conectada
        val network = connectivityManager.activeNetwork ?: return false

        // getNetworkCapabilities é null quando a rede não tem capacidades configuradas
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // NET_CAPABILITY_INTERNET = a rede tem rota para a internet (Wi-Fi, dados móveis, VPN)
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
