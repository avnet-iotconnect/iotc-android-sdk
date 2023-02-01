package com.iotconnectsdk.webservices.api

import com.iotconnectsdk.webservices.requestbean.SyncServiceRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface IotSDKApiInterface {
    @Headers("Content-Type: application/json")
    @GET
    suspend fun sync(@Url url: String?): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @GET
    suspend fun getDiscoveryApi(@Url url: String?): Response<ResponseBody>
}