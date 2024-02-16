package com.jillesvangurp.jsondsl.readme

import com.jillesvangurp.jsondsl.*
import com.jillesvangurp.kotlin4example.SourceRepository
import kotlin.test.Test
import java.io.File
import kotlin.math.PI
import kotlin.reflect.KProperty

const val githubLink = "https://github.com/formation-res/pg-docstore"

val sourceGitRepository = SourceRepository(
    repoUrl = githubLink,
    sourcePaths = setOf("src/commonMain/kotlin", "src/jvmTest/kotlin")
)

class ReadmeGenerationTest {

    @Test
    fun `generate docs`() {
        File(".", "README.md").writeText(
            """
            # JsonDsl

        """.trimIndent().trimMargin() + "\n\n" + readmeMd.value
        )
    }
}

val readmeMd = sourceGitRepository.md {
    includeMdFile("intro.md")

    section("Examples") {
        subSection("Hello World") {
            example {
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
            }.let {
                +"""
                   The json extension function uses the json serializer to produce 
                   pretty printed json:
                """.trimIndent()

                mdCodeBlock(it.stdOut, type = "json")
            }
        }

        subSection("Common Kotlin Types") {
            +"""
                JSON is a fairly simple data format. There are numbers, booleans, strings, lists and dictionaries.
                
                Kotlin is of course a bit richer and mapping that to JSON is key to providing rich Kotlin DSL.
                
                JsonDsl does a best effort to do map Kotlin types correctly to the intended JSON equivalent.              
            """.trimIndent()

            example {
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
            }.result.getOrThrow()!!.let {
                +"""
                    This does the right things to all the Kotlin types, including `Any`:
                """.trimIndent()
                mdCodeBlock(it.json(true), type = "json")
            }
        }
        subSection("Manipulating the Map directly") {
            +"""
                All JsonDsl does is provide a delegate implementation of a `MutableMap<String, Any?>`.
                
                So all sub classes have direct access to that map. And you can put whatever into it.
            """.trimIndent()
            example {
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
            }.result.getOrThrow()!!.let {
                mdCodeBlock(it.json(true), type = "json")
            }
        }
        subSection("snake_casing, custom names, defaults") {
            +"""
                A lot of JSON dialects use snake cased dictionary keys. Kotlin of course uses 
                camel case for its identifiers and it has certain things that you can't redefine.
                
                Like the `size` property on `Map`.
                
            """.trimIndent()

            example {
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
            }.result.getOrThrow()!!.let {
                mdCodeBlock(it.json(true), type = "json")
            }
        }
    }
    section("A real life, complex example") {
        +"""
            Here's a bit of the kt-search Kotlin DSL that I lifted
            from my kt-search library. It defines a minimal 
            query that only adds one of the (many) types of queries
            supported by Elasticsearch. Unfortunately, like many real life
            JSON, the Elasticsearch DSL is quite complicated and challenging 
            to model. 
            
            However, JsonDsl provides you all you need to wrap this with 
            a nice type safe Kotlin DSL.            
        """.trimIndent()
        exampleFromSnippet(ReadmeGenerationTest::class, "kt-search based example", allowLongLines = true)
        +"""
            And this is how you would use it.
        """.trimIndent()
        example {
            class MyModelClassInES(val myField: String)
            query {
                query = term(MyModelClassInES::myField, "some value")
            }
        }.result.getOrThrow()!!.let { q ->
            +"""
                In JSON form this looks as follows:                
            """.trimIndent()
            mdCodeBlock(q.toString(),"json")
            +"""
                Note how it correctly wrapped the term query with an object. And how it correctly 
                assigns the `TermConfiguration` in an object that has the field value as the key.
                
                Also note how we use a property reference here to avoid having to use 
                a string literal. The Elasticsearch Query DSL support in [kt-search]() is a great 
                reference for how to use JsonDsl.
            """.trimIndent()
        }
    }
}

// BEGIN kt-search based example
@DslMarker
annotation class SearchDSLMarker

interface QueryClauses

// common parent for all query variants
@SearchDSLMarker
open class ESQuery(
    val name: String,
    val queryDetails: JsonDsl = JsonDsl()
) : IJsonDsl by queryDetails {

    // Elasticsearch wraps everything in an outer object
    // with the name as its only key
    fun wrapWithName(): Map<String, Any?> = dslObject { this[name] = queryDetails }

    override fun toString(): String {
        return wrapWithName().toString()
    }
}

// configuration for term queries
class TermQueryConfig : JsonDsl() {
    var value by property<String>()
    var boost by property<Double>()
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
        put(field, termQueryConfig, PropertyNamingConvention.AsIs)
        termQueryConfig.value = value
        block?.invoke(termQueryConfig)
    }
}

fun QueryClauses.term(
    field: KProperty<*>,
    value: String,
    block: (TermQueryConfig.() -> Unit)? = null
) =
    TermQuery(field.name, value, block = block)

fun QueryClauses.term(
    field: String,
    value: String,
    block: (TermQueryConfig.() -> Unit)? = null
) =
    TermQuery(field, value, block = block)

// abbreviated version of the
// Elasticsearch Query DSL in kt-search
@Suppress("UNCHECKED_CAST")
class QueryDsl:
    JsonDsl(namingConvention =
      PropertyNamingConvention.ConvertToSnakeCase),
    QueryClauses
{
    // Elasticsearch has this object in
    // an object kind of thing that we need to emulate.
    var query: ESQuery
        get() {
            val map =
                this["query"] as Map<String, JsonDsl>
            val (name, details) = map.entries.first()
            return ESQuery(name, details)
        }
        set(value) {
            this["query"] = value.wrapWithName()
        }}

fun query(block: QueryDsl.()->Unit): QueryDsl {
    return QueryDsl().apply(block)
}
// END kt-search based example
