package com.coffeepeek.feature.contentreport.domain.model

enum class ContentReportProblem { InvalidInput, SignInRequired, Forbidden, NotFound, RateLimited, Rejected, Unconfirmed }

/** Only a business-rejection envelope may supply user-facing wording; transport details stay private. */
class ContentReportFailure(val problem: ContentReportProblem, val serverMessage: String? = null) : Exception(problem.name)
