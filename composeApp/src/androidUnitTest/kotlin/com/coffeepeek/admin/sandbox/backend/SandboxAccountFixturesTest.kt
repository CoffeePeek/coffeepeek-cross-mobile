package com.coffeepeek.admin.sandbox.backend

import com.coffeepeek.admin.ui.screen.auth.LoginFailureCopy
import com.coffeepeek.admin.ui.screen.auth.message
import com.coffeepeek.core.session.JwtUtils
import com.coffeepeek.feature.auth.api.LocalUserData
import com.coffeepeek.feature.auth.data.createAuthRepository
import com.coffeepeek.feature.auth.domain.classifyLoginFailure
import com.coffeepeek.feature.contributions.data.repository.createContributionCatalogSource
import com.coffeepeek.feature.contributions.data.repository.createContributionsRepository
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.ChangeDraft
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.domain.model.NewRoaster
import com.coffeepeek.feature.contributions.domain.model.NewShop
import com.coffeepeek.feature.profile.data.createPublicProfileData
import com.coffeepeek.feature.profile.data.repository.createProfileAccountData
import com.coffeepeek.feature.roaster.data.repository.RoasterCatalogStore
import com.coffeepeek.feature.roaster.data.repository.createRoasterRepository
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The account, auth and contribution fixtures of the Sandbox server, read through the migrated feature clients. */
class SandboxAccountFixturesTest {
    private val loginCopy = LoginFailureCopy("Неверный email или пароль", "Не удалось подключиться. Проверьте интернет-соединение", "Не удалось войти. Проверьте email и пароль")

    @get:Rule val temporary = TemporaryFolder()

    @Test fun favoritesMutationsRoundTripThroughActualApiAndRemainLocal() = runTest { withSandboxFixture(temporary) { _, client ->
        val favorites = sandboxAccountFavorites(client)
        val saved = favorites.getFavorites(refresh = true).getOrThrow()
        assertEquals(3, saved.size)
        val cafe = saved.first { it.slug == "sandbox-cafe" }
        favorites.setFavorite(cafe, false).getOrThrow()
        assertEquals(2, favorites.getFavorites(refresh = true).getOrThrow().size)
        assertFalse(shopIsFavorite(client.client, "sandbox-cafe"))
        favorites.setFavorite(cafe, true).getOrThrow()
        assertEquals(3, favorites.getFavorites(refresh = true).getOrThrow().size)
        assertTrue(shopIsFavorite(client.client, "sandbox-cafe"))
    } }

    @Test fun profileChangesPersistInFakeBackendAndResetRestoresFixtures() = runTest { withSandboxFixture(temporary) { backend, client ->
        val data = createProfileAccountData(client.client, client.uploadClient, LocalSandboxSession(), backgroundScope)
        assertEquals("sandbox@example.invalid", data.accounts.loadOwnProfile(true).getOrThrow().email)
        data.edit.updateUsername("Local Tester").getOrThrow()
        data.edit.updateAbout("Local biography").getOrThrow()
        assertEquals("Local Tester", data.accounts.loadOwnProfile(true).getOrThrow().userName)
        assertEquals("Local biography", data.accounts.loadOwnProfile(true).getOrThrow().about)
        backend.reset(true)
        assertEquals("Sandbox", data.accounts.loadOwnProfile(true).getOrThrow().userName)
    } }

    @Test fun writeErrorScenarioDoesNotChangeProfileFavoritesOrVisits() = runTest { withSandboxFixture(temporary) { backend, client ->
        val data = createProfileAccountData(client.client, client.uploadClient, LocalSandboxSession(), backgroundScope)
        backend.scenario = SandboxScenario.SaveError
        assertTrue(data.edit.updateAbout("Rejected").isFailure)
        assertTrue(sandboxAccountFavorites(client).setFavorite(sandboxShopFavorite("sandbox-cafe"), false).isFailure)
        backend.scenario = SandboxScenario.Ready
        assertNotEquals("Rejected", data.accounts.loadOwnProfile(true).getOrThrow().about)
        assertEquals(3, sandboxAccountFavorites(client).getFavorites(refresh = true).getOrThrow().size)
    } }

