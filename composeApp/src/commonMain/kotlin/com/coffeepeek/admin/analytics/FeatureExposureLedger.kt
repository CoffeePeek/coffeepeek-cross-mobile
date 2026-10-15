package com.coffeepeek.admin.analytics

/** One foreground screen visit; UI supplies measured clipped area and uninterrupted visible time. */
internal class FeatureExposureLedger {
    private var visit = ""
    private val sent = mutableSetOf<String>()
    fun report(visit: String, feature: String, placement: String, fraction: Float, foreground: Boolean,
        unobscured: Boolean, visibleMillis: Long, send: () -> Boolean): Boolean {
        if (this.visit != visit) { sent.clear(); this.visit = visit }
        val key = "$feature:$placement"
        if (!fraction.isFinite() || fraction < .1f || fraction > 1f || !foreground || !unobscured || visibleMillis < 500 || key in sent) return false
        if (!send()) return false
        sent.add(key)
        return true
    }
}
