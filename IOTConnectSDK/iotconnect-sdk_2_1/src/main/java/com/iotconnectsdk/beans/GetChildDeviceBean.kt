package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


internal data class GetChildDeviceBean(
    @SerializedName("tg")
    var tg: String?=null,
    @SerializedName("id")
    var id: String?=null,
)

