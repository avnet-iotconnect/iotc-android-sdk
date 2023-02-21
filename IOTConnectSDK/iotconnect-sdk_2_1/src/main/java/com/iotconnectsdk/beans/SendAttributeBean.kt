package com.iotconnectsdk.beans


import com.google.gson.annotations.SerializedName

internal data class SendAttributeBean(
    @SerializedName("dt")
    val dt: String, // 2023-02-08T14:26:53.477Z
    @SerializedName("d")
    val d: List<D>
) {
    data class D(
        @SerializedName("dt")
        val dt: String, // 2023-02-08T14:26:53.512Z
        @SerializedName("d")
        val d: D
    ) {
        data class D(
            @SerializedName("Temp")
            val temp: String, // 1
            @SerializedName("Humidity")
            val humidity: String, // 2
            @SerializedName("Gyroscope")
            val gyroscope: Gyroscope
        ) {
            data class Gyroscope(
                @SerializedName("x")
                val x: String, // 3
                @SerializedName("y")
                val y: String // 4
            )
        }
    }
}