# JsonDsl

[![Process Pull Request](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml)

JsonDsl is a multi platform kotlin library that helps you build Kotlin DSLs for JSON and YAML dialects. 
DSLs made with this library are easy to extend with custom fields by users via a MutableMap.

# About DSLs

A Domain Specific Language (DSL) is a language that is intended to allow users to program or specify things in language that closely matches their domain. They are popular for configuration files, for use with certain frameworks or tools, and there are a lot of niche tools, frameworks, and other software packages out there that implement them. 

Some general purpose languages have a syntax that makes it easy to (ab)use their features to do something similar. Lisp and Ruby are a good example of languages that have historically been used for this. Like those languages, Kotlin includes a few features that enable this. Which makes Kotlin very well suited for implementing all sorts of DSLs.

There are Kotlin DSLs for all sorts of things. A popular example is the HTML DSL that comes with things like Ktor and kotlin-js. Spring bundles a lot of Kotlin DSLs for it's Java framework that make using that a lot nicer than from Java. There are lots of examples.

## Making easy to extend Kotlin DSLs for JSON/YAML dialects

Of course, creating model classes for your JSON domain model and annotating thosre with annotations for e.g. `kotlinx.serialization`, `jackson`, etc. is a perfectly valid way to start creating a DSL for your JSON or YAML dialect of choice.

However, using JSON frameworks like that have some limitations. What if your JSON dialect evolves and somebody adds some new features? Unless you change your model class, it would not be possible to access such new features via the Kotlin DSL. Or what if your JSON dialect is vast and complicated. Do you have to support all of its features? How do you decide what to allow and not allow in your Kotlin DSL.

This library started out as few classes in my [kt-search](https://github.com/jillesvangurp/kt-search) project. Kt-search implements a client library for Elasticsearch and Opensearch. Elasticsearch has several JSON dialects that are used for querying, defining index mappings, settings, and a few other things. Especially the query language has a large number of features and is constantly evolving. 

Not only do I have to worry about implementing each and every little feature these DSLs have and keeping up with upstream additions to OpenSearch and Elasticsearch. I also have to worry about supporting query and mapping features added via custom plugins. This is very challenging. And it was the main reason I created json-dsl: so I don't have to keep up.

## Strongly typed and Flexible

The key feature in json-dsl is that it uses a `MutableMap` and property delegation for implementing DSL classes. This simple approach enables you to define classes with properties that delegate storing their value to this map. For anything that your
classes don't implement, the user can simply modify the underlying map directly using a simple `put`.

This simple approach gives users a nice fallback for things your DSL classes don't implement and it relieves Kotlin DSL creators from having to provide support for every new feature the upstream JSON dialect has or adds over time. You can provide a decent experience for your users with minimal effort. And you users can always work around whatever you did not implement.

With kt-search, I simply focus on supporting all the commonly used, and some less commonly used things in the Elastic DSLs. But for everything else, I just rely on letting the user modify the underlying map themselves. A lot of pull requests I get on this project are people adding features they need in the DSLs. So, over time, feature support has gotten more comprehensive.

## Gradle

This library is published to our own maven repository. Simply add the repository like this:

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        // optional but it speeds up the gradle dependency resolution
        content {
            includeGroup("com.jillesvangurp")
        }
    }
}
```

And then you can add the dependency:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:json-dsl:3.x.y")
```

If you were using json-dsl via kt-search before, you can update simply by bumping the version of json-dsl to 3.0. Previously, 2.x was released along with kt-search and has now been removed from that project.


## Examples

All the examples in this README are implemented using
    my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library. You can find 
    the source code that generates this README [here](https://github.com/jillesvangurp/json-dsl/blob/main/src/jvmTest/kotlin/com/jillesvangurp/jsondsl/readme/ReadmeGenerationTest.kt). 
    
A more expanded version of these examples can be found in the form of a 
Jupyter notebook [here](https://github.com/jillesvangurp/json-dsl-jupyter)

### Hello World

Let's start with a simple example.

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

The json extension function uses a json serializer to produce 
pretty printed json:

```json
{
  "message": "Hello world"
}
```

There is also a YAML serializer. More on that below.

### Common Kotlin Types

JSON is a fairly simple data format. It has numbers, booleans, strings, lists and dictionaries. And null
values.  Kotlin has a bit richer type system and mapping that to JSON is key to providing rich Kotlin DSL.                

JsonDsl does a best effort to do map Kotlin types correctly to the intended JSON equivalent. 
           
It understands all the primitives, Maps and Lists. But also Arrays, Sets, Sequences, etc.                
And of course other JsonDsl classes, so you can nest them.  And it falls back to using 
`toString()` for everything else.              

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
      mapOf("this" to "is valid JSON"), "mixing types is allowed in JSON"
    ),
    "sequences" to sequenceOf(1,"2",3.0)
  )
}
```

This does the right things with all the used Kotlin types, including `Any`:

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
      }, 
      "mixing types is allowed in JSON"
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

As mentioned, JsonDsl delegates the storing of properties to a `MutableMap<String, Any?>`. 

So, all sub classes have direct access to that map. And you can put anything you want into it.

```kotlin
class MyDsl : JsonDsl() {
  var foo by property<String>()
}

