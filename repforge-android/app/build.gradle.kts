import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
}

android {
    namespace = "edu.umg.repforge"
    compileSdk = 35

    defaultConfig {
        // Las pruebas instrumentadas pueden usar un paquete independiente para proteger datos reales.
        applicationId = if (providers.gradleProperty("pruebasAisladas").orNull == "true")
            "edu.umg.repforge.pruebas" else "edu.umg.repforge"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { viewBinding = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    implementation("edu.umg.repforge:repforge-core:1.0.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
}
