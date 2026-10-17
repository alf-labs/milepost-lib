import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.room)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.alflabs.milepostlib"
    compileSdk {
        version = release(37)
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

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.android)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.usb.serial.android)

    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)

    // This is needed for launchFragmentInHiltContainer in Robolectric+Hilt testing.
    debugImplementation(libs.androidx.fragment.testing)

    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.truth)
    testImplementation(libs.androidx.fragment.testing)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.junit.ktx)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}

// Prevent a conflict between Truth 1.4.5 (the expected one)
// and Truth 1.1.5 (injected from some dependency)
configurations.all {
    resolutionStrategy {
        force(libs.truth)
    }
}

tasks.withType<Test> {
    // Fix for Robolectric 4.17 issue with SDK 37.
    // see https://github.com/robolectric/robolectric/issues/11434
    jvmArgs(
        "--add-exports",
        "java.base/jdk.internal.access=ALL-UNNAMED",
    )

    // Fix: Attach Mockito as a Java Agent at test runtime. Needed for recent JVM (17+).
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        val agentJar = classpath.files.find { file ->
            file.name.contains("mockito-core") || file.name.contains("byte-buddy-agent")
        }
        println("Mockito Agent runtime class path: $agentJar")
        if (agentJar != null) {
            listOf("-javaagent:${agentJar.absolutePath}")
        } else {
            emptyList()
        }
    })
}
