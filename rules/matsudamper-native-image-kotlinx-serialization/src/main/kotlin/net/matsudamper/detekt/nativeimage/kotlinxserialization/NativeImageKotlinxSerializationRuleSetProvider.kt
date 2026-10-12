package net.matsudamper.detekt.nativeimage.kotlinxserialization

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class NativeImageKotlinxSerializationRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("matsudamper-native-image-kotlinx-serialization")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(::KotlinxRuntimeSerializerLookup),
        )
    }
}
