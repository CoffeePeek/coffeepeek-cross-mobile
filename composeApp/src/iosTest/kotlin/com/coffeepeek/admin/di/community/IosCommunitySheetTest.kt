package com.coffeepeek.admin.di.community

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.community.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosCommunitySheetTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = SheetAccounts()
    private val sheets = mutableListOf<IosCommunitySheet>()

    @BeforeTest fun prepare() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun finish() {
        sheets.forEach { it.close() }
        Dispatchers.resetMain()
    }

    private fun sheet(busy: Boolean = false, calls: MutableList<String>) =
        IosCommunitySheet("{\"active\":true}", busy, { action, value -> calls += "$action:$value" }, sessions)
            .also(sheets::add)

    @Test fun savingBlocksAllCallbacksIncludingDismissAndCatalogRetry() = runTest(dispatcher) {
        val calls = mutableListOf<String>()
        val source = sheet(true, calls)
        listOf("text", "submit", "dismiss", "retry", "coffee", "drink").forEach { source.perform(it, "5") }
        assertTrue(calls.isEmpty())
        source.update("{}", false) { action, _ -> calls += action }
        source.perform("retry", "")
        assertEquals(listOf("retry"), calls)
    }

    @Test fun disposedBodyCannotMutateAReopenedSheet() = runTest(dispatcher) {
        val calls = mutableListOf<String>()
        val old = sheet(calls = calls)
        old.perform("dismiss", "")
        old.close() // Shared owner accepted dismissal and composition disposed its body.
        val reopened = sheet(calls = calls)
        old.perform("text", "late")
        old.update("{}", false) { _, _ -> error("revoked") }
        reopened.perform("text", "new")
        assertEquals(listOf("dismiss:", "text:new"), calls)
        assertFalse(Json.parseToJsonElement(old.current()).jsonObject.getValue("active").jsonPrimitive.boolean)
    }

    @Test fun sameAccountReloginRevokesBeforeTheFlowCollectorRuns() = runTest(dispatcher) {
        val calls = mutableListOf<String>()
        val source = sheet(calls = calls)
        sessions.replace("one")
        source.perform("submit", "")
        assertTrue(calls.isEmpty())
        assertEquals("{\"active\":false}", source.current())
        runCurrent()
        assertEquals("{\"active\":false}", source.current())
    }

    @Test fun accountReplacementClearsAnObservedDraft() = runTest(dispatcher) {
        val source = sheet(calls = mutableListOf())
        val frames = mutableListOf<String>()
        val observation = source.observe(frames::add)
        runCurrent()
        sessions.replace("two")
        runCurrent()
        assertEquals("{\"active\":false}", frames.last())
        observation.cancel()
    }

    @Test fun closingAndGuestsCannotSubmitOrKeepDraftState() = runTest(dispatcher) {
        val calls = mutableListOf<String>()
        val source = sheet(calls = calls)
        source.close()
        source.perform("submit", "")
        sessions.replace(null)
        val guest = sheet(calls = calls)
        guest.perform("submit", "")
        assertTrue(calls.isEmpty())
        assertEquals("{\"active\":false}", guest.current())
    }

    @Test fun sheetTransportRetainsEscapedReportTextAndServerError() {
        val report = CommunityReportPresentation("Report", "Reason", "line\n\"quoted\"", 2000, "Server message", "Send", "Cancel", true)
        val json = Json.parseToJsonElement(report.nativeJson()).jsonObject
        assertEquals("line\n\"quoted\"", json.getValue("report").jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals("Server message", json.getValue("error").jsonPrimitive.content)
        assertTrue(json.getValue("busy").jsonPrimitive.boolean)
    }

    @Test fun editorAndReportDispatchOnlyTheirOwnCommands() {
        val calls = mutableListOf<String>()
        val report = CommunityReportActions({ calls += "report:$it" }, { calls += "report:submit" }, { calls += "report:dismiss" })
        report.perform("coffee", "5")
        report.perform("text", "reason")
        report.perform("submit", "")
        report.perform("dismiss", "")
        assertEquals(listOf("report:reason", "report:submit", "report:dismiss"), calls)
    }
}

private class SheetAccounts : SessionReader {
    private val state = MutableStateFlow<Session?>(Session("fixture", userId = "one"))
    private var generation = 0L
    override fun accountGeneration() = generation
    override fun peekSession() = state.value
    override fun isActiveSession(session: Session?) = session?.accessToken?.isNotBlank() == true
    override fun observeSession() = state
    override suspend fun getSession() = state.value
    fun replace(user: String?) {
        generation++
        state.value = user?.let { Session("fixture-$generation", userId = it) }
    }
}
