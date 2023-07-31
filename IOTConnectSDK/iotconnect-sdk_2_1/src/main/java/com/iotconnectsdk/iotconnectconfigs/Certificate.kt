package com.iotconnectsdk.iotconnectconfigs

import com.google.gson.annotations.SerializedName

class Certificate {
    @SerializedName("SSLCaPath")
    var sSLCaPath: String? = null
        private set

    @SerializedName("SSLKeyPath")
    var sSLKeyPath: String? = null
        private set

    @SerializedName("SSLCertPath")
    var sSLCertPath: String? = null
        private set

    fun setsSLCaPath(sSLCaPath: String?) {
        this.sSLCaPath = sSLCaPath
    }

    fun setsSLKeyPath(sSLKeyPath: String?) {
        this.sSLKeyPath = sSLKeyPath
    }

    fun setsSLCertPath(sSLCertPath: String?) {
        this.sSLCertPath = sSLCertPath
    }
}