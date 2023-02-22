package com.iotconnectsdk.mqtt

import android.content.Context
import android.util.Log
import com.iotconnectsdk.R
import com.iotconnectsdk.SDKClient
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.PublishMessageCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.utils.IotSDKLogUtils
import com.iotconnectsdk.utils.IotSDKUtils
import com.iotconnectsdk.webservices.responsebean.IdentityServiceResponse
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

/**
 * Service class for MQTT data upload to cloud
 */
internal class IotSDKMQTTService private constructor(
    private val context: Context, private val protocolBean: IdentityServiceResponse.D.P,
    private val hubToSdkCallback: HubToSdkCallback,
    private val publishMessageCallback: PublishMessageCallback,
    private val twinCallbackMessage: TwinUpdateCallback, private val iotSDKLogUtils: IotSDKLogUtils,
    private val isDebug: Boolean, private val uniqueId: String
) {
    private val TAG = IotSDKMQTTService::class.java.simpleName

    private var mqttAndroidClient: MqttAndroidClient? = null

    //- To receive the desired property and update the Reported Property
    private val TWIN_PUB_TOPIC = "\$iothub/twin/PATCH/properties/reported/?\$rid=1"

    private val TWIN_SUB_TOPIC = "\$iothub/twin/PATCH/properties/desired/#"

    // - To Publish the blank message on publish and Subscribe message with desired and reported property
    private val TWIN_PUB_TOPIC_BLANK_MSG = "\$iothub/twin/GET/?\$rid=0"

    private val TWIN_SUB_TOPIC_BLANK_MSG = "\$iothub/twin/res/#"

    private val DESIRED = "desired"

    private val UNIQUE_ID = "uniqueId"

    private val TWIN_SUBTOPIC_CONTAINT = "\$iothub/twin/"

    private var subscriptionTopic: String? =
        null // = "devices/520uta-sdk003/messages/devicebound/#";

    private var publishTopic: String? = null // = "devices/520uta-sdk003/messages/events/";


    companion object {

        @Volatile
        private var iotSDKMQTTService: IotSDKMQTTService? = null
        fun getInstance(
            context: Context, protocolBean: IdentityServiceResponse.D.P,
            hubToSdkCallback: HubToSdkCallback, publishMessageCallback: PublishMessageCallback,
            twinCallbackMessage: TwinUpdateCallback, iotSDKLogUtils: IotSDKLogUtils,
            isDebug: Boolean, uniqueId: String
        ): IotSDKMQTTService? {

            synchronized(this) {
                if (iotSDKMQTTService == null) {
                    iotSDKMQTTService = IotSDKMQTTService(
                        context, protocolBean, hubToSdkCallback, publishMessageCallback,
                        twinCallbackMessage, iotSDKLogUtils, isDebug, uniqueId
                    )
                }
                return iotSDKMQTTService
            }

        }
    }

    fun clearInstance() {
        iotSDKMQTTService = null
    }


    fun connectMQTT() {
        //init log.
        iotSDKLogUtils.log(false, isDebug, "INFO_IN04", context.getString(R.string.INFO_IN04))
        subscriptionTopic = protocolBean.topics.c2d
        //   publishTopic = protocolBean.pub
        mqttAndroidClient = MqttAndroidClient(
            context, "ssl://" + protocolBean.h + ":" + protocolBean.p, protocolBean.id
        )
        mqttAndroidClient?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
            //    if (reconnect) {
                    // Because Clean Session is true, we need to re-subscribe
                    if (subscriptionTopic != null) {
                        subscribeToTopic()
                    }
                /*} else {
//                    addToHistory("Connected to: " + serverURI);
                }*/
            }

            override fun connectionLost(cause: Throwable) {
                hubToSdkCallback.onConnectionStateChange(false)
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, message: MqttMessage) {
                try {
                    if (message.payload.toString().isEmpty()) {
                        return
                    }
                    if (topic.contains(TWIN_SUB_TOPIC.substring(0, TWIN_SUB_TOPIC.length - 1))) {
                        val mainObj = JSONObject()
                        mainObj.put(DESIRED, JSONObject(String(message.payload)))
                        mainObj.put(UNIQUE_ID, uniqueId)
                        twinCallbackMessage.twinUpdateCallback(mainObj)
                    } else if (topic.contains(
                            TWIN_SUB_TOPIC_BLANK_MSG.substring(
                                0, TWIN_SUB_TOPIC_BLANK_MSG.length - 1
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
                Log.d("deliveryComplete", "::$token");
            }
        })
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = false
        mqttConnectOptions.isCleanSession = true
        mqttConnectOptions.userName = protocolBean.un
        if (protocolBean.pwd != null) {
            mqttConnectOptions.password = protocolBean.pwd.toCharArray()
        }
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

    fun subscribeToTopic() {
        iotSDKLogUtils.log(false, isDebug, "INFO_IN05", context.getString(R.string.INFO_IN05))
        try {
            val topics = arrayOf(subscriptionTopic, TWIN_SUB_TOPIC, TWIN_SUB_TOPIC_BLANK_MSG)
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

    fun getAllTwins() {
        if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
            try {
                val message = MqttMessage()
                message.payload = "".toByteArray()
                mqttAndroidClient!!.publish(TWIN_PUB_TOPIC_BLANK_MSG, message)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    fun updateTwin(msgPublish: String) {
        if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected) {
            try {
                val message = MqttMessage()
                message.payload = msgPublish.toByteArray()
                mqttAndroidClient!!.publish(TWIN_PUB_TOPIC, message)
                iotSDKLogUtils.log(
                    false, isDebug, "INFO_TP01",
                    context.getString(R.string.INFO_TP01) + " " + IotSDKUtils.currentDate
                )
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    fun publishMessage(topics: String, msgPublish: String?) {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient!!.isConnected && msgPublish != null) {
                val message = MqttMessage()
                message.payload = msgPublish.toByteArray()
                mqttAndroidClient?.publish(topics, message)
                hubToSdkCallback.onSendMsgUI(msgPublish)
                iotSDKLogUtils.log(
                    false, isDebug, "INFO_SD01",
                    context.getString(R.string.INFO_SD01) + " " + IotSDKUtils.currentDate
                )
            } else {
                iotSDKLogUtils.log(
                    true, isDebug, "ERR_SD10",
                    context.getString(R.string.ERR_SD10) + " : " + IotSDKUtils.currentDate
                )
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

}