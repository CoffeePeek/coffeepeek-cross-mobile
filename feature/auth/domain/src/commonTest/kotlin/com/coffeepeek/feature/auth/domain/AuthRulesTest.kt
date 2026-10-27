package com.coffeepeek.feature.auth.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRulesTest {
    @Test fun emailIsRequiredTrimmedAndMatchedAgainstASimplePattern() {
        assertEquals(EmailIssue.Empty, validateEmail(""))
        assertEquals(EmailIssue.Empty, validateEmail("   "))
        assertNull(validateEmail(" person@example.com "))
        assertNull(validateEmail("a.b+tag@sub.example.co"))
        for (bad in listOf("person", "person@", "@example.com", "person@example", "person@example.c", "per son@example.com", "person@@example.com")) {
            assertEquals(EmailIssue.Invalid, validateEmail(bad), bad)
        }
    }

    @Test fun passwordLengthsAreSixForLoginAndEightForRegistrationAndSpacesCount() {
        assertEquals(PasswordIssue.Empty, validatePassword("", MIN_LOGIN_PASSWORD_LENGTH))
        assertEquals(PasswordIssue.Empty, validatePassword("      ", MIN_LOGIN_PASSWORD_LENGTH))
        assertEquals(PasswordIssue.TooShort(6), validatePassword("12345", MIN_LOGIN_PASSWORD_LENGTH))
        assertNull(validatePassword("123456", MIN_LOGIN_PASSWORD_LENGTH))
        assertNull(validatePassword(" 12345 ", MIN_LOGIN_PASSWORD_LENGTH))
        assertEquals(PasswordIssue.TooShort(8), validatePassword("1234567", MIN_REGISTRATION_PASSWORD_LENGTH))
        assertNull(validatePassword("12345678", MIN_REGISTRATION_PASSWORD_LENGTH))
    }

    @Test fun theEmailStepContinuesAsSoonAsSomethingIsTyped() {
        assertFalse(canContinueWithEmail(""))
        assertFalse(canContinueWithEmail("   "))
        assertTrue(canContinueWithEmail("x"))
    }

    @Test fun theRegistrationSubmitNeedsAShortNameALongPasswordAndTheTerms() {
        assertTrue(canSubmitRegistration("Al", "12345678", true))
        assertTrue(canSubmitRegistration(" Al ", "12345678", true))
        assertFalse(canSubmitRegistration("A", "12345678", true))
        assertFalse(canSubmitRegistration("  A  ", "12345678", true), "The name is trimmed first")
        assertFalse(canSubmitRegistration("Al", "1234567", true))
        assertFalse(canSubmitRegistration("Al", "12345678", false))
        assertTrue(canSubmitRegistration("Al", "        ", true), "Only the length of the password is checked here")
    }

    @Test fun theLastFailingCheckDecidesTheSingleRegistrationMessage() {
        class Case(val name: String, val email: String, val password: String, val terms: Boolean, val expected: RegistrationIssue?)
        val cases = listOf(
            Case("Coffee User", "person@example.com", "TestPassword123", true, null),
            Case("Coffee User", "person@example.com", "TestPassword123", false, RegistrationIssue.TermsRequired),
            Case("Coffee User", "person@example.com", "short", false, RegistrationIssue.PasswordTooShort),
            Case("Coffee User", "bad", "short", false, RegistrationIssue.EmailInvalid),
            Case("Coffee User", "", "short", false, RegistrationIssue.EmailRequired),
            Case("", "", "short", false, RegistrationIssue.NameRequired),
            Case("", "person@example.com", "TestPassword123", true, RegistrationIssue.NameRequired),
            Case("   ", "bad", "TestPassword123", true, RegistrationIssue.NameRequired),
            Case("Coffee User", "bad", "TestPassword123", true, RegistrationIssue.EmailInvalid),
        )
        for (case in cases) {
            val check = checkRegistration(case.name, case.email, case.password, case.terms)
            assertEquals(case.expected, check.reported, "${case.name}/${case.email}/${case.password}/${case.terms}")
            assertEquals(case.expected == null, check.valid)
        }
        val all = checkRegistration("", "bad", "short", false)
        assertEquals(EmailIssue.Invalid, all.email)
        assertEquals(PasswordIssue.TooShort(8), all.password)
        assertTrue(all.nameMissing && all.termsMissing, "Every field error is still marked")
    }

    @Test fun theStrengthMeterAddsOnePointEachForLengthADigitAndACapital() {
        assertEquals(PasswordStrength.None, passwordStrength(""))
        assertEquals(PasswordStrength.None, passwordStrength("abc"))
        assertEquals(PasswordStrength.Weak, passwordStrength("abcdefgh"))
        assertEquals(PasswordStrength.Weak, passwordStrength("abc1"))
        assertEquals(PasswordStrength.Weak, passwordStrength("Abc"))
        assertEquals(PasswordStrength.Medium, passwordStrength("password1"))
        assertEquals(PasswordStrength.Medium, passwordStrength("Abc1"))
        assertEquals(PasswordStrength.Strong, passwordStrength("Password1"))
    }
}
