package com.coffeepeek.feature.discovery.domain.model

enum class FailureKind { SignInRequired, Other }

/**
 * Why a request failed. [serverMessage] is the server's own wording when it sent one; the UI shows it or a default text, never the transport error.
 * [FailureKind.SignInRequired] is an answer 401: the visited and favorites filters need an account.
 */
class DiscoveryFailure(val kind: FailureKind, val serverMessage: String? = null) : Exception(kind.name)
