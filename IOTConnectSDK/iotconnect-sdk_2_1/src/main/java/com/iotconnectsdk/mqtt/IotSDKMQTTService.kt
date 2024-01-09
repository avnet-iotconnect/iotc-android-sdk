package com.iotconnectsdk.mqtt

import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.iotconnectsdk.BuildConfig
import com.iotconnectsdk.R
import com.iotconnectsdk.enums.BrokerType
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.PublishMessageCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.utils.DateTimeUtils
import com.iotconnectsdk.utils.IotSDKLogUtils
import com.iotconnectsdk.utils.IotSDKUrls
import com.iotconnectsdk.utils.SDKClientUtils.generateSasToken
import com.iotconnectsdk.utils.SDKClientUtils.generateSasTokenLatest
import com.iotconnectsdk.utils.SecurityHelper.createSocketFactory
import com.iotconnectsdk.webservices.responsebean.IdentityServiceResponse
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject


/**
 * Service class for MQTT data upload to cloud
 */
internal class IotSDKMQTTService private constructor(
    private val context: Context,
    private val sdkOptions: String?,
    private val protocolBean: IdentityServiceResponse.D.P,
    private val authenticationType: Int,
    private val hubToSdkCallback: HubToSdkCallback,
    private val publishMessageCallback: PublishMessageCallback,
    private val twinCallbackMessage: TwinUpdateCallback,
    private val iotSDKLogUtils: IotSDKLogUtils,
    private val isDebug: Boolean,
    private val uniqueId: String,
    private val cpId: String,
) {

    private var mqttAndroidClient: MqttAndroidClient? = null

    //- To receive the desired property and update the Reported Property
    private var TWIN_SHADOW_PUB_TOPIC = ""

    private var TWIN_SHADOW_SUB_TOPIC = ""

    // - To Publish the blank message on publish and Subscribe message with desired and reported property
    private var TWIN_SHADOW_PUB_TOPIC_BLANK_MSG = ""

    private var TWIN_SHADOW_SUB_TOPIC_BLANK_MSG = ""

    private val DESIRED = "desired"

    private val UNIQUE_ID = "uniqueId"

    private var subscriptionTopic: String? = null


    private val CPID_DEVICEID = "$cpId-$uniqueId"


    companion object {

        @Volatile
        private var iotSDKMQTTService: IotSDKMQTTService? = null
        fun getInstance(
            context: Context, sdkOptions: String?, protocolBean: IdentityServiceResponse.D.P,
            authenticationType: Int, hubToSdkCallback: HubToSdkCallback,
            publishMessageCallback: PublishMessageCallback, twinCallbackMessage: TwinUpdateCallback,
            iotSDKLogUtils: IotSDKLogUtils, isDebug: Boolean, uniqueId: String, cpId: String
        ): IotSDKMQTTService? {

            synchronized(this) {
                if (iotSDKMQTTService == null) {
                    iotSDKMQTTService = IotSDKMQTTService(
                        context,
                        sdkOptions,
                        protocolBean,
                        authenticationType,
                        hubToSdkCallback,
                        publishMessageCallback,
                        twinCallbackMessage,
                        iotSDKLogUtils,
                        isDebug,
                        uniqueId,
                        cpId
                    )
                }
                return iotSDKMQTTService
            }

        }
    }

    fun clearInstance() {
        iotSDKMQTTService = null
    }


    /*connect MQTT*/
    fun connectMQTT() {
        //init log.
        iotSDKLogUtils.log(false, isDebug, "INFO_IN04", context.getString(R.string.INFO_IN04))
        subscriptionTopic = protocolBean.topics.c2d
        mqttAndroidClient = MqttAndroidClient(
            context, "ssl://" + protocolBean.h + ":" + protocolBean.p, protocolBean.id
        )
        mqttAndroidClient?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                // Because Clean Session is true, we need to re-subscribe
                if (reconnect) {
                    if (subscriptionTopic != null) {
                        subscribeToTopic()
                    }
                }

            }

            override fun connectionLost(cause: Throwable?) {
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    if (message.payload.toString().isEmpty()) {
                        return
                    }
                    if (topic.contains(
                            TWIN_SHADOW_SUB_TOPIC.substring(
                                0,
                                TWIN_SHADOW_SUB_TOPIC.length - 1
                            )
                        )
                    ) {
                        val mainObj = JSONObject()
                        mainObj.put(DESIRED, JSONObject(String(message.payload)))
                        mainObj.put(UNIQUE_ID, uniqueId)
                        twinCallbackMessage.twinUpdateCallback(mainObj)
                    } else if (topic.contains(
                            TWIN_SHADOW_SUB_TOPIC_BLANK_MSG.substring(
                                0,
                                TWIN_SHADOW_SUB_TOPIC_BLANK_MSG.length - 1
                            )
                        )
                    ) {
                        val mainObj = JSONObject(String(message.payload))
                        mainObj.put(UNIQUE_ID, uniqueId)
                        twinCallbackMessage.twinUpdateCallback(mainObj)
                    } else {
                        hubToSdkCallback.onReceiveMsg(String(message.payload))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
            }
        })
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = true

        var sdkObj: JSONObject
        try {
            if (sdkOptions != null) {
                sdkObj = JSONObject(sdkOptions)

                if (BuildConfig.BrokerType == BrokerType.AZ.value) {
                    TWIN_SHADOW_PUB_TOPIC = "\$iothub/twin/PATCH/properties/reported/?\$rid=1"
                    TWIN_SHADOW_SUB_TOPIC = "\$iothub/twin/PATCH/properties/desired/#"

                    TWIN_SHADOW_PUB_TOPIC_BLANK_MSG = "\$iothub/twin/GET/?\$rid=0"
                    TWIN_SHADOW_SUB_TOPIC_BLANK_MSG = "\$iothub/twin/res/#"
                } else if (BuildConfig.BrokerType == BrokerType.AWS.value) {
                    TWIN_SHADOW_PUB_TOPIC =
                        "\$rid=1\$aws/things/$CPID_DEVICEID/shadow/name/${CPID_DEVICEID}_twin_shadow/report"
                    TWIN_SHADOW_SUB_TOPIC =
                        "\$aws/things/$CPID_DEVICEID/shadow/name/${CPID_DEVICEID}_twin_shadow/property-shadow"

                    TWIN_SHADOW_PUB_TOPIC_BLANK_MSG =
                        "\$aws/things/$CPID_DEVICEID/shadow/name/${CPID_DEVICEID}_twin_shadow/get"
                    TWIN_SHADOW_SUB_TOPIC_BLANK_MSG =
                        "\$aws/things/$CPID_DEVICEID/shadow/name/${CPID_DEVICEID}_twin_shadow/get/all"
                } else {
                    TWIN_SHADOW_PUB_TOPIC = "\$iothub/twin/PATCH/properties/reported/?\$rid=1"
                    TWIN_SHADOW_SUB_TOPIC = "\$iothub/twin/PATCH/properties/desired/#"

                    TWIN_SHADOW_PUB_TOPIC_BLANK_MSG = "\$iothub/twin/GET/?\$rid=0"
                    TWIN_SHADOW_SUB_TOPIC_BLANK_MSG = "\$iothub/twin/res/#"
                }

                if (authenticationType == IotSDKUrls.AUTH_TYPE_SELF_SIGN || authenticationType == IotSDKUrls.AUTH_TYPE_CA_SIGN ||
                    authenticationType == IotSDKUrls.AUTH_TYPE_CA_SIGN_INDI
                ) {
                    if (sdkObj.has("certificate")) {
                        val certificate = sdkObj.getJSONObject("certificate")

                        var caFile: String? = null
                        var clientCrtFile: String? = null
                        var clientKeyFile: String? = null



                        if (certificate.has("SSLCaPath")) {
                            caFile = certificate.getString("SSLCaPath")
                        }

                        if (certificate.has("SSLCertPath")) {
                            clientCrtFile = certificate.getString("SSLCertPath")
                        }

                        if (certificate.has("SSLKeyPath")) {
                            clientKeyFile = certificate.getString("SSLKeyPath")
                        }
                        val clientKeyPassword = ""
                        if (TextUtils.isEmpty(caFile) && TextUtils.isEmpty(clientCrtFile)
                            && TextUtils.isEmpty(clientKeyFile)
                        ) {
                            return
                        }

                        val socketFactory = createSocketFactory(
                            caFile!!,
                            clientCrtFile!!,
                            clientKeyFile!!,
                            "",
                            "",
                            ""
                        )
                        mqttConnectOptions.socketFactory = socketFactory
                    }

                } else if (authenticationType == IotSDKUrls.AUTH_TYPE_SYMMETRIC_KEY) {
                    if (sdkObj.has("devicePK")) {
                        val devicePK = sdkObj.getString("devicePK")
                        if (TextUtils.isEmpty(devicePK)) {
                            return
                        }

                        val resourceUri = "${protocolBean.h}/devices/${protocolBean.id}"
                        val generateSasToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            generateSasTokenLatest(resourceUri, devicePK)
                        } else {
                            generateSasToken(resourceUri, devicePK)
                        }
                        if (generateSasToken != null) {
                            mqttConnectOptions.password = generateSasToken.toCharArray()
                        }
                    }
                } else if (authenticationType == IotSDKUrls.AUTH_TYPE_TOKEN) {
                    if (protocolBean.pwd != null) {
                        mqttConnectOptions.password = protocolBean.pwd.toCharArray()
                    }
                }

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        mqttConnectOptions.userName = protocolBean.un

        try {
            mqttAndroidClient?.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient?.setBufferOpts(disconnectedBufferOptions)
                    subscribeToTopic()

                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    hubToSdkCallback.onConnectionStateChange(false)
                    iotSDKLogUtils.log(
                        true, isDebug, "ERR_IN13", context.getString(R.string.ERR_IN13)
                    )
                }
            })
        } catch (ex: MqttException) {
            ex.printStackTrace()
        }
    }

    /*Subscribe to specific topic*/
    fun subscribeToTopic() {
        iotSDKLogUtils.log(false, isDebug, "INFO_IN05", context.getString(R.string.INFO_IN05))
        try {
            val topics =
                arrayOf(subscriptionTopic, TWIN_SHADOW_SUB_TOPIC, TWIN_SHADOW_SUB_TOPIC_BLANK_MSG)
            mqttAndroidClient?.subscribe(topics, intArrayOf(0, 0, 0), null,
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        hubToSdkCallback.onConnectionStateChange(true)
                        publishMessageCallback.onSendMsg()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        hubToSdkCallback.onConnectionStateChange(false)
                    }
                })
        } catch (ex: MqttException) {
            ex.printStackTrace()
        }
    }

    /*Disconnect from MQTT client*/
    fun disconnectClient() {
        if (mqttAndroidClient != null) {
            try {
                mqttAndroidClient?.disconnect()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
            mqttAndroidClient?.unregisterResources()
            mqttAndroidClient = null
            hubToSdkCallback.onConnectionStateChange(false)
        }
    }

    /*get all twins from portal*/
    fun getAllTwins() {
        if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
            try {
                val message = MqttMessage()
                message.payload = "".toByteArray()
                mqttAndroidClient!!.publish(TWIN_SHADOW_PUB_TOPIC_BLANK_MSG, message)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    /*update twin in there is any changes*/
    fun updateTwin(msgPublish: String) {
        if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
            try {
                val message = MqttMessage()
                message.payload = msgPublish.toByteArray()
                mqttAndroidClient!!.publish(TWIN_SHADOW_PUB_TOPIC, message)
                iotSDKLogUtils.log(
                    false, isDebug, "INFO_TP01",
                    context.getString(R.string.INFO_TP01) + " " + DateTimeUtils.currentDate
                )
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }


    /*publish message on portal on different topics*/
    fun publishMessage(topics: String, msgPublish: String?) {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected && msgPublish != null) {
                val message = MqttMessage()
                message.payload = msgPublish.toByteArray()
                mqttAndroidClient?.publish(topics, message)
                hubToSdkCallback.onSendMsgUI(msgPublish)
                iotSDKLogUtils.log(
                    false, isDebug, "INFO_SD01",
                    context.getString(R.string.INFO_SD01) + " " + DateTimeUtils.currentDate
                )
            } else {
                iotSDKLogUtils.log(
                    true, isDebug, "ERR_SD10",
                    context.getString(R.string.ERR_SD10) + " : " + DateTimeUtils.currentDate
                )
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

}