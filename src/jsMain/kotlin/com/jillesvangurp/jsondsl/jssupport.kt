package com.jillesvangurp.jsondsl

/**
 * Extension function to convert the Kotlin JsonDsl to an equivalent Javascript object that you
 * can pass as a parameter to Javascript libraries.
 *
 * This enables you to use JsonDsl as an alternative to complex type mappings for such libraries.
 * Since JSON is effectively valid javascript, a lot of javascript libraries use some form
 * of JSON as their input.
 *
 * Returns a Javascript object from the Kotlin JsonDsl.
 */
fun JsonDsl.toJsObject(): dynamic {
    fun convert(obj: Any?): dynamic {
        return when (obj) {
            null -> null
            is RawJson -> JSON.parse(obj.value)
            is Number -> obj.toDouble() // some number types come out looking a bit funny otherwise
            is Boolean, is String -> obj
            is Char -> obj.toString()
            is Map<*, *> -> {
                val jsObj = js("{}")
                obj.forEach { (key, value) ->
                    if (key is String) {
                        jsObj[key] = convert(value)
                    }
                }
                jsObj
            }
            is List<*> -> obj.map { convert(it) }.toTypedArray()
            is Set<*> -> obj.map { convert(it) }.toTypedArray()
            is Array<*> -> obj.map { convert(it) }.toTypedArray()
            is Iterator<*> -> obj.asSequence().map { convert(it) }.toList().toTypedArray()
            is Iterable<*> -> obj.map { convert(it) }.toTypedArray()
            is CustomValue<*> -> convert(obj.value)
            else -> obj.toString() // Fallback for unknown types
        }
    }
    return convert(this)
}