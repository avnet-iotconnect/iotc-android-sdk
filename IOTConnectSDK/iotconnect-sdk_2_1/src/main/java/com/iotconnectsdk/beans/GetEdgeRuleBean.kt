package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


internal data class GetEdgeRuleBean(
    @SerializedName("g")
    val g: String,
    @SerializedName("es")
    val es: String,
    @SerializedName("con")
    val con: String,
    @SerializedName("cmd")
    val cmd: String,
)

