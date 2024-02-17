package com.jillesvangurp.jsondsl

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class YamlTest {
    val serializer = SimpleYamlSerializer()
    @Test
    fun shouldProduceYaml() {
        val q = query {
            query = term("foo","bar") {
                boost=2.0
            }
        }
        val yaml = serializer.serialize(q)
        println(yaml)
    }

    @Test
    fun shouldNotAlterSimpleAlphanumericStrings() {
        val input = "simpleString123"
        input.yamlEscape() shouldBe "simpleString123"
    }

    @Test
    fun shouldQuoteStringsWithSpecialCharacters() {
        val input = "special:string, with:special#characters"
        input.yamlEscape() shouldBe "\"special:string, with:special#characters\""
    }

    @Test
    fun shouldQuoteStringsThatAreBooleanLiterals() {
        val trueString = "true"
        trueString.yamlEscape() shouldBe "\"true\""

        val falseString = "false"
        falseString.yamlEscape() shouldBe "\"false\""
    }

    @Test
    fun shouldQuoteStringsWithLeadingOrTrailingSpaces() {
        val input = " leadingOrTrailingSpaces "
        input.yamlEscape() shouldBe "\" leadingOrTrailingSpaces \""
    }

    @Test
    fun shouldEscapeBackslashesAndQuotes() {
        val input = "string with \\backslash and \"quote\""
        input.yamlEscape() shouldBe "\"string with \\\\backslash and \\\"quote\\\"\""
    }

    @Test
    fun shouldQuoteEmptyStrings() {
        val input = ""
        input.yamlEscape() shouldBe "\"\""
    }

    @Test
    fun shouldQuoteNumericStrings() {
        val input = "12345"
        input.yamlEscape() shouldBe "\"12345\""
    }

    @Test
    fun shouldQuoteStringsThatLookLikeNumbers() {
        val input = "12.345"
        input.yamlEscape() shouldBe "\"12.345\""
    }

    @Test
    fun thisShouldBeValid() {
        val complexData = mapOf(
            "specialCharacters" to "special:string, with:special#characters",
            "booleanLiteral" to "true",
            "numericString" to "12345",
            "multilineString" to """
        Line one
        Line two
        Line three
    """.trimIndent(),
            "listOfValues" to listOf(
                "simpleValue",
                "12.345",
                mapOf(
                    "nestedMap" to mapOf(
                        "nestedKey" to "nested:value",
                        "emptyString" to "",
                        "spaceString" to " ",
                        "quotedString" to "string with \"quotes\" and \\backslashes\\"
                    ),
                    "nestedList" to listOf(
                        "item1",
                        "item with spaces",
                        "true", // Boolean literal as a string
                        "null" // Null literal as a string
                    )
                )
            ),
            "leadingTrailingSpaces" to " leadingOrTrailingSpaces ",
            "booleanTrue" to true, // Actual boolean value
            "booleanFalse" to false, // Actual boolean value
            "nullValue" to null, // Actual null value
            "numericValue" to 67890,
            "lorem" to loremIpsumSample
        )

        val yaml = serializer.serialize(withJsonDsl {
            this["complex"] = complexData
        })
        // FIXME assert some stuff to validate this
        // for now, I've passed this through yamllint.com and it seems OK
        println(yaml)
    }
}



val loremIpsumText = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum. 
Sed ac felis sit amet ligula pharetra condimentum. Morbi in sem quis dui placerat ornare. Pellentesque odio nisi, 
euismod in, pharetra a, ultricies in, diam. Sed arcu. Cras consequat.

    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum. 
    Sed ac felis sit amet ligula pharetra condimentum. Morbi in sem quis dui placerat ornare. Pellentesque odio nisi, 
    euismod in, pharetra a, ultricies in, diam. Sed arcu. Cras consequat.

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum. 
Sed ac felis sit amet ligula pharetra condimentum. Morbi in sem quis dui placerat ornare. Pellentesque odio nisi, 
euismod in, pharetra a, ultricies in, diam. Sed arcu. Cras consequat.



            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus lacinia odio vitae vestibulum vestibulum. 
            Sed ac felis sit amet ligula pharetra condimentum. Morbi in sem quis dui placerat ornare. Pellentesque odio nisi, 
            euismod in, pharetra a, ultricies in, diam. Sed arcu. Cras consequat.


The end.
""".trimIndent()

val loremIpsumSample = withJsonDsl {
    this["foo"] = withJsonDsl {
        this["bar"] = loremIpsumText
        this["foo"] = listOf(
            loremIpsumText,
            loremIpsumText,
            loremIpsumText,
            loremIpsumText,
        )
    }
}
