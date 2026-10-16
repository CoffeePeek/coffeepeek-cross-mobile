package com.coffeepeek.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerifyApkRuntimeClassesTaskTest {
    @Test fun onlyDefinedTypesCountEvenWhenMissingClassDescriptorIsReferenced() {
        val bytes = dexWithTypes(listOf("Ldefined/Present;", "Lreferenced/Missing;"), listOf(0))
        assertEquals(setOf("Ldefined/Present;"), readDexClassDescriptors(bytes))
    }

    @Test fun readsSeveralDefinitionsAndMultibyteStringLengthPrefix() {
        val longDescriptor = "L${"long/package/".repeat(12)}Present;"
        val descriptors = listOf("Lone/Present;", longDescriptor)
        assertEquals(descriptors.toSet(), readDexClassDescriptors(dexWithTypes(descriptors, listOf(1, 0))))
    }

    @Test fun emptyDefinitionsAndInvalidHeadersAreNotMistakenForPresentClasses() {
        assertEquals(emptySet(), readDexClassDescriptors(dexWithTypes(listOf("Lreferenced/Missing;"), emptyList())))
        assertFailsWith<IllegalArgumentException> { readDexClassDescriptors(byteArrayOf(1, 2, 3)) }
    }

    private fun dexWithTypes(descriptors: List<String>, definedTypeIndices: List<Int>): ByteArray {
        val stringsOffset = 112
        val typesOffset = stringsOffset + descriptors.size * 4
        val classesOffset = typesOffset + descriptors.size * 4
        val dataOffset = classesOffset + definedTypeIndices.size * 32
        val bytes = ByteArray(dataOffset + descriptors.sumOf { it.length + 6 })
        "dex\n035\u0000".toByteArray().copyInto(bytes)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x3c, stringsOffset)
        buffer.putInt(0x44, typesOffset)
        buffer.putInt(0x60, definedTypeIndices.size)
        buffer.putInt(0x64, classesOffset)
        var offset = dataOffset
        descriptors.forEachIndexed { index, descriptor ->
            buffer.putInt(stringsOffset + index * 4, offset)
            buffer.putInt(typesOffset + index * 4, index)
            var length = descriptor.length
            do {
                val part = length and 0x7f
                length = length ushr 7
                bytes[offset++] = (part or if (length > 0) 0x80 else 0).toByte()
            } while (length > 0)
            descriptor.toByteArray().copyInto(bytes, offset)
            offset += descriptor.length + 1
        }
        definedTypeIndices.forEachIndexed { index, type -> buffer.putInt(classesOffset + index * 32, type) }
        return bytes
    }
}
