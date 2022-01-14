package com.greg.uberclonerider.remote

import com.greg.uberclonerider.utils.Constant.Companion.BASE_URL
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitService {

    @GET("maps/api/directions/json")
    fun getDirections(
        @Query("mode") mode: String?,
        @Query("transit_routing_preference") transitRouting: String?,
        @Query("origin") from: String?,
        @Query("destination") to: String?,
        @Query("key") key: String?,
    ): Observable<String>? //TODO check if io.reactivex.Observable is the good import 5:58

    companion object{

        private var retrofitService: RetrofitService? = null
        private lateinit var retrofit: Retrofit

        //------------------------------------------------------------------------------------------
        //-------------------------------- Initialize retrofit -------------------------------------
        //------------------------------------------------------------------------------------------

        fun getInstance(): RetrofitService{
            if (retrofitService == null){
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                getApi()
            }
            return retrofitService!!
        }

        //------------------------------------------------------------------------------------------
        //-------------------------------- Initialize RetrofitService ------------------------------
        //------------------------------------------------------------------------------------------

        private fun getApi() {
            retrofitService = retrofit.create(RetrofitService::class.java)
        }
    }
}