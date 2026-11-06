package com.coffeepeek.core.network

import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.http.content.OutgoingContent

internal object CurlInterceptor {

    fun HttpRequestBuilder.asCurlString(): String {
        val method = method.value
        // Signed URLs, recovery tokens, and userinfo must never reach diagnostic logs.
        val target = url.build()
        val url = "${target.protocol.name}://${target.host}:${target.port}${target.encodedPath}"
        val headers = headers.entries().joinToString(" ") {
            "-H '${it.key}: ${it.key.redactHeaderValue(it.value.joinToString(", "))}'"
        }
        val body = body.let { body ->
            when (body) {
                is TextContent -> {
                    "-H 'Content-Type: ${body.contentType}; charset=${this.headers["Accept-Charset"]}'  -d '${body.text.redactSensitiveFields()}'"
                }
                is OutgoingContent.ByteArrayContent -> " --data-binary '<binary>'"
                is ByteArray -> " --data-binary '<${body.size} bytes>'"
                else -> ""
            }
        }

        return "cURL -X $method '$url' $headers $body"
    }

    private fun String.redactSensitiveFields(): String = replace(
        Regex(""""(password|newPassword|currentPassword|token|idToken|refreshToken|accessToken)"\s*:\s*"(?:\\.|[^"\\])*"""", RegexOption.IGNORE_CASE),
    ) { match -> """"${match.groupValues[1]}": "<redacted>"""" }

    private fun String.redactHeaderValue(value: String): String {
        val header = lowercase()
        return if (
            header == "authorization" ||
            header == "cookie" ||
            header == "set-cookie" ||
            header.contains("token")
        ) {
            "<redacted>"
        } else {
            value
        }
    }

}
