## Multi platform

This is a Kotlin multi platform library that should work on most  kotlin platforms (jvm, js, ios, wasm, linux/windows/mac native, android, etc).

My intention is to keep this code very portable and lightweight and not introduce any dependencies other than the Kotlin standard library. 

## Using JsonDsl with Javascript libraries

If you use kotlin-js, there are a lot of Javascript libraries out there that have functions that expect some sort of Javascript objects as a parameter. Integrating such libraries into Kotlin typically requires writing some type mappings to be able to call functions in these libraries and defining external class definitions for any models.

With JsonDsl you can skip a lot of these class definitions and simply create a Kotlin DSL instead. Or use simply use it in schemaless mode and rely on the convenient mappings included for the default Kotlin collection classes. You can use lists, enums, maps, etc. and it will do the right thing. You have the full flexibility of JsonDsl to make things as type safe as you need them to be.

There is a `toJsObject()` extension function that is available in kotlin-js that you can use to convert any JsonDsl instance to a javascript object.

## Parsing

This library is not intended as a substitute for kotlinx.serialization or other JSON parsing frameworks. It is instead intended for writing Kotlin DSLs that you can use to generate valid JSON or YAML. JsonDsl does not support any form of parsing.

## Development and stability

Before I extracted it from there, this library was part of [kt-search](https://github.com/jillesvangurp/kt-search), which has been out there for several years and  has a steadily growing user base. So, even though this library is relatively new, the code base has been stable and actively used for several years.

Other than cleaning the code a bit up for public use, there were no compatibility breaking changes. I want to keep the API for json-dsl stable and will not make any major changes unless there is a really good reason. 

This also means there won't be a lot of commits or updates since things are stable and pretty much working as intended. And because I have a few users of kt-search, I also don't want to burden them with compatibility breaking changes. Unless somebody finds a bug or asks for reasonable changes, the only changes likely to happen will be occasional dependency updates.

## Libraries using Json Dsl

- [kt-search](https://github.com/jillesvangurp/kt-search)

If you use this and find it useful, please add yourself to the list by creating a pull request on
[outro.md](src/jvmTest/com/jillesvangurp/jsondsl/readme/outro.md) as this readme is generated using
my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library.
