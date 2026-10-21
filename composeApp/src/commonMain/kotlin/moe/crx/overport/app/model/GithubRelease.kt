package moe.crx.overport.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("html_url")
    var htmlUrl: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val draft: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long,
    val digest: String? = null,
    val state: String = "uploaded",
)
