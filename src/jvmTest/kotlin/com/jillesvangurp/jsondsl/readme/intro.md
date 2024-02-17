[![Process Pull Request](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml/badge.svg)](https://github.com/jillesvangurp/json-dsl/actions/workflows/pr_master.yaml)

JsonDsl is a multi platform kotlin library that helps you build Kotlin DSLs for JSON and YAML dialects. 
The DSLs are easy to extend with custom fields by users via a MutableMap.

A DSL (Domain Specific Language) differs from General Purpose Languages, such as Kotlin, in that a DSL is intended to program or drive a tool or framework for some domain or API. Kotlin like several other languages is suitable for creating internal DSLs that (ab)use the syntax of the host language to implement a DSL.

## The problem

Of course creating model classes for your json domain model and annotating them with annotations for e.g. kotlinx.serialization is a valid way to start creating a DSL for your JSON dialect of choice.

However, this has some limitations. What if your JSON dialect evolves and somebody adds some new features? Unless you change your model class, it would not be possible to access such new features via the Kotlin DSL.

This library started out as few classes in my [kt-search](https://github.com/jillesvangurp/kt-search) project, which implements an Elasticsearch and Opensearch client. Elasticsearch has several JSON dialects that are used for querying, defining mappings, and a few other things. Especially the query language has a large number of features and is constantly evolving. 

Not only do I have to worry about upstream additions to OpenSearch and Elasticsearch. I also have to worry about supporting query and mapping features added via custom plugins. This is of course very challenging.

## Strongly typed and Flexible

The key feature in json-dsl is that it uses a `MutableMap` for storing property values. This enables you
to define classes with properties that delegate storing their value to this map. For anything that your
classes don't implement, the user can always write to the map directly using a simple `put`.

This gives users a nice fallback for things your DSL classes don't implement and it relieves Kotlin DSL implementors from having to provide support for every new feature the upstream JSON dialect has or adds over time.

## Gradle

This library is published to our own maven repository.

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

And then you can add the dependency:

```kotlin
    // check the latest release tag for the latest version
    implementation("com.jillesvangurp:json-dsl:1.x.y")
```
