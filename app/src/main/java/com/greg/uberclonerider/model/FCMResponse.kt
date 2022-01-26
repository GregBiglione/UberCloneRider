package com.greg.uberclonerider.model

data class FCMResponse(
    var multiCastId: Long = 0,
    var success: Int = 0,
    var failure: Int = 0,
    var canonicalIds: Int = 0,
    var result: List<FCMResult>? = null,
    var massageId: Long = 0
)
