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

