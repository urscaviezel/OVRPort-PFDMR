import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.file.FileSystems
import java.nio.file.Files
version = providers.gradleProperty("releaseVersion").get()
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}
kotlin {
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    sourceSets {
        val desktopMain by getting
        commonMain.dependencies {
            implementation(project(":overportcli"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.serialization.json)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.com.formdev.flatlaf)
        }
    }
}
compose.desktop {
    application {
        mainClass = "moe.crx.overport.app.ComposeMainKt"
        buildTypes.release.proguard.isEnabled = false
    }
}

tasks.register("dependencyInventory") {
    doLast {
        val output = layout.buildDirectory.file("dependency-inventory.tsv").get().asFile
        output.parentFile.mkdirs()
        val artifacts = configurations.getByName("desktopRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
        output.writeText(artifacts.sortedBy { it.moduleVersion.id.toString() }.joinToString("\n") {
            "${it.moduleVersion.id}\t${it.file.absolutePath}"
        })
    }
}

// Signatures from dependencies do not describe the merged executable JAR.
tasks.matching { it.name == "packageUberJarForCurrentOS" }.configureEach {
    doLast {
        fileTree(layout.buildDirectory.dir("compose/jars")).matching { include("*.jar") }.forEach { jar ->
            FileSystems.newFileSystem(jar.toPath(), null as ClassLoader?).use { zip ->
                val meta = zip.getPath("/META-INF")
                if (Files.isDirectory(meta)) {
                    Files.list(meta).use { entries ->
                        entries.filter {
                            it.fileName.toString().uppercase().matches(Regex(".*\\.(SF|RSA|DSA|EC)"))
                        }.forEach { Files.delete(it) }
                    }
                }
            }
        }
    }
}
