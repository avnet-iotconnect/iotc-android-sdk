package com.iotconnectsdk.interfaces

internal interface HubToSdkCallback {
    fun onReceiveMsg(message: String?)
    fun onSendMsgUI(message: String?)
    fun onConnectionStateChange(isConnected: Boolean)
}