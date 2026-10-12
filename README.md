# detekt-rules

GraalVM native image 向けの detekt ルールを、ルールセットごとの Maven アーティファクトで配ります。必要なものだけ `detektPlugins` に足します。

| モジュール | 座標 | バージョンの定義 |
|---|---|---|
| `rules/matsudamper-native-image-ktor` | `net.matsudamper.detekt.rules:matsudamper-native-image-ktor` | そのモジュールの `build.gradle.kts` |
| `rules/matsudamper-native-image-kotlinx-serialization` | `net.matsudamper.detekt.rules:matsudamper-native-image-kotlinx-serialization` | そのモジュールの `build.gradle.kts` |

バージョンはモジュールごとに独立です。共通処理は `support/reified-call` にあり、公開しません。各 JAR へ同梱し、ルールセットごとのパッケージへ移動します。両方を別バージョンで classpath に載せても、共通クラスや Detekt のバージョンでは干渉しません。Detekt API は `compileOnly` です。

```shell
./gradlew :rules:matsudamper-native-image-ktor:publishToMavenLocal
./gradlew :rules:matsudamper-native-image-kotlinx-serialization:publishToMavenLocal
```

各ルールの検知内容と GraalVM で必要な理由は、モジュールの README にあります。
