package com.iotconnectsdk.interfaces

import org.json.JSONObject

interface DeviceCallback {
    fun onDeviceCommand(message: String?)
    fun onOTACommand(message: String?)
    fun onModuleCommand(message: String?)
    fun onAttrChangeCommand(message: String?)
    fun onTwinChangeCommand(message: String?)
    fun onRuleChangeCommand(message: String?)
    fun onDeviceChangeCommand(message: String?)
    fun onReceiveMsg(message: String?)
    fun twinUpdateCallback(data: JSONObject?)
}