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

    /**
     * Exibe notificação de atualização disponível para um app.
     *
     * @param context        Contexto da aplicação
     * @param updateInfo     Dados da versão disponível (versionName, apkUrl, etc.)
     * @param displayName    Nome legível do app para o título da notificação.
     *                       Null = usa o nome do app atual (para o próprio MenuAutomatico).
     * @param notificationId ID único da notificação. Usar IDs diferentes por app evita
     *                       que notificações de apps distintos se sobreponham.
     */
    fun showUpdateNotification(
        context: Context,
        updateInfo: UpdateInfo,
        displayName: String? = null,
        notificationId: Int = NOTIFICATION_ID
    ) {
        createNotificationChannel(context)

        val appName = displayName ?: getAppName(context)

        val downloadIntent = Intent(context, ApkDownloadReceiver::class.java).apply {
            action = "com.updater.lib.DOWNLOAD_APK"
            putExtra("apk_url", updateInfo.apkUrl)
            putExtra("version_name", updateInfo.versionName)
            putExtra("version_code", updateInfo.versionCode)
            putExtra("app_name", appName) // usado pelo receiver para nomear o arquivo APK
        }

        // requestCode único por (notificationId + versionCode) para não colidir entre apps
        val reqBase = notificationId * 10000 + (updateInfo.versionCode % 10000).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqBase,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ApkDownloadReceiver::class.java).apply {
            action = "com.updater.lib.DISMISS_UPDATE"
            putExtra("version_code", updateInfo.versionCode)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            reqBase + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
        manager.notify(notificationId, notification)
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
