import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.matsudamper.detekt.gradle.RulePublishExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.util.zip.ZipFile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.gradleup.shadow")
    `maven-publish`
}

extensions.configure<JavaPluginExtension>("java") {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

val rulePublish = extensions.create<RulePublishExtension>("rulePublish")

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

afterEvaluate {
    val bundledPackage = rulePublish.bundledPackage.get()
    tasks.named<ShadowJar>("shadowJar") {
        relocate("net.matsudamper.detekt.support", bundledPackage)
        dependencies {
            exclude { dependency ->
                val group = dependency.moduleGroup
                group == "org.jetbrains.kotlin" ||
                    group == "org.jetbrains.kotlinx" ||
                    group == "org.jetbrains" ||
                    group == "dev.detekt"
            }
        }
    }

    tasks.matching { it.name.startsWith("generateMetadataFileFor") }.configureEach {
        enabled = false
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name
            artifact(tasks.named("shadowJar"))
            pom {
                name.set(project.name)
                description.set(provider { project.description })
            }
        }
    }
}

val checkStandalonePublication = tasks.register("checkStandalonePublication") {
    val shadowJar = tasks.named<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    dependsOn("generatePomFileForMavenPublication")
    doLast {
        val jar = shadowJar.get().archiveFile.get().asFile
        val bundledPath = rulePublish.bundledPackage.get().replace('.', '/') + "/"
        val names = buildList {
            ZipFile(jar).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    add(entries.nextElement().name)
                }
            }
        }
        check(names.any { it.startsWith(bundledPath) }) {
            "同梱した support が $jar に無い"
        }
        check(names.none { it.startsWith("net/matsudamper/detekt/support/") }) {
            "support パッケージがリロケートされていない: $jar"
        }
        check(names.any { it == "META-INF/services/dev.detekt.api.RuleSetProvider" }) {
            "RuleSetProvider が $jar に無い"
        }
        check(names.none { it.startsWith("org/jetbrains/") || it.startsWith("dev/detekt/") }) {
            "Detekt または Kotlin 本体が $jar に入っている"
        }

        val publicationDir = layout.buildDirectory.dir("publications/maven").get().asFile
        val poms = publicationDir.walkTopDown().filter { it.extension == "xml" }.toList()
        check(poms.isNotEmpty()) { "POM が $publicationDir に無い" }
        poms.forEach { pom ->
            val text = pom.readText()
            check("<dependency>" !in text && "reified-call" !in text && "dev.detekt" !in text) {
                "公開 POM に依存関係がある: $pom\n$text"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(checkStandalonePublication)
}
