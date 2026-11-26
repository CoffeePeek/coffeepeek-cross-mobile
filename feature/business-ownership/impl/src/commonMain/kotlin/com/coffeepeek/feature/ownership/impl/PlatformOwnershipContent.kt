package com.coffeepeek.feature.ownership.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.ownership.api.OwnershipPhotoPicker
import com.coffeepeek.feature.ownership.domain.model.OwnershipAccount
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel

/** Native UI is selected at every platform entry; Android keeps the existing feature renderer. */
@Composable
internal expect fun PlatformOwnershipContent(model: OwnershipViewModel, account: OwnershipAccount,
    picker: OwnershipPhotoPicker, tab: Boolean = false, claim: Boolean = false,
    checkInLink: (String) -> String? = { null }, shareQr: (ByteArray, String) -> Unit = { _, _ -> },
    openBusiness: (String, String) -> Unit = { _, _ -> }): Boolean
