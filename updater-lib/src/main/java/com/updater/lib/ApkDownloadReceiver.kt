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

class ApkDownloadReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ApkDownloadReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.updater.lib.DOWNLOAD_APK" -> {
                val apkUrl = intent.getStringExtra("apk_url") ?: return
                val versionName = intent.getStringExtra("version_name") ?: "update"
                downloadAndInstall(context, apkUrl, versionName)
            }

            "com.updater.lib.DISMISS_UPDATE" -> {
                val versionCode = intent.getLongExtra("version_code", -1)
                if (versionCode > 0) {
                    context.getSharedPreferences(AppUpdateChecker.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(AppUpdateChecker.KEY_DISMISSED_VERSION, versionCode)
                        .apply()
                    Log.d(TAG, "Versão $versionCode descartada pelo usuário")
                }
            }
        }
    }

    private fun downloadAndInstall(context: Context, apkUrl: String, versionName: String) {
        val appName = getAppName(context)
        val fileName = "${appName.replace(" ", "_")}_v${versionName}.apk"

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Atualizando $appName")
            setDescription("Baixando versão $versionName...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")

            try {
                AppUpdateChecker.getConfig().githubToken?.let { token ->
                    addRequestHeader("Authorization", "Bearer $token")
                    addRequestHeader("Accept", "application/octet-stream")
                }
            } catch (_: Exception) {}
        }

        val downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "Baixando atualização...", Toast.LENGTH_SHORT).show()

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return

                ctx.unregisterReceiver(this)
                installApk(ctx, fileName)
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, filter)
        }
    }

    private fun installApk(context: Context, fileName: String) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (!file.exists()) {
            Toast.makeText(context, "Erro: arquivo não encontrado", Toast.LENGTH_LONG).show()
            return
        }

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.updater.provider", file)
        } else {
            Uri.fromFile(file)
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao instalar", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAppName(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) { "App" }
    }
}
