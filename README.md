# JsonDsl

[![Process Pull Request](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml)

JsonDsl is a multi platform kotlin library that helps you build Kotlin DSLs for JSON and YAML dialects. 
The DSLs are easy to extend with custom fields by users via a MutableMap.

A DSL (Domain Specific Language) differs from General Purpose Languages, such as Kotlin, in that a DSL is intended to program or drive a tool or framework for some domain or API. Kotlin like several other languages is suitable for creating internal DSLs that (ab)use the syntax of the host language to implement a DSL.

## The problem

Of course creating model classes for your json domain model and annotating them with annotations for e.g. `kotlinx.serialization` is a valid way to start creating a DSL for your JSON or YAML dialect of choice.

However, this has some limitations. What if your JSON dialect evolves and somebody adds some new features? Unless you change your model class, it would not be possible to access such new features via the Kotlin DSL. 

This library started out as few classes in my [kt-search](https://github.com/jillesvangurp/kt-search) project, which implements an Elasticsearch and Opensearch client. Elasticsearch has several JSON dialects that are used for querying, defining mappings, and a few other things. Especially the query language has a large number of features and is constantly evolving. 

Not only do I have to worry about implementing each and every little feature these DSLs have and keeping up with upstream additions to OpenSearch and Elasticsearch. I also have to worry about supporting query and mapping features added via custom plugins. This is of course very challenging. And it was the main reason I created json-dsl: so I don't have to keep up.

## Strongly typed and Flexible

The key feature in json-dsl is that it uses a `MutableMap` for storing property values. This enables you
to define classes with properties that delegate storing their value to this map. For anything that your
classes don't implement, the user can always write to the map directly using a simple `put`.

This gives users a nice fallback for things your DSL classes don't implement and it relieves Kotlin DSL implementors from having to provide support for every new feature the upstream JSON dialect has or adds over time.

With kt-search, I focus on supporting all the commonly used, and some less commonly used things in the Elastic DSLs. But for everything else, I just rely on letting the user modify the underlying map themselves. 

## Gradle

This library is published to our own maven repository.

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        // optional but it speeds up the gradle dependency resolution
        content {
            includeGroup("com.jillesvangurp")
            includeGroup("com.tryformation")
        }
    }
}
```

And then you can add the dependency:

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

// a helper function to create MyDsl instances
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
           
So it understands all the primitives, Maps and Lists. But also Arrays, Sets, Sequences.                
And of course other JsonDsl classes, so you can nest them.                
And when it falls back to using `toString()`                

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
  idontknow = mapOf(
    "arrays" to arrayOf(
      1, 2, "3", 4.0,
      mapOf("this" to "is valid JSON")
    ),
    "sequences" to sequenceOf(1,"2",3.0)
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
  "idontknow": {
    "arrays": [
      1, 
      2, 
      "3", 
      4.0, 
      {
        "this": "is valid JSON"
      }
    ],
    "sequences": [
      1, 
      "2", 
      3.0
    ]
  }
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

### Custom values

Sometimes you need to have the serialized version of a value be different
from the kotlin identifier. For this we have added the CustomValue interface.

This is useful in combination with for example Enums.

```kotlin
enum class Grades(override val value: Double) : CustomValue<Double> {
  Excellent(7.0),
  Pass(5.51),
  Fail(3.0),
  ;
}
```

```kotlin
println(withJsonDsl {
  this["grade"] = Grades.Excellent
}.json(true))
```

Note how the grade's value is used instead of the name

```json
{
  "grade": 7.0
}
```

You can also rely on the `toString()` function:

```kotlin
data class FooBar(val foo:String="foo", val bar: String="bar")
println(withJsonDsl {
  this["foo"]=FooBar()
})
```

Note how it simply uses `toString()` on the data class

```json
{
  "foo": "FooBar(foo=foo, bar=bar)"
}
```

This also works for things like enums, value classes, and other Kotlin language constructs.

## YAML

While initially written to support JSON, I also added a YAML serializer that you may use to 
create Kotlin DSLs for YAML based DSLs. 

```kotlin
class YamlDSL : JsonDsl() {
  var str by property<String>()
  var map by property<Map<String,Any>>()
  var list by property<List<Any>>()
}
val dsl = YamlDSL().apply {
  str="""
    Multi line
    Strings are 
       supported
       and
    preserve their
      indentation!
  """.trimIndent()
  map = mapOf(
    "foo" to "bar",
    "num" to PI,
    "bool" to true,
    "notABool" to "false"
  )
}
// default is true for including ---
print(dsl.yaml(includeYamlDocumentStart = false))
```

This prints the YAML below:

```yaml
str: |
  Multi line
  Strings are 
     supported
       and
  preserve their
    indentation!
map: 
  foo: bar
  num: 3.141592653589793
  bool: true
  notABool: "false"
```

There are other tree like formats that might be supported in the future like TOML, properties, 
and other formats. I welcome pull requests for this provided they don't add any library dependencies.

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

## Development and stability

Before I extracted it from there, this library was part of [kt-search](https://github.com/jillesvangurp/kt-search), which has been out there for several years and  has a steadily growing user base. 

Other than cleaning the code a bit up for public use, there were no compatibility breaking changes. This means I want to keep the API for json-dsl stable and will not make any major changes unless there is a really good reason. 

This also means there won't be a lot of commits or updates since things are stable and pretty much working as intended. Unless somebody finds a bug or asks for reasonable changes, the only changes likely to happen will be occasional dependency updates.

## Libraries using Json Dsl

- [kt-search](https://github.com/jillesvangurp/kt-search)

If you use this and find it useful, please add yourself to the list by creating a pull request on
[outro.md](src/jvmTest/com/jillesvangurp/jsondsl/readme/outro.md) as this readme is generated using
my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library.

