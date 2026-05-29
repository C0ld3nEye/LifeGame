plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  kotlin("android")
  kotlin("kapt")
}

android {
    namespace = "com.example.resiliencesandbox"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.resiliencesandbox"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

kotlin {
    jvmToolchain(21)
}
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    androidResources {
        noCompress.addAll(listOf("task", "bin", "litertlm"))
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
      jniLibs {
        useLegacyPackaging = true
      }
    }
}



dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  "kapt"(libs.androidx.room.compiler)

  // Gson (JSON Parsing)
  implementation(libs.gson)

  // LiteRT-LM
  implementation(libs.litertlm.android)

  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.9.0")
}

// Copie automatique de l'APK finale vers un dossier 'APK' à la racine du projet après compilation
tasks.register<Copy>("copyApkToRoot") {
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("app-debug.apk")
    into(rootProject.projectDir.resolve("APK"))
    rename("app-debug.apk", "LifeGame.apk")
}

tasks.whenTaskAdded {
    if (name == "assembleDebug") {
        finalizedBy("copyApkToRoot")
    }
}
