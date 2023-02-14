package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName

data class GetAttToView(
    @SerializedName("d")
    val d: List<D>? = null
) {
    data class D(
        @SerializedName("device")
        var device: Device? = null,
        @SerializedName("attributes")
        var attributes: List<GetAttributeBean>? = null
    ) {
        data class Device(
            @SerializedName("id")
            var id: String? = null, // AndroidGateway
            @SerializedName("tg")
            var tg: String? = null // p
        )

        data class Attribute(
            @SerializedName("dt")
            val dt: Int? = null, // 5
            @SerializedName("dv")
            val dv: String? = null,
            @SerializedName("ln")
            val ln: String? = null, // Temp
            @SerializedName("sq")
            val sq: Int? = null, // 1
            @SerializedName("tg")
            val tg: String? = null, // p
            @SerializedName("tw")
            val tw: String? = null,
            @SerializedName("d")
            val d: List<D>? = null,
            @SerializedName("p")
            val p: String? = null // Gyroscope
        ) {
            data class D(
                @SerializedName("dt")
                val dt: Int? = null, // 5
                @SerializedName("dv")
                val dv: String? = null,
                @SerializedName("ln")
                val ln: String? = null, // x
                @SerializedName("sq")
                val sq: Int? = null, // 1
                @SerializedName("tg")
                val tg: String? = null, // p
                @SerializedName("tw")
                val tw: String? = null
            )
        }
    }
}