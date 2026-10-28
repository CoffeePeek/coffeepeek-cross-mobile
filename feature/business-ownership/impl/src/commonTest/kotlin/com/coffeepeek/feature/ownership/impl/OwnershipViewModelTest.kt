package com.coffeepeek.feature.ownership.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

private val original = BusinessProfile("business", BusinessKind.Roaster, 7, "Name")
private val sampleClaim = OwnershipClaim("claim", BusinessKind.Roaster, "business", "Name", "slug", "contact", "proof", ClaimStatus.Approved, "2026-10-28", null, null, false)
private class Account : OwnershipAccount {
    val value = MutableStateFlow<String?>("user")
    override fun current() = value.value
    override fun observe() = value
}
private class Repository : BusinessOwnershipRepository {
    var reads = 0
    var submissions = 0
    var saves = 0
    var submitResult: suspend () -> Result<OwnershipClaim> = { Result.success(sampleClaim) }
    var profileResult: suspend () -> Result<BusinessProfile> = { Result.success(original) }
    var saveResult: suspend () -> Result<BusinessProfile> = { Result.success(original.copy(revision = 8)) }
    override suspend fun claims(page: Int): Result<BusinessPage<OwnershipClaim>> {
        reads++
        return Result.success(BusinessPage(listOf(sampleClaim), 1, page, 20))
    }
    override suspend fun claim(id: String) = Result.success(sampleClaim)
    override suspend fun submit(input: ClaimSubmission): Result<OwnershipClaim> {
        submissions++
        return submitResult()
    }
    override suspend fun businesses(kind: BusinessKind, page: Int) = Result.success(BusinessPage(listOf(OwnedBusiness("business", kind, "Name")), 1, page, 20))
    override suspend fun profile(kind: BusinessKind, id: String) = profileResult()
    override suspend fun save(original: BusinessProfile, draft: ProfileDraft): Result<BusinessProfile> {
        saves++
        return saveResult()
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
class OwnershipViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun after() {
        store.clear()
        Dispatchers.resetMain()
    }
    private fun model(repository: Repository = Repository(), account: Account = Account()) =
        OwnershipViewModel(repository, account).also { store.put("model", it) }
    private fun TestScope.edit(model: OwnershipViewModel) {
        model.onAction(OwnershipAction.OpenBusiness(BusinessKind.Roaster, "business"))
        runCurrent()
        model.onAction(OwnershipAction.ProfileText(ProfileField.Name, "Unsaved draft"))
        runCurrent()
    }
    @Test fun guestsNeverReadClaimsOrOwnedBusinesses() = runTest(dispatcher) {
        val repository = Repository()
        val account = Account().apply { value.value = null }
        val model = model(repository, account)
        runCurrent()
        model.onAction(OwnershipAction.Refresh)
        runCurrent()
        assertEquals(0, repository.reads)
        assertNull(model.state.value.account)
    }
    @Test fun sessionChangesImmediatelyRemovePrivateStateAndDrafts() = runTest(dispatcher) {
        val account = Account()
        val model = model(account = account)
        runCurrent()
        edit(model)
        account.value.value = null
        runCurrent()
        assertNull(model.state.value.profile)
        assertNull(model.state.value.draft)
        assertNull(model.state.value.claims)
        assertNull(model.state.value.account)
    }
    @Test fun invalidSubmissionDoesNotCallTheRepository() = runTest(dispatcher) {
        val repository = Repository()
        val model = model(repository)
        runCurrent()
        model.onAction(OwnershipAction.NewClaim)
        model.onAction(OwnershipAction.Submit)
        runCurrent()
        assertEquals(0, repository.submissions)
        assertEquals(OwnershipProblem.Validation, model.state.value.problem)
    }
    @Test fun conflictsKeepDraftAndBlockSavingUntilExplicitReload() = runTest(dispatcher) {
        val repository = Repository().apply { saveResult = { Result.failure(OwnershipFailure(OwnershipProblem.Conflict)) } }
        val model = model(repository)
        runCurrent()
        edit(model)
        model.onAction(OwnershipAction.AskSave)
        runCurrent()
        model.onAction(OwnershipAction.Save)
        runCurrent()
        assertEquals("Unsaved draft", model.state.value.draft?.name)
        assertTrue(model.state.value.reloadRequired)
        model.onAction(OwnershipAction.ProfileText(ProfileField.Name, "Another draft"))
        runCurrent()
        assertEquals(OwnershipProblem.Conflict, model.state.value.problem)
        model.onAction(OwnershipAction.AskSave)
        model.onAction(OwnershipAction.Save)
        runCurrent()
        assertEquals(1, repository.saves)
    }
    @Test fun duplicateSaveWhileARequestIsPendingDoesNotRepeatIt() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<BusinessProfile>>()
        val repository = Repository().apply { saveResult = { response.await() } }
        val model = model(repository)
        runCurrent()
        edit(model)
        model.onAction(OwnershipAction.AskSave)
        runCurrent()
        model.onAction(OwnershipAction.Save)
        model.onAction(OwnershipAction.Save)
        runCurrent()
        assertEquals(1, repository.saves)
        response.complete(Result.success(original.copy(revision = 8)))
        runCurrent()
        assertTrue(model.state.value.saved)
    }
    @Test fun revokedAccessClearsEditorAndDoesNotLeaveSaveEnabled() = runTest(dispatcher) {
        val repository = Repository().apply { saveResult = { Result.failure(OwnershipFailure(OwnershipProblem.NotFound)) } }
        val model = model(repository)
        runCurrent()
        edit(model)
        model.onAction(OwnershipAction.AskSave)
        runCurrent()
        model.onAction(OwnershipAction.Save)
        runCurrent()
        assertNull(model.state.value.profile)
        assertNull(model.state.value.draft)
        assertEquals(OwnershipProblem.NotFound, model.state.value.problem)
    }
    @Test fun backingOutOfAnEditedProfileRequiresDiscardConfirmation() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        edit(model)
        model.onAction(OwnershipAction.Back)
        runCurrent()
        assertTrue(model.state.value.confirmDiscard)
        assertEquals(OwnershipDestination.Editor, model.state.value.destination)
        model.onAction(OwnershipAction.CancelConfirmation)
        runCurrent()
        assertEquals("Unsaved draft", model.state.value.draft?.name)
    }
    @Test fun anOldPhotoPickerResultCannotCrossAccountGenerations() = runTest(dispatcher) {
        val account = Account()
        val model = model(account = account)
        runCurrent()
        val generation = model.state.value.generation
        account.value.value = "another-user"
        runCurrent()
        edit(model)
        model.onAction(OwnershipAction.AddPhotos(generation, listOf(BusinessPhoto("old", fileName = "private.jpg", bytes = byteArrayOf(1)))))
        runCurrent()
        assertTrue(model.state.value.draft!!.photos.isEmpty())
    }
    @Test fun cancelledLookupsCannotRestoreOldAccountData() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<BusinessProfile>>()
        val account = Account()
        val repository = Repository().apply { profileResult = { withContext(NonCancellable) { response.await() } } }
        val model = model(repository, account)
        runCurrent()
        model.onAction(OwnershipAction.OpenBusiness(BusinessKind.Roaster, "business"))
        runCurrent()
        account.value.value = null
        runCurrent()
        response.complete(Result.success(original))
        runCurrent()
        assertNull(model.state.value.profile)
        assertNull(model.state.value.draft)
        assertNull(model.state.value.account)
    }
    @Test fun duplicateSubmissionIsGuardedWhilePending() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<OwnershipClaim>>()
        val repository = Repository().apply { submitResult = { response.await() } }
        val model = model(repository)
        runCurrent()
        model.onAction(OwnershipAction.NewClaim)
        model.onAction(OwnershipAction.ClaimText(ClaimField.Slug, "example"))
        model.onAction(OwnershipAction.ClaimText(ClaimField.Contact, "contact"))
        model.onAction(OwnershipAction.ClaimText(ClaimField.Evidence, "proof"))
        runCurrent()
        model.onAction(OwnershipAction.Submit)
        model.onAction(OwnershipAction.Submit)
        runCurrent()
        assertEquals(1, repository.submissions)
        response.complete(Result.success(sampleClaim))
        runCurrent()
        assertEquals(OwnershipDestination.Claim, model.state.value.destination)
    }
    @Test fun approvedHistoryDoesNotInventCurrentOwnership() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        model.onAction(OwnershipAction.OpenClaim("claim"))
        runCurrent()
        assertEquals(ClaimStatus.Approved, model.state.value.selectedClaim?.status)
        assertFalse(model.state.value.selectedClaim!!.hasCurrentAccess)
    }
}
