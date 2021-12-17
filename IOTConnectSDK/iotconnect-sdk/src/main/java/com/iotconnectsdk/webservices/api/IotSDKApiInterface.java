package com.iotconnectsdk.webservices.api;

import com.iotconnectsdk.webservices.requestbean.SyncServiceRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface IotSDKApiInterface {

    @Headers("Content-Type: application/json")
    @POST
    Call<ResponseBody> sync(@Url String url, @Body SyncServiceRequest syncRequest);

    @Headers("Content-Type: application/json")
    @GET
    Call<ResponseBody> getDiscoveryApi(@Url String url);
}