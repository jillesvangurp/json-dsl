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
