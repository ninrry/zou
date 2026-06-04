plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

import java.util.Properties
import java.util.UUID
import org.gradle.api.tasks.testing.Test

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "luzzr.zou"
    compileSdk = 37

    defaultConfig {
        applicationId = "luzzr.zou"
        minSdk = 29
        targetSdk = 37
        versionCode = 8
        versionName = "0.3.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // 统一签名：debug 也用密钥签名，确保无缝升级
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // APK 分离打包：arm64 覆盖主流手机，x86_64 覆盖 ChromeOS 与模拟器安装场景
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    // AAB 分离配置
    bundle {
        abi {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        language {
            enableSplit = true
        }
    }

    // 资源语言过滤：仅保留中文和英文
    androidResources {
        localeFilters += setOf("zh", "en")
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
        // Version drift is reviewed through Dependabot PRs; lint remains a code-correctness gate.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency")
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        animationsDisabled = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.com.google.dagger.hilt.android)
    implementation(libs.com.google.android.material)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.androidx.hilt.compiler)
    ksp(libs.com.google.dagger.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<Test>().configureEach {
    val runId = (project.findProperty("testRunId") as String?)
        ?: "auto-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
    val testTempDirectory = layout.buildDirectory.dir("tmp/$name/$runId")
    maxParallelForks = 1
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$name/binary-$runId"))
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$name/xml-$runId"))
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$name-$runId"))
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    jvmArgs(
        "-Djava.io.tmpdir=${testTempDirectory.get().asFile.absolutePath}",
    )
    doFirst {
        testTempDirectory.get().asFile.mkdirs()
    }
}
