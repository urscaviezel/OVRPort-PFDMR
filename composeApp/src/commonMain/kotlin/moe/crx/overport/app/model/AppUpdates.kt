package moe.crx.overport.app.model

import kotlinx.serialization.json.*
import moe.crx.overport.pfdmr.UpdateFiles
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.math.BigInteger

object AppUpdates {
    private val json = Json { ignoreUnknownKeys = true }
    private val versionPattern = Regex("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?")
    fun compareVersions(a: String, b: String): Int {
        val x = requireNotNull(versionPattern.matchEntire(a)).groupValues
        val y = requireNotNull(versionPattern.matchEntire(b)).groupValues
        for (i in 1..3) { val c = BigInteger(x[i]).compareTo(BigInteger(y[i])); if (c != 0) return c }
        if (x[4] == y[4]) return 0
        if (x[4].isEmpty()) return 1
        if (y[4].isEmpty()) return -1
        val left=x[4].split('.'); val right=y[4].split('.')
        for (i in 0 until minOf(left.size,right.size)) {
            val l=left[i]; val r=right[i]; val ln=l.all(Char::isDigit); val rn=r.all(Char::isDigit)
            val c=if(ln && rn) BigInteger(l).compareTo(BigInteger(r)) else if(ln!=rn) { if(ln) -1 else 1 } else l.compareTo(r)
            if(c!=0)return c
        }
        return left.size.compareTo(right.size)
    }
    fun selectRelease(text: String, current: String): GithubRelease? = json.decodeFromString<List<GithubRelease>>(text)
        .filter { !it.draft && versionPattern.matches(it.tagName) && compareVersions(it.tagName,current)>0 && asset(it)!=null }
        .maxWithOrNull { a,b -> compareVersions(a.tagName,b.tagName) }
    fun asset(release: GithubRelease): GithubAsset? {
        val name="OVRPort-PFDMR-Edition-${release.tagName.removePrefix("v")}-Windows.zip"
        return release.assets.singleOrNull { it.name==name && it.state=="uploaded" && it.size in 1..536870912 &&
            it.url=="${UpdateFiles.REPO}/releases/download/${release.tagName}/$name" && it.digest?.matches(Regex("sha256:[0-9a-f]{64}"))==true }
    }
    fun check(current: String): GithubRelease? {
        val temp=Files.createTempFile("pfdmr-releases-", ".json")
        try {
            UpdateFiles.download("https://api.github.com/repos/urscaviezel/OVRPort-PFDMR/releases?per_page=100",temp,8L*1024*1024)
            return selectRelease(Files.readString(temp),current)
        } finally { Files.deleteIfExists(temp) }
    }
    // Download and validate fully before asking the helper to wait for this process.
    fun prepare(release: GithubRelease, status: (String)->Unit): Process {
        val root=UpdateFiles.installation(AppUpdates::class.java)
        val selected=requireNotNull(asset(release)) { "No complete Windows update available." }
        val stage=Files.createTempDirectory(root,".pfdmr-update-")
        try {
            FileChannel.open(root.resolve(".pfdmr-update.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE).use { channel ->
                val lock=channel.tryLock() ?: error("Another update is already in progress.")
                lock.use {
                    status("Downloading ${release.tagName}...")
                    val zip=stage.resolve("update.zip")
                    UpdateFiles.download(selected.url,zip,selected.size)
                    status("Verifying update...")
                    UpdateFiles.verify(zip,selected.size,selected.digest!!)
                    val payload=stage.resolve("payload")
                    UpdateFiles.extract(zip,payload,release.tagName.removePrefix("v"))
                    val info=json.parseToJsonElement(Files.readString(payload.resolve("BUILD-INFO.json"))).jsonObject
                    check(info["version"]?.jsonPrimitive?.content==release.tagName.removePrefix("v")) { "Update version mismatch." }
                    check(info["jar_sha256"]?.jsonPrimitive?.content==UpdateFiles.sha256(payload.resolve("OVRPort-PFDMR.jar"))) { "Update JAR checksum mismatch." }
                    Files.delete(zip)
                    // Use the current trusted helper, outside files that it replaces.
                    val helper=stage.resolve("installer.exe")
                    Files.copy(root.resolve("PFDMR-Updater.exe"),helper,StandardCopyOption.REPLACE_EXISTING)
                    val process=ProcessBuilder(helper.toString(),root.toString(),stage.toString(),ProcessHandle.current().pid().toString()).directory(root.toFile()).start()
                    for(i in 0..100) {
                        if(Files.exists(stage.resolve("ready"))) return process
                        check(process.isAlive) { "Update helper could not start. The app has not been changed." }
                        Thread.sleep(100)
                    }
                    process.destroy()
                    error("Update helper did not respond. Please try again.")
                }
            }
        } catch(e: Exception) {
            // Only this freshly created staging directory; never touch the installation on download failure.
            stage.toFile().deleteRecursively()
            throw e
        }
    }
}
