package com.coffeepeek.feature.homecheckin.impl.ui.data

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomePreparationIssue.editorText(): String = stringResource(when (this) {
    HomePreparationIssue.Offline -> Res.string.home_check_in_offline
    HomePreparationIssue.Timeout -> Res.string.home_check_in_timeout
    HomePreparationIssue.Unauthorized -> Res.string.home_check_in_save_unauthorized
    HomePreparationIssue.Forbidden -> Res.string.home_check_in_save_forbidden
    HomePreparationIssue.NotFound -> Res.string.home_check_in_not_found
    HomePreparationIssue.RateLimited -> Res.string.home_check_in_rate_limited
    HomePreparationIssue.Unavailable -> Res.string.home_check_in_unavailable
    HomePreparationIssue.InvalidMethod -> Res.string.home_check_in_error_method
    HomePreparationIssue.BeanNameTooLong -> Res.string.home_check_in_error_bean_length
    HomePreparationIssue.InvalidBean, HomePreparationIssue.BeanUnavailable,
    HomePreparationIssue.ConflictingBeanSelection, HomePreparationIssue.InvalidBeanName -> Res.string.home_check_in_error_bean
    HomePreparationIssue.InvalidDose, HomePreparationIssue.DoseTooSmall, HomePreparationIssue.DoseTooLarge,
    HomePreparationIssue.DosePrecision -> Res.string.home_check_in_error_dose
    HomePreparationIssue.DurationTooSmall, HomePreparationIssue.DurationTooLarge,
    HomePreparationIssue.InvalidDuration -> Res.string.home_check_in_error_duration
    HomePreparationIssue.InvalidRating -> Res.string.home_check_in_error_rating
    HomePreparationIssue.NoteTooLong -> Res.string.home_check_in_error_note
    HomePreparationIssue.InvalidDrink, HomePreparationIssue.DrinkUnavailable,
    HomePreparationIssue.InvalidDrinkName -> Res.string.home_check_in_error_drink
    HomePreparationIssue.PhotoEditUnsupported -> Res.string.home_check_in_error_photo_edit
    HomePreparationIssue.TooManyPhotos, HomePreparationIssue.DuplicatePhotos,
    HomePreparationIssue.InvalidPhotos, HomePreparationIssue.PhotosNotClaimed,
    HomePreparationIssue.PhotosAlreadyUsed -> Res.string.home_check_in_error_photos
    HomePreparationIssue.MissingDateOffset, HomePreparationIssue.InvalidDate, HomePreparationIssue.InvalidTime,
    HomePreparationIssue.InvalidDateOffset, HomePreparationIssue.CivilDateMismatch -> Res.string.home_check_in_error_date
    HomePreparationIssue.FutureDate -> Res.string.home_check_in_error_future_date
    HomePreparationIssue.InvalidVisibility -> Res.string.home_check_in_error_visibility
    HomePreparationIssue.InvalidInput -> Res.string.home_check_in_error_input
    HomePreparationIssue.InvalidResponse, HomePreparationIssue.Conflict,
    HomePreparationIssue.RequestFailed -> Res.string.home_check_in_save_failed
})
