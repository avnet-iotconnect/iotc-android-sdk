package com.iotconnectsdk

import com.google.gson.annotations.SerializedName


enum class IoTCEnvironment(val value: String) {
    @SerializedName("POC")
    POC("POC"),
    @SerializedName("preqa")
    PREQA("preqa"),
    @SerializedName("prod")
    PROD("prod")
}
