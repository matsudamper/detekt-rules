package net.matsudamper.detekt.support

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * 公開しない。各ルール JAR へ同梱し、ルールセットごとのパッケージへ移動する。
 */
fun KaNamedFunctionSymbol.isInlineReifiedWithoutSerializerArgument(): Boolean {
    if (!isInline || typeParameters.none { it.isReified }) return false
    if (valueParameters.any { it.returnType.referencesClass(serializerArgumentClassIds) }) return false
    return true
}

fun String.isWithinPackage(packageName: String): Boolean {
    return this == packageName || startsWith("$packageName.")
}

fun KaType.referencesClass(targets: Set<ClassId>): Boolean {
    return referencesClass(targets, emptySet())
}

private fun KaType.referencesClass(targets: Set<ClassId>, seen: Set<ClassId>): Boolean {
    val classType = this as? KaClassType ?: return false
    val classId = classType.classId
    if (classId in seen) return false
    if (classId in targets) return true
    val classSymbol = classType.symbol as? KaClassSymbol ?: return false
    val nextSeen = seen + classId
    return classSymbol.superTypes.any { superType -> superType.referencesClass(targets, nextSeen) }
}

val serialFormatClassIds: Set<ClassId> = setOf(
    ClassId.topLevel(FqName("kotlinx.serialization.SerialFormat")),
    ClassId.topLevel(FqName("kotlinx.serialization.StringFormat")),
    ClassId.topLevel(FqName("kotlinx.serialization.BinaryFormat")),
)

val kSerializerClassIds: Set<ClassId> = setOf(
    ClassId.topLevel(FqName("kotlinx.serialization.KSerializer")),
)

val serializerArgumentClassIds: Set<ClassId> = setOf(
    ClassId.topLevel(FqName("kotlinx.serialization.SerializationStrategy")),
    ClassId.topLevel(FqName("kotlinx.serialization.DeserializationStrategy")),
    ClassId.topLevel(FqName("kotlinx.serialization.KSerializer")),
)
