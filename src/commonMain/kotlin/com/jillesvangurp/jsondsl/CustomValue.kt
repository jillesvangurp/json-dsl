package com.jillesvangurp.jsondsl

/**
 * Used for custom serialization where toString method does not provide the correct value.
 *
 * Note, you can use any type for the value. Including for example JsonDsl. So, you can construct
 * arbitrarily complex custom values for your classes.
 */
interface CustomValue<T> {
    val value: T
}