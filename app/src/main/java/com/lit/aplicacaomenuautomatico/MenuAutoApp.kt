package com.lit.aplicacaomenuautomatico

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.updater.lib.AppUpdateChecker
import com.updater.lib.UpdateConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class principal do app.
 *
 * @HiltAndroidApp ativa a geração de código do Hilt para toda a aplicação.
 *
 * Implementa [Configuration.Provider] para inicializar o WorkManager manualmente
 * com o [HiltWorkerFactory], permitindo que Workers usem @HiltWorker e recebam
 * dependências via injeção. Sem isso, o WorkManager iniciaria antes do Hilt
 * e a injeção nos Workers falharia silenciosamente.
 *
 * Também inicializa o sistema de atualização automática via GitHub (OTA),
 * que verifica periodicamente se há um APK mais novo disponível no repositório.
 */
@HiltAndroidApp
class MenuAutoApp : Application(), Configuration.Provider {

    /**
     * Factory do Hilt para criação de Workers com injeção de dependências.
     * Injetada automaticamente após o Hilt ser inicializado.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Inicia o verificador periódico de atualizações OTA via GitHub Releases.
        // Compara o versionCode local com o versionCode em version.json no repositório.
        AppUpdateChecker.init(
            context = this,
            config = UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "MenuAutomatico",
                branch      = "main_MenuAutomatico",
                githubToken = "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"
            )
        )
    }

    /**
     * Configuração do WorkManager usando o HiltWorkerFactory.
     * Chamado pelo WorkManager ao inicializar (substitui a inicialização automática
     * desabilitada no AndroidManifest.xml).
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
