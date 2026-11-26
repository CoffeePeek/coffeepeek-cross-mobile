package com.coffeepeek.admin.di.system

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosGlobalDialogTest {
    @BeforeTest fun before() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun after() = Dispatchers.resetMain()

    @Test fun aClosedErrorCannotDismissANewerPresentation() {
        var dismissals = 0
        val model = IosGlobalDialog(true, "title", "message", "close") { dismissals++ }
        model.dismiss()
        model.close()
        model.dismiss()
        assertEquals(1, dismissals)
    }

    @Test fun blockingLoadingHasNoDismissAction() {
        var dismissals = 0
        val model = IosGlobalDialog(false, "loading", null, "") { dismissals++ }
        model.dismiss()
        assertEquals(0, dismissals)
        model.close()
    }
}
