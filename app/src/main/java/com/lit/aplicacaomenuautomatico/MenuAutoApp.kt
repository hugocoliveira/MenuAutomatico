package com.lit.aplicacaomenuautomatico

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / AndroidX
// ─────────────────────────────────────────────────────────────────────────────
import android.app.Application           // classe base do processo Android — ponto de entrada do app
import androidx.hilt.work.HiltWorkerFactory // factory do Hilt para criação de Workers com injeção
import androidx.work.Configuration       // configuração do WorkManager (define qual factory usar)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — recursos do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.BuildConfig // acessa GITHUB_TOKEN gerado pelo Gradle build

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — biblioteca de atualização OTA
// ─────────────────────────────────────────────────────────────────────────────
import com.updater.lib.AppUpdateChecker // inicializa o sistema de verificação de atualizações via GitHub
import com.updater.lib.UpdateConfig     // configuração do repositório GitHub para o OTA do próprio app

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt / injeção de dependência
// ─────────────────────────────────────────────────────────────────────────────
import dagger.hilt.android.HiltAndroidApp // ativa a geração de código Hilt para toda a aplicação
import javax.inject.Inject                // anotação padrão JSR-330 para injeção de campo

// ═════════════════════════════════════════════════════════════════════════════
// APPLICATION CLASS: MenuAutoApp
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Application class principal do app.
 * Criada pelo sistema Android antes de qualquer Activity, Service ou Broadcast.
 *
 * Responsabilidades:
 *  1. Ativar o Hilt para injeção de dependências em todo o app (@HiltAndroidApp)
 *  2. Inicializar o WorkManager com o [HiltWorkerFactory] para que Workers
 *     possam receber dependências via injeção
 *  3. Inicializar o sistema de atualização automática OTA via GitHub Releases
 *
 * Por que implementar [Configuration.Provider]:
 * O WorkManager inicializa automaticamente via ContentProvider antes do Hilt,
 * o que impediria a injeção nos Workers. Ao implementar [Configuration.Provider]
 * e desabilitar a inicialização automática no AndroidManifest.xml, garantimos
 * que o WorkManager use o [HiltWorkerFactory] e a injeção funcione corretamente.
 */
@HiltAndroidApp // gera o componente Hilt raiz (AppComponent) e habilita injeção em toda a hierarquia
class MenuAutoApp : Application(), Configuration.Provider {

    // ─── DEPENDÊNCIA INJETADA ─────────────────────────────────────────────────

    /**
     * Factory do Hilt para criação de Workers com injeção de dependências.
     * Injetada automaticamente após a inicialização do Hilt no onCreate().
     * Passada ao WorkManager via [workManagerConfiguration].
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // ─── CICLO DE VIDA ────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate() // inicializa o Hilt (gerado por @HiltAndroidApp)

        // ─── INICIALIZAÇÃO DO OTA ─────────────────────────────────────────────
        // Inicia o verificador periódico de atualizações do próprio app via GitHub Releases.
        // Compara o versionCode instalado com o versionCode em version.json no repositório.
        // Token opcional: sem ele, requests são anônimos (limite de 60/hora no GitHub API).
        AppUpdateChecker.init(
            context = this,
            config  = UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "MenuAutomatico",
                branch      = "main_MenuAutomatico",
                // takeIf evita passar string vazia — sem token, usa request anônimo
                githubToken = BuildConfig.GITHUB_TOKEN.takeIf { it.isNotEmpty() },

                // ─────────────────────────────────────────────────────────────
                // FREQUÊNCIA DE VERIFICAÇÃO DE ATUALIZAÇÃO
                // Altere o valor abaixo para mudar o intervalo (em horas):
                //   1L  → verifica a cada 1 hora
                //   2L  → verifica a cada 2 horas
                //   6L  → verifica a cada 6 horas (padrão atual)
                //   24L → verifica uma vez por dia
                // Atenção: o Android impõe mínimo de 15 minutos para Workers
                // periódicos, mas recomenda-se no mínimo 1 hora para não
                // impactar bateria e consumo de dados.
                // ─────────────────────────────────────────────────────────────
                checkIntervalHours = 6L
            )
        )
    }

    // ─── CONFIGURAÇÃO DO WORKMANAGER ──────────────────────────────────────────

    /**
     * Configuração do WorkManager usando o [HiltWorkerFactory].
     * Chamado pelo WorkManager ao inicializar — substitui a factory padrão pela
     * factory do Hilt, que sabe como injetar dependências nos Workers (@HiltWorker).
     *
     * A inicialização automática do WorkManager deve estar desabilitada no
     * AndroidManifest.xml para que esta configuração seja respeitada.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory) // usa a factory do Hilt para criar Workers com injeção
            .build()
}
