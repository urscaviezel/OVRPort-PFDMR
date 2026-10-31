package moe.crx.overport.app

import moe.crx.overport.patches.IconResizer
import moe.crx.overport.utils.ImageIOIconResizer
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat

/** Skia decodes WebP; the APK always receives an actual PNG. */
object SkiaIconResizer : IconResizer {
    override fun resize(bytes: ByteArray, width: Int, height: Int): ByteArray {
        val png = Image.makeFromEncoded(bytes).use { image ->
            image.encodeToData(EncodedImageFormat.PNG)?.use { it.bytes }
                ?: error("Could not convert the app icon to PNG")
        }
        return ImageIOIconResizer.resize(png, width, height)
    }
}
