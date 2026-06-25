plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "com.tvhanan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tvhanan"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
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

    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.datastore) // Menggunakan preferences datastore dari Version Catalog
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.core.ktx)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.android.compiler)

    // Protobuf
    implementation(libs.protobuf)
    implementation(libs.protobuf.kotlin)

    // Datastore Protobuf (Dialihkan ke Version Catalog agar sinkron)
    implementation(libs.datastore.core) // Menggantikan datastore.core:1.0.0 (typo titik & versi lawas)

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito.core:5.0.0")
    testImplementation("org.mockito.kotlin:mockito.kotlin:4.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx.coroutines.test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.core:core-ktx:1.12.0") // Diperbaiki: titik (.) menjadi strip (-)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.robolectric:robolectric:4.12")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5") // Diperbaiki: titik (.) menjadi strip (-)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
}

configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}

kapt {
    correctErrorTypes = true
}