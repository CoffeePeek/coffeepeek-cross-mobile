package com.coffeepeek.admin.ui.screen.shopchange

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.admin.di.platformViewModel
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.GroupSection
import com.coffeepeek.admin.ui.component.NavigationRow
import com.coffeepeek.admin.ui.component.RowSeparator
import com.coffeepeek.domain.model.ShopChangeSection
import org.koin.core.parameter.parametersOf

@Composable
fun SuggestShopChangeScreen(shopId: String) {
    val localizedLegacySuggestShopChangeScreenContributionsSuggestTitle = stringResource(Res.string.legacy_suggest_shop_change_screen_contributions_suggest_title)
    val localizedLegacySuggestShopChangeScreenContributionsSuggestHeading = stringResource(Res.string.legacy_suggest_shop_change_screen_contributions_suggest_heading)

    val vm: SuggestShopChangeViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { CpTopBar(localizedLegacySuggestShopChangeScreenContributionsSuggestTitle) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CoffeePeekLoader()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = coffeePeekScrollContentPadding(PaddingValues(CpDimens.spacing4)),
        ) {
            item {
                Text(
                    text = if (state.shopTitle.isBlank()) {
                        localizedLegacySuggestShopChangeScreenContributionsSuggestHeading
                    } else {
                        stringResource(Res.string.legacy_shop_change_named, state.shopTitle)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(CpDimens.spacing5))
            }
            item {
                GroupSection {
                    ShopChangeSection.entries
                        .filterNot { it == ShopChangeSection.Equipment }
                        .forEachIndexed { index, section ->
                        if (index > 0) RowSeparator()
                        NavigationRow(
                            label = section.localizedTitle(),
                            description = section.localizedHint(),
                            onClick = {
                                Navigator.navigate(
                                    Navigator.Screen.ShopChangeEditor(
                                        shopId = shopId,
                                        section = section.name,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
