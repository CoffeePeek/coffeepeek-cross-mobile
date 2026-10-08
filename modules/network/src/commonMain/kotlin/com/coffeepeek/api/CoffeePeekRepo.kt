package com.coffeepeek.api

import com.coffeepeek.api.service.UserApiService

class CoffeePeekRepo(httpClient: CoffeePeekClient) {

    val authService = httpClient.authService
    private val client = httpClient.client

    val shopApiService = com.coffeepeek.api.service.ShopApiService(client)
    val userApiService = UserApiService(client)
    val photoApiService = com.coffeepeek.api.service.PhotoApiService(client, httpClient.uploadClient)
    val reviewApiService = com.coffeepeek.api.service.ReviewApiService(client)
    val checkInApiService = com.coffeepeek.api.service.CheckInApiService(client)
    val feedApiService = com.coffeepeek.api.feature.feed.FeedApiService(client)
    val shopIssueReportApiService = com.coffeepeek.api.service.ShopIssueReportApiService(client)
    val roasterApiService = com.coffeepeek.api.service.RoasterApiService(client)
    val shopChangeRequestApiService = com.coffeepeek.api.service.ShopChangeRequestApiService(client)
}
