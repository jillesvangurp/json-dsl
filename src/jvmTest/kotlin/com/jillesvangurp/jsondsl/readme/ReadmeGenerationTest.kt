package com.jillesvangurp.jsondsl.readme

import com.jillesvangurp.jsondsl.*
import com.jillesvangurp.kotlin4example.SourceRepository
import kotlin.test.Test
import java.io.File
import kotlin.math.PI
import kotlin.reflect.KProperty

const val githubLink = "https://github.com/jillesvangurp/json-dsl"

val sourceGitRepository = SourceRepository(
    repoUrl = githubLink,
    sourcePaths = setOf("src/commonMain/kotlin", "src/commonTest/kotlin","src/jvmTest/kotlin")
)

// grades-enum
enum class Grades(override val value: Double) : CustomValue<Double> {
    Excellent(7.0),
    Pass(5.51),
    Fail(3.0),
    ;
}
// grades-enum

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
        +"""
            All the examples in this README are implemented using
                my [kotlin4example](https://github.com/jillesvangurp/kotlin4example) library. You can find 
                the source code that generates this README ${mdLinkToSelf("here")}. 
        """.trimIndent()
        subSection("Hello World") {
            +"""
                Let's start with a simple example.
            """.trimIndent()
            example {
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
            }.let {
                +"""
                   The json extension function uses a json serializer to produce 
                   pretty printed json:
                """.trimIndent()

                mdCodeBlock(it.stdOut, type = "json")

                +"""
                    There is also a YAML serializer. More on that below.
                """.trimIndent()
            }
        }

        subSection("Common Kotlin Types") {
            +"""
                JSON is a fairly simple data format. It has numbers, booleans, strings, lists and dictionaries. And null
                values. 
                
                Kotlin has a bit richer type system and mapping that to JSON is key to providing rich Kotlin DSL.                
                
                JsonDsl does a best effort to do map Kotlin types correctly to the intended JSON equivalent. 
                           
                It understands all the primitives, Maps and Lists. But also Arrays, Sets, Sequences.                
                And of course other JsonDsl classes, so you can nest them.  And it falls back to using 
                `toString()` for everything else.              
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
                    idontknow = mapOf(
                        "arrays" to arrayOf(
                            1, 2, "3", 4.0,
                            mapOf("this" to "is valid JSON")
                        ),
                        "sequences" to sequenceOf(1,"2",3.0)
                    )
                }
            }.result.getOrThrow()!!.let {
                +"""
                    This does the right things with all the Kotlin types, including `Any`:
                """.trimIndent()
                mdCodeBlock(it.json(true), type = "json")
            }
        }
        subSection("Manipulating the Map directly") {
            +"""
                As mentioned, JsonDsl delegates the storing of properties to a `MutableMap<String, Any?>`. 
                
                So, all sub classes have direct access to that map. And you can put anything you want into it.
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
                            "you":"can even add json in string form",
                            "RawJson":"is a value class"
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
                
                Like the `size` property on `Map`, which is implemented by JsonDsl; or certain keywords.
                
            """.trimIndent()

            example {
                class MyDsl : JsonDsl(
                    namingConvention = PropertyNamingConvention.ConvertToSnakeCase
                ) {
                    // this will be snake cased
                    var camelCase by property<Boolean>()
                    var mySize by property<Int>(
                        customPropertyName = "size"
                    )
                    var myVal by property<String>(
                        customPropertyName = "val"
                    )
                    // explicitly set name and provide a default
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
            }.result.getOrThrow()!!.let {
                mdCodeBlock(it.json(true), type = "json")
            }
        }
        subSection("Custom values") {
            +"""
                Sometimes you might want to have the serialized version of a value be different
                from the kotlin type that you are using. For this we have added the 
                CustomValue interface.
                
                A simple use case for this could be Enums:
                                
            """.trimIndent()

            exampleFromSnippet(ReadmeGenerationTest::class,"grades-enum")
            example {
                println(withJsonDsl {
                    this["grade"] = Grades.Excellent
                }.json(true))
            }.let {
                +"""
                    Note how the grade's Double value is used instead of the name.
                    
                    The withJsonDsl function is a simple extension function that 
                    creates a JsonDsl for you and applies the block to it.
                """.trimIndent()
                mdCodeBlock(it.stdOut,"json")
            }

            +"""
                You can also construct more complex ways to serialize your classes.
            """.trimIndent()

            example {
                data class Person(
                    val firstName: String,
                    val lastName: String): CustomValue<List<String>> {
                    override val value =
                        listOf(firstName, lastName)
                }

                withJsonDsl {
                    this["person"] = Person("Jane", "Doe")
                }.json(true)
            }

            +"""
                And of course your custom value can be a JsonDsl too.
            """.trimIndent()
            example {
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
            }

            +"""
                You can also rely on the `toString()` function:
            """.trimIndent()
            example {
                data class FooBar(val foo:String="foo", val bar: String="bar")
                println(withJsonDsl {
                    this["foo"]=FooBar()
                })
            }.let {
                +"""
                    Note how it simply uses `toString()` on the data class
                """.trimIndent()
                mdCodeBlock(it.stdOut,"json")
                +"""
                    This also works for things like enums, value classes, and other Kotlin language constructs.
                """.trimIndent()
            }
        }
    }
    section("YAML") {
            +"""
                While initially written to support JSON, I also added a YAML serializer that you may use to 
                create Kotlin DSLs for YAML based DSLs. So, you could use this to build Kotlin DSLs for things
                like Github actions, Kubernetes, or other common things that use YAML.
            """.trimIndent()

            example {
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
            }.let {
                +"""
                    This prints the YAML below:
                """.trimIndent()

                mdCodeBlock(it.stdOut,"yaml", reIndent = false)
            }
            +"""
                There are other tree like formats that might be supported in the future like TOML, properties, 
                and other formats. I welcome pull requests for this provided they don't add any library dependencies.
            """.trimIndent()

    }
    section("A real life, complex example") {
        +"""
            Here's a bit of the kt-search Kotlin DSL that I lifted
            from my kt-search library. It implements a minimal 
            query and only supports one of the (many) types of queries
            supported by Elasticsearch.
             
            Like many real life
            JSON, the Elasticsearch DSL is quite complicated and challenging 
            to model. This is why I created this library.
            
            The code below is a good illustration of several things you can
            do in Kotlin to make life nice for your DSL users.
        """.trimIndent()
        exampleFromSnippet("com/jillesvangurp/jsondsl/termquery.kt", "kt-search-based-example", allowLongLines = true)
        +"""
            And this is how your users would use this DSL.
        """.trimIndent()
        example {
            class MyModelClassInES(val myField: String)
            val q = query {
                query = term(MyModelClassInES::myField, "some value")
            }
            val pretty = q.json(pretty = true)
            println(pretty)
        }.let {
            +"""
                So, we created a query outer object with a query property.
                And we assigned a term query instance to that.
                
                In JSON form this looks as follows:                
            """.trimIndent()
            mdCodeBlock(it.stdOut,"json")
            +"""
                Note how it correctly wrapped the term query with an object. And how it correctly 
                assigns the `TermConfiguration` in an object that has the field value as the key.
                Also note how we use a property reference here to avoid having to use 
                a string literal. 
                
                Of course, the Elasticsearch Query DSL support in 
                [kt-search](https://github.com/jillesvangurp/kt-search) is a great 
                reference for how to use JsonDsl.
            """.trimIndent()
        }
    }
    includeMdFile("outro.md")
}

