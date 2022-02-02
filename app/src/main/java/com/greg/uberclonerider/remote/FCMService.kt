package com.greg.uberclonerider.remote

import com.greg.uberclonerider.model.FCMResponse
import com.greg.uberclonerider.model.FCMSendData
import com.greg.uberclonerider.utils.Constant
import com.greg.uberclonerider.utils.Constant.Companion.FIREBASE_KEY
import io.reactivex.rxjava3.core.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMService {
    @Headers(
            "Content-Type:application/json",
            "Authorization:key=$FIREBASE_KEY"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?): Observable<FCMResponse?>?

    companion object{

        private var fcmService: FCMService? = null
        private lateinit var retrofit: Retrofit

        //------------------------------------------------------------------------------------------
        //-------------------------------- Initialize retrofit -------------------------------------
        //------------------------------------------------------------------------------------------

        fun getInstance(): FCMService{
            if (fcmService == null){
                retrofit = Retrofit.Builder()
                    .baseUrl(Constant.BASE_URL_FCM)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .build()
                getApi()
            }
            return fcmService!!
        }

        //------------------------------------------------------------------------------------------
        //-------------------------------- Initialize RetrofitService ------------------------------
        //------------------------------------------------------------------------------------------

        private fun getApi() {
            fcmService = retrofit.create(FCMService::class.java)
        }
    }
}