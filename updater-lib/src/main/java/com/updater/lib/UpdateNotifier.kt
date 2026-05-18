package com.updater.lib

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object UpdateNotifier {

    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 9001

    fun showUpdateNotification(context: Context, updateInfo: UpdateInfo) {
        createNotificationChannel(context)

        val downloadIntent = Intent(context, ApkDownloadReceiver::class.java).apply {
            action = "com.updater.lib.DOWNLOAD_APK"
            putExtra("apk_url", updateInfo.apkUrl)
            putExtra("version_name", updateInfo.versionName)
            putExtra("version_code", updateInfo.versionCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            updateInfo.versionCode.toInt(),
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ApkDownloadReceiver::class.java).apply {
            action = "com.updater.lib.DISMISS_UPDATE"
            putExtra("version_code", updateInfo.versionCode)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            updateInfo.versionCode.toInt() + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appName = getAppName(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("$appName — atualização disponível")
            .setContentText("Versão ${updateInfo.versionName} pronta para instalar")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Versão ${updateInfo.versionName} disponível.\n${updateInfo.releaseNotes}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Atualizar",
                pendingIntent
            )
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Atualizações",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa quando há uma nova versão do app"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getAppName(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "App"
        }
    }
}
