# JsonDsl

[![Process Pull Request](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml)

JsonDsl is a multi platform kotlin library to allow people to create Kotlin DSLs that serialize to JSON.

A DSL (Domain Specific Language) differs from General Purpose Languages, such as Kotlin, in that a DSL is intended to program or drive a tool or framework for some domain or API. Kotlin like several other languages is suitable for creating internal DSLs that (ab)use the syntax of the host language to implement a DSL.

## The problem

Of course creating model classes for your json domain model and annotating them with annotations for e.g. kotlinx.serialization is a valid way to start creating a DSL for your JSON dialect of choice.

However, this has some limitations. What if your JSON dialect evolves and somebody adds some new features? Unless you change your model class, it would not be possible to access such new features via the Kotlin DSL.

This library started out as few classes in my [kt-search](https://github.com/jillesvangurp/kt-search) project, which implements an Elasticsearch and Opensearch client. Elasticsearch has several JSON dialects that are used for querying, defining mappings, and a few other things. Especially the query language has a large number of features and is constantly evolving. 

Not only do I have to worry about upstream additions to OpenSearch and Elasticsearch. I also have to worry about supporting query and mapping features added via custom plugins. This is of course very challenging.

## Strongly typed and Flexible

JsonDsl was created to address this problem. It allows the creation of rich, type safe Kotlin DSLs with all the bells and whistles that Kotlin users are used to. But users can trivially extend any JsonDsl  based Kotlin DSL simply by accessing the underlying `MutableMap<Any,String>`. If a particular property is not implemented, you can simply add it with a `put`. 

This gives users a nice fallback and relieves Kotlin DSL implementors from having to provide support for every new feature the upstream JSON dialect has or adds over time.

## Gradle

Add the `maven.tryformation.com` repository:

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        content {
            includeGroup("com.jillesvangurp")
        }
    }
}
```

And then the dependency to commonsMain or main:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:json-dsl:1.x.y")
```

## Examples

### Hello World

```kotlin
class MyDsl : JsonDsl() {
  // adds a string property that the user can assign
  var message by property<String>()
}

fun myDsl(block: MyDsl.() -> Unit): MyDsl {
  return MyDsl().apply(block)
}

val json = myDsl {
  message = "Hello world"
}.json(pretty = true)

println(json)
```

The json extension function uses the json serializer to produce 
pretty printed json:

```json
{
  "message": "Hello world"
}
```

### Common Kotlin Types

JSON is a fairly simple data format. There are numbers, booleans, strings, lists and dictionaries.

Kotlin is of course a bit richer and mapping that to JSON is key to providing rich Kotlin DSL.

JsonDsl does a best effort to do map Kotlin types correctly to the intended JSON equivalent.              

```kotlin
class MyDsl : JsonDsl() {
  var intVal by property<Int>()
  var boolVal by property<Boolean>()
  var doubleVal by property<Double>()
  var arrayVal by property<Array<String>>()
  var listVal by property<List<String>>()
  var mapVal by property<Map<String, String>>()
  var idontknow by property<Any>()
}

// using kotlin's apply here, you can add helper functions of course
MyDsl().apply {
  intVal = 1
  boolVal = true
  doubleVal = PI
  arrayVal = arrayOf("hello", "world")
  listVal = listOf("1", "2")
  mapVal = mapOf(
    "Key" to "Value"
  )

  // The Any type is a bit of free for all
  idontknow = setOf(
    arrayOf(
      1, 2, "3", 4.0,
      mapOf("this" to "is valid JSON")
    )
  )
}
```

This does the right things to all the Kotlin types, including `Any`:

```json
{
  "int_val": 1,
  "bool_val": true,
  "double_val": 3.141592653589793,
  "array_val": [
    "hello", 
    "world"
  ],
  "list_val": [
    "1", 
    "2"
  ],
  "map_val": {
    "Key": "Value"
  },
  "idontknow": [
    [
      1, 
      2, 
      "3", 
      4.0, 
      {
        "this": "is valid JSON"
      }
    ]
  ]
}
```

### Manipulating the Map directly

All JsonDsl does is provide a delegate implementation of a `MutableMap<String, Any?>`.

So all sub classes have direct access to that map. And you can put whatever into it.

```kotlin
class MyDsl : JsonDsl() {
  var foo by property<String>()
}

MyDsl().apply {
  // nicely typed.
  foo = "bar"

  this["bar"] = "foo"
  this["going_off_script"] = listOf(
    MyDsl().apply {
      this["anything"] = "is possible"
    },
    42
  )
  this["inline_json"] = RawJson("""
    {
      "if":"you need to",
      "you":"can even add json in string form"
    }
  """.trimIndent())
}
```