    @Test fun profileEditingFollowAndDeletionFixturesFollowTheRealContracts() = runTest { withSandboxFixture(temporary) { backend, client ->
        val data = createProfileAccountData(client.client, client.uploadClient, LocalSandboxSession(), backgroundScope)
        // Edit: validation and a taken name fail without changing the profile; valid writes persist.
        assertTrue(data.edit.updateUsername("A").isFailure)
        assertTrue(data.edit.updateUsername(SandboxApplicationBackend.FRIEND_NAME).isFailure)
        assertTrue(data.edit.updateAbout("x".repeat(601)).isFailure)
        assertEquals("Sandbox", data.accounts.loadOwnProfile(true).getOrThrow().userName)
        assertTrue(data.edit.updateUsername("Renamed").isSuccess)
        assertTrue(data.edit.updateAbout("New about").isSuccess)
        val renamed = data.accounts.loadOwnProfile(true).getOrThrow()
        assertEquals("Renamed", renamed.userName)
        assertEquals("New about", renamed.about)

        // Deletion: no request until DELETE; the request then exists with a resend window; reset clears it.
        assertNull(data.deletion.existingRequest().getOrThrow())
        val created = data.deletion.request().getOrThrow()
        assertEquals(created.resendAvailableAtUtc, data.deletion.existingRequest().getOrThrow()?.resendAvailableAtUtc)
        assertTrue(java.time.Instant.parse(created.resendAvailableAtUtc).isAfter(java.time.Instant.now()))
        backend.reset(populated = true)
        assertNull(data.deletion.existingRequest().getOrThrow())

        // Follow: friend only, counters follow the state; self 400; unknown slug 404 for follow and profile.
        val profiles = createPublicProfileData(client.client)
        val friend = SandboxApplicationBackend.FRIEND_SLUG
        assertEquals(false, profiles.getProfile(friend).getOrThrow().isFollowing)
        assertEquals(true, profiles.setFollowing(friend, true).getOrThrow().isFollowing)
        assertEquals(1, profiles.getProfile(friend).getOrThrow().stats.followers)
        assertEquals(false, profiles.setFollowing(friend, false).getOrThrow().isFollowing)
        assertTrue(profiles.setFollowing("sandbox", true).isFailure)
        assertTrue(profiles.setFollowing("nobody", true).isFailure)
        assertTrue(profiles.getProfile("nobody").isFailure)

        // Writes fail closed under the error scenarios and leave the state unchanged.
        backend.scenario = SandboxScenario.SaveError
        assertTrue(profiles.setFollowing(friend, true).isFailure)
        assertTrue(data.deletion.request().isFailure)
        backend.scenario = SandboxScenario.Ready
        assertEquals(false, profiles.getProfile(friend).getOrThrow().isFollowing)
        assertNull(data.deletion.existingRequest().getOrThrow())
    } }

    @Test fun accountFixturesAnswerTheRealAuthServiceAndRecoveryRepository() = runTest { withSandboxFixture(temporary) { backend, client ->
        val sessions = LocalSandboxSession()
        val auth = createAuthRepository(client.plainClient, client.client, sessions, object : LocalUserData { override suspend fun clear() = Unit })

        // Default: sign-in succeeds with a token whose subject is the Sandbox account; nothing is taken; OAuth stays blocked.
        val login = auth.signIn(" Tester@Example.com ", "any-password").getOrThrow()
        assertEquals("11111111-1111-4111-8111-111111111111", login.userId)
        val stored = assertNotNull(sessions.peekSession())
        assertEquals("11111111-1111-4111-8111-111111111111", JwtUtils.extractUserId(stored.accessToken))
        assertEquals("sandbox-refresh-token", stored.refreshToken)
        assertEquals(false, auth.isEmailTaken("new@example.com").getOrThrow())
        assertTrue(auth.register("Tester", "new@example.com", "password1").isSuccess)
        assertTrue(auth.requestPasswordReset("tester@example.com").isSuccess)
        assertTrue(auth.signOut().isSuccess)
        assertTrue(auth.signInWithGoogle("token").isFailure)
        assertTrue(auth.beginAppleSignIn().isFailure)

        backend.authPolicy = SandboxAuthPolicy.WrongPassword
        val rejected = auth.signIn("tester@example.com", "wrong").exceptionOrNull()
        assertNotNull(rejected)
        assertEquals("Неверный email или пароль", classifyLoginFailure(rejected.message).message(loginCopy))
        assertTrue(auth.register("Tester", "new@example.com", "password1").isSuccess)

        backend.authPolicy = SandboxAuthPolicy.EmailTaken
        assertEquals(true, auth.isEmailTaken("taken@example.com").getOrThrow())
        assertTrue(auth.signIn("tester@example.com", "password1").isSuccess)

        backend.authPolicy = SandboxAuthPolicy.RegistrationFailure
        assertTrue(auth.register("Tester", "new@example.com", "password1").isFailure)
        assertEquals(false, auth.isEmailTaken("new@example.com").getOrThrow())

        backend.authPolicy = SandboxAuthPolicy.RecoveryFailure
        assertTrue(auth.requestPasswordReset("tester@example.com").isFailure)
        assertTrue(auth.register("Tester", "new@example.com", "password1").isSuccess)

        // The scenario chips still win over the account policy, so offline and 5xx also reach sign-in.
        backend.authPolicy = SandboxAuthPolicy.Works
        backend.scenario = SandboxScenario.Offline
        assertTrue(auth.signIn("tester@example.com", "password1").isFailure)
        backend.scenario = SandboxScenario.ServerError
        assertTrue(auth.signIn("tester@example.com", "password1").isFailure)
        backend.scenario = SandboxScenario.Ready
        backend.reset(true)
        assertEquals(SandboxAuthPolicy.Works, backend.authPolicy)
    } }

