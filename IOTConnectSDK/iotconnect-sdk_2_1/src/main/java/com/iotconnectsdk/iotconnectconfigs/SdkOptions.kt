package com.iotconnectsdk.iotconnectconfigs

import com.google.gson.annotations.SerializedName
import com.iotconnectsdk.IoTCEnvironment
import com.iotconnectsdk.enums.BrokerType

class SdkOptions {
    @JvmField
    @SerializedName("offlineStorage")
    var offlineStorage: OfflineStorage? = null

    @JvmField
    @SerializedName("certificate")
    var certificate: Certificate? = null

    @JvmField
    @SerializedName("discoveryUrl")
    var discoveryUrl: String? = null

    @JvmField
    @SerializedName("isDebug")
    var isDebug = false

    @JvmField
    @SerializedName("skipValidation")
    var isSkipValidation = false

    @JvmField
    @SerializedName("devicePK")
    var devicePK: String? = null

    @JvmField
    @SerializedName("cpId")
    var cpId = ""

    @JvmField
    @SerializedName("env")
    var env: IoTCEnvironment? = null

    @JvmField
    @SerializedName("pf")
    var pf = ""

}