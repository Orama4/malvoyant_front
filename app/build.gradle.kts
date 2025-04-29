plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.malvoayant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.malvoayant"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
        aaptOptions {
            noCompress+= "tflite"
        }
    }

    dependencies {
        implementation("org.java-websocket:Java-WebSocket:1.5.3")
        implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

        implementation("org.tensorflow:tensorflow-lite:2.14.0")
        implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
        implementation("org.tensorflow:tensorflow-lite-metadata:0.4.3")
        implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.3")
        implementation ("org.tensorflow:tensorflow-lite-gpu:2.9.0")
        implementation ("com.google.mlkit:text-recognition:16.0.0")
        implementation("androidx.camera:camera-core:1.3.2")
        implementation("androidx.camera:camera-camera2:1.3.2")
        implementation("androidx.camera:camera-lifecycle:1.3.2")
        implementation("androidx.camera:camera-view:1.3.2")
        implementation("androidx.camera:camera-extensions:1.3.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")
        implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
        implementation(libs.androidx.core.ktx)
    implementation("androidx.compose.material:material-icons-extended:1.5.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.espresso.core)
        testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}