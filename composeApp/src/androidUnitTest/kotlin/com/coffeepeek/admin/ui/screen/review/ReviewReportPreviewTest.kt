package com.coffeepeek.admin.ui.screen.review

import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.SessionRepository
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ReviewReportPreviewTest {
    @Test
    fun previewValidatesTheFormWithoutCallingAuthenticationOrTheApi() {
        val vm = ReviewReportViewModel(
            reviewId = "preview-maria",
            reviews = unusedRepository(ReviewRepository::class.java),
            sessions = unusedRepository(SessionRepository::class.java),
            isPreview = true,
        )

        vm.updateText("   ")
        vm.submit()
        assertFalse(vm.state.value.isSubmitted)
        assertNotNull(vm.state.value.error)

        vm.updateText("Пример причины жалобы")
        vm.submit()
        vm.submit()
        assertTrue(vm.state.value.isPreview)
        assertTrue(vm.state.value.isSubmitted)
        assertFalse(vm.state.value.isSubmitting)
        assertNull(vm.state.value.error)
    }

    private fun <T : Any> unusedRepository(type: Class<T>): T = requireNotNull(type.cast(
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            fail("Preview must not call ${type.simpleName}.${method.name}")
        },
    ))
}
