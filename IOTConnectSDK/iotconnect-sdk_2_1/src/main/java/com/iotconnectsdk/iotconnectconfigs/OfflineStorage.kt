package com.iotconnectsdk.iotconnectconfigs

import com.google.gson.annotations.SerializedName

class OfflineStorage {
    @JvmField
    @SerializedName("availSpaceInMb")
    var availSpaceInMb = 0

    @SerializedName("disabled")
    var isDisabled = false

    @JvmField
    @SerializedName("fileCount")
    var fileCount = 1
}