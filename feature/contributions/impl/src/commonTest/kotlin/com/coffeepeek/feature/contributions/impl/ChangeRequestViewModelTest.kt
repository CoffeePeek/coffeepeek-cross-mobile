package com.coffeepeek.feature.contributions.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestAction
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestViewModel
import com.coffeepeek.feature.contributions.impl.request.RequestError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeRequestViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun TestScope.model(repository: FakeRepository, account: FakeAccount = FakeAccount()) =
        ChangeRequestViewModel("c1", repository, account).also { store.put("request", it); runCurrent() }

    @Test fun theRequestIsLoadedByItsId() = runTest(dispatcher) {
        val repository = FakeRepository().apply { change = { Result.success(request(it)) } }
        val state = model(repository).state.value
        assertEquals("c1", state.request?.id); assertFalse(state.isLoading); assertNull(state.error)
        assertEquals(listOf("change:c1"), repository.requests)
    }

    @Test fun aGuestIsAskedToSignInWithoutARequest() = runTest(dispatcher) {
        val repository = FakeRepository()
        val vm = model(repository, FakeAccount(null))
        assertEquals(RequestError.SignInRequired, vm.state.value.error)
        vm.onAction(ChangeRequestAction.Refresh); runCurrent()
        assertTrue(repository.requests.isEmpty())
    }

    @Test fun failuresAreTypedAndKeepTheServerMessageButNeverTheTransportText() = runTest(dispatcher) {
        val repository = FakeRepository()
        repository.change = { Result.failure(ContributionFailure(FailureKind.NotFound, "Заявка не найдена")) }
        val missing = model(repository).state.value
        assertEquals(RequestError.NotFound, missing.error); assertEquals("Заявка не найдена", missing.serverMessage); assertNull(missing.request)
        repository.change = { Result.failure(ContributionFailure(FailureKind.SignInRequired)) }
        assertEquals(RequestError.SignInRequired, model(repository).state.value.error)
        repository.change = { Result.failure(IllegalStateException("socket closed")) }
        val broken = model(repository).state.value
        assertEquals(RequestError.LoadFailed, broken.error); assertNull(broken.serverMessage)
    }

    @Test fun refreshLoadsAgainAfterAFailure() = runTest(dispatcher) {
        var fail = true
        val repository = FakeRepository().apply { change = { if (fail) Result.failure(IllegalStateException()) else Result.success(request(it)) } }
        val vm = model(repository)
        assertEquals(RequestError.LoadFailed, vm.state.value.error)
        fail = false
        vm.onAction(ChangeRequestAction.Refresh); runCurrent()
        assertNull(vm.state.value.error); assertNotNull(vm.state.value.request)
    }

    @Test fun aNewAccountDropsTheOldRequestAndItsLateAnswer() = runTest(dispatcher) {
        val slow = CompletableDeferred<Unit>()
        val repository = FakeRepository().apply { change = { Result.success(request("mine")) } }
        val account = FakeAccount("one")
        val vm = model(repository, account)
        assertEquals("mine", vm.state.value.request?.id)
        repository.change = { slow.await(); Result.success(request("late")) }
        account.key.value = "two"; runCurrent()
        assertTrue(vm.state.value.isLoading); assertNull(vm.state.value.request)
        repository.change = { Result.success(request("other")) }
        account.key.value = "three"; runCurrent()
        slow.complete(Unit); runCurrent()
        assertEquals("other", vm.state.value.request?.id)
    }
}
