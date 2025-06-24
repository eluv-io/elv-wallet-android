pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://muxinc.jfrog.io/artifactory/default-maven-release-local")
        }
    }
}

rootProject.name = "elv-wallet-android"
include(":app")
