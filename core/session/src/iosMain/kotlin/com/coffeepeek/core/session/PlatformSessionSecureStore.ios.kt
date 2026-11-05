@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.core.session

import cnames.structs.__CFData
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** A delete succeeded when the item is gone: removed now, or not there to begin with. */
internal fun keychainDeleteSucceeded(status: Int): Boolean = status == errSecSuccess || status == errSecItemNotFound

private class IosKeychainSessionStore : SessionSecureStore {
    private val state = MutableStateFlow(readFromKeychain())

    override suspend fun read(): SessionCredentials? = state.value ?: readFromKeychain().also { state.value = it }

    override suspend fun write(data: SessionCredentials?) {
        val deleteStatus = baseQuery().useAndRelease { query -> SecItemDelete(query) }
        // A failed removal must not be reported as a completed logout: the old item would be read again after a restart.
        if (data == null) check(keychainDeleteSucceeded(deleteStatus)) { "Unable to remove secure session from Keychain" }

        if (data != null) {
            val bytes = SessionJson.encodeToString(data).encodeToByteArray()
            val valueData = bytes.usePinned { pinned ->
                CFDataCreate(
                    allocator = null,
                    bytes = pinned.addressOf(0).reinterpret(),
                    length = bytes.size.toLong(),
                )
            } ?: error("Unable to encode secure session")

            try {
                baseQuery().useAndRelease { attributes ->
                    CFDictionarySetValue(attributes, kSecValueData, valueData)
                    CFDictionarySetValue(
                        attributes,
                        kSecAttrAccessible,
                        kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    )
                    check(SecItemAdd(attributes, null) == errSecSuccess) {
                        "Unable to write secure session to Keychain"
                    }
                }
            } finally {
                CFRelease(valueData)
            }
        }
        state.value = data
    }

    override fun observe(): Flow<SessionCredentials?> = state.asStateFlow()

    private fun readFromKeychain(): SessionCredentials? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = baseQuery().useAndRelease { query ->
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            SecItemCopyMatching(query, result.ptr)
        }
        if (status != errSecSuccess) return@memScoped null

        val retainedResult = result.value ?: return@memScoped null
        try {
            val data = retainedResult.reinterpret<__CFData>()
            val size = CFDataGetLength(data).toInt()
            val raw = CFDataGetBytePtr(data)?.readBytes(size)?.decodeToString()
                ?: return@memScoped null
            runCatching { SessionJson.decodeFromString<SessionCredentials>(raw) }.getOrNull()
        } finally {
            CFRelease(retainedResult)
        }
    }

    private fun baseQuery(): CFMutableDictionaryRef {
        val query = CFDictionaryCreateMutable(
            allocator = null,
            capacity = 4,
            keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
            valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: error("Unable to create Keychain query")

        val service = CFStringCreateWithCString(null, SERVICE, kCFStringEncodingUTF8)
            ?: error("Unable to create Keychain service key")
        val account = CFStringCreateWithCString(null, ACCOUNT, kCFStringEncodingUTF8)
            ?: error("Unable to create Keychain account key")
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
        } finally {
            CFRelease(service)
            CFRelease(account)
        }
        return query
    }

    private inline fun <T> CFMutableDictionaryRef.useAndRelease(
        block: (CFMutableDictionaryRef) -> T,
    ): T = try {
        block(this)
    } finally {
        CFRelease(this)
    }

    private companion object {
        const val SERVICE = "com.coffeepeek.session"
        const val ACCOUNT = "current"
    }
}

actual fun createPlatformSessionSecureStore(
    platformContext: Any?,
    legacyRows: LegacyCredentialRows,
): SessionSecureStore = IosKeychainSessionStore()
