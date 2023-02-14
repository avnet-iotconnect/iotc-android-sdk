package com.softweb.iotconnectsdk.model


import com.google.gson.annotations.SerializedName

data class GetDeviceAttributes @JvmOverloads constructor(
    @SerializedName("d")
    val d: D
) {
    data class D @JvmOverloads constructor(
        @SerializedName("att")
        val att: List<Att>,
        @SerializedName("ct")
        val ct: Int, // 201
        @SerializedName("dt")
        val dt: String, // 2023-02-07T10:39:03.9471016Z
        @SerializedName("ec")
        val ec: Int // 0
    ) {
        data class Att @JvmOverloads constructor(
            @SerializedName("d")
            val d: List<D>,
            @SerializedName("dt")
            val dt: Int, // 0
            @SerializedName("p")
            val p: String,
            @SerializedName("tg")
            val tg: String,
            var deviceId: String,
        ) {
            data class D @JvmOverloads constructor(
                @SerializedName("dt")
                val dt: Int, // 1
                @SerializedName("dv")
                val dv: String,
                @SerializedName("ln")
                val ln: String, // Temp
                @SerializedName("sq")
                val sq: Int, // 1
                @SerializedName("tg")
                val tg: String,
                val deviceId: String=""
            )
        }
    }
}