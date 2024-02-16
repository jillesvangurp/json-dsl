# Json DSL

JsonDsl is a multi platform kotlin library to allow people to create Kotlin DSLs that serialize to json.

This library started out as few classes in my kt-search project, which implements an Elasticsearch and Opensearch client with support for several of the Json
DSLs that these products support. Since it seems more generally applicable, I decided to extract this to a separate library.

## Design goals

- allow users to easily create type safe DSLs for json dialects
- kt-search is a multiplatform library, so this library too has to be multiplatform
- no runtime dependencies other than the Kotlin standard library. I want this to be lightweight.
- users should be able to easily extend any DSLs based on JsonDsl so that they don't get stuck because of missing support for new properties (on the json side)

## How does it work?

You can extend the open `JsonDsl` base class to create your own DSL classes. JsonDSL uses interface delegation to implement the `MutableMap<String, Any?>` interface and it provides helper functions that allow you to define delegate properties that use this map for storing their content. You can optionally override property naming conventions which is useful when dealing with json dialects that use snake casing (lowercase and underscores) instead of camel casing as is common for Kotlin identifiers.

## Example

This minimal example defines a simple DSL.

```kotlin
class MyDsl:JsonDsl() {
    var foo by property<String>()
    // will be snake_cased in the json
    var meaningOfLife by property<Int>()
    // we override the property name here
    var l by property<List<Any>>("a_custom_list")
    var m by property<Map<Any,Any>>()
}

val myDsl = MyDsl().apply {
    foo = "Hello\tWorld"
    meaningOfLife = 42
    l = listOf("1", 2, 3.0)
    m = mapOf(42 to "fortytwo")
}
println(myDsl.json(pretty=true))
```

becomes

```json
{
  "foo": "Hello\tWorld",
  "meaning_of_life": 42,
  "a_custom_list": [
    "1",
    2,
    3.0
  ],
  "m": {
    "42": "fortytwo"
  }
}
```

