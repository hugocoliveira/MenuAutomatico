package com.updater.lib

import android.content.Context
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
            config.checkIntervalHours, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Log.d(TAG, "Verificação de atualização agendada a cada ${config.checkIntervalHours}h")
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

    fun checkForUpdate(): UpdateInfo? {
        return try {
            val json = fetchVersionJson() ?: return null
            val remoteVersionCode = json.getLong("versionCode")
            val currentVersionCode = getCurrentVersionCode()

            Log.d(TAG, "Versão local: $currentVersionCode | Remota: $remoteVersionCode")

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
            Log.e(TAG, "Erro ao verificar atualização", e)
            null
        }
    }

    private fun fetchVersionJson(): JSONObject? {
        val url = URL(config.versionJsonUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.useCaches = false

            config.githubToken?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP ${connection.responseCode} ao buscar version.json")
                return null
            }

            val responseBody = connection.inputStream.bufferedReader().readText()

            return if (config.githubToken != null) {
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
