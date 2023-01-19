package com.iotconnectsdk.interfaces

 interface DeviceCallback {
    fun onReceiveMsg(message: String?)
}