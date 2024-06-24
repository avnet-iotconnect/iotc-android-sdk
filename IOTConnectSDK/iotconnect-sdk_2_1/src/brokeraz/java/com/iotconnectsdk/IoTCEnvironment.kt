package com.iotconnectsdk

import com.google.gson.annotations.SerializedName
import java.io.Serializable


enum class IoTCEnvironment(val value: String) {

    /* @SerializedName("dev")
     DEV("dev"),*/
    @SerializedName("preqa")
    PREQA("preqa"),

    @SerializedName("qa")
    QA("qa"),

    @SerializedName("avnet")
    AVNET("avnet"),

    @SerializedName("emea")
    EMEA("emea"),

    @SerializedName("prod")
    PROD("prod")
}
