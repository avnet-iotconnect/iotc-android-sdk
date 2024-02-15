package com.iotconnectsdk.interfaces

import org.json.JSONObject

internal interface TwinUpdateCallback {
    fun twinUpdateCallback(data: JSONObject?)
}