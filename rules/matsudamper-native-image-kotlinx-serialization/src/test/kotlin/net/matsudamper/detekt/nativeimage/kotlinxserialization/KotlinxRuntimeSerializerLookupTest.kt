package net.matsudamper.detekt.nativeimage.kotlinxserialization

import dev.detekt.api.Config
import dev.detekt.api.RuleSetProvider
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import java.util.ServiceLoader

class KotlinxRuntimeSerializerLookupTest : FunSpec(
    {
        val environment = createEnvironment()
        val rule = KotlinxRuntimeSerializerLookup(Config.empty)

        fun lint(code: String) = rule.lintWithContext(environment, code, *dependencyStubs)

        test("メンバー呼び出しの encodeToString を検知する") {
            val code = """
                import example.Jsons

                class Body
                fun save(data: Body) {
                    Jsons.kotlinxSerialization.encodeToString(data)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("serializer を渡した encodeToString は検知しない") {
            val code = """
                import kotlinx.serialization.SerializationStrategy
                import kotlinx.serialization.json.Json

                class Body {
                    companion object {
                        fun serializer(): SerializationStrategy<Body> = error("stub")
                    }
                }
                fun save(json: Json, data: Body) {
                    json.encodeToString(Body.serializer(), data)
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("reified な decodeFromString を検知する") {
            val code = """
                import kotlinx.serialization.json.Json

                class Body
                fun load(json: Json, text: String) {
                    json.decodeFromString<Body>(text)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("名前をルールに持たない新しい format API を検知する") {
            val code = """
                import kotlinx.serialization.json.Json

                class Body
                fun save(json: Json, data: Body) {
                    json.encodeToFutureFormat(data)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("StringFormat の新しい reified 拡張を検知する") {
            val code = """
                import kotlinx.serialization.StringFormat
                import kotlinx.serialization.encodeToFuture

                class Body
                fun save(format: StringFormat, data: Body) {
                    format.encodeToFuture(data)
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("KSerializer を受け取る reified API は検知しない") {
            val code = """
                import kotlinx.serialization.KSerializer
                import kotlinx.serialization.json.Json

                class Body
                fun save(json: Json, serializer: KSerializer<Body>, data: Body) {
                    json.encodeWithSerializer(serializer, data)
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("Base64.encodeToString は検知しない") {
            val code = """
                import java.util.Base64

                fun encode() {
                    Base64.getEncoder().encodeToString(byteArrayOf())
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("reified な serializer 関数を検知する") {
            val code = """
                import kotlinx.serialization.serializer

                class Body
                fun lookup() {
                    serializer<Body>()
                }
            """.trimIndent()
            lint(code).shouldHaveSize(1)
        }

        test("Ktor の respond は検知しない") {
            val code = """
                import io.ktor.server.application.ApplicationCall
                import io.ktor.server.response.respond

                class Body
                suspend fun handle(call: ApplicationCall) {
                    call.respond(Body())
                }
            """.trimIndent()
            lint(code).shouldBeEmpty()
        }

        test("ServiceLoader が matsudamper-native-image-kotlinx-serialization を公開する") {
            val ids = ServiceLoader.load(RuleSetProvider::class.java).map { it.ruleSetId.value }
            ids.shouldContain("matsudamper-native-image-kotlinx-serialization")
        }
    },
)

private val dependencyStubs = arrayOf(
    """
        package kotlinx.serialization

        interface SerialFormat
        interface StringFormat : SerialFormat
        interface BinaryFormat : SerialFormat
        interface SerializationStrategy<T>
        interface DeserializationStrategy<T>
        interface KSerializer<T> : SerializationStrategy<T>, DeserializationStrategy<T>

        inline fun <reified T> serializer(): KSerializer<T> = error("stub")
        inline fun <reified T> StringFormat.encodeToFuture(value: T): String = ""
    """.trimIndent(),
    """
        package kotlinx.serialization.json

        import kotlinx.serialization.DeserializationStrategy
        import kotlinx.serialization.KSerializer
        import kotlinx.serialization.SerializationStrategy
        import kotlinx.serialization.StringFormat

        class Json : StringFormat {
            inline fun <reified T> encodeToString(value: T): String = ""
            fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String = ""
            inline fun <reified T> decodeFromString(string: String): T = error("stub")
            inline fun <reified T> encodeToFutureFormat(value: T): String = ""
            inline fun <reified T> encodeWithSerializer(serializer: KSerializer<T>, value: T): String = ""
        }
    """.trimIndent(),
    """
        package example

        import kotlinx.serialization.json.Json

        object Jsons {
            val kotlinxSerialization: Json = Json()
        }
    """.trimIndent(),
    """
        package io.ktor.server.application

        class ApplicationCall
    """.trimIndent(),
    """
        package io.ktor.server.response

        import io.ktor.server.application.ApplicationCall

        suspend inline fun <reified T : Any> ApplicationCall.respond(message: T) {}
    """.trimIndent(),
)
