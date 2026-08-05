package com.updater.lib

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Receiver responsável por baixar e instalar APKs.
 *
 * IMPORTANTE: o Context recebido em onReceive() é um ReceiverRestrictedContext
 * que proíbe registerReceiver() — por isso todas as operações usam applicationContext.
 */
class ApkDownloadReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ApkDownloadReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // applicationContext evita ReceiverCallNotAllowedException ao registrar receivers internos
        val appCtx = context.applicationContext
        when (intent.action) {
            "com.updater.lib.DOWNLOAD_APK" -> {
                val apkUrl = intent.getStringExtra("apk_url") ?: return
                val versionName = intent.getStringExtra("version_name") ?: "update"
                val appName = intent.getStringExtra("app_name")
                downloadAndInstall(appCtx, apkUrl, versionName, appName)
            }
            "com.updater.lib.DISMISS_UPDATE" -> {
                val versionCode = intent.getLongExtra("version_code", -1)
                if (versionCode > 0) {
                    appCtx.getSharedPreferences(AppUpdateChecker.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(AppUpdateChecker.KEY_DISMISSED_VERSION, versionCode)
                        .apply()
                    Log.d(TAG, "Versão $versionCode descartada pelo usuário")
                }
            }
        }
    }

    private fun downloadAndInstall(
        appCtx: Context,
        apkUrl: String,
        versionName: String,
        appNameOverride: String? = null
    ) {
        try {
            val appName = appNameOverride ?: getAppName(appCtx)

            // Remove acentos e caracteres especiais — DownloadManager falha com paths não-ASCII
            val nomeSeguro = java.text.Normalizer
                .normalize(appName, java.text.Normalizer.Form.NFD)
                .replace(Regex("[^\\p{ASCII}]"), "")
                .replace(" ", "_")
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                .ifBlank { "app" }
            val fileName = "${nomeSeguro}_v${versionName}.apk"

            val downloadManager = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: run { Log.e(TAG, "DownloadManager não disponível"); return }

            // Remove arquivo anterior para evitar conflito de nome
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                .takeIf { it.exists() }?.delete()

            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Instalando $appName")
                setDescription("Baixando versão $versionName...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
                // Sem header de autenticação — repos públicos não precisam e o token quebra redirecionamentos CDN
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(appCtx, "Baixando $appName...", Toast.LENGTH_SHORT).show()

            // Registra via applicationContext — ReceiverRestrictedContext proibiria isso
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id != downloadId) return

                    try { ctx.applicationContext.unregisterReceiver(this) }
                    catch (e: Exception) { Log.w(TAG, "unregisterReceiver: ${e.message}") }

                    // Verifica status real do download antes de instalar
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    val sucesso = cursor?.use { c ->
                        c.moveToFirst() &&
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
                    } ?: false

                    if (sucesso) {
                        installApk(ctx.applicationContext, fileName)
                    } else {
                        Log.e(TAG, "Download falhou — $fileName")
                        Toast.makeText(ctx.applicationContext, "Falha ao baixar $appName", Toast.LENGTH_LONG).show()
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appCtx.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
            } else {
                appCtx.registerReceiver(onComplete, filter)
            }

        } catch (e: Exception) {
            Log.e(TAG, "downloadAndInstall falhou: ${e.message}", e)
            Toast.makeText(appCtx, "Erro ao iniciar download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installApk(appCtx: Context, fileName: String) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (!file.exists()) {
                Toast.makeText(appCtx, "Arquivo não encontrado após download", Toast.LENGTH_LONG).show()
                Log.e(TAG, "APK ausente: ${file.absolutePath}")
                return
            }

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(appCtx, "${appCtx.packageName}.updater.provider", file)
            } else {
                @Suppress("DEPRECATION")
                Uri.fromFile(file)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            appCtx.startActivity(installIntent)

        } catch (e: Exception) {
            Log.e(TAG, "installApk falhou: ${e.message}", e)
            Toast.makeText(appCtx, "Erro ao instalar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAppName(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) { "App" }
    }
}
