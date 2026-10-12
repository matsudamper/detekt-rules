plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // ルールモジュールの Kotlin バージョンは gradle/libs.versions.toml の kotlin と揃える。
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.4.10")
    implementation("com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.6.1")
}
