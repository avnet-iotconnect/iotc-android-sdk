package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


internal data class GetSettingBean(
    @SerializedName("ln")
    val ln: String,
    @SerializedName("dt")
    val dt: Int,
    @SerializedName("dv")
    val dv: String,
)

