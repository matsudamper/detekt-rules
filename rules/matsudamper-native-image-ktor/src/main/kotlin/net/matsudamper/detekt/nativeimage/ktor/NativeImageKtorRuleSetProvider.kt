package net.matsudamper.detekt.nativeimage.ktor

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class NativeImageKtorRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("matsudamper-native-image-ktor")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(::KtorRuntimeSerializerLookup),
        )
    }
}
