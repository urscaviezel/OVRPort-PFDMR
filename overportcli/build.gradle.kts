import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.21"
    application
    id("com.gradleup.shadow") version "9.2.2"
}

group = "moe.crx"
version = providers.gradleProperty("releaseVersion").get()

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

dependencies {
    implementation(libs.android.tools.build)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(fileTree("libs"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    doFirst {
        check(file("src/main/resources/pfdmr/audio/Audio48.smali").isFile) {
            "Build the audio helper first: python audio-helper/build_helper.py --jdk <JDK> --sdk <SDK>"
        }
        check(file("src/main/resources/pfdmr/arm64-v8a/libopenxr_loader.so").isFile) {
            "Build the stereo wrapper first: python native/build_native.py --ndk <NDK path>"
        }
    }
}

application {
    mainClass = "moe.crx.overport.cli.CliMainKt"
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
}
