package com.coffeepeek.feature.shelf.data.scan

import com.coffeepeek.feature.shelf.domain.repository.BagPhotoCropper

fun createIosBagPhotoCropper(): BagPhotoCropper = manualBagCropper(IosPackImaging)
