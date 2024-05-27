package com.iotconnectsdk.utils

import com.iotconnectsdk.log.IotSDKGlobals
import com.iotconnectsdk.webservices.api.AppGSonBuilder
import com.iotconnectsdk.webservices.api.IotSDKApiInterface
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.MalformedURLException
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException


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
            val retrofit = Retrofit.Builder()
                .baseUrl(host)
                .client(getUnsafeOkHttpClient(interceptorBody).build())
                .addConverterFactory(GsonConverterFactory.create(AppGSonBuilder.external))
                .build()
            return retrofit.create(IotSDKApiInterface::class.java)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getUnsafeOkHttpClient(interceptorBody: HttpLoggingInterceptor): OkHttpClient.Builder {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
            )

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()

            builder.sslSocketFactory(
                sslSocketFactory,
                (trustAllCerts[0] as X509TrustManager)!!
            )
            builder.hostnameVerifier { hostname, session -> true }
            builder.readTimeout(60, TimeUnit.SECONDS)
            builder.connectTimeout(60, TimeUnit.SECONDS)
            builder.addInterceptor(interceptorBody)
            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}