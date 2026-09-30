package com.coffeepeek.buildlogic

import org.gradle.api.artifacts.Dependency
import org.gradle.kotlin.dsl.DependencyHandlerScope

/** Feature dependencies declared at the top level belong to KMP commonMain. */
fun DependencyHandlerScope.implementation(notation: Any): Dependency? =
    add("commonMainImplementation", notation)

/** Public dependencies declared at the top level belong to KMP commonMain. */
fun DependencyHandlerScope.api(notation: Any): Dependency? =
    add("commonMainApi", notation)

/** Multiplatform unit tests use commonTest, not Android's JVM-only test source set. */
fun DependencyHandlerScope.testImplementation(notation: Any): Dependency? =
    add("commonTestImplementation", notation)

/** Keep platform-only UI tooling out of commonMain. */
fun DependencyHandlerScope.androidImplementation(notation: Any): Dependency? =
    add("androidMainImplementation", notation)

fun DependencyHandlerScope.androidInstrumentedTestImplementation(notation: Any): Dependency? =
    add("androidInstrumentedTestImplementation", notation)
