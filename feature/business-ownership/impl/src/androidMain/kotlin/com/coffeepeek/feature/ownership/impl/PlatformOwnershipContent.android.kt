package com.coffeepeek.feature.ownership.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.ownership.api.OwnershipPhotoPicker
import com.coffeepeek.feature.ownership.domain.model.OwnershipAccount
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel

@Composable
internal actual fun PlatformOwnershipContent(model: OwnershipViewModel, account: OwnershipAccount,
    picker: OwnershipPhotoPicker, tab: Boolean, claim: Boolean, checkInLink: (String) -> String?,
    shareQr: (ByteArray, String) -> Unit, openBusiness: (String, String) -> Unit): Boolean = false
