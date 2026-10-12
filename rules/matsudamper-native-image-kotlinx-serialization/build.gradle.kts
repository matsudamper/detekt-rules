plugins {
    id("detekt-rule-publish")
}

group = "net.matsudamper.detekt.rules"
version = "0.1.0"
description = "GraalVM native image で失敗する kotlinx.serialization の実行時 serializer 探索を detekt で検知する"

extensions.configure<net.matsudamper.detekt.gradle.RulePublishExtension>("rulePublish") {
    bundledPackage.set("net.matsudamper.detekt.nativeimage.kotlinxserialization.bundled")
}

dependencies {
    compileOnly(libs.detektApi)
    compileOnly(libs.detektKotlinAnalysisApi)
    compileOnly(libs.kotlinCompiler)
    implementation(project(":support:reified-call"))

    testImplementation(libs.detektApi)
    testImplementation(libs.detektKotlinAnalysisApi)
    testImplementation(libs.detektTest)
    testImplementation(libs.kotlinCompiler)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestAssertionsCore)
}
