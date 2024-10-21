import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("plugin.serialization") version "1.9.21"
    id("com.google.devtools.ksp") version "2.0.20-1.0.25"
}

android {
    namespace = "com.cwuom.ouo"
    compileSdk = 34

    val appVerCode: Int by lazy {
        val versionCode = SimpleDateFormat("yyMMddHH", Locale.ENGLISH).format(Date())
        println("versionCode: $versionCode")
        versionCode.toInt()
    }


    defaultConfig {
        applicationId = "com.cwuom.ouo"
        minSdk = 29
        targetSdk = 34
        versionCode = appVerCode
        versionName = "${getShortGitRevision()}.${getCurrentDate()}"
    }

    buildFeatures {
        buildConfig = true
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

fun getShortGitRevision(): String {
    val command = "git rev-parse --short HEAD"
    val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
    val process = processBuilder.start()

    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()

    if (exitCode == 0) {
        return output.trim()
    } else {
        // 你需要配置好Git环境并且至少做一次提交
        throw RuntimeException("Failed to get the commit count. " +
                "Make sure you have Git installed and your working directory is a Git repository." +
                "If this is a new repository, you need to make at least one commit.")
    }
}


fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("MMddHHmm", Locale.getDefault())
    return sdf.format(Date())
}

dependencies {
    implementation(libs.core.ktx)
    compileOnly(libs.xposed)
    compileOnly(libs.appcompat)
    compileOnly(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.kotlinx.io.jvm)
    implementation(libs.ezXHelper)
    implementation(projects.xView)

    implementation(libs.dexkit)
    compileOnly(projects.qqStub)
    implementation(libs.hiddenapibypass)
    implementation(libs.gson)

    implementation(kotlinx("io-jvm", "0.1.16"))

    implementation(ktor("serialization", "kotlinx-json"))
    implementation(grpc("protobuf", "1.62.2"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
