pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "detekt-rules"

include(
    ":support:reified-call",
    ":rules:matsudamper-native-image-ktor",
    ":rules:matsudamper-native-image-kotlinx-serialization",
)
