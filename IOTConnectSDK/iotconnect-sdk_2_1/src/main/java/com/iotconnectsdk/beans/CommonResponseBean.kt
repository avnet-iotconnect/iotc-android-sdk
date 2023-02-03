package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName

data class CommonResponseBean(
    @SerializedName("d")
    val d: D
) {
    data class D(
        @SerializedName("att")
        val att: List<GetAttributeBean>,
        @SerializedName("ct")
        val ct: Int, // 201
        @SerializedName("ec")
        val ec: Int, // 0
        @SerializedName("dt")
        val dt: String // 2023-02-03T08:36:00.4237005Z
    )
}