package com.updater.lib

data class UpdateConfig(
    val githubOwner: String,
    val githubRepo: String,
    val branch: String = "main",
    val githubToken: String? = null,
    val checkIntervalHours: Long = 6
) {
    val versionJsonUrl: String
        get() = if (githubToken != null) {
            "https://api.github.com/repos/$githubOwner/$githubRepo/contents/version.json?ref=$branch"
        } else {
            "https://raw.githubusercontent.com/$githubOwner/$githubRepo/$branch/version.json"
        }
}
