@file:Suppress("UNCHECKED_CAST")

package com.jillesvangurp.jsondsl

import com.jillesvangurp.jsondsl.*
import kotlin.reflect.KProperty

// BEGIN kt-search-based-example
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

// END kt-search-based-example
