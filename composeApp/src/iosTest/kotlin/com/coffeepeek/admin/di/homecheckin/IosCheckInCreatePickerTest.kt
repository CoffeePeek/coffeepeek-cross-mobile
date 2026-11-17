package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class IosCheckInCreatePickerTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun useTestDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun clearStores() {
        stores.forEach(ViewModelStore::clear)
        Dispatchers.resetMain()
    }

    @Test
    fun shopAndShelfChoicesDoNotOpenOrDuplicateTheHomeEditor() = runTest(dispatcher) {
        val shop = fixture()
        shop.picker.chooseShop()
        shop.picker.dismiss()
        shop.picker.chooseHome()
        assertEquals(GlobalCheckInOutcome.Shop, shop.claim()?.outcome)
        assertNull(shop.claim())
        assertEquals(0, shop.editors)

        val shelf = fixture()
        shelf.picker.chooseShelfBag()
        shelf.picker.chooseShop()
        assertEquals(GlobalCheckInOutcome.ShelfBag, shelf.claim()?.outcome)
        assertNull(shelf.claim())
        assertEquals(0, shelf.editors)
    }

    @Test
    fun dismissingTheOldSheetCannotCancelItsChosenHomeEditor() = runTest(dispatcher) {
        val fixture = fixture()
        fixture.picker.chooseHome()
        val home = assertIs<GlobalCheckInDestination.Home>(fixture.model.state.value)
        fixture.picker.dismiss()
        fixture.picker.chooseHome()
        assertSame(home, fixture.model.state.value)
        assertEquals(1, fixture.editors)
    }

    @Test
    fun dismissalAndSessionReplacementBothRejectLatePickerActions() = runTest(dispatcher) {
        val closed = fixture()
        closed.picker.dismiss()
        closed.picker.chooseHome()
        assertEquals(GlobalCheckInOutcome.Close, closed.claim()?.outcome)
        assertEquals(0, closed.editors)

        val replaced = fixture()
        replaced.sessions.state.value = Session("replacement", userId = "another-user")
        replaced.picker.chooseShelfBag()
        replaced.picker.chooseHome()
        runCurrent()
        assertEquals(GlobalCheckInOutcome.Close, replaced.claim()?.outcome)
        assertEquals(0, replaced.editors)
    }

    private fun fixture() = Fixture(ViewModelStore().also(stores::add))

    private class Fixture(store: ViewModelStore) {
        val sessions = TestSessions()
        var editors = 0
            private set
        val model = GlobalCheckInFlow(sessions) { _, _ ->
            editors++
            Editor
        }
        val picker = IosCheckInCreatePicker("ru", model::pickerActions) {}

        init {
            store.put("check-in-create-picker", model)
        }

        fun claim() = model.claim(assertIs<GlobalCheckInDestination.Closed>(model.state.value))
    }

    private class TestSessions : SessionReader {
        val state = MutableStateFlow<Session?>(Session("original", userId = "owner"))

        override fun peekSession() = state.value
        override fun observeSession() = state
        override suspend fun getSession() = state.value
        override fun isActiveSession(session: Session?) = session != null
    }

    private object Editor : HomeCheckInEditorEntry {
        @Composable
        override fun Content(
            preparationId: String?, sessionGeneration: Int, accessAllowed: Boolean,
            onClose: () -> Unit, onSaved: (String, Boolean) -> Unit,
            onDismissAllowedChange: (Boolean) -> Unit, modifier: Modifier, bean: HomeCheckInBean?,
        ) = error("The picker contract test does not render the editor")
    }
}
