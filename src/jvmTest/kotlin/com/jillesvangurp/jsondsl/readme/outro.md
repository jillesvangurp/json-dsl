## Multi platform

This is a Kotlin multi platform library that should work on most  kotlin platforms (jvm, js, ios, android, etc). Wasm will be added later, after Kotlin 2.0 stabilizes.

My intention is to keep this code very portable and not introduce any dependencies other than
the Kotlin standard library.

## Development and stability

Before I extracted it from there, this library was part of [kt-search](https://github.com/jillesvangurp/kt-search), which has been out there for several years and  has a steadily growing user base. 

Other than cleaning the code a bit up for public use, there were no compatibility breaking changes. This means I want to keep the API for json-dsl stable and will not make any major changes unless there is a really good reason. 

This also means there won't be a lot of commits or updates since things are stable and pretty much working as intended. Unless somebody finds a bug or asks for reasonable changes, the only changes likely to happen will be occasional dependency updates.

## Libraries using Json Dsl

- [kt-search](https://github.com/jillesvangurp/kt-search)

If you use this and find it useful, please add yourself to the list by creating a pull request on
[outro.md](src/jvmTest/com/jillesvangurp/jsondsl/readme/outro.md) as this readme is generated using
my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library.
