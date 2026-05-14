package com.lit.aplicacaomenuautomatico.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker responsável pela sincronização periódica em background dos dados do SAP.
 * Executado pelo WorkManager a cada 60 minutos enquanto houver conexão de rede,
 * sem qualquer indicação visual ao usuário.
 *
 * @HiltWorker habilita a injeção de dependências neste Worker.
 * @AssistedInject é necessário porque WorkerParameters é fornecido pelo WorkManager
 * (não pelo Hilt) e precisa ser passado manualmente — os demais parâmetros são injetados.
 *
 * Para funcionar, o WorkManager deve ser inicializado com HiltWorkerFactory em [MenuAutoApp].
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    /** Repositório injetado pelo Hilt — orquestra a sincronização com o SAP */
    private val menuRepository: MenuRepository
) : CoroutineWorker(context, workerParams) {

    /**
     * Lógica principal do Worker — executada em Dispatchers.IO automaticamente
     * pelo CoroutineWorker.
     *
     * Fluxo:
     *  1. Verifica se há rede disponível — sem rede, agenda retry automático
     *  2. Recupera as credenciais salvas no EncryptedSharedPreferences
     *  3. Sincroniza os dados do SAP via repositório
     *  4. Retorna success ou retry baseado no resultado
     *  O log é gravado pelo próprio repositório em ambos os casos.
     */
    override suspend fun doWork(): Result {
        // Sem rede, não há como sincronizar — WorkManager tentará novamente depois
        if (!verificarConectividade()) {
            return Result.retry()
        }

        // Se não há credenciais salvas, nunca houve login — não é possível sincronizar
        val credenciais = menuRepository.getCredenciais()
            ?: return Result.failure()

        val (username, password) = credenciais

        // Sincroniza com o SAP — o repositório grava o log independente do resultado
        return menuRepository.sincronizarDoServidor(username, password).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    /**
     * Verifica se o dispositivo tem conexão de rede ativa com capacidade de internet.
     * Usa [NetworkCapabilities] (API 23+) — compatível com o minSdk 24 do app.
     */
    private fun verificarConectividade(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
