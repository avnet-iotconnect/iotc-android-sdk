package com.iotconnectsdk.enums

import com.google.gson.annotations.SerializedName

enum class BrokerType(val value:String) {

    @SerializedName("az")
    AZ ("az"),

    @SerializedName("aws")
    AWS ("aws")

}