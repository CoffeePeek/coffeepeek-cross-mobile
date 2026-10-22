package com.coffeepeek.admin.ui.screen.roaster

import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface

import com.coffeepeek.core.designsystem.component.CoffeePeekSelectField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectOption
import coffeepeek.composeapp.generated.resources.select_city_none
import coffeepeek.composeapp.generated.resources.select_city_label
import org.jetbrains.compose.resources.stringResource

import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.PlatformAlert
import com.coffeepeek.admin.ui.component.SubmissionStatus
import com.coffeepeek.admin.ui.component.SubmissionStatusScreen
import com.coffeepeek.admin.ui.component.SubmissionErrorActions
import com.coffeepeek.admin.ui.component.HapticEvent
import com.coffeepeek.admin.ui.component.platformHaptic
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.component.CpTopBar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_happy
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CoffeeShopPlaceholderImage
import com.coffeepeek.admin.ui.component.PhotoAttachmentsSection
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.domain.model.City
import com.coffeepeek.admin.di.platformViewModel
import org.jetbrains.compose.resources.painterResource

private const val ROASTER_PHOTO_LIMIT = 5

@Composable
fun AddRoasterScreen(vm: AddRoasterViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()

    state.result?.let { result ->
        RoasterSubmittedContent(
            name = state.name,
            cityName = state.selectedCity?.name,
            address = state.address,
            about = state.about,
            coverPhoto = state.photos.firstOrNull()?.bytes,
            addressValidated = result.isAddressValidated,
            message = result.message,
            onDone = Navigator::popBack,
        )
        return
    }

    if (state.isSubmitting || state.submissionError != null) {
        val failed = state.submissionError != null
        SubmissionStatusScreen(
            screenTitle = "Добавить обжарщика",
            status = if (failed) SubmissionStatus.Error else SubmissionStatus.Sending,
            title = if (failed) "Обжарщик не отправлен" else "Отправляем обжарщика…",
            description = state.submissionError?.let { "$it\n\nВведённые данные остались в форме." }
                ?: "Отправляем информацию и фотографии на проверку",
            onBack = if (failed) vm::clearSubmissionError else Navigator::popBack,
        ) {
            if (failed) SubmissionErrorActions(vm::submit, vm::clearSubmissionError)
        }
        return
    }

    state.error?.let { error ->
        PlatformAlert(title = "Не удалось загрузить города", message = error, onConfirm = vm::clearError, onDismiss = vm::clearError)
    }

    Scaffold(
        topBar = {
            CpTopBar("Добавить обжарщика")
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoadingCatalogs) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CoffeePeekLoader() }
            return@Scaffold
        }

        PlatformScrollableColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
        ) {
            Text(
                text = "Расскажите об обжарщике. После проверки он появится в общем каталоге.",
                modifier = Modifier.padding(top = CpDimens.spacing4),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoasterField(
                label = "Название",
                value = state.name,
                onValueChange = vm::onNameChange,
                maxLength = 100,
                placeholder = "Например, Coffee Circus",
                error = if (state.name.isNotEmpty()) state.nameError else null,
            )
            RoasterField(
                label = "Об обжарщике",
                value = state.about,
                onValueChange = vm::onAboutChange,
                placeholder = "История, подход к обжарке, особенности",
                singleLine = false,
                minLines = 4,
            )

            Text(
                text = "Адрес (необязательно)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Город и адрес нужно указать вместе.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RoasterCityPicker(
                cities = state.cities,
                selected = state.selectedCity,
                onSelect = vm::onCitySelect,
            )
            RoasterField(
                label = "Адрес",
                value = state.address,
                onValueChange = vm::onAddressChange,
                placeholder = "Улица, дом",
                error = state.locationError,
                leadingIcon = CpIcons.Location,
            )

            RoasterField(
                label = "Instagram",
                value = state.instagram,
                onValueChange = vm::onInstagramChange,
                placeholder = "@username",
                error = state.instagramError,
                leadingIcon = CpIcons.Instagram,
                keyboardType = KeyboardType.Uri,
            )
            RoasterField(
                label = "Сайт",
                value = state.website,
                onValueChange = vm::onWebsiteChange,
                placeholder = "https://example.com",
                error = state.websiteError,
                leadingIcon = CpIcons.Globe,
                keyboardType = KeyboardType.Uri,
            )

            PhotoAttachmentsSection(
                photos = state.photos,
                maxPhotos = ROASTER_PHOTO_LIMIT,
                onPhotosAdded = vm::addPhotos,
                onRemovePhoto = vm::removePhoto,
                title = "Фотографии обжарщика",
                hint = "До $ROASTER_PHOTO_LIMIT фотографий (необязательно).",
            )

            Button(
                onClick = vm::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(CpDimens.buttonRadius),
            ) {
                Text("Отправить на модерацию", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(CpDimens.spacing8))
        }
    }
}

