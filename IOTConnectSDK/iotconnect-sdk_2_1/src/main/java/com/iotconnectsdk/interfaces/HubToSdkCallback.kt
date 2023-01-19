package com.iotconnectsdk.interfaces

interface HubToSdkCallback {
    fun onReceiveMsg(message: String?)
    fun onSendMsg(message: String?)
    fun onConnectionStateChange(isConnected: Boolean)
}