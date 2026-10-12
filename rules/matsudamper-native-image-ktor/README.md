# matsudamper-native-image-ktor

GraalVM native image で失敗する、Ktor の実行時 serializer 探索を detekt で検知します。

ルールセット ID は `matsudamper-native-image-ktor`、ルール名は `KtorRuntimeSerializerLookup` です。

## GraalVM で失敗する理由

GraalVM native image は、ビルド時に到達できない Kotlin の反射を含めません。Ktor の `respond` / `receive` や、型引数付きの routing は inline かつ reified で、本文の型に対応する serializer を実行時に探します。この探索は native image 上で `SerializationException` や `KotlinReflectionInternalError` になります。

`KSerializer` を引数で渡すと、コンパイラが生成した serializer を呼び出し箇所が直接参照します。native image はその参照をたどって serializer を含めます。`respondText` / `receiveText` は serializer を探しません。

`HttpStatusCode` だけを渡す `respond` は、本文をシリアライズせずステータスを設定するだけなので検知しません。

## 検知するもの

対象は次のパッケージにある、inline かつ reified で、値引数に `SerializationStrategy` / `DeserializationStrategy` / `KSerializer` を持たない関数です。

- `io.ktor.server.response`
- `io.ktor.server.request`
- `io.ktor.server.routing`
- `io.ktor.server.websocket`
- `io.ktor.client.call`
- `io.ktor.client.request`
- `io.ktor.client.plugins.websocket`
- `io.ktor.client.plugins.sse`
- `io.ktor.serialization`

関数名では見ていません。これらのパッケージに増える reified API も検知します。`kotlinx.serialization` の `encodeToString` はこのアーティファクトでは検知しません。そちらは `matsudamper-native-image-kotlinx-serialization` です。

```kotlin
call.respond(body) // 検知する
call.respond(HttpStatusCode.NoContent) // 検知しない
call.respondText("ok") // 検知しない
```

## 使い方

このアーティファクトだけのバージョンです。他のルールセットとは揃えません。

```kotlin
dependencies {
    detektPlugins("net.matsudamper.detekt.rules:matsudamper-native-image-ktor:0.1.0")
}
```

```yaml
matsudamper-native-image-ktor:
  KtorRuntimeSerializerLookup:
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
./gradlew :rules:matsudamper-native-image-ktor:publishToMavenLocal
```
