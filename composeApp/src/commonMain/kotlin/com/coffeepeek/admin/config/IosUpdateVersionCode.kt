package com.coffeepeek.admin.config

private val iosBuildNumber = Regex("[0-9]+(?:\\.[0-9]+)?")

/** CI builds are run.attempt; the update policy compares run numbers, independently of upload retries. */
internal fun iosUpdateVersionCode(bundleVersion: String): Long? {
    val build = bundleVersion.trim()
    if (!iosBuildNumber.matches(build)) return null
    return build.substringBefore('.').toLongOrNull()
}
