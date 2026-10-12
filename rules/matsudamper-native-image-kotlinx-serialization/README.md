# matsudamper-native-image-kotlinx-serialization

GraalVM native image で失敗する、kotlinx.serialization の実行時 serializer 探索を detekt で検知します。

ルールセット ID は `matsudamper-native-image-kotlinx-serialization`、ルール名は `KotlinxRuntimeSerializerLookup` です。

## GraalVM で失敗する理由

GraalVM native image は、ビルド時に到達できない Kotlin の反射を含めません。`encodeToString` や `decodeFromString`、`serializer()` のような inline かつ reified な API は、引数に serializer が無いと実行時に serializer を探します。この探索は native image 上で `SerializationException` や `KotlinReflectionInternalError` になります。

`KSerializer` を引数で渡すと、コンパイラが生成した serializer を呼び出し箇所が直接参照します。native image はその参照をたどって serializer を含めます。

## 検知するもの

`kotlinx.serialization` パッケージの、inline かつ reified で、値引数に `SerializationStrategy` / `DeserializationStrategy` / `KSerializer` を持たない関数です。レシーバが `SerialFormat` / `StringFormat` / `BinaryFormat`（またはそのサブタイプ）であるか、戻り値が `KSerializer` であるときだけ検知します。

関数名では見ていません。同じ形の新しい API も検知します。メンバー呼び出しの `encodeToString` も、import が無くても検知します。Ktor の `respond` はこのアーティファクトでは検知しません。そちらは `matsudamper-native-image-ktor` です。

```kotlin
json.encodeToString(data) // 検知する
json.encodeToString(Body.serializer(), data) // 検知しない
serializer<Body>() // 検知する
```

## 使い方

このアーティファクトだけのバージョンです。他のルールセットとは揃えません。

```kotlin
dependencies {
    detektPlugins("net.matsudamper.detekt.rules:matsudamper-native-image-kotlinx-serialization:0.1.0")
}
```

```yaml
matsudamper-native-image-kotlinx-serialization:
  KotlinxRuntimeSerializerLookup:
    active: true
```

`RequiresAnalysisApi` のルールは、型解決なしの `detekt` タスクでは動きません。JVM は `detektMain` / `detektTest`、KMP は `detektMainJvm` / `detektTestJvm` に依存させます。

```kotlin
tasks.named("detekt").configure {
    dependsOn(
        tasks.matching {
            it.name == "detektMain" ||
                it.name == "detektTest" ||
                it.name == "detektMainJvm" ||
                it.name == "detektTestJvm"
        },
    )
}
```

コンパイル対象の Detekt は 2.0.0-alpha.6 です。Detekt 本体は依存関係に出さないので、実行に使う Detekt のバージョンはこのライブラリが選びません。

ローカルへ出すには、このモジュールだけを指定します。

```shell
./gradlew :rules:matsudamper-native-image-kotlinx-serialization:publishToMavenLocal
```
