package com.iotconnectsdk.beans

import com.google.gson.annotations.SerializedName

internal class Data {
    @SerializedName("ack")
    var ackId: Any? = null

    @SerializedName("cpid")
    var cpid: String? = null

    @SerializedName("ct")
    var cmdType = 0

    @SerializedName("ackb")
    var isAck = false

    @SerializedName("guid")
    var guid: String? = null

    @SerializedName("uniqueId")
    var uniqueId: String? = null

    @SerializedName("command")
    var command: String? = null
}