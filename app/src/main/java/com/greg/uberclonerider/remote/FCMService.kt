package com.greg.uberclonerider.remote

import com.greg.uberclonerider.model.FCMResponse
import com.greg.uberclonerider.model.FCMSendData
import com.greg.uberclonerider.utils.Constant.Companion.FIREBASE_KEY
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMService { // check if pb with Firebase key 5:00
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=$FIREBASE_KEY"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?): Observable<FCMResponse?>?
}