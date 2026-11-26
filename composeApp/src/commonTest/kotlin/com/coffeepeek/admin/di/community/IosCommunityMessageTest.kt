package com.coffeepeek.admin.di.community

import kotlin.test.Test
import kotlin.test.assertEquals

class IosCommunityMessageTest {
    @Test fun queuedCallbacksCannotActOnAReplacedOrClosedMessage() {
        var current = true
        var dismissed = 0
        var acted = 0
        val source = IosCommunityMessage("message", "action", "dismiss", { current }, { dismissed++ }, { acted++ })
        source.performAction()
        source.dismiss()
        current = false
        source.performAction()
        source.dismiss()
        current = true
        source.close()
        source.performAction()
        source.dismiss()
        assertEquals(1, dismissed)
        assertEquals(1, acted)
    }

    @Test fun invalidAndLateGeometryCannotResizeTheNextMessage() {
        val source = IosCommunityMessage("message", null, "dismiss", { true }, {}, { error("No action") })
        source.performAction()
        source.heightChanged(120.0)
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0).forEach(source::heightChanged)
        source.close()
        source.heightChanged(240.0)
        assertEquals(120.0, source.height.value)
    }
}
