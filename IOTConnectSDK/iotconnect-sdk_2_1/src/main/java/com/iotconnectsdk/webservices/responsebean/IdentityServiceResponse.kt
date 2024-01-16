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
        val ec: Int, // 0 Error Code:  0 – No error
        @SerializedName("has")
        val has: Has, // Device ability information
        @SerializedName("meta")
        val meta: Meta,  // Device Meta information
        @SerializedName("p")
        val p: P // Protocol information
    ) {
        data class Has(
            @SerializedName("attr")
            val attr: Int, // If 1 – Device can send 201 message to get all attribute details
            @SerializedName("d")
            val d: Int, //  If 1 – Gateway Device can send 204 message to get all child devices
            @SerializedName("ota")
            val ota: Int, //If 1 – Device can send 205 message to get pending OTA
            @SerializedName("r")
            val r: Int, //If 1 – Edge Device can send 203 message to get all rules
            @SerializedName("set")
            val set: Int //If 1 – Device can send 202 message to get updates on settings/twins
        )

        data class Meta(
            @SerializedName("at")
            val at: Int,  // Authentication type of device.
            @SerializedName("cd")
            val cd: String, // Unique code with 7 or 8 characters.
            @SerializedName("df")
            var df: Int,  //Data Frequency defined on template to control device data send frequency
            @SerializedName("edge")
            val edge: Int, //1 – If device is Edge Device
            @SerializedName("gtw")
            val gtw: Gtw, //** gtw will be null if device is not Gateway
            @SerializedName("hwv")
            val hwv: String, // Hardware version of the firmware pushed by OTA
            @SerializedName("pf")
            val pf: Int, // 1 – If need to add prefix while enrolling into Azure DPS
            @SerializedName("swv")
            val swv: String,  // Software version of the firmware pushed by OTA Device software version
            @SerializedName("v")
            val v: Double // 2.1 --- Message Protocol Version
        ) {
            data class Gtw(
                @SerializedName("g")
                val g: String, // GUID represents the device GUID
                @SerializedName("tg")
                val tg: String // String represents the gateway Tag
            )
        }

        data class P(
            @SerializedName("auth")
            val auth: String,  //A string represents the authorization of url
            @SerializedName("h")
            val h: String, // A string represents the mqtt host name to connect
            @SerializedName("id")
            val id: String, //A string represents the mqtt client id
            @SerializedName("n")
            val n: String,  // mqtt -- A string represents the name of protocol.
            @SerializedName("p")
            val p: Int, // A fixed integer represents port to connect mqtt host
            @SerializedName("pwd")
            val pwd: String,  //A string represents the password to connect mqtt broker
            @SerializedName("rid")
            val rid: String,  //A string represents registration id (if device is not acquired)
            @SerializedName("topics")
            val topics: Topics,  // Object contains the topics for device to send and receive messages
            @SerializedName("un")
            val un: String,  //A string represents the username for mqtt connection
            @SerializedName("url")
            val url: String // A string represents the mqtt host name to connect
        ) {
            data class Topics(
                @SerializedName("ack")
                val ack: String,  // A string represents the publish topic to send acknowledgements
                @SerializedName("c2d")
                val c2d: String,  // A string represents the topic for subscribe to receive cloud to device messages
                @SerializedName("di")
                val di: String,   // A string represents the publish topic to send Device Identity messages
                @SerializedName("dl")
                val dl: String,  // A string represents the publish topic to send Device Logs
                @SerializedName("erm")
                val erm: String,  // A string represents the publish topic of rule matched message
                @SerializedName("erpt")
                val erpt: String,  // A string represents the publish topic of edge reporting message
                @SerializedName("flt")
                val flt: String,  // A string represents the publish topic of fault message
                @SerializedName("hb")
                val hb: String,  // A string represents the publish topic of heartbeat message
                @SerializedName("od")
                val od: String,  // A string represents the publish topic of offline message
                @SerializedName("rpt")
                val rpt: String   // A string represents the publish topic of reporting message
            )
        }
    }
}