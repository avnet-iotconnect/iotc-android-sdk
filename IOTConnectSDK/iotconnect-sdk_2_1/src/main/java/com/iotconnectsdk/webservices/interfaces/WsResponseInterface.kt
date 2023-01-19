package com.iotconnectsdk.webservices.interfaces

 interface WsResponseInterface {
    fun onSuccessResponse(methodName: String?, response: String?)
    fun onFailedResponse(methodNam: String?, errorCode: Int, message: String?)
    fun onFailedResponse(message: String?)
}