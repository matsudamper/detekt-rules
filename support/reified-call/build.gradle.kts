plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(libs.detektApi)
    compileOnly(libs.detektKotlinAnalysisApi)
    compileOnly(libs.kotlinCompiler)
}
