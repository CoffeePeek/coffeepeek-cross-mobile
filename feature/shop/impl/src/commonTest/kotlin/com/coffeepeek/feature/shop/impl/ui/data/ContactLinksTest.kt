package com.coffeepeek.feature.shop.impl.ui.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactLinksTest {
    @Test fun websiteAddsSchemeAndRejectsNonWebSchemes() {
        assertEquals("https://coffeepeek.app/menu", websiteLink("coffeepeek.app/menu")?.target)
        assertEquals("coffeepeek.app/menu", websiteLink("https://www.coffeepeek.app/menu")?.label)
        assertNull(websiteLink("javascript:alert(1)"))
        assertNull(websiteLink("https://user@coffeepeek.app/"))
        assertNull(websiteLink("https://coffeepeek.app/\nmalicious"))
    }

    @Test fun instagramOnlyAcceptsHandleOrInstagramDomain() {
        assertEquals("https://instagram.com/coffee.peek", instagramLink("@coffee.peek")?.target)
        assertEquals("@coffee", instagramLink("https://www.instagram.com/coffee/?q=1")?.label)
        assertNull(instagramLink("https://instagram.com.evil.test/coffee"))
        assertNull(instagramLink("@bad handle"))
    }

    @Test fun phoneAndEmailProduceTypedTargetsWithoutControlCharacters() {
        assertEquals("tel:+375291234567", phoneLink("+375 (29) 123-45-67")?.target)
        assertEquals("mailto:hello@coffeepeek.app", emailLink(" hello@coffeepeek.app ")?.target)
        assertNull(phoneLink("123\n456"))
        assertNull(emailLink("hello@coffeepeek.app\r\nBcc:evil@example.com"))
    }
}