MyDsl().apply {
  // nicely typed.
  foo = "bar"

  // but we never defined a bar property
  this["bar"] = "foo"
  // or this ...
  this["whatever"] = listOf(
    MyDsl().apply {
      this["you"] = "can add anything you want"
    },
    42
  )

  // RawJson is a Kotlin value class
  this["inline_json"] = RawJson("""
    {
      "if":"you need to",
      "you":"can even add json in string form",               
    }
  """.trimIndent())
}
```

```json
{
  "foo": "bar",
  "bar": "foo",
  "whatever": [
    {
      "you": "can add anything you want"
    }, 
    42
  ],
  "inline_json": {
    "if":"you need to",
    "you":"can even add json in string form",                           
}
}
```

### snake_casing, custom names, defaults

A lot of JSON dialects use snake cased field names. Kotlin of course uses 
camel case for its identifiers and it has certain things that you can't redefine.
Like the `size` property on `Map`, which is implemented by JsonDsl; or certain keywords.

```kotlin
class MyDsl : JsonDsl(
  // this will snake case all the names
  namingConvention = PropertyNamingConvention.ConvertToSnakeCase
) {
  // -> camel_case
  var camelCase by property<Boolean>()
  // unfortunately Map defines a size val already
  var mySize by property<Int>(
    customPropertyName = "size"
  )
  // val is a keyword in Kotlin
  var myVal by property<String>(
    customPropertyName = "val"
  )
  // Kotlin has default values, JSON does not.
  var m by property(
    customPropertyName = "meaning_of_life",
    defaultValue = 42
  )
}

MyDsl().apply {
  camelCase = true
  mySize = Int.MAX_VALUE
  myVal = "hello"
}
```

```json
{
  "meaning_of_life": 42,
  "camel_case": true,
  "size": 2147483647,
  "val": "hello"
}
```

### Custom values

Sometimes you want to have the serialized version of a value be different
from the kotlin type that you are using. For this we have added the 
CustomValue interface.

A simple use case for this could be Enums:                                

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

Note how the grade's Double value is used instead of the name.

The withJsonDsl function is a simple extension function that 
creates a JsonDsl for you and applies the block to it.

```json
{
  "grade": 7.0
}
```

You can also construct more complex ways to serialize your classes.

```kotlin
data class Person(
  val firstName: String,
  val lastName: String): CustomValue<List<String>> {
  override val value =
    listOf(firstName, lastName)
}

withJsonDsl {
  this["person"] = Person("Jane", "Doe")
}.json(true)
```

And of course your custom value can be a JsonDsl too.

```kotlin
data class Person(
  val firstName: String,
  val lastName: String): CustomValue<JsonDsl> {
  override val value = withJsonDsl {
    this["person"] = withJsonDsl {
      this["fn"] = firstName
      this["ln"] = lastName
      this["full_name"] = "$firstName $lastName"
    }
  }
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
create Kotlin DSLs for YAML based DSLs. So, you could use this to build Kotlin DSLs for things
like Github actions, Kubernetes, or other common things that use YAML.

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
from my kt-search library. It implements a minimal 
query and only supports one of the (many) types of queries
supported by Elasticsearch.
 
Like many real life
JSON, the Elasticsearch DSL is quite complicated and challenging 
to model. This is why I created this library.

The code below is a good illustration of several things you can
do in Kotlin to make life nice for your DSL users.

```kotlin
// using DslMarkers is useful with
// complicated DSLs
@DslMarker
annotation class SearchDSLMarker

interface QueryClauses

// abbreviated version of the
// Elasticsearch Query DSL in kt-search
class QueryDsl:
  JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase),
  // helper interface that we define
  // extension functions on
  QueryClauses
{
  // Elasticsearch often wraps objects in
  // another object. So we use a custom
  // setter here to hide that.
  var query: ESQuery
    get() {
      val map =
        this["query"] as Map<String, JsonDsl>
      val (name, details) = map.entries.first()
      // reconstruct the ESQuery
      return ESQuery(name, details)
    }
    set(value) {
      // queries extend ESQuery
      // which takes care of the wrapping
      // via wrapWithName
      this["query"] = value.wrapWithName()
    }}

// easy way to create a query
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
// this is one of the most basic queries
// in elasticsearch
@SearchDSLMarker
class TermQuery(
  field: String,
  value: String,
  termQueryConfig: TermQueryConfig = TermQueryConfig(),
  block: (TermQueryConfig.() -> Unit)? = null
) : ESQuery("term") {
  // on init, apply the block to the configuration and
  // assign it in the queryDetails from the parent
  init {
    queryDetails.put(field, termQueryConfig, PropertyNamingConvention.AsIs)
    termQueryConfig.value = value
    block?.invoke(termQueryConfig)
  }
}

// configuration for term queries
// this is a subset of the supported
// properties.
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

// of course users of this DSL would
// be storing json documents in elasticsearch
// and they probably have model classes with
// properties.
// so supporting property references
// for field names is a nice thing
fun QueryClauses.term(
  field: KProperty<*>,
  value: String,
  block: (TermQueryConfig.() -> Unit)? = null
) =
  TermQuery(field.name, value, block = block)
```

And this is how your users would use this DSL.

```kotlin
class MyModelClassInES(val myField: String)
val q = query {
  query = term(MyModelClassInES::myField, "some value")
}
val pretty = q.json(pretty = true)
println(pretty)
```

So, we created a query outer object with a query property.
And we assigned a term query instance to that.

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
a string literal. 

Of course, the Elasticsearch Query DSL support in 
[kt-search](https://github.com/jillesvangurp/kt-search) is a great 
reference for how to use JsonDsl.

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