    @Test fun contributionFixturesServeTheModerationListsAndAcceptSubmissionsThroughTheRealServices() = runTest { withSandboxFixture(temporary) { backend, client ->
        val contributions = createContributionsRepository(client.client, client.uploadClient) { 0 }
        val roasterStore = object : RoasterCatalogStore {
            private var saved: String? = null
            override suspend fun read() = saved
            override suspend fun write(value: String) { saved = value }
        }
        val roasterCatalog = createRoasterRepository(client.client, SandboxBackend.BASE_URL, roasterStore) { 0L }
        val catalogs = createContributionCatalogSource(client.client) {
            roasterCatalog.getRoasters().map { list -> list.mapNotNull { r -> r.slug?.let { CatalogOption(it, r.name) } } }
        }

        // Seeded: a few submissions per status; an approved shop opens the catalog only when it is published.
        val approved = contributions.shops(ModerationStatus.Approved, 1, 20).getOrThrow()
        assertEquals(listOf("shop-approved-published", "shop-approved-waiting"), approved.items.map { it.id })
        assertEquals(listOf("sandbox-cafe", null), approved.items.map { it.publishedShopSlug })
        assertEquals(1, contributions.shops(ModerationStatus.Pending, 1, 20).getOrThrow().totalCount)
        val rejected = contributions.shops(ModerationStatus.Rejected, 1, 20).getOrThrow()
        assertEquals(SandboxContributionsBackend.REJECTED_REASON, rejected.items.single().rejectedReason)
        assertEquals(1, contributions.roasters(ModerationStatus.Approved, 1, 20).getOrThrow().items.size)
        assertEquals("Недостаточно данных", contributions.roasters(ModerationStatus.Rejected, 1, 20).getOrThrow().items.single().rejectedReason)
        assertEquals(listOf("change-approved"), contributions.changes(ModerationStatus.Approved, 1, 20).getOrThrow().items.map { it.id })
        assertEquals(listOf("change-rejected"), contributions.changes(ModerationStatus.Rejected, 1, 20).getOrThrow().items.map { it.id })
        assertEquals(listOf("change-pending"),
            contributions.changes(ModerationStatus.Pending, 1, 20, "sandbox-cafe", ChangeSection.Contacts).getOrThrow().items.map { it.id })
        assertTrue(contributions.changes(ModerationStatus.Pending, 1, 20, "sandbox-lab").getOrThrow().items.isEmpty())

        // A submitted shop appears as pending; invalid input is refused and stores nothing.
        contributions.submitShop(NewShop(name = "Новая кофейня", address = "Улица, 5", cityId = "minsk")).getOrThrow()
        assertEquals(2, contributions.shops(ModerationStatus.Pending, 1, 20).getOrThrow().totalCount)
        val invalid = contributions.submitShop(NewShop(name = " ", address = "Улица, 5", cityId = "minsk")).exceptionOrNull()
        assertEquals(FailureKind.Invalid, assertIs<ContributionFailure>(invalid).kind)
        // The migrated client refuses the blank name locally, so the server's own wording is read from the raw endpoint.
        val rawInvalid = client.client.post("/api/ModerationShops") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":" ","address":"Улица, 5","city":"minsk"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, rawInvalid.status)
        assertTrue("Укажите название, адрес и город" in rawInvalid.bodyAsText())
        assertEquals(2, contributions.shops(ModerationStatus.Pending, 1, 20).getOrThrow().totalCount)

        // Roaster: the server message and the address check come back; a city without an address is refused.
        val submitted = contributions.submitRoaster(NewRoaster(name = "Новый обжарщик", cityId = "minsk", address = "Улица, 1")).getOrThrow()
        assertEquals("Pending", submitted.status); assertTrue(submitted.isAddressValidated)
        assertEquals("Обжарщик отправлен на модерацию", submitted.message)
        assertTrue(contributions.submitRoaster(NewRoaster(name = "Без адреса", cityId = "minsk")).isFailure)
        assertEquals(2, contributions.roasters(ModerationStatus.Pending, 1, 20).getOrThrow().totalCount)

        // Changes: create, read, edit (stays pending), unknown shop, approved request cannot be edited.
        val created = contributions.submitChange(ChangeDraft.Description("sandbox-lab", "Новое")).getOrThrow()
        assertEquals(ModerationStatus.Pending, created.status)
        assertEquals("Новое", contributions.change(created.id).getOrThrow().payload.description)
        val edited = contributions.editChange(created.id, ChangeDraft.Selection("sandbox-lab", ChangeSection.Tags, listOf("wifi"))).getOrThrow()
        assertEquals(ChangeSection.Tags, edited.section); assertEquals(created.createdAtUtc, edited.createdAtUtc)
        assertTrue(contributions.submitChange(ChangeDraft.Selection("unknown", ChangeSection.Tags, emptyList())).isFailure)
        assertTrue(contributions.editChange("change-approved", ChangeDraft.Selection("sandbox-cafe", ChangeSection.Tags, emptyList())).isFailure)
        assertTrue(contributions.change("missing").isFailure)

        // The wizard and the editor read the equipment, bean and tag catalogs.
        val reference = catalogs.catalogs().getOrThrow()
        assertEquals(2, reference.equipment.size)
        assertEquals(2, reference.beans.size)
        assertEquals(2, reference.tags.size)

        // Paging: 45 extra approved shops are three pages of 20 with the remainder last.
        backend.contributions = SandboxContributions.Paged
        val first = contributions.shops(ModerationStatus.Approved, 1, 20).getOrThrow()
        assertEquals(47, first.totalCount); assertEquals(3, first.totalPages)
        assertEquals(7, contributions.shops(ModerationStatus.Approved, 3, 20).getOrThrow().items.size)

        backend.contributions = SandboxContributions.Empty
        assertTrue(contributions.shops(ModerationStatus.Approved, 1, 20).getOrThrow().items.isEmpty())
        assertTrue(contributions.changes(ModerationStatus.Approved, 1, 20).getOrThrow().items.isEmpty())

        backend.contributions = SandboxContributions.ListFailure
        assertTrue(contributions.shops(ModerationStatus.Approved, 1, 20).isFailure)
        assertTrue(contributions.roasters(ModerationStatus.Approved, 1, 20).isFailure)
        assertTrue(contributions.changes(null, 1, 20).isFailure)

        backend.contributions = SandboxContributions.SubmitFailure
        val refused = contributions.submitShop(NewShop(name = "A", address = "B", cityId = "minsk")).exceptionOrNull()
        assertEquals(SandboxContributionsBackend.SUBMIT_FAILURE, assertIs<ContributionFailure>(refused).serverMessage)
        assertTrue(contributions.submitRoaster(NewRoaster(name = "A")).isFailure)
        assertTrue(contributions.submitChange(ChangeDraft.Selection("sandbox-cafe", ChangeSection.Tags, emptyList())).isFailure)
        assertTrue(contributions.shops(ModerationStatus.Approved, 1, 20).isSuccess)

        backend.contributions = SandboxContributions.Paged
        backend.reset(true)
        assertEquals(SandboxContributions.Seeded, backend.contributions)
        assertEquals(2, contributions.shops(ModerationStatus.Approved, 1, 20).getOrThrow().totalCount)
    } }

    private suspend fun shopIsFavorite(client: io.ktor.client.HttpClient, slug: String): Boolean {
        val root = Json.parseToJsonElement(client.get("/api/CoffeeShops/$slug").bodyAsText()).jsonObject
        return (root["data"]?.jsonObject ?: root).getValue("isFavorite").jsonPrimitive.boolean
    }
}
