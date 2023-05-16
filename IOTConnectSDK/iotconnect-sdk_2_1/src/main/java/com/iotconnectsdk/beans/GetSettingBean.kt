package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


data class GetSettingBean(
    @SerializedName("ln")
    val ln: String,
    @SerializedName("dt")
    val dt: Int, // 0
    @SerializedName("dv")
    val dv: String,
)

