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
        val githubToken = BuildConfig.GITHUB_TOKEN.takeIf { it.isNotEmpty() }

        AppUpdateChecker.init(
            context = this,
            config  = UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "MenuAutomatico",
                branch      = "main_MenuAutomatico",
                githubToken = githubToken,

                // ─────────────────────────────────────────────────────────────
                // FREQUÊNCIA DE VERIFICAÇÃO DE ATUALIZAÇÃO
                // Altere o valor abaixo para mudar o intervalo (em minutos):
                //   15L → verifica a cada 15 minutos (mínimo permitido pelo Android)
                //   30L → verifica a cada 30 minutos
                //   60L → verifica a cada 1 hora
                //   360L → verifica a cada 6 horas
                // Atenção: o Android impõe mínimo absoluto de 15 minutos para
                // Workers periódicos — valores menores são ignorados pelo sistema.
                // ─────────────────────────────────────────────────────────────
                checkIntervalMinutes = 15L
            )
        )

        // ─── REGISTRO DE APPS EXTERNOS PARA VERIFICAÇÃO EM BACKGROUND ────────
        // O UpdateCheckWorker usa estes registros a cada ciclo periódico.
        // Adicionar/remover um app aqui é suficiente — o worker pega automaticamente.
        val appsExternos = listOf(
            Triple("Entrada Fornecimento", "com.entrada.fornecimento",
                UpdateConfig(githubOwner = "hugocoliveira", githubRepo = "EntradaFornecimento",
                    branch = "main", githubToken = githubToken, packageId = "com.entrada.fornecimento")),
            Triple("Entrada Transporte", "com.entrada.transporte",
                UpdateConfig(githubOwner = "hugocoliveira", githubRepo = "EntradaTransporte",
                    branch = "master", githubToken = githubToken, packageId = "com.entrada.transporte")),
            Triple("Busca Material", "br.com.lit.busca.material",
                UpdateConfig(githubOwner = "hugocoliveira", githubRepo = "BuscaMaterial",
                    branch = "main", githubToken = githubToken, packageId = "br.com.lit.busca.material")),
            Triple("Busca Posicao", "br.com.lit.busca.posicao",
                UpdateConfig(githubOwner = "hugocoliveira", githubRepo = "BuscaPosicao",
                    branch = "main", githubToken = githubToken, packageId = "br.com.lit.busca.posicao")),
            Triple("Busca Por Fila", "br.com.lit.busca.fila",
                UpdateConfig(githubOwner = "hugocoliveira", githubRepo = "BuscaPorFila",
                    branch = "main", githubToken = githubToken, packageId = "br.com.lit.busca.fila")),
            Triple("Busca Por UC", "br.com.lit.busca.uc",
                UpdateConfig(githubOwner = "hugocoliveira", githubRepo = "BuscaPorUC",
                    branch = "main", githubToken = githubToken, packageId = "br.com.lit.busca.uc"))
        )
        appsExternos.forEach { (nome, packageId, config) ->
            AppUpdateChecker.registerExternalApp(nome, packageId, config)
        }
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
