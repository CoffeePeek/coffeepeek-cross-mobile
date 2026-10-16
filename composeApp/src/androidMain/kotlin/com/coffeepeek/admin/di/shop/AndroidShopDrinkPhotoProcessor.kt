package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.feature.checkin.data.createDrinkPhotoExtractor
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.feature.shop.domain.model.ShopCheckInPhoto
import com.coffeepeek.feature.shop.impl.ui.data.ShopDrinkPhotoProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Map the feature port to main's bundled offline extractor; never start a second ML Kit pipeline. */
internal class AndroidShopDrinkPhotoProcessor(
    private val extractor: DrinkPhotoExtractor = createDrinkPhotoExtractor(),
) : ShopDrinkPhotoProcessor {
    override suspend fun process(source: ShopCheckInPhoto): Result<List<ShopCheckInPhoto>> = try {
        val candidates = extractor.extract(PendingPhotoUpload(source.fileName, source.contentType, source.bytes))
        currentCoroutineContext().ensureActive()
        Result.success(candidates.map { ShopCheckInPhoto(it.bytes, it.fileName, it.contentType) })
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Result.failure(error) }
}
