package com.iotconnectsdk.webservices.responsebean


import com.google.gson.annotations.SerializedName

internal data class DiscoveryApiResponse(
    @SerializedName("d")
    val d: D,
    @SerializedName("message")
    val message: String,    /* Success
                               based on value of ec
                            // 0 – Success
                            // 1 – Invalid value of SID
                            // 2 – Company not found
                            // 3 – Subscription Expired */

    @SerializedName("status")
    val status: Int // 200
) {
    data class D(
        @SerializedName("bu")
        val bu: String, // Base URL of the Identity service
        @SerializedName("ec")
        val ec: Int, // 0 Error Code:  0 – No error
        @SerializedName("log:https")
        val logHttps: String, // HTTPS url where log message needs to send [POST API]
        @SerializedName("log:mqtt")
        val logMqtt: LogMqtt  // MQTT connection details to optionally send device logging
    ) {
        data class LogMqtt(
            @SerializedName("hn")
            val hn: String, // Hostname of MQTT broker
            @SerializedName("pwd")
            val pwd: String, // Password to connect MQTT broker
            @SerializedName("topic")
            val topic: String,  // Topic on which log messages can be sent
            @SerializedName("un")
            val un: String  // Username to connect MQTT broker
        )
    }
}