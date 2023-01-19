package com.iotconnectsdk.interfaces

import org.json.JSONObject

interface TwinUpdateCallback {
    fun twinUpdateCallback(data: JSONObject?)
}