package com.updater.lib

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "UpdateCheckWorker"
    }

    override fun doWork(): Result {
        Log.d(TAG, "Verificando atualizações...")

        return try {
            val prefs = applicationContext.getSharedPreferences(
                AppUpdateChecker.PREFS_NAME, Context.MODE_PRIVATE
            )

            // ─── 1. Verifica o próprio MenuAutomatico ─────────────────────────
            val updateInfo = AppUpdateChecker.checkForUpdate()
            if (updateInfo != null) {
                val dismissedVersion = prefs.getLong(AppUpdateChecker.KEY_DISMISSED_VERSION, -1)
                if (updateInfo.versionCode != dismissedVersion) {
                    Log.d(TAG, "Nova versão do MenuAutomatico: ${updateInfo.versionName}")
                    UpdateNotifier.showUpdateNotification(applicationContext, updateInfo)
                } else {
                    Log.d(TAG, "Versão ${updateInfo.versionName} já foi descartada pelo usuário")
                }
            } else {
                Log.d(TAG, "MenuAutomatico está atualizado")
            }

            // ─── 2. Verifica apps externos registrados ────────────────────────
            // Cada app externo usa um notificationId derivado do packageId para
            // que múltiplas notificações coexistam sem se sobrepor.
            val externalUpdates = AppUpdateChecker.checkAllExternalUpdates()
            externalUpdates.forEach { (nome, packageId, info) ->
                // ID estável derivado do packageId — mesmo app sempre usa o mesmo ID
                val notifId = 9100 + (packageId.hashCode().and(0x7fffffff) % 900)
                Log.d(TAG, "Atualização disponível para $nome: ${info.versionName}")
                UpdateNotifier.showUpdateNotification(
                    context        = applicationContext,
                    updateInfo     = info,
                    displayName    = nome,
                    notificationId = notifId
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erro na verificação", e)
            Result.retry()
        }
    }
}
