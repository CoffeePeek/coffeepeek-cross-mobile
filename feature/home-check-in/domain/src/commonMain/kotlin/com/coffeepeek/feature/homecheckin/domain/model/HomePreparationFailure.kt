package com.coffeepeek.feature.homecheckin.domain.model

enum class HomePreparationField { Date, Method, Bean, Dose, Duration, Rating, Note, Drink, Photos, Visibility }
enum class HomePreparationOperation { Save, PhotoUpload }

/** Stable reasons, not translated UI text or raw HTTP messages. */
enum class HomePreparationIssue {
    InvalidMethod, ConflictingBeanSelection, BeanNameTooLong, InvalidBean, BeanUnavailable,
    InvalidDose, DoseTooSmall, DoseTooLarge, DosePrecision, DurationTooSmall, DurationTooLarge,
    InvalidRating, NoteTooLong, InvalidDrinkName, InvalidDrink, DrinkUnavailable,
    PhotoEditUnsupported, TooManyPhotos, DuplicatePhotos, InvalidCutout, InvalidPhotos,
    PhotosNotClaimed, PhotosAlreadyUsed, MissingDateOffset, InvalidDate, InvalidTime,
    InvalidDateOffset, CivilDateMismatch, FutureDate, InvalidVisibility, InvalidDuration,
    InvalidBeanName, InvalidInput, Timeout, Offline, InvalidResponse, Unauthorized, Forbidden,
    RateLimited, Unavailable, NotFound, Conflict, RequestFailed,
}

/** The UI maps reasons to its resources; transport details never escape through message/cause. */
class HomePreparationFailure(
    val issue: HomePreparationIssue,
    val fieldErrors: Map<HomePreparationField, HomePreparationIssue> = emptyMap(),
    val operation: HomePreparationOperation = HomePreparationOperation.Save,
) : Exception("Home preparation: ${issue.name}")