@Composable
private fun RoasterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int = Int.MAX_VALUE,
    placeholder: String,
    error: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )
        CompactOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            maxLength = maxLength,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(CpDimens.buttonHeight) else Modifier),
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon?.let { icon ->
                { Icon(icon, contentDescription = null) }
            },
            singleLine = singleLine,
            contentPadding = if (singleLine) {
                CpDimens.singleLineFieldContentPadding
            } else {
                OutlinedTextFieldDefaults.contentPadding()
            },
            minLines = minLines,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = if (singleLine) {
                RoundedCornerShape(percent = 50)
            } else {
                RoundedCornerShape(CpDimens.buttonRadius)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = CpDimens.spacing2),
            )
        }
    }
}

@Composable
private fun RoasterCityPicker(
    cities: List<City>,
    selected: City?,
    onSelect: (City?) -> Unit,
) {
    val none = stringResource(Res.string.select_city_none)
    CoffeePeekSelectField(
        value = selected?.name ?: none,
        options = listOf(CoffeePeekSelectOption("none", none, selected == null) { onSelect(null) }) + cities.map { city ->
            CoffeePeekSelectOption("city:${city.id}", city.name, city.id == selected?.id) { onSelect(city) }
        },
        contentDescription = stringResource(Res.string.select_city_label),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RoasterSubmittedContent(
    name: String,
    cityName: String?,
    address: String,
    about: String,
    coverPhoto: ByteArray?,
    addressValidated: Boolean,
    message: String,
    onDone: () -> Unit,
) {
    LaunchedEffect(Unit) { platformHaptic(HapticEvent.Success) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing4),
            ) {
                AppButton(text = "Готово", onClick = onDone)
            }
        },
    ) { padding ->
        PlatformScrollableColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

                .padding(horizontal = CpDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(CpDimens.spacing6))
            Image(
                painter = painterResource(Res.drawable.maskot_happy),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(CpDimens.authMascotSize),
            )
            Spacer(Modifier.height(CpDimens.spacing4))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CpIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(CpDimens.spacing4))
            Text(
                text = "Обжарщик отправлен!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(CpDimens.spacing2))
            Text(
                text = message.ifBlank {
                    if (addressValidated) "Мы проверим информацию и опубликуем обжарщика в течение 24 часов."
                    else "Заявка принята. Адрес дополнительно проверит модератор."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(CpDimens.spacing6))
            RoasterPreviewCard(
                name = name,
                cityName = cityName,
                address = address,
                about = about,
                coverPhoto = coverPhoto,
            )
            Spacer(Modifier.height(CpDimens.spacing8))
        }
    }
}

@Composable
private fun RoasterPreviewCard(
    name: String,
    cityName: String?,
    address: String,
    about: String,
    coverPhoto: ByteArray?,
) {
    val location = listOfNotNull(cityName?.takeIf { it.isNotBlank() }, address.takeIf { it.isNotBlank() })
        .joinToString(", ")
    val cardShape = RoundedCornerShape(CpDimens.cardRadius)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = CpDimens.cardRadius, topEnd = CpDimens.cardRadius)),
            ) {
                if (coverPhoto != null) {
                    CpImage(
                        data = coverPhoto,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    CoffeeShopPlaceholderImage(labelSize = 16.sp)
                }
            }
            Column(modifier = Modifier.padding(CpDimens.spacing4)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (location.isNotBlank()) {
                    Spacer(Modifier.height(CpDimens.spacing2))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = CpIcons.Location,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                if (about.isNotBlank()) {
                    Spacer(Modifier.height(CpDimens.spacing2))
                    Text(
                        text = about,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoasterCityPickerPreview(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface { Column(Modifier.padding(16.dp)) {
        RoasterCityPicker(listOf(City("minsk", "Минск"), City("brest", "Брест")), null, {})
    } }
}
@Preview @Composable private fun RoasterCityPickerLightPreview() = RoasterCityPickerPreview(false)
@Preview @Composable private fun RoasterCityPickerDarkPreview() = RoasterCityPickerPreview(true)
