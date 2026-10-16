package com.coffeepeek.feature.shopreport.domain.model

/** The server may have accepted a write before its response was lost. */
class ShopReportDeliveryUnconfirmed(cause: Throwable? = null) : Exception("Report delivery was not confirmed", cause)
