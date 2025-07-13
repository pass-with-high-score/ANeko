import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.nqmgaming.aneko"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.nqmgaming.aneko"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        multiDexEnabled = true
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val keystoreProperties = Properties()
        try {
            file(rootProject.file("local.properties")).inputStream()
                .use { keystoreProperties.load(it) }.let {
                    create("release") {
                        storeFile = file(keystoreProperties.getProperty("KEYSTORE_FILE"))
                        storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
                        keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
                        keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
                    }
                }
        } catch (_: Exception) {
            println("local.properties not found, using default values")
            create("release") {
                storeFile = file(rootProject.file("app/keystore"))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    // Core UI
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)

    // Runtime
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.accompanist.permissions)
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)

    // Compose Destination
    implementation(libs.compose.destination.animation.core)
    implementation(libs.compose.destination.core)
    ksp(libs.compose.destination.ksp)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Serialization
    implementation(libs.bundles.serialization)

    implementation(libs.androidx.compose.animation.graphics)

    implementation(libs.bundles.ktor)

    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.material)
    implementation(libs.timber)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.graphics)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}