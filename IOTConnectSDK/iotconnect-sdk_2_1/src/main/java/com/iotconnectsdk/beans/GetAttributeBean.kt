package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


internal data class GetAttributeBean(
    @SerializedName("p")
    val p: String,
    @SerializedName("dt")
    val dt: Int,
    @SerializedName("tg")
    val tg: String? = null,
    @SerializedName("tw")
    val tw: String? = null,
    @SerializedName("d")
    val d: List<D>
) {
    data class D(
        @SerializedName("ln")
        val ln: String,
        @SerializedName("dt")
        val dt: Int,
        @SerializedName("dv")
        val dv: String,
        @SerializedName("sq")
        val sq: Int,
        @SerializedName("tg")
        val tg: String,
        @SerializedName("tw")
        val tw: String? = null,
        @SerializedName("faultyTime")
        var faultyTime: Long? = null
    )
}

