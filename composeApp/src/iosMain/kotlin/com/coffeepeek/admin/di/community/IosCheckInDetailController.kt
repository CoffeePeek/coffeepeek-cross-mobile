package com.coffeepeek.admin.di.community

import platform.UIKit.UIViewController

private lateinit var detailFactory: (IosCheckInDetail) -> UIViewController
internal fun configureNativeCheckInDetails(factory: (IosCheckInDetail) -> UIViewController) { detailFactory = factory }
internal fun communityPostController(detail: IosCheckInDetail) = detailFactory(detail)
