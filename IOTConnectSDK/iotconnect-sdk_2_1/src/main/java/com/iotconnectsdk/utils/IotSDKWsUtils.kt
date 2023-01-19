package com.iotconnectsdk.utils

import com.iotconnectsdk.log.IotSDKGlobals
import com.iotconnectsdk.webservices.api.AppGSonBuilder
import com.iotconnectsdk.webservices.api.IotSDKApiInterface
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

internal object IotSDKWsUtils {
    /**
     * Get APIService object from Retrofit.
     *
     * @return APIService
     */
    fun getAPIService(fullUrl: String?): IotSDKApiInterface? {
        var url: URL? = null
        var host: String? = null
        try {
            url = URL(fullUrl)
            host = url.protocol + "://" + url.host
            val interceptorBody = HttpLoggingInterceptor()
            if (IotSDKGlobals.isTest) {
                interceptorBody.setLevel(HttpLoggingInterceptor.Level.BODY)
            } else {
                interceptorBody.setLevel(HttpLoggingInterceptor.Level.NONE)
            }
            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptorBody)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(host)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(AppGSonBuilder.external))
                .build()
            return retrofit.create(IotSDKApiInterface::class.java)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return null
    }
}