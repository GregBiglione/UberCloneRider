package com.greg.uberclonerider.model

data class FCMSendData(
    var to: String,
    var data: Map<String, String>
)