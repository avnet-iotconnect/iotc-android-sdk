package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


data class GetAttributeBean(
    @SerializedName("p")
    val p: String,
    @SerializedName("dt")
    val dt: Int, // 0
    @SerializedName("d")
    val d: List<D>
) {
    data class D(
        @SerializedName("ln")
        val ln: String, // Temp
        @SerializedName("dt")
        val dt: Int, // 1
        @SerializedName("dv")
        val dv: String,
        @SerializedName("sq")
        val sq: Int // 1
    )
}

