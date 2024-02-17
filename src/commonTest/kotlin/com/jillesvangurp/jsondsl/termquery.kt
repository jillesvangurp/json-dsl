@file:Suppress("UNCHECKED_CAST")

package com.jillesvangurp.jsondsl

import com.jillesvangurp.jsondsl.*
import kotlin.reflect.KProperty

// BEGIN kt-search-based-example
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

// END kt-search-based-example
