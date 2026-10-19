package com.coffeepeek.feature.homecheckin.impl.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue
import com.coffeepeek.feature.homecheckin.impl.R

@Composable
internal fun HomePreparationIssue.editorText(): String = stringResource(when (this) {
    HomePreparationIssue.Offline -> R.string.home_check_in_offline
    HomePreparationIssue.Timeout -> R.string.home_check_in_timeout
    HomePreparationIssue.Unauthorized -> R.string.home_check_in_unauthorized
    HomePreparationIssue.Forbidden -> R.string.home_check_in_forbidden
    HomePreparationIssue.NotFound -> R.string.home_check_in_not_found
    HomePreparationIssue.RateLimited -> R.string.home_check_in_rate_limited
    HomePreparationIssue.Unavailable -> R.string.home_check_in_unavailable
    HomePreparationIssue.InvalidMethod -> R.string.home_check_in_error_method
    HomePreparationIssue.BeanNameTooLong -> R.string.home_check_in_error_bean_length
    HomePreparationIssue.InvalidBean, HomePreparationIssue.BeanUnavailable,
    HomePreparationIssue.ConflictingBeanSelection, HomePreparationIssue.InvalidBeanName -> R.string.home_check_in_error_bean
    HomePreparationIssue.InvalidDose, HomePreparationIssue.DoseTooSmall, HomePreparationIssue.DoseTooLarge,
    HomePreparationIssue.DosePrecision -> R.string.home_check_in_error_dose
    HomePreparationIssue.DurationTooSmall, HomePreparationIssue.DurationTooLarge,
    HomePreparationIssue.InvalidDuration -> R.string.home_check_in_error_duration
    HomePreparationIssue.InvalidRating -> R.string.home_check_in_error_rating
    HomePreparationIssue.NoteTooLong -> R.string.home_check_in_error_note
    HomePreparationIssue.InvalidDrink, HomePreparationIssue.DrinkUnavailable,
    HomePreparationIssue.InvalidDrinkName -> R.string.home_check_in_error_drink
    HomePreparationIssue.PhotoEditUnsupported -> R.string.home_check_in_error_photo_edit
    HomePreparationIssue.TooManyPhotos, HomePreparationIssue.DuplicatePhotos,
    HomePreparationIssue.InvalidPhotos, HomePreparationIssue.PhotosNotClaimed,
    HomePreparationIssue.PhotosAlreadyUsed -> R.string.home_check_in_error_photos
    HomePreparationIssue.MissingDateOffset, HomePreparationIssue.InvalidDate, HomePreparationIssue.InvalidTime,
    HomePreparationIssue.InvalidDateOffset, HomePreparationIssue.CivilDateMismatch -> R.string.home_check_in_error_date
    HomePreparationIssue.FutureDate -> R.string.home_check_in_error_future_date
    HomePreparationIssue.InvalidVisibility -> R.string.home_check_in_error_visibility
    HomePreparationIssue.InvalidInput -> R.string.home_check_in_error_input
    HomePreparationIssue.InvalidResponse, HomePreparationIssue.Conflict,
    HomePreparationIssue.RequestFailed -> R.string.home_check_in_save_failed
})
