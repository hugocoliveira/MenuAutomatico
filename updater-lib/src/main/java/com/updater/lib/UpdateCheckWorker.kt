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
            val updateInfo = AppUpdateChecker.checkForUpdate()

            if (updateInfo != null) {
                val prefs = applicationContext.getSharedPreferences(
                    AppUpdateChecker.PREFS_NAME, Context.MODE_PRIVATE
                )
                val dismissedVersion = prefs.getLong(AppUpdateChecker.KEY_DISMISSED_VERSION, -1)

                if (updateInfo.versionCode != dismissedVersion) {
                    Log.d(TAG, "Nova versão encontrada: ${updateInfo.versionName}")
                    UpdateNotifier.showUpdateNotification(applicationContext, updateInfo)
                } else {
                    Log.d(TAG, "Versão ${updateInfo.versionName} já foi descartada pelo usuário")
                }
            } else {
                Log.d(TAG, "App está atualizado")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erro na verificação", e)
            Result.retry()
        }
    }
}
