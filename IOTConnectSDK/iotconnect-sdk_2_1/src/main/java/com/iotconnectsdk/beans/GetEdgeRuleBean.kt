package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


data class GetEdgeRuleBean(
    @SerializedName("g")
    val g: String,
    @SerializedName("es")
    val es: String, // 0
    @SerializedName("con")
    val con: String,
    @SerializedName("cmd")
    val cmd: String,
)

