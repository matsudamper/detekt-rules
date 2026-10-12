package net.matsudamper.detekt.gradle

import org.gradle.api.provider.Property

interface RulePublishExtension {
    /**
     * 同梱する support クラスの移動先。
     * ルールセットごとに別パッケージにして、別バージョンを同時に classpath へ載せてもクラスが衝突しないようにする。
     */
    val bundledPackage: Property<String>
}
