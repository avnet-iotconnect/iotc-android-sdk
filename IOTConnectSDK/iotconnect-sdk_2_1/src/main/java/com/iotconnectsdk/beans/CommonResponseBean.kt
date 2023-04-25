package com.iotconnectsdk.beans


import com.google.gson.JsonDeserializer
import com.google.gson.annotations.SerializedName


data class CommonResponseBean(
    @SerializedName("d")
    val d: D? = D()
) {
    data class D(
        @SerializedName("att")
        val att: List<GetAttributeBean>? = null,
        @SerializedName("set")
        val set: List<GetSettingBean>? = null,
        @SerializedName("r")
        val edge: List<GetEdgeRuleBean>? = null,
        @SerializedName("d")
        var childDevice: ArrayList<GetChildDeviceBean> = ArrayList(),
        @SerializedName("ota")
        val ota: GetOTAUpdateBean? = null,
        @SerializedName("ct")
        val ct: Int? = null, // 201
        @SerializedName("ec")
        val ec: Int? = null, // 0
        @SerializedName("dt")
        val dt: String? = null // 2023-02-03T08:36:00.4237005Z
    )
/*
    var deserializer: JsonDeserializer<CommonResponseBean> =
        JsonDeserializer<Any?> { json, typeOfT, context ->
            val location: MutableList<String> = ArrayList()
            if (json.isJsonArray) {
                val jsonArray = json.asJsonArray
                for (jsonElement in jsonArray) {
                    location.add(jsonElement.asString)
                }
            } else {
                location.add(json.asString)
            }
            UserLocation(location)
        }*/
}

