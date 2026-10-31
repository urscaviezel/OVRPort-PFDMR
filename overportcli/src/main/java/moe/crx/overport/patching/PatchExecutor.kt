package moe.crx.overport.patching

import com.reandroid.apk.ApkModule
import com.reandroid.apk.ApkModuleJsonDecoder
import com.reandroid.arsc.chunk.PackageBlock
import com.reandroid.json.JSONObject
import kotlinx.serialization.json.*
import moe.crx.overport.config.OverportConfig
import java.io.File

class PatchExecutor(private val librariesDir: File, private val workingDir: File, private val apkModule: ApkModule) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        // Apply only fields actually changed by a patch; retain runtime extensions.
        fun mergeConfigChanges(original: JsonElement, before: JsonElement, after: JsonElement): JsonElement {
            if (before == after) return original
            if (before is JsonObject && after is JsonObject && original is JsonObject) {
                val result = original.toMutableMap()
                for (key in before.keys + after.keys) {
                    if (before[key] == after[key]) continue
                    val next = after[key]
                    if (next == null) result.remove(key)
                    else result[key] = mergeConfigChanges(original[key] ?: JsonNull, before[key] ?: JsonNull, next)
                }
                return JsonObject(result)
            }
            if (before is JsonArray && after is JsonArray && original is JsonArray) {
                // Database entries and assets have stable identities, independent of order.
                val identity = listOf("app_id", "asset_id").firstOrNull { key ->
                    (before + after).isNotEmpty() && (before + after).all { it is JsonObject && key in it }
                }
                if (identity != null) {
                    fun id(value: JsonElement) = (value as? JsonObject)?.get(identity)
                    return JsonArray(after.map { next ->
                        val previous = before.firstOrNull { id(it) == id(next) }
                        val raw = original.firstOrNull { id(it) == id(next) }
                        if (previous != null && raw != null) mergeConfigChanges(raw, previous, next) else next
                    })
                }
            }
            return after
        }
    }

    fun getLibraries(): File {
        return librariesDir
    }

    fun applicationPackage(): String {
        return apkModule.androidManifest.packageName
    }

    fun selectWorkspace(block: FileTransformer.() -> Unit) {
        workingDir.run {
            if (exists()) {
                FileTransformer(this).block()
            }
        }
    }

    fun selectFile(fileName: String, block: FileTransformer.() -> Unit) {
        workingDir.resolve(fileName).run {
            if (exists()) {
                FileTransformer(this).block()
            }
        }
    }

    fun createFile(fileName: String, block: FileTransformer.() -> Unit) {
        workingDir.resolve(fileName).run {
            parentFile?.mkdirs()
            createNewFile()
            FileTransformer(this).block()
        }
    }

    fun selectResources(block: FileTransformer.() -> Unit) {
        selectFile("resources/resources.arsc.json", block)
    }

    fun selectManifest(block: FileTransformer.() -> Unit) {
        selectFile("AndroidManifest.xml.json", block)
    }

    fun selectResourcesJson(block: JSONObject?.() -> Unit) {
        selectResources {
            val json = readJson()
            block(json)
            json?.let { writeJson(it) }
        }
    }

    fun selectManifestJson(block: JSONObject?.() -> Unit) {
        selectManifest {
            val json = readJson()
            block(json)
            json?.let { writeJson(it) }
        }
    }

    fun selectPackageBlock(block: PackageBlock.() -> Unit) {
        apkModule.tableBlock.getPackages(apkModule.androidManifest.packageName).forEach {
            block(it)
        }
        ApkModuleJsonDecoder(apkModule).decodeResourceTable(workingDir)
    }

    fun selectConfig(block: OverportConfig.() -> Unit) {
        createLibrary("liboverport.config.so") {
            val original = readText().trim().takeIf { it.isNotEmpty() }
                ?.let { json.parseToJsonElement(it) as? JsonObject
                    ?: error("OverPort configuration must be a JSON object") }
                ?: JsonObject(emptyMap())
            val config = if (original.isEmpty()) OverportConfig(version = 1)
                else json.decodeFromJsonElement<OverportConfig>(original)
            val before = if (original.isEmpty()) JsonObject(emptyMap()) else json.encodeToJsonElement(config)
            block(config)
            val after = json.encodeToJsonElement(config)
            writeText(mergeConfigChanges(original, before, after).toString())
        }
    }

    fun selectLibrary(fileName: String, block: FileTransformer.() -> Unit) {
        listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64").forEach { arch ->
            val archDirectory = workingDir.resolve("root/lib/$arch")
            if (archDirectory.exists()) {
                archDirectory.resolve(fileName).run {
                    if (exists()) {
                        FileTransformer(this).block()
                    }
                }
            }
        }
    }

    fun createLibrary(fileName: String, block: FileTransformer.() -> Unit) {
        listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64").forEach { arch ->
            val archDirectory = workingDir.resolve("root/lib/$arch")
            if (archDirectory.exists()) {
                archDirectory.resolve(fileName).run {
                    createNewFile()
                    FileTransformer(this).block()
                }
            }
        }
    }

    fun selectSmali(vararg fileNames: String, block: FileTransformer.() -> Unit) {
        workingDir.resolve("smali").listFiles()?.forEach { dir ->
            if (fileNames.isEmpty()) {
                dir.walk().forEach { file ->
                    if (file.isFile && file.name.endsWith(".smali")) {
                        FileTransformer(file).block()
                    }
                }
            } else {
                fileNames.forEach { fileName ->
                    dir.resolve("${fileName}.1.smali").run {
                        if (exists()) {
                            FileTransformer(this).block()
                        }
                    }
                    dir.resolve("${fileName}.smali").run {
                        if (exists()) {
                            FileTransformer(this).block()
                        }
                    }
                }
            }
        }
    }

    fun createSmali(fileName: String, block: FileTransformer.() -> Unit) {
        workingDir.resolve("smali").listFiles()?.forEach { dir ->
            dir.resolve("${fileName}.1.smali").run {
                if (exists()) {
                    FileTransformer(this).block()
                    return
                }
            }
            dir.resolve("${fileName}.smali").run {
                if (exists()) {
                    FileTransformer(this).block()
                    return
                }
            }
        }

        workingDir
            .resolve("smali")
            .listFiles()
            ?.reduceOrNull { left, right ->
                val leftValue = left.name.substringAfter("classes").toIntOrNull() ?: 0
                val rightValue = right.name.substringAfter("classes").toIntOrNull() ?: 0
                if (rightValue > leftValue) right else left
            }
            .let {
                it ?: workingDir.resolve("smali/classes").apply { mkdirs() }
            }
            .resolve("${fileName}.smali")
            .apply {
                parentFile?.mkdirs()
                createNewFile()
            }
            .run {
                if (exists()) {
                    FileTransformer(this).block()
                }
            }
    }
}
