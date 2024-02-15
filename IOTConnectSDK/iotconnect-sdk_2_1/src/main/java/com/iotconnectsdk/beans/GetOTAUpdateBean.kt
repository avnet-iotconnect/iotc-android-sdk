package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


internal data class GetOTAUpdateBean(
    @SerializedName("cmd")
    val cmd: String? = null,
    @SerializedName("ack")
    val ack: String? = null,
    @SerializedName("ver")
    val ver: Ver? = null,
    @SerializedName("urls")
    val urls: List<Url>? = null
) {
    data class Ver(
        @SerializedName("sw")
        val sw: String? = null,
        @SerializedName("hw")
        val hw: String? = null
    )

    data class Url(
        @SerializedName("url")
        val url: String? = null,
        @SerializedName("fileName")
        val fileName: String? = null,
        @SerializedName("tg")
        val tg: String? = null
    )
}

