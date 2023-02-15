package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName


data class GetAttributeBean(
    @SerializedName("p")
    val p: String,
    @SerializedName("dt")
    val dt: Int, // 0
    @SerializedName("tg")
    val tg: String? = null,
    @SerializedName("dv")
    val dv: String? = null,
    @SerializedName("ln")
    val ln: String? = null, // Temp
    @SerializedName("sq")
    val sq: Int? = null, // 1
    @SerializedName("tw")
    val tw: String? = null,
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
        val sq: Int, // 1
        @SerializedName("tg")
        val tg: String,
        @SerializedName("tw")
        val tw: String? = null
    )
}
