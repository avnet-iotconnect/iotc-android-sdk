package com.iotconnectsdk.webservices.responsebean


import com.google.gson.annotations.SerializedName

data class IdentityServiceResponse(
    @SerializedName("d")
    val d: D
) {
    data class D(
        @SerializedName("ct")
        val ct: Int,
        @SerializedName("ec")
        val ec: Int,
        @SerializedName("has")
        val has: Has,
        @SerializedName("meta")
        val meta: Meta,
        @SerializedName("p")
        val p: P
    ) {
        data class Has(
            @SerializedName("attr")
            val attr: Int,
            @SerializedName("d")
            val d: Int,
            @SerializedName("ota")
            val ota: Int,
            @SerializedName("r")
            val r: Int,
            @SerializedName("set")
            val set: Int
        )

        data class Meta(
            @SerializedName("at")
            val at: Int,
            @SerializedName("cd")
            val cd: String,
            @SerializedName("df")
            var df: Int,
            @SerializedName("edge")
            val edge: Int,
            @SerializedName("gtw")
            val gtw: Gtw,
            @SerializedName("hwv")
            val hwv: String,
            @SerializedName("pf")
            val pf: Int,
            @SerializedName("swv")
            val swv: String,
            @SerializedName("v")
            val v: Double
        ) {
            data class Gtw(
                @SerializedName("g")
                val g: String,
                @SerializedName("tg")
                val tg: String
            )
        }

        data class P(
            @SerializedName("auth")
            val auth: String,
            @SerializedName("h")
            val h: String,
            @SerializedName("id")
            val id: String,
            @SerializedName("n")
            val n: String,
            @SerializedName("p")
            val p: Int,
            @SerializedName("pwd")
            val pwd: String,
            @SerializedName("rid")
            val rid: String,
            @SerializedName("topics")
            val topics: Topics,
            @SerializedName("un")
            val un: String,
            @SerializedName("url")
            val url: String
        ) {
            data class Topics(
                @SerializedName("ack")
                val ack: String,
                @SerializedName("c2d")
                val c2d: String,
                @SerializedName("di")
                val di: String,
                @SerializedName("dl")
                val dl: String,
                @SerializedName("erm")
                val erm: String,
                @SerializedName("erpt")
                val erpt: String,
                @SerializedName("flt")
                val flt: String,
                @SerializedName("hb")
                val hb: String,
                @SerializedName("od")
                val od: String,
                @SerializedName("rpt")
                val rpt: String
            )
        }
    }
}