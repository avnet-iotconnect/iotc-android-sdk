package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


data class GetChildDeviceBean(
    @SerializedName("tg")
    val tg: String,
    @SerializedName("id")
    val id: String,
)

