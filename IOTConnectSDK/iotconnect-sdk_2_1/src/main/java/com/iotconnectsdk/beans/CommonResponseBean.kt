package com.iotconnectsdk.beans


import com.google.gson.JsonDeserializer
import com.google.gson.annotations.SerializedName


internal data class CommonResponseBean(
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
        val ct: Int? = null,
        @SerializedName("ec")
        val ec: Int? = null,
        @SerializedName("dt")
        val dt: String? = null
    )
}

