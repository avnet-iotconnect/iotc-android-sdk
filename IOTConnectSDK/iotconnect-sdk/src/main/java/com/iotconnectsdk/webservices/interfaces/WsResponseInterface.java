package com.iotconnectsdk.webservices.interfaces;

public interface WsResponseInterface {

    void onSuccessResponse(String methodName, String response);

    void onFailedResponse(String methodNam, int errorCode, String message);

    void onFailedResponse(String message);
}
