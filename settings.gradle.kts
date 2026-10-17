pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MilepostLib"

include(":LibUtilsJava")
project(":LibUtilsJava").projectDir = File(rootProject.projectDir, "../LibUtils/LibUtilsJava")

include(":LibUtilsAndroid")
project(":LibUtilsAndroid").projectDir = File(rootProject.projectDir, "../LibUtils/LibUtilsAndroid")

include(":AndroidNmeaParser")
project(":AndroidNmeaParser").projectDir = File(rootProject.projectDir, "../AndroidNmeaParser")
