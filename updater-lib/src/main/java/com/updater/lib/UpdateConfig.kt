package com.updater.lib

data class UpdateConfig(
    val githubOwner: String,
    val githubRepo: String,
    val branch: String = "main",
    val githubToken: String? = null,
    // intervalo em minutos — mínimo 15 (limite imposto pelo Android WorkManager)
    val checkIntervalMinutes: Long = 15,
    /** Package ID do app a verificar. Null = usa o packageName do próprio processo. */
    val packageId: String? = null
) {
    val versionJsonUrl: String
        get() = if (githubToken != null) {
            "https://api.github.com/repos/$githubOwner/$githubRepo/contents/version.json?ref=$branch"
        } else {
            "https://raw.githubusercontent.com/$githubOwner/$githubRepo/$branch/version.json"
        }
}
