package com.iotconnectsdk


import android.content.Context
import com.google.gson.Gson
import com.iotconnectsdk.beans.D2CSendAckBean
import com.iotconnectsdk.interfaces.DeviceCallback

import com.iotconnectsdk.utils.*
import com.iotconnectsdk.utils.DateTimeUtils.currentDate
import org.json.JSONObject

/**
 * class for SDKClient
 */
class SDKClient(
    private val context: Context?,
    private val uniqueId: String?
) {

    private var iotSDKLogUtils: IotSDKLogUtils? = null

    private var isDispose = false

    private var isDebug = false

    private var sdkClientManager: SDKClientManager? = null


    /*return singleton object for this class.
     * */
    companion object {
        @Volatile
        private var sdkClient: SDKClient? = null

        @JvmStatic
        fun getInstance(
            context: Context?,
            uniqueId: String?,
            deviceCallback: DeviceCallback?,
            sdkOptions: String?,
        ): SDKClient {
            synchronized(this) {
                if (sdkClient == null) {
                    sdkClient = SDKClient(context, uniqueId)
                }
                sdkClient?.callSdkClientManager(
                    context,
                    uniqueId,
                    deviceCallback,
                    sdkOptions
                )
                return sdkClient!!
            }

        }

    }

    @JvmSynthetic
   private fun callSdkClientManager(
        context: Context?,
        uniqueId: String?,
        deviceCallback: DeviceCallback?,
        sdkOptions: String?
    ) {
        sdkClientManager = SDKClientManager.getInstance(
            context,
            uniqueId,
            deviceCallback,
            sdkOptions
        )
    }



    fun getAttributes(): String? {
        return sdkClientManager?.getAttributes()
    }


    /*
    * Get all twins from IOT connect portal
    * */
    fun getTwins() {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP04", context!!.getString(R.string.ERR_TP04))
            return
        }
        sdkClientManager?.getTwins()
    }


    /**
     *update twin if any changes is there in key value pair
     *  @param key
     * @param value
     *
     * */
    fun updateTwin(key: String?, value: String?) {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP01", context!!.getString(R.string.ERR_TP01))
            return
        }
        sdkClientManager?.updateTwin(key, value)
    }


    /**
     * send acknowledgment to IOT connect portal
     * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-to-cloud-d2c-messages/#Device_Acknowledgement
     *
     * @param ackGuid     ackGuid
     * @param status      status
     * @param msg         message
     * @param childId    childDevice(If device is of Gateway type)
     *
     *
     */

    @JvmOverloads
    fun sendAckCmd(ackGuid: String, status: Int, msg: String, childId: String="") {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_CM04", context!!.getString(R.string.ERR_CM04))
            return
        }

        val d2CSendAckBean = D2CSendAckBean(
            currentDate, D2CSendAckBean.Data(ackGuid, 0, status, msg, childId))
        val gson = Gson()
        val jsonString = gson.toJson(d2CSendAckBean)

        sdkClientManager?.sendAck(jsonString)
    }


    /**
     * send OTA Command to IOT connect portal
     * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-to-cloud-d2c-messages/#OTA
     *
     * @param ackGuid     ackGuid
     * @param status      status
     * @param msg         message
     * @param childId    childDevice(If device is of Gateway type)
     *
     *
     */
    @JvmOverloads
    fun sendOTAAckCmd(ackGuid: String, status: Int, msg: String, childId: String="") {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_CM04", context!!.getString(R.string.ERR_CM04))
            return
        }
        val d2CSendAckBean = D2CSendAckBean(
            currentDate, D2CSendAckBean.Data(ackGuid, 1, status, msg, childId))
        val gson = Gson()
        val jsonString = gson.toJson(d2CSendAckBean)

        sdkClientManager?.sendAck(jsonString)
    }


    /**
     * send Module Command to IOT connect portal
     *  https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-to-cloud-d2c-messages/#Module
     *
     * @param ackGuid     ackGuid
     * @param status      status
     * @param msg         message
     * @param childId    childDevice(If device is of Gateway type)
     *
     *
     */
    @JvmOverloads
    fun sendAckModule(ackGuid: String, status: Int, msg: String, childId: String="") {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_CM04", context!!.getString(R.string.ERR_CM04))
            return
        }

        val d2CSendAckBean = D2CSendAckBean(
            currentDate, D2CSendAckBean.Data(ackGuid, 2, status, msg, childId))
        val gson = Gson()
        val jsonString = gson.toJson(d2CSendAckBean)

        sdkClientManager?.sendAck(jsonString)

    }


    /*Disconnect the device from MQTT connection.
     * stop all timers and change the device connection status.
     * */
    fun dispose() {
        isDispose = true
        sdkClientManager?.dispose()
        sdkClient = null
    }


    /**
     * Send device data to server by calling publish method.
     *
     * @param jsonData json data from client as below.
     *
     */
    fun sendData(jsonData: String?) {
        if (isDispose) {
            if (uniqueId != null) {
                iotSDKLogUtils!!.log(
                    true, isDebug, "ERR_SD04", context!!.getString(R.string.ERR_SD04)
                )
            }
            return
        }
        sdkClientManager?.sendData(jsonData)
    }


    /*
     *https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-identity-messages/#devices
     *
     */
    fun getChildDevices() {
        sdkClientManager?.getChildDevices()
    }

    /**
     * create child device to IOT connect portal
     * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-identity-messages/#Create_Child
     *
     * @param deviceId     deviceId
     * @param deviceTag    deviceTag
     * @param displayName  displayName
     *
     *
     */
    fun createChildDevice(deviceId: String, deviceTag: String, displayName: String) {
        val innerObject = JSONObject()
        innerObject.put("id",deviceId)
        innerObject.put("tg", deviceTag)
        innerObject.put("dn", displayName)
        sdkClientManager?.createChildDevice(innerObject)
    }

    /**
     * delete child device from IOT connect portal
     * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-identity-messages/#Delete_Child
     *
     *
     * @param deviceId     deviceId
     *
     */

    fun deleteChildDevice(deviceId: String) {
        val innerObject = JSONObject()
        innerObject.put("id",deviceId )
        sdkClientManager?.deleteChildDevice(innerObject)
    }

}

