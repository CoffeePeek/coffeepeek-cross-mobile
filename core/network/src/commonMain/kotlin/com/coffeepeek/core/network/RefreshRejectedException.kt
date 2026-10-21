package com.coffeepeek.core.network

/** Definitive credential rejection, distinct from transport and temporary server failures. */
class RefreshRejectedException : Exception("Refresh credentials were rejected")
