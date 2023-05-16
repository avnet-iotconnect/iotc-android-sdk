package com.softweb.iotconnectsdk.model


import com.google.gson.annotations.SerializedName

data class D2CSendAckBean @JvmOverloads constructor(
    @SerializedName("dt")
    val dt: String,
    @SerializedName("d")
    val d: Data
) {

    data class Data @JvmOverloads constructor(
        @SerializedName("ack")
        val ack: String,
        @SerializedName("type")
        val type: Int, // 0
        @SerializedName("st")
        val st: Int, // 0
        @SerializedName("msg")
        val msg: String,
        @SerializedName("cid")
        val cid: String? = null
    )
}