package com.iotconnectsdk.utils;

import com.google.gson.Gson;
import com.iotconnectsdk.log.IotSDKGlobals;
import com.iotconnectsdk.webservices.api.AppGSonBuilder;
import com.iotconnectsdk.webservices.api.IotSDKApiInterface;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IotSDKWsUtils {

    /**
     * Get APIService object from Retrofit.
     *
     * @return APIService
     */
    public static IotSDKApiInterface getAPIService(String fullUrl) {

        URL url = null;
        String host = null;
        try {
            url = new URL(fullUrl);

            host = url.getProtocol() + "://" + url.getHost();

            HttpLoggingInterceptor interceptorBody = new HttpLoggingInterceptor();

            if (IotSDKGlobals.isTest) {
                interceptorBody.setLevel(HttpLoggingInterceptor.Level.BODY);
            } else {
                interceptorBody.setLevel(HttpLoggingInterceptor.Level.NONE);
            }

            final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(interceptorBody)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(host)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(AppGSonBuilder.getExternal()))
                    .build();
            return retrofit.create(IotSDKApiInterface.class);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }
}

