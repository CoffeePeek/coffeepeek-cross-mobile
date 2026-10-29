package com.coffeepeek.admin.di.coffee

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.coffee_tab_catalog
import coffeepeek.composeapp.generated.resources.coffee_tab_shelf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.core.designsystem.component.CoffeePeekCapsuleSegmentedControl
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.room.model.Setting
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val MODE_KEY = "coffee_tab_mode"
private const val NOT_READ = "?"

internal enum class CoffeeTabMode { Shelf, Catalog }

/**
 * "Моя полка / Каталог" at the top of the Coffee tab. The choice is stored, so the tab reopens where the user left it;
 * the shelf is the default.
 */
internal class CoffeeShelfTab(private val entry: ShelfEntry, private val sessions: SessionReader, private val settings: SettingRepository) {
    @Composable
    fun Content(catalog: @Composable (header: @Composable () -> Unit) -> Unit) {
        val scope = rememberCoroutineScope()
        val stored by remember { settings.readFlow(MODE_KEY).map { it?.value } }.collectAsState(NOT_READ)
        // A choice made here wins until the stored one changes, e.g. after a bag was added elsewhere.
        var chosen by rememberSaveable(stored) { mutableStateOf<CoffeeTabMode?>(null) }
        if (chosen == null && stored == NOT_READ) return
        val mode = chosen ?: CoffeeTabMode.entries.firstOrNull { it.name == stored } ?: CoffeeTabMode.Shelf
        val select = { next: CoffeeTabMode ->
            chosen = next
            scope.launch { settings.save(Setting(MODE_KEY, next.name)) }
        }
        val header = @Composable { CoffeeTabSwitch(mode, { select(it) }) }
        when (mode) {
            CoffeeTabMode.Shelf -> Shelf(onBrowseCatalog = { select(CoffeeTabMode.Catalog) }, header = header)
            CoffeeTabMode.Catalog -> catalog(header)
        }
    }

    @Composable
    private fun Shelf(onBrowseCatalog: () -> Unit, header: @Composable () -> Unit) {
        val session by remember { sessions.observeSession() }.collectAsState(sessions.peekSession())
        val signedIn = session?.takeIf(sessions::isActiveSession)?.userId?.isNotBlank() == true
        entry.Shelf(signedIn,
            onOpenArchive = { Navigator.navigate(Navigator.Screen.ShelfArchive) },
            onBrowseCatalog = onBrowseCatalog,
            onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
            onAddBag = { Navigator.navigate(Navigator.Screen.ShelfAddBag) },
            onOpenBag = { Navigator.navigate(Navigator.Screen.ShelfBag(it)) },
            modifier = Modifier,
            bottomClearance = LocalFloatingNavClearance.current,
            header = { header() })
    }
}

/** Makes the Coffee tab open on the shelf. */
internal suspend fun SettingRepository.showShelfInCoffeeTab() = save(Setting(MODE_KEY, CoffeeTabMode.Shelf.name))

@Composable
private fun CoffeeTabSwitch(mode: CoffeeTabMode, onSelected: (CoffeeTabMode) -> Unit) {
    val shelf = stringResource(Res.string.coffee_tab_shelf)
    val catalog = stringResource(Res.string.coffee_tab_catalog)
    CoffeePeekCapsuleSegmentedControl(CoffeeTabMode.entries, mode, { if (it == CoffeeTabMode.Shelf) shelf else catalog }, onSelected,
        Modifier.fillMaxWidth().padding(bottom = CoffeePeekDimens.spacing3))
}

@Composable
private fun CoffeeTabSwitchPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { CoffeeTabSwitch(CoffeeTabMode.Shelf) {} }
}

@Preview @Composable private fun CoffeeTabSwitchLightPreview() = CoffeeTabSwitchPreview(false)
@Preview @Composable private fun CoffeeTabSwitchDarkPreview() = CoffeeTabSwitchPreview(true)
