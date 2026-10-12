package net.matsudamper.detekt.nativeimage.ktor

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import net.matsudamper.detekt.support.isInlineReifiedWithoutSerializerArgument
import net.matsudamper.detekt.support.isWithinPackage
import net.matsudamper.detekt.support.referencesClass
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * reified な serializer 探索は GraalVM native image で SerializationException や
 * KotlinReflectionInternalError になる。Ktor の対象パッケージにある inline reified 関数を検知する。
 */
class KtorRuntimeSerializerLookup(config: Config) :
    Rule(
        config,
        "Ktor の reified な serializer 探索は GraalVM native image で失敗する。" +
            "KSerializer を明示するか、respondText / receiveText を使う",
    ),
    RequiresAnalysisApi {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        analyze(expression) {
            val call = expression.resolveToCall()?.successfulFunctionCallOrNull() ?: return
            val symbol = call.signature.symbol as? KaNamedFunctionSymbol ?: return
            if (!isRuntimeSerializerLookup(symbol, call)) return
            val callableId = symbol.callableId?.asSingleFqName()?.asString() ?: symbol.name.asString()
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "$callableId は実行時に serializer を探すため、GraalVM native image で失敗する。" +
                        "KSerializer を明示するか、respondText / receiveText を使う",
                ),
            )
        }
    }

    /**
     * HttpStatusCode だけを渡す respond は DefaultTransform がステータス設定に使い、本文の serializer 探索をしない。
     */
    private fun isRuntimeSerializerLookup(symbol: KaNamedFunctionSymbol, call: KaFunctionCall<*>): Boolean {
        if (!symbol.isInlineReifiedWithoutSerializerArgument()) return false
        val packageName = symbol.callableId?.packageName?.asString() ?: return false
        if (ktorRuntimeTypeInfoPackagePrefixes.none { packageName.isWithinPackage(it) }) return false
        return !isStatusCodeOnly(symbol, call)
    }

    private fun isStatusCodeOnly(symbol: KaNamedFunctionSymbol, call: KaFunctionCall<*>): Boolean {
        val reifiedParameters = symbol.typeParameters.filter { it.isReified }
        if (reifiedParameters.isEmpty()) return false
        return reifiedParameters.all { parameter ->
            val typeArgument = call.typeArgumentsMapping[parameter] ?: return@all false
            typeArgument.referencesClass(httpStatusCodeClassIds)
        }
    }

    private companion object {
        private val httpStatusCodeClassIds = setOf(
            ClassId.topLevel(FqName("io.ktor.http.HttpStatusCode")),
        )
        private val ktorRuntimeTypeInfoPackagePrefixes = listOf(
            "io.ktor.server.response",
            "io.ktor.server.request",
            "io.ktor.server.routing",
            "io.ktor.server.websocket",
            "io.ktor.client.call",
            "io.ktor.client.request",
            "io.ktor.client.plugins.websocket",
            "io.ktor.client.plugins.sse",
            "io.ktor.serialization",
        )
    }
}
