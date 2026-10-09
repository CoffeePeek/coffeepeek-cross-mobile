package com.coffeepeek.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Verifies definitions in the packaged DEX, not mere references on a compile classpath. */
@DisableCachingByDefault(because = "Runtime packaging verification must run for every assembled debug APK")
abstract class VerifyApkRuntimeClassesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    @get:Input
    abstract val requiredClasses: ListProperty<String>

    @TaskAction
    fun verify() {
        val apks = apkDirectory.get().asFile.listFiles { file -> file.extension == "apk" }.orEmpty()
        if (apks.isEmpty()) throw GradleException("No APK found to verify in ${apkDirectory.get().asFile}")
        val required = requiredClasses.get().associateWith { "L${it.replace('.', '/')};" }
        for (apk in apks) {
            val defined = ZipFile(apk).use { zip ->
                zip.entries().asSequence().filter { it.name.matches(Regex("classes(?:[0-9]+)?\\.dex")) }
                    .flatMap { entry -> zip.getInputStream(entry).use { readDexClassDescriptors(it.readBytes()) }.asSequence() }
                    .toSet()
            }
            val missing = required.filterValues { it !in defined }.keys
            if (missing.isNotEmpty()) throw GradleException(
                "${apk.name} is missing runtime classes: ${missing.joinToString()}. " +
                    "Compilation alone is insufficient. Rebuild the affected library and app outputs without build cache.",
            )
            logger.lifecycle("Verified runtime class definitions in ${apk.name}")
        }
    }
}

/** DEX class_defs -> type_ids -> string_ids. Referenced-but-undefined types do not count. */
internal fun readDexClassDescriptors(bytes: ByteArray): Set<String> {
    require(bytes.size >= 112 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(100, 101, 120, 10))) {
        "Invalid DEX header"
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val stringIdsOffset = buffer.getInt(0x3c)
    val typeIdsOffset = buffer.getInt(0x44)
    val classCount = buffer.getInt(0x60)
    val classDefsOffset = buffer.getInt(0x64)
    return (0 until classCount).mapTo(mutableSetOf()) { index ->
        val typeIndex = buffer.getInt(classDefsOffset + index * 32)
        val descriptorIndex = buffer.getInt(typeIdsOffset + typeIndex * 4)
        var start = buffer.getInt(stringIdsOffset + descriptorIndex * 4)
        // string_data_item starts with ULEB128 UTF-16 size, followed by a zero-terminated descriptor.
        while ((bytes[start++].toInt() and 0x80) != 0) Unit
        var end = start
        while (bytes[end].toInt() != 0) end++
        bytes.copyOfRange(start, end).toString(Charsets.UTF_8)
    }
}
