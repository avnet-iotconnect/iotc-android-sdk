package com.iotconnectsdk

import com.google.gson.annotations.SerializedName
import java.io.Serializable


enum class IoTCEnvironment(val value: String){
    @SerializedName("avnet")
    AVNET("avnet"),
    @SerializedName("dev")
    DEV("dev"),
    @SerializedName("qa")
    QA("qa"),
    @SerializedName("prod")
    PROD("prod")
}
