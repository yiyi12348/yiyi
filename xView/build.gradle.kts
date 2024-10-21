@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("com.android.base")
    kotlin("android")
}

android {
    namespace = "com.lxj.xpopup"
    compileSdk = 34

    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("XPopup/library/src/main/java", "EasyAdapter/easy-adapter/src/main/java"))
            res.setSrcDirs(listOf("EasyAdapter/easy-adapter/src/main/res", "XPopup/library/src/main/res"))
        }
    }
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        resourceConfigurations += listOf("zh", "en")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.glide)
    implementation(libs.davemorrissey.subsampling.scale.image.view)
}
