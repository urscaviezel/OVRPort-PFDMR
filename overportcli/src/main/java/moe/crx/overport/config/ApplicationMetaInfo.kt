package moe.crx.overport.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moe.crx.overport.utils.HttpUtil

@Serializable
data class ApplicationMetaImage(
    @SerialName("file_name")
    var fileName: String,
    @SerialName("id")
    var id: String,
    @SerialName("image_type")
    var imageType: String,
    @SerialName("uri")
    var uri: String,
)

@Serializable
data class ApplicationMetaInfo(
    @SerialName("metaID")
    var metaId: String,
    @SerialName("displayName")
    var displayName: String,
    @SerialName("images")
    var images: List<ApplicationMetaImage>,
)

private val json = Json { ignoreUnknownKeys = true }

fun getApplicationInfo(pkg: String): ApplicationMetaInfo? {
    LocalAppIcons.read(pkg)?.let { bytes ->
        // Folder images have no title metadata: preserve the APK's application label.
        return ApplicationMetaInfo("local", "", listOf(ApplicationMetaImage(
            "$pkg.local", pkg, "APP_IMG_ICON", java.util.Base64.getEncoder().encodeToString(bytes)
        )))
    }
    RemoteAppIcons.cached(pkg)?.let { bytes ->
        return ApplicationMetaInfo("icon-cache", "", listOf(ApplicationMetaImage(
            "$pkg.cached", pkg, "APP_IMG_ICON", java.util.Base64.getEncoder().encodeToString(bytes)
        )))
    }
    val url = "https://ovrp.crx.moe/images/by_package?package=$pkg"
    val online = runCatching {
        val string = String(HttpUtil.download(url, timeout = 20000))
        json.decodeFromString<ApplicationMetaInfo>(string)
    }.getOrNull()
    if (online != null && online.images.isNotEmpty()) return online
    return RemoteAppIcons.read(pkg)?.let { bytes ->
        ApplicationMetaInfo("icon-cache", online?.displayName ?: "", listOf(ApplicationMetaImage(
            "$pkg.cached", pkg, "APP_IMG_ICON", java.util.Base64.getEncoder().encodeToString(bytes)
        )))
    } ?: online
}