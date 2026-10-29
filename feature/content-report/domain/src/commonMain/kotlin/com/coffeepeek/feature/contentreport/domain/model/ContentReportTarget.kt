package com.coffeepeek.feature.contentreport.domain.model

enum class ContentReportKind { CheckIn, Review }
data class ContentReportTarget(val kind: ContentReportKind, val id: String)

const val CONTENT_REPORT_MAX_LENGTH = 2000
fun validReportReason(text: String): Boolean = text.trim().length in 1..CONTENT_REPORT_MAX_LENGTH
