package com.iotconnectsdk.iotconnectconfigs

import com.google.gson.annotations.SerializedName
import com.iotconnectsdk.enums.BrokerType

class SdkOptions {
    @JvmField
    @SerializedName("offlineStorage")
    var offlineStorage: OfflineStorage? = null

    @JvmField
    @SerializedName("certificate")
    var certificate: Certificate? = null

    @SerializedName("discoveryUrl")
    var discoveryUrl: String? = null

    @SerializedName("isDebug")
    var isDebug = false

    @SerializedName("skipValidation")
    var isSkipValidation = false

    @JvmField
    @SerializedName("devicePK")
    var devicePK: String? = null

}