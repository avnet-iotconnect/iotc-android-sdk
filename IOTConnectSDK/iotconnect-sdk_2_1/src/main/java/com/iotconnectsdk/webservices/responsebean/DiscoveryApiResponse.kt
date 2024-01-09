package com.iotconnectsdk.webservices.responsebean


import com.google.gson.annotations.SerializedName

internal data class DiscoveryApiResponse(
    @SerializedName("d")
    val d: D,
    @SerializedName("message")
    val message: String,

    @SerializedName("status")
    val status: Int
) {
    data class D(
        @SerializedName("bu")
        val bu: String,
        @SerializedName("ec")
        val ec: Int,
        @SerializedName("log:https")
        val logHttps: String,
        @SerializedName("pf")
        val pf: String,
        @SerializedName("log:mqtt")
        val logMqtt: LogMqtt
    ) {
        data class LogMqtt(
            @SerializedName("hn")
            val hn: String,
            @SerializedName("pwd")
            val pwd: String,
            @SerializedName("topic")
            val topic: String,
            @SerializedName("un")
            val un: String
        )
    }
}