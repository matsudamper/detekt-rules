package net.matsudamper.detekt.nativeimage.kotlinxserialization

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import net.matsudamper.detekt.support.isInlineReifiedWithoutSerializerArgument
import net.matsudamper.detekt.support.isWithinPackage
import net.matsudamper.detekt.support.kSerializerClassIds
import net.matsudamper.detekt.support.referencesClass
import net.matsudamper.detekt.support.serialFormatClassIds
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * reified な serializer 探索は GraalVM native image で SerializationException や
 * KotlinReflectionInternalError になる。kotlinx.serialization の inline reified 関数を検知する。
 */
class KotlinxRuntimeSerializerLookup(config: Config) :
    Rule(
        config,
        "kotlinx.serialization の reified な serializer 探索は GraalVM native image で失敗する。KSerializer を明示する",
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
                    message = "$callableId は実行時に serializer を探すため、GraalVM native image で失敗する。KSerializer を明示する",
                ),
            )
        }
    }

    private fun isRuntimeSerializerLookup(symbol: KaNamedFunctionSymbol, call: KaFunctionCall<*>): Boolean {
        if (!symbol.isInlineReifiedWithoutSerializerArgument()) return false
        val packageName = symbol.callableId?.packageName?.asString() ?: return false
        if (!packageName.isWithinPackage("kotlinx.serialization")) return false
        return call.hasSerialFormatReceiver() || symbol.returnType.referencesClass(kSerializerClassIds)
    }

    private fun KaFunctionCall<*>.hasSerialFormatReceiver(): Boolean {
        val receiverTypes = listOfNotNull(
            dispatchReceiver?.type,
            extensionReceiver?.type,
        )
        return receiverTypes.any { it.referencesClass(serialFormatClassIds) }
    }
}
