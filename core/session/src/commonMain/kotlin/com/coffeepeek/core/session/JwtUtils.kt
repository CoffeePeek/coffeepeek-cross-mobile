package com.coffeepeek.core.session

@OptIn(kotlin.time.ExperimentalTime::class)
object JwtUtils {
    fun extractUserId(accessToken: String): String? = runCatching {
        val json = decodePayloadJson(accessToken) ?: return null
        readJsonString(json, "sub")
    }.getOrNull()

    fun extractExp(accessToken: String): Long? = runCatching {
        val json = decodePayloadJson(accessToken) ?: return null
        readJsonLong(json, "exp")
    }.getOrNull()

    fun isAccessTokenExpired(accessToken: String, leewaySeconds: Long = 30): Boolean {
        val exp = extractExp(accessToken) ?: return false
        val nowSeconds = kotlin.time.Clock.System.now().epochSeconds
        return nowSeconds >= exp - leewaySeconds
    }

    private fun decodePayloadJson(accessToken: String): String? {
        val payload = accessToken.split('.').getOrNull(1) ?: return null
        val padded = payload.padEnd((payload.length + 3) / 4 * 4, '=')
        return decodeBase64Url(padded).decodeToString()
    }

    private fun readJsonString(json: String, key: String): String? {
        val token = "\"$key\":\""
        val start = json.indexOf(token)
        if (start < 0) return null
        val from = start + token.length
        val end = json.indexOf('"', from)
        if (end < 0) return null
        return json.substring(from, end)
    }

    private fun readJsonLong(json: String, key: String): Long? {
        val token = "\"$key\":"
        val start = json.indexOf(token)
        if (start < 0) return null
        val from = start + token.length
        val end = json.indexOfFirst(from) { it == ',' || it == '}' }
        if (end < 0) return null
        return json.substring(from, end).trim().toLongOrNull()
    }

    private fun String.indexOfFirst(startIndex: Int, predicate: (Char) -> Boolean): Int {
        for (index in startIndex until length) {
            if (predicate(this[index])) return index
        }
        return -1
    }

    private fun decodeBase64Url(input: String): ByteArray {
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val normalized = input.replace('-', '+').replace('_', '/')
        val output = mutableListOf<Byte>()
        var buffer = 0
        var bits = 0
        for (char in normalized) {
            if (char == '=') break
            val value = table.indexOf(char)
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }
}
