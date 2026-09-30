package com.coffeepeek.config

import org.gradle.api.JavaVersion

object Config {
    const val JVM_VERSION = "17"
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

    const val APPLICATION_ID = "com.coffeepeek"

    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
}
