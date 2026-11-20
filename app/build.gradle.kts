import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 compiles Kotlin itself (built-in Kotlin); only the Compose
    // compiler plugin still has to be applied separately.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
// AMap Android keys: add `amap.api.key.debug` / `amap.api.key.release` to
// local.properties (or pass -PAMAP_API_KEY_DEBUG / -PAMAP_API_KEY_RELEASE).
// A legacy `amap.api.key` is used as the fallback for both variants.
val legacyAmapKey = localProperties.getProperty("amap.api.key")
    ?: providers.gradleProperty("AMAP_API_KEY").orNull
    ?: ""
val amapReleaseKey = localProperties.getProperty("amap.api.key.release")
    ?: providers.gradleProperty("AMAP_API_KEY_RELEASE").orNull
    ?: legacyAmapKey
val useReleaseSigningForDebug = localProperties
    .getProperty("amap.debug.useReleaseSigning")
    ?.toBoolean() ?: false
val amapDebugKey = localProperties.getProperty("amap.api.key.debug")
    ?: providers.gradleProperty("AMAP_API_KEY_DEBUG").orNull
    ?: if (useReleaseSigningForDebug) amapReleaseKey else legacyAmapKey
fun amapBuildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.lunary.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lunary.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 31
        versionName = "0.9.7"
        manifestPlaceholders["amapApiKey"] = amapReleaseKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["amapApiKey"] = amapDebugKey
            buildConfigField("String", "AMAP_API_KEY", amapBuildConfigString(amapDebugKey))
            if (useReleaseSigningForDebug && keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            manifestPlaceholders["amapApiKey"] = amapReleaseKey
            buildConfigField("String", "AMAP_API_KEY", amapBuildConfigString(amapReleaseKey))
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                // R8 full-mode crashed on launch (IllegalAccessError in Kotlin sequences);
                // keep the signed release installable until AGP/R8 rules are fixed.
                enable = false
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkTestSources = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Compose preview tooling and Robolectric-style tests read resources.
            isIncludeAndroidResources = true
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.exifinterface)
    implementation(files("libs/amap/AMap3DMap_11.3.100_AMapSearch_9.8.1_AMapLocation_11.3.101_20260924.jar"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        // Compose previews need the host-test component for every build type.
        (variant as HasHostTestsBuilder)
            .hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

// Android Studio's "Make Project" build action requests these lifecycle tasks.
// AGP 9 no longer registers them (built-in Kotlin), so alias them to the debug
// test-source compilation tasks the IDE expects.
tasks.register("unitTestClasses") {
    group = "build"
    description = "Compiles debug unit test classes for the IDE build action."
    dependsOn("compileDebugUnitTestSources")
}

tasks.register("androidTestClasses") {
    group = "build"
    description = "Compiles debug instrumented test classes for the IDE build action."
    dependsOn("compileDebugAndroidTestSources")
}
