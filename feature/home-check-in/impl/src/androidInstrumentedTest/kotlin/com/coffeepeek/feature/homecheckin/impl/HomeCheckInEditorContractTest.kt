package com.coffeepeek.feature.homecheckin.impl

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInEditorViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.component.HomeCheckInEditorDuration
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

/** Self-targeted host, fake catalogues/images/writes only; no production Application or camera. */
class HomeCheckInEditorContractTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(dark: Boolean = false, content: @Composable () -> Unit) =
        ActivityScenario.launch(HomeCheckInTestActivity::class.java).also { scenario ->
            scenario.onActivity { it.setContent {
                CoffeePeekTheme(FontFamily.Default, darkTheme = dark) { Surface { content() } }
            } }
        }

    @Composable private fun Content(
        state: HomeCheckInEditorState = previewHomeEditorState(),
        actions: (HomeCheckInEditorAction) -> Unit = {},
        pickerLoading: Boolean = false,
        onAdd: (Int) -> Unit = {},
        convertDate: (String) -> String = { error("Unexpected date conversion") },
    ) = HomeCheckInEditorScreenContent(state, actions, "2026-10-17", { "17 октября 2026" },
        convertDate, { it }, previewHomeEditorMedia(), pickerLoading, onAdd)

    private fun scrollTo(description: String) = compose.onNodeWithContentDescription(description).performScrollTo()
    private fun awaitText(text: String) = compose.waitUntil(5_000) { compose.onNodeWithText(text).isDisplayed() }

    @Test fun createDefaultAndAllRecipeFieldsAreReachableInBothThemes() {
        for (dark in listOf(false, true)) render(dark) { Content() }.use {
            compose.onNodeWithText("Кофе дома").assertIsDisplayed()
            compose.onNodeWithText("Сохранить чекин").assertIsEnabled()
            compose.onNode(isToggleable()).assertIsOff()
            compose.onNodeWithContentDescription("Как получился кофе?: выбрать 5 из 5").assertIsSelected()
            listOf("Выбрать дату приготовления", "Выбрать способ заваривания", "Выбрать зерно", "Количество кофе, г",
                "Минуты заваривания", "Секунды заваривания", "Заметка", "Напиток", "Добавить фото").forEach {
                scrollTo(it); compose.onNodeWithContentDescription(it).assertIsDisplayed()
            }
        }
    }

    @Test fun controlsEmitTypedActionsWithoutDirectWrites() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        render {
            var state by remember { mutableStateOf(previewHomeEditorState()) }
            Content(state, actions = { action ->
                actions += action
                state = when (action) {
                    is HomeCheckInEditorAction.DoseChanged -> state.copy(draft = state.draft.copy(doseGrams = action.value))
                    is HomeCheckInEditorAction.DrinkNameChanged -> state.copy(draft = state.draft.copy(customDrinkName = action.value))
                    else -> state
                }
            })
        }.use {
            compose.onNode(isToggleable()).performClick()
            compose.onNodeWithContentDescription("Как получился кофе?: выбрать 5 из 5").performClick()
            scrollTo("Количество кофе, г")
            compose.onNodeWithContentDescription("Количество кофе, г").performTextInput("18,5")
            scrollTo("Напиток")
            compose.onNodeWithContentDescription("Напиток").performTextInput("Капучино")
            compose.onNodeWithText("Сохранить чекин").performClick()
            assertEquals(listOf(HomeCheckInEditorAction.VisibilityChanged(HomePreparationVisibility.Public),
                HomeCheckInEditorAction.TasteRatingTapped(5), HomeCheckInEditorAction.DoseChanged("18,5"),
                HomeCheckInEditorAction.DrinkNameChanged("Капучино"), HomeCheckInEditorAction.Save), actions)
        }
    }

    @Test fun sameDateDoesNotConvertOrReplaceRecordedTimeAndPickerCanBeCancelled() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        val conversions = mutableListOf<String>()
        render { Content(actions = actions::add, convertDate = { date ->
            conversions += date
            date + "T00:00:00Z"
        }) }.use {
            scrollTo("Выбрать дату приготовления")
            compose.onNodeWithContentDescription("Выбрать дату приготовления").performClick()
            awaitText("Готово")
            compose.onNodeWithText("Готово").performClick()
            compose.onNodeWithText("Готово").assertDoesNotExist()
            assertTrue(actions.isEmpty())
            compose.onNodeWithContentDescription("Выбрать дату приготовления").performClick()
            awaitText("Отмена")
            compose.onNodeWithText("Отмена").performClick()
            assertTrue(actions.isEmpty())
            assertTrue(conversions.isEmpty())
            compose.onNodeWithContentDescription("Выбрать дату приготовления").performClick()
            awaitText("Готово")
            // Material exposes the full localized date as its semantic text, not the visible day number.
            val locale = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0]
            val dayLabel = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL, locale).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(Instant.parse("2026-10-16T00:00:00Z").toEpochMilliseconds()))
            awaitText(dayLabel)
            compose.onNodeWithText(dayLabel).assertIsEnabled().performClick()
            compose.onNodeWithText("Готово").performClick()
            assertEquals(listOf("2026-10-16"), conversions)
            assertEquals(listOf(HomeCheckInEditorAction.DateChosen("2026-10-16", "2026-10-16T00:00:00Z")), actions)
        }
    }

    @Test fun methodMenuKeepsOptionalChoiceAndSelectedMethod() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        val state = previewHomeEditorState().copy(methods = listOf(HomeCheckInBrewMethod("v60", "Hario V60")))
        render { Content(state, actions::add) }.use {
            scrollTo("Выбрать способ заваривания")
            compose.onNodeWithContentDescription("Выбрать способ заваривания").performClick()
            awaitText("Hario V60")
            compose.onNodeWithText("Hario V60").performClick()
            assertEquals(listOf(HomeCheckInEditorAction.MethodChanged("v60")), actions)
        }
    }

    @Test fun beanPickerShowsResultsAndKeepsCustomSelectionSeparate() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        render { Content(previewHomeEditorState().copy(isBeanPickerOpen = true,
            beans = listOf(HomeCheckInBeanOption("ethiopia", "Эфиопия", "Обжарщик"))), actions::add) }.use {
            compose.onNodeWithText("Эфиопия").performClick()
            compose.onNodeWithContentDescription("Название своего зерна").performTextInput("Моё зерно")
            compose.onNodeWithText("Использовать своё зерно").performClick()
            assertEquals(listOf(HomeCheckInEditorAction.BeanSelected("ethiopia"),
                HomeCheckInEditorAction.CustomBeanChosen("Моё зерно")), actions)
        }
    }

    @Test fun beanFailureRetryAndBackAreExplicitAndNeverSave() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        render { Content(previewHomeEditorState().copy(isBeanPickerOpen = true, beansIssue = HomePreparationIssue.Offline), actions::add) }.use {
            compose.onNodeWithText("Не удалось загрузить зерно").assertIsDisplayed()
            compose.onNodeWithText("Повторить").performClick()
            compose.onNodeWithContentDescription("Назад к приготовлению").performClick()
            assertEquals(listOf(HomeCheckInEditorAction.SearchBeans, HomeCheckInEditorAction.CloseBeanPicker), actions)
            compose.onNodeWithText("Сохранить чекин").assertDoesNotExist()
        }
    }

    @Test fun durationPartialZeroIsRetainedAndInvalidSecondsAreNotAccepted() {
        val values = mutableListOf<Int?>()
        render {
            var duration by remember { mutableStateOf<Int?>(null) }
            HomeCheckInEditorDuration(duration, true, null) { duration = it; values += it }
        }.use {
            compose.onNodeWithContentDescription("Минуты заваривания").performTextInput("0")
            compose.onNodeWithContentDescription("Минуты заваривания").assertTextEquals("0")
            compose.onNodeWithContentDescription("Секунды заваривания").performTextInput("60")
            compose.onNodeWithContentDescription("Секунды заваривания")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            compose.onNodeWithContentDescription("Секунды заваривания").performTextInput("45")
            assertEquals(listOf(0, 45), values)
        }
    }

    @Test fun fieldErrorsAreLocalizedInSemanticsAndDraftIsNotDiscarded() {
        val state = previewHomeEditorState().copy(draft = previewHomeEditorState().draft.copy(doseGrams = "0"),
            fieldErrors = mapOf(HomePreparationField.Dose to HomePreparationIssue.DoseTooSmall))
        render { Content(state) }.use {
            compose.onNodeWithText("Проверьте поля с ошибками: 1").assertIsDisplayed()
            scrollTo("Количество кофе, г")
            compose.onNodeWithContentDescription("Количество кофе, г").assertTextEquals("0")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error,
                    "Укажите от 0,01 до 10 000 г, не более двух знаков после запятой."))
        }
    }

    @Test fun editKeepsIssuedPhotosInsteadOfOfferingReplacement() {
        val entry = previewHomePreparation()
        render { Content(previewHomeEditorState().copy(entry = entry)) }.use {
            compose.onNodeWithText("Редактировать чекин").assertIsDisplayed()
            compose.onNodeWithText("Фотографии этой записи сохранятся без изменений").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Добавить фото").assertDoesNotExist()
        }
    }

    @Test fun failedEditLoadHasOnlyRetryAndCloseNeverAnEmptyCreateForm() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        render { Content(HomeCheckInEditorState(loadIssue = HomePreparationIssue.NotFound), actions::add) }.use {
            compose.onNodeWithText("Чекин не найден.").assertIsDisplayed()
            compose.onNodeWithText("Сохранить чекин").assertDoesNotExist()
            compose.onNodeWithContentDescription("Добавить фото").assertDoesNotExist()
            compose.onNodeWithText("Повторить").performClick()
            compose.onNodeWithContentDescription("Закрыть форму").performClick()
            assertEquals(listOf(HomeCheckInEditorAction.RetryDetail, HomeCheckInEditorAction.Close), actions)
        }
    }

    @Test fun addPortReceivesRemainingLimitAndMultipleCutoutsRequireChoice() {
        val actions = mutableListOf<HomeCheckInEditorAction>()
        val remaining = mutableListOf<Int>()
        val photo = upload("original")
        val candidates = listOf(upload("first"), upload("second"))
        val state = previewHomeEditorState().let { it.copy(draft = it.draft.copy(photos = listOf(photo)),
            photoPreparation = HomeCheckInPhotoState(source = photo, candidates = candidates)) }
        render { Content(state, actions::add, onAdd = remaining::add) }.use {
            scrollTo("Добавить фото"); compose.onNodeWithContentDescription("Добавить фото").performClick()
            assertEquals(listOf(4), remaining)
            compose.onNodeWithText("Сохранить чекин").assertIsNotEnabled()
            // Scroll the vertical body first, not the candidate's nearest (horizontal) scroll ancestor.
            compose.onNodeWithText("Без вырезки").performScrollTo()
            compose.onNodeWithContentDescription("Объект 2: выбрать как чашку").assertIsDisplayed().performClick()
            compose.onNodeWithText("Без вырезки").performScrollTo().performClick()
            assertEquals(listOf(HomeCheckInEditorAction.ChooseDrinkPhoto(1), HomeCheckInEditorAction.SkipDrinkPhoto), actions)
        }
    }

    @Test fun pickerBusyOrSaveBlocksConflictingActions() {
        render { Content(pickerLoading = true) }.use {
            compose.onNodeWithText("Сохранить чекин").assertIsNotEnabled()
            scrollTo("Добавить фото"); compose.onNodeWithContentDescription("Добавить фото").assertIsNotEnabled()
        }
        render { Content(previewHomeEditorState().copy(isSaving = true)) }.use {
            compose.onNodeWithText("Сохраняем…").assertIsNotEnabled()
            compose.onNodeWithContentDescription("Закрыть форму").assertIsNotEnabled()
            scrollTo("Количество кофе, г"); compose.onNodeWithContentDescription("Количество кофе, г").assertIsNotEnabled()
        }
    }

    @Test fun revokedAndClosedSnapshotsNeverComposePrivateDraftOrMedia() {
        for (revoked in listOf(false, true)) render {
            Content(previewHomeEditorState().let { it.copy(draft = it.draft.copy(text = "Private"),
                isClosed = !revoked, isAccessRevoked = revoked) })
        }.use {
            compose.onNodeWithText("Private").assertDoesNotExist()
            compose.onNodeWithText("Сохранить чекин").assertDoesNotExist()
        }
    }

    @Test fun narrowLargeFontKeepsFooterAndBottomFieldsReachable() {
        render { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.6f)) {
            Box(Modifier.width(320.dp).height(520.dp)) { Content() }
        } }.use {
            compose.onNodeWithText("Сохранить чекин").assertIsDisplayed()
            scrollTo("Добавить фото"); compose.onNodeWithContentDescription("Добавить фото").assertIsDisplayed()
            scrollTo("Напиток"); compose.onNodeWithContentDescription("Напиток").assertIsDisplayed()
        }
    }

    @Composable private fun Runtime(repo: Repository, allowed: Boolean = true, onEvent: (HomeCheckInEditorEvent) -> Unit = {},
        onDismiss: (Boolean) -> Unit = {}, lease: () -> Boolean = { true }) {
        val access by rememberUpdatedState(allowed)
        val vm = viewModel {
            HomeCheckInEditorViewModel(repo, object : HomeCheckInEditorCatalog {
                override suspend fun brewMethods() = Result.success(emptyList<HomeCheckInBrewMethod>())
                override suspend fun searchBeans(query: String) = Result.success(emptyList<HomeCheckInBeanOption>())
            }, { Result.success(emptyList()) }, "2026-10-17T07:30:00Z", "2026-10-17", null, 1, { access && lease() },
                { Instant.parse("2026-10-17T12:00:00Z") })
        }
        HomeCheckInEditorScreen(vm, allowed, "2026-10-17", { it }, { it + "T00:00:00Z" }, { it },
            previewHomeEditorMedia(), false, {}, onDismiss, onEvent)
    }

    @Test fun runtimeWriteBlocksDismissalAndDeliversOnlyOneOutcomeToLatestCallback() {
        val repo = Repository().apply { gate = CompletableDeferred() }
        var dismissAllowed = true
        var callback by mutableStateOf<(HomeCheckInEditorEvent) -> Unit>({ error("Stale callback") })
        val events = mutableListOf<HomeCheckInEditorEvent>()
        render { Runtime(repo, onEvent = callback, onDismiss = { dismissAllowed = it }) }.use {
            awaitText("Сохранить чекин")
            compose.onNodeWithText("Сохранить чекин").performClick()
            compose.waitUntil { repo.creates == 1 && !dismissAllowed }
            compose.onNodeWithContentDescription("Закрыть форму").assertIsNotEnabled()
            compose.runOnIdle { callback = events::add }
            // Publish the new callback through composition before the asynchronous write completes.
            compose.waitForIdle()
            compose.runOnIdle { repo.gate!!.complete(Unit) }
            compose.waitUntil { events.size == 1 && dismissAllowed }
            assertEquals(listOf(HomeCheckInEditorEvent.Saved("fixture-home", false)), events)
            compose.onNodeWithText("Сохранить чекин").assertDoesNotExist()
        }
    }

    @Test fun runtimeRevocationHidesDraftAndLateWriteDoesNotDeliverSaved() {
        val repo = Repository().apply { gate = CompletableDeferred() }
        var allowed by mutableStateOf(true)
        val events = mutableListOf<HomeCheckInEditorEvent>()
        render { Runtime(repo, allowed, events::add, lease = { allowed }) }.use {
            awaitText("Сохранить чекин")
            compose.onNodeWithText("Сохранить чекин").performClick()
            compose.waitUntil { repo.creates == 1 }
            compose.runOnIdle { allowed = false; repo.gate!!.complete(Unit) }
            compose.onNodeWithText("Кофе дома").assertDoesNotExist()
            compose.onNodeWithText("Сохранить чекин").assertDoesNotExist()
            compose.runOnIdle { assertTrue(events.isEmpty()) }
        }
    }

    @Test fun brandFontLayoutFixtureRendersInBothThemes() {
        for (dark in listOf(false, true)) render(dark) {
            CoffeePeekTheme(darkTheme = dark) { Surface { Content() } }
        }.use {
            compose.onNodeWithText("Кофе дома").assertIsDisplayed()
            val file = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "home-editor-" + (if (dark) "dark" else "light") + ".png")
            file.outputStream().use { check(compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        }
    }

    private class Repository : HomePreparationRepository {
        @Volatile var creates = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun create(input: HomePreparationInput): Result<HomePreparation> {
            creates++; gate?.await(); return Result.success(previewHomePreparation())
        }
        override suspend fun update(id: String, input: HomePreparationInput): Result<HomePreparation> = error("Unexpected update")
        override suspend fun detail(id: String): Result<HomePreparation> = error("Unexpected detail")
        override suspend fun delete(id: String): Result<Unit> = error("Unexpected delete")
        override suspend fun page(page: Int, pageSize: Int, range: HomePreparationDateRange?): Result<HomePreparationPage> = error("Unexpected page")
        override suspend fun calendar(range: HomePreparationDateRange): Result<List<HomePreparationDay>> = error("Unexpected calendar")
    }

    private fun upload(name: String) = HomePreparationUpload(name, "image/png", byteArrayOf(1))
}
