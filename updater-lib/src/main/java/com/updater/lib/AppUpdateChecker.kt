package com.updater.lib

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.work.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    private const val WORK_NAME = "update_check"
    internal const val PREFS_NAME = "updater_prefs"
    internal const val KEY_DISMISSED_VERSION = "dismissed_version"

    private lateinit var config: UpdateConfig
    private lateinit var appContext: Context

    fun init(context: Context, config: UpdateConfig) {
        this.appContext = context.applicationContext
        this.config = config

        val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            config.checkIntervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        // UPDATE garante que mudanças no intervalo (checkIntervalHours) sejam aplicadas imediatamente.
        // KEEP ignoraria qualquer alteração de configuração enquanto o job já estivesse registrado.
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Verificação de atualização agendada a cada ${config.checkIntervalMinutes} minutos")
    }

    fun getConfig(): UpdateConfig = config
    fun getContext(): Context = appContext

    fun getCurrentVersionCode(): Long {
        val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * Verifica atualização do próprio app usando a config inicializada via [init].
     * Chamado pelo [UpdateCheckWorker] no ciclo periódico e no launch.
     */
    fun checkForUpdate(): UpdateInfo? = checkForUpdate(config, appContext)

    /**
     * Verifica atualização para qualquer app usando uma config explícita.
     * Permite que o MenuAutomatico verifique os 3 apps na tela de login.
     *
     * @param config Configuração do app a verificar (packageId define qual versão local comparar)
     * @param context Context para acessar PackageManager
     */
    fun checkForUpdate(config: UpdateConfig, context: Context): UpdateInfo? {
        return try {
            val json = fetchVersionJsonFor(config) ?: return null
            val remoteVersionCode = json.getLong("versionCode")
            val currentVersionCode = getInstalledVersionCode(config.packageId, context)

            Log.d(TAG, "[${config.githubRepo}] Local: $currentVersionCode | Remoto: $remoteVersionCode")

            if (remoteVersionCode > currentVersionCode) {
                UpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = json.getString("versionName"),
                    apkUrl = json.getString("apkUrl"),
                    releaseNotes = json.optString("releaseNotes", "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar atualização de ${config.githubRepo}", e)
            null
        }
    }

    /**
     * Retorna o versionCode do app instalado.
     * Se [packageId] for null, usa o packageName do processo atual.
     * Se o app não estiver instalado, retorna 0 (força atualização).
     */
    private fun getInstalledVersionCode(packageId: String?, context: Context): Long {
        val pkg = packageId ?: context.packageName
        return try {
            val info = context.packageManager.getPackageInfo(pkg, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
            else @Suppress("DEPRECATION") info.versionCode.toLong()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "App $pkg não instalado — versionCode = 0")
            0L
        }
    }

    private fun fetchVersionJson(): JSONObject? = fetchVersionJsonFor(config)

    private fun fetchVersionJsonFor(cfg: UpdateConfig): JSONObject? {
        val url = URL(cfg.versionJsonUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.useCaches = false

            cfg.githubToken?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP ${connection.responseCode} ao buscar version.json de ${cfg.githubRepo}")
                return null
            }

            val responseBody = connection.inputStream.bufferedReader().readText()

            return if (cfg.githubToken != null) {
                val apiResponse = JSONObject(responseBody)
                val content = apiResponse.getString("content").replace("\n", "")
                val decoded = String(Base64.decode(content, Base64.DEFAULT))
                JSONObject(decoded)
            } else {
                JSONObject(responseBody)
            }
        } finally {
            connection.disconnect()
        }
    }
}

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)