```json
{
  "foo": "bar",
  "bar": "foo",
  "going_off_script": [
    {
      "anything": "is possible"
    }, 
    42
  ],
  "inline_json": {
    "if":"you need to",
    "you":"can even add json in string form"
}
}
```

### snake_casing, custom names, defaults

A lot of JSON dialects use snake cased dictionary keys. Kotlin of course uses 
camel case for its identifiers and it has certain things that you can't redefine.

Like the `size` property on `Map`.

```kotlin
class MyDsl : JsonDsl(
  namingConvention = PropertyNamingConvention.ConvertToSnakeCase
) {
  var camelCase by property<Boolean>()
  var mySize by property<Int>(
    customPropertyName = "size"
  )
  var m by property(
    customPropertyName = "meaning_of_life",
    defaultValue = 42
  )
}

MyDsl().apply {
  camelCase = true
  mySize = Int.MAX_VALUE
}
```

```json
{
  "meaning_of_life": 42,
  "camel_case": true,
  "size": 2147483647
}
```

## A real life, complex example

Here's a bit of the kt-search Kotlin DSL that I lifted
from my kt-search library. It defines a minimal 
query that only adds one of the (many) types of queries
supported by Elasticsearch. Unfortunately, like many real life
JSON, the Elasticsearch DSL is quite complicated and challenging 
to model. 

However, JsonDsl provides you all you need to wrap this with 
a nice type safe Kotlin DSL.            

```kotlin
@DslMarker
annotation class SearchDSLMarker

interface QueryClauses

// abbreviated version of the
// Elasticsearch Query DSL in kt-search
class QueryDsl:
  JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase),
  QueryClauses
{
  // Elasticsearch has this object wrapped in
  // another object
  var query: ESQuery
    get() {
      val map =
        this["query"] as Map<String, JsonDsl>
      val (name, details) = map.entries.first()
      // reconstruct the ESQuery
      return ESQuery(name, details)
    }
    set(value) {
      this["query"] = value.wrapWithName()
    }}

fun query(block: QueryDsl.()->Unit): QueryDsl {
  return QueryDsl().apply(block)
}

@SearchDSLMarker
open class ESQuery(
  val name: String,
  val queryDetails: JsonDsl = JsonDsl()
)  {

  // Elasticsearch wraps everything in an outer object
  // with the name as its only key
  fun wrapWithName() = withJsonDsl() {
    this[name] = queryDetails
  }
}

// the dsl class for creating term queries
@SearchDSLMarker
class TermQuery(
  field: String,
  value: String,
  termQueryConfig: TermQueryConfig = TermQueryConfig(),
  block: (TermQueryConfig.() -> Unit)? = null
) : ESQuery("term") {

  init {
    queryDetails.put(field, termQueryConfig, PropertyNamingConvention.AsIs)
    termQueryConfig.value = value
    block?.invoke(termQueryConfig)
  }
}

// configuration for term queries
class TermQueryConfig : JsonDsl() {
  var value by property<String>()
  var boost by property<Double>()
}

// making this an extension function ensures it is available
// on the receiver of the query {..} block
fun QueryClauses.term(
  field: String,
  value: String,
  block: (TermQueryConfig.() -> Unit)? = null
) =
  TermQuery(field, value, block = block)

fun QueryClauses.term(
  field: KProperty<*>,
  value: String,
  block: (TermQueryConfig.() -> Unit)? = null
) =
  TermQuery(field.name, value, block = block)
```

And this is how you would use it.

```kotlin
class MyModelClassInES(val myField: String)
val q = query {
  query = term(MyModelClassInES::myField, "some value")
}
val pretty = q.json(pretty = true)
println(pretty)
```

In JSON form this looks as follows:                

```json
{
  "query": {
    "term": {
      "myField": {
        "value": "some value"
      }
    }
  }
}
```

Note how it correctly wrapped the term query with an object. And how it correctly 
assigns the `TermConfiguration` in an object that has the field value as the key.

Also note how we use a property reference here to avoid having to use 
a string literal. The Elasticsearch Query DSL support in [kt-search]() is a great 
reference for how to use JsonDsl.

## Multi platform

This is a Kotlin multi platform library that should work on most  kotlin platforms (jvm, js, ios, android, etc). Wasm will be added later, after Kotlin 2.0 stabilizes.

My intention is to keep this code very portable and not introduce any dependencies other than
the Kotlin standard library.

## Libraries using Json Dsl

- [kt-search](https://github.com/jillesvangurp/kt-search)

If you use this and find it useful, please add yourself to the list by creating a pull request on
[outro.md](src/jvmTest/com/jillesvangurp/jsondsl/readme/outro.md) as this readme is generated using
my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library.

