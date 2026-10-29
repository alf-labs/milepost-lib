import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.room)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.alflabs.milepostlib"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        // API level target vs devices:
        // Android 12 / S / API 31 on Samsung SM-T500 (Galaxy Tab A7 10")
        // Android 13 / T / API 33 on the Umidigi MT10 (dev/testing lower API)
        // Android 14 / U / API 34 on the Pixel 5, Samsung SM-T220 (Galaxy Tab A7 7")
        // Android 15 / V / API 35 on Samsung Galaxy Tab A9+ Plus
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":LibUtilsJava"))
    implementation(project(":LibUtilsAndroid"))
    implementation(project(":AndroidNmeaParser"))

    implementation(libs.hilt.android)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.x.appcompat)
    implementation(libs.material)
    implementation(libs.x.constraintlayout)
    implementation(libs.x.core.ktx)
    implementation(libs.x.datastore)
    implementation(libs.x.datastore.core)
    implementation(libs.x.datastore.preferences)
    implementation(libs.x.lifecycle.livedata.ktx)
    implementation(libs.x.lifecycle.service)
    implementation(libs.x.navigation.fragment.ktx)
    implementation(libs.x.navigation.ui.ktx)
    implementation(libs.x.preference.ktx)
    implementation(libs.x.room.guava)
    implementation(libs.x.room.ktx)
    implementation(libs.x.room.runtime)
    implementation(libs.x.webkit)
    implementation(libs.x.work.runtime.ktx)
    implementation(libs.usb.serial.android)

    ksp(libs.hilt.compiler)
    ksp(libs.x.room.compiler)

    // This is needed for launchFragmentInHiltContainer in Robolectric+Hilt testing.
    debugImplementation(libs.x.fragment.testing)

    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.x.fragment.testing)
    testImplementation(libs.x.test.core.ktx)
    testImplementation(libs.x.test.espresso.core)
    testImplementation(libs.x.test.junit.ktx)
    testImplementation(libs.x.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.x.test.espresso.core)
    androidTestImplementation(libs.x.test.junit)
}

// Prevent a conflict between Truth 1.4.5 (the expected one)
// and Truth 1.1.5 (injected from some dependency)
configurations.all {
    resolutionStrategy {
        force(libs.truth)
    }
}
