package com.iotconnectsdk.interfaces

interface HubToSdkCallback {
    fun onReceiveMsg(message: String?)
    fun onSendMsgUI(message: String?)
    fun onConnectionStateChange(isConnected: Boolean)
}