package com.iotconnectsdk

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.webkit.URLUtil
import com.google.gson.Gson
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.interfaces.DeviceCallback
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.mqtt.IotSDKMQTTService
import com.iotconnectsdk.utils.*
import com.iotconnectsdk.utils.IotSDKConstant.ATTRIBUTE_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.CORE_COMMAND
import com.iotconnectsdk.utils.IotSDKConstant.DEVICE_CONNECTION_STATUS
import com.iotconnectsdk.utils.IotSDKConstant.DEVICE_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.FIRMWARE_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.PASSWORD_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.RULE_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.SETTING_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.STOP_SDK_CONNECTION

import com.iotconnectsdk.utils.IotSDKUtils.currentDate
import com.iotconnectsdk.utils.SDKClientUtils.compareForInputValidation
import com.iotconnectsdk.utils.SDKClientUtils.createCommandFormat
import com.iotconnectsdk.utils.SDKClientUtils.evaluateEdgeDeviceRuleValue
import com.iotconnectsdk.utils.SDKClientUtils.getAttName
import com.iotconnectsdk.utils.SDKClientUtils.getAttributeName
import com.iotconnectsdk.utils.SDKClientUtils.getAttributesList
import com.iotconnectsdk.utils.SDKClientUtils.getEdgeDevicePublishMainObj
import com.iotconnectsdk.utils.SDKClientUtils.getFileSizeInKB
import com.iotconnectsdk.utils.SDKClientUtils.getMainObject
import com.iotconnectsdk.utils.SDKClientUtils.getPublishStringEdgeDevice
import com.iotconnectsdk.utils.SDKClientUtils.getSdk
import com.iotconnectsdk.utils.SDKClientUtils.getSyncServiceRequest
import com.iotconnectsdk.utils.SDKClientUtils.getTag
import com.iotconnectsdk.utils.SDKClientUtils.isDigit
import com.iotconnectsdk.utils.SDKClientUtils.publishEdgeDeviceInputData
import com.iotconnectsdk.utils.SDKClientUtils.updateEdgeDeviceGyroObj
import com.iotconnectsdk.utils.SDKClientUtils.updateEdgeDeviceObj
import com.iotconnectsdk.webservices.CallWebServices
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

/**
 * class for SDKClient
 */
class SDKClient private constructor(
    private val context: Context,
    cpId: String,
    uniqueId: String,
    private val deviceCallback: DeviceCallback,
    private val twinUpdateCallback: TwinUpdateCallback,
    sdkOptions: String,
    environment: String
) : WsResponseInterface, HubToSdkCallback, TwinUpdateCallback,
    NetworkStateReceiver.NetworkStateReceiverListener {

    private var validationUtils: ValidationUtils? = null
    private var iotSDKLogUtils: IotSDKLogUtils? = null
    private var mqttService: IotSDKMQTTService? = null
    private val cpId: String
    private val uniqueId: String?
    private var commandType: String? = null
    private val sdkOptions: String?
    private var environment = "PROD"
    private var isConnected = false
    private var isDispose = false
    private var isSaveToOffline = false
    private var isDebug = false
    private var idEdgeDevice = false
    private val appVersion: String = "2.0"
    private var discoveryUrl = ""
    private var savedTime: Long = 0
    public var devicePK: String? = null
    public var skipValidation: Boolean = false
    public var keepalive: Int = 0

    //for Edge Device
    private var edgeDeviceTimersList: MutableList<Timer>? = null
    private var publishObjForRuleMatchEdgeDevice: JSONObject? = null
    private var networkStateReceiver: NetworkStateReceiver? = null
    private var fileSizeToCreateInMb = 0
    private var directoryPath: String? = null
    private var fileCount = 0
    private fun registerNetworkState() {
        try {
            networkStateReceiver = NetworkStateReceiver()
            networkStateReceiver!!.addListener(this)
            context.registerReceiver(
                networkStateReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connect() {
        directoryPath = ""
        reCheckingCountTime = 0
        commandType = null
        isDispose = false
        idEdgeDevice = false
        isSaveToOffline = false
        isDebug = false
        fileCount = 0

        //get is debug option.
        var sdkObj: JSONObject? = null
        try {
            if (sdkOptions != null) {
                sdkObj = JSONObject(sdkOptions)
                if (sdkObj.has(IS_DEBUG)) {
                    isDebug = sdkObj.getBoolean(IS_DEBUG)
                }
                if (sdkObj.has("offlineStorage")) {
                    val offlineStorage = sdkObj.getJSONObject("offlineStorage")
                    if (offlineStorage.has("disabled")) {
                        isSaveToOffline = offlineStorage.getBoolean("disabled")
                        if (!isSaveToOffline) { // false = offline data storing, true = not storing offline data
                            isSaveToOffline = isSaveToOffline

                            //Add below configuration in respective sdk configuration. We want this setting to be done form firmware. default fileCount 1 and availeSpaceInMb is unlimited.
                            fileCount =
                                if (offlineStorage.has("fileCount") && offlineStorage.getInt("fileCount") > 0) {
                                    offlineStorage.getInt("fileCount")
                                } else {
                                    1
                                }
                            if (offlineStorage.has("availSpaceInMb") && offlineStorage.getInt("availSpaceInMb") > 0) {
                                val availSpaceInMb = offlineStorage.getInt("availSpaceInMb")
                                fileSizeToCreateInMb = availSpaceInMb * 1000 / fileCount
                            } else {
                                fileSizeToCreateInMb = 0
                            }
                            directoryPath = DIRECTORY_PATH + cpId + "_" + uniqueId
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            iotSDKLogUtils!!.log(true, isDebug, "IN01", e.message!!)
            return
        }
        iotSDKLogUtils = IotSDKLogUtils.getInstance(context, cpId, uniqueId!!)
        validationUtils = ValidationUtils.getInstance(iotSDKLogUtils!!, context, isDebug)
        discoveryUrl = if (sdkOptions != null) {

            //get discovery url by checking validations.
            if (!validationUtils!!.checkDiscoveryURL(DISCOVERY_URL, sdkObj!!)) {
                if (sdkObj.has(DISCOVERY_URL)) {
                    try {
                        val discovery_Url = sdkObj.getString(DISCOVERY_URL)
                        if (!URLUtil.isValidUrl(discovery_Url)) {
                            return
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                DEFAULT_DISCOVERY_URL //set default discovery url when it is empty from client end.
            } else {
                try {
                    sdkObj.getString(DISCOVERY_URL)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    iotSDKLogUtils!!.log(true, isDebug, "ERR_IN01", e.message!!)
                    return
                }
            }
        } else {
            DEFAULT_DISCOVERY_URL //set default discovery url when sdkOption is null.
        }
        if (!validationUtils!!.isEmptyValidation(
                cpId,
                "ERR_IN04",
                context.getString(R.string.ERR_IN04)
            )
        ) return
        if (!validationUtils!!.isEmptyValidation(
                uniqueId,
                "ERR_IN05",
                context.getString(R.string.ERR_IN05)
            )
        ) return
        callDiscoveryService()
    }

    /*API call for discovery.
     *@param discoveryUrl  discovery URL ("discoveryUrl" : "https://discovery.iotconnect.io")
     * */
    private fun callDiscoveryService() {
        if (!validationUtils!!.networkConnectionCheck()) return

//        appVersion = VERSION_NAME;
        if (appVersion != null) {
            val discoveryApi =
                discoveryUrl + URL_PATH + CPID + cpId + LANG_ANDROID_VER + appVersion + ENV + environment
            CallWebServices().getDiscoveryApi(discoveryApi, this)
        }
    }

    /*get the saved sync response from shared preference.
     * */
    private val syncResponse: SyncServiceResponse?
        private get() = IotSDKPreferences.getInstance(context)!!
            .getSyncResponse(IotSDKPreferences.SYNC_RESPONSE)

    /**
     * Send device data to server by calling publish method.
     *
     * @param jsonData json data from client as bellow.
     * [{"uniqueId":"ddd2","time":"2020-10-05T08:22:50.698Z","data":{"Temp":"25","humidity":"0","abc":"abc","gyro":{"x":"0","y":"blue","z":"5"}}},{"uniqueId":"c1","time":"2020-10-05T08:22:50.704Z","data":{"Temperature":"0","Humidity":"50"}},{"uniqueId":"c2","time":"2020-10-05T08:22:50.709Z","data":{"pressure":"500","vibration":"0","gyro":{"x":"5","y":"10"}}}]
     */
    fun sendData(jsonData: String?) {
        if (isDispose) {
            if (uniqueId != null) {
                iotSDKLogUtils!!.log(
                    true,
                    isDebug,
                    "ERR_SD04",
                    context.getString(R.string.ERR_SD04)
                )
            }
            return
        }
        if (!validationUtils!!.isValidInputFormat(jsonData!!, uniqueId!!)) return
        if (!idEdgeDevice) { // simple device.
            publishDeviceInputData(jsonData)
        } else { //Edge device
            processEdgeDeviceInputData(jsonData)
        }
    }

    /*process input data to publish.
     * 1.Publish input data based on interval of "df" value.
     * "df"="60" than data is published in interval of 60 seconds. If data is publish lass than 60 second time than data is ignored.
     * 2.If "df" = 0, input data can be published on button click.
     *
     * @param jsonData       input data from framework.
     * */
    fun publishDeviceInputData(jsonData: String?) {
        val response = syncResponse
        var df = 0
        if (response != null) {
            df = response.d.sc.df
        }
        if (savedTime == 0L) {
            savedTime = currentTime
            savedTime = savedTime + df
        } else {
            val currentTime = currentTime
            if (currentTime <= savedTime) {
                return
            } else {
                savedTime = savedTime + df
            }
        }
        if (response != null && response.d != null) {
            publishDeviceInputData(jsonData, response.d)
        }
    }

    private val currentTime: Long
        private get() = System.currentTimeMillis() / 1000
    val allTwins: Unit
        get() {
            if (isDispose) {
                iotSDKLogUtils!!.log(
                    true,
                    isDebug,
                    "ERR_TP04",
                    context.getString(R.string.ERR_TP04)
                )
                return
            }
            if (mqttService != null) {
                iotSDKLogUtils!!.log(
                    false,
                    isDebug,
                    "INFO_TP02",
                    context.getString(R.string.INFO_TP02)
                )
                mqttService!!.allTwins
            }
        }

    fun updateTwin(key: String?, value: String?) {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP01", context.getString(R.string.ERR_TP01))
            return
        }
        if (!validationUtils!!.validateKeyValue(key!!, value!!)) return
        try {
            if (mqttService != null) publishMessage(JSONObject().put(key, value).toString(), true)
        } catch (e: JSONException) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP01", e.message!!)
            e.printStackTrace()
        }
    }

    /**
     * send acknowledgment
     *
     * @param obj         JSONObject object for "d"
     * @param messageType Message Type
     */
    fun sendAck(obj: JSONObject?, messageType: String?) {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_CM04", context.getString(R.string.ERR_CM04))
            return
        }
        if (!validationUtils!!.validateAckParameters(obj, messageType!!)) return
        val objMain = JSONObject()
        try {
            objMain.put(UNIQUE_ID, uniqueId)
            objMain.put(CP_ID, cpId)
            objMain.put(CURRENT_DATE, currentDate)
            objMain.put(MESSAGE_TYPE, messageType)
            objMain.putOpt(
                SDK_OBJ, getSdk(
                    environment, appVersion
                )
            )
            objMain.putOpt(D_OBJ, obj)
            publishMessage(objMain.toString(), false)
            iotSDKLogUtils!!.log(
                false,
                isDebug,
                "INFO_CM10",
                context.getString(R.string.INFO_CM10) + " " + currentDate
            )
        } catch (e: JSONException) {
            iotSDKLogUtils!!.log(true, isDebug, "CM01_CM01", e.message!!)
            e.printStackTrace()
        }
    }

    /*Disconnect the device from MQTT connection.
     * stop all timers and change the device connection status.
     * */
    fun dispose() {
        isDispose = true
        /* if (!isDispose) {
            dispose();
        } else {
            iotSDKLogUtils.log(false, this.isDebug, "INFO_DC01", context.getString(R.string.INFO_DC01));
        }*/edgeDeviceTimerStop()
        //        onDeviceConnectionStatus(false);
        if (mqttService != null) {
            mqttService!!.disconnectClient()
            mqttService!!.clearInstance() //destroy single ton object.
        }
        timerStop(reCheckingTimer)
        timerStop(timerCheckDeviceState)
        timerStop(timerOfflineSync)
        unregisterReceiver()
    }

    /* Unregister network receiver.
     *
     * */
    private fun unregisterReceiver() {
        networkStateReceiver!!.removeListener(this)
        context.unregisterReceiver(networkStateReceiver)
    }

    /* ON DEVICE CONNECTION STATUS CHANGE command create json with bellow format and provide to framework.
     *
     * {"cmdType":"0x16","data":{"cpid":"","guid":"","uniqueId":"","command":true,"ack":false,"ackId":"","cmdType":"0x16"}}
     *
     * command = (true = connected, false = disconnected)
     * */
    private fun onDeviceConnectionStatus(isConnected: Boolean) {
        val strJson = createCommandFormat(
            DEVICE_CONNECTION_STATUS,
            cpId,
            "",
            uniqueId,
            isConnected.toString() + "",
            false,
            ""
        )
        deviceCallback.onReceiveMsg(strJson)
    }

    /* On edge device rule match, send below json format to firmware.
     *{"cmdType":"0x01","data":{"cpid":"deviceData.cpId","guid":"deviceData.company","uniqueId":"device uniqueId","command":"json.cmd","ack":true,"ackId":null,"cmdType":"config.commandType.CORE_COMMAND, 0x01"}}
     *
     * */
    private fun onEdgeDeviceRuleMatched(bean: SyncServiceResponse.DBeanXX.RuleBean) {
        val strJson = createCommandFormat(CORE_COMMAND, cpId, bean.g, uniqueId, bean.cmd, true, "")
        deviceCallback.onReceiveMsg(strJson)
    }

    /*Success call back method called on service response.
     * methods : discovery service
     *           sync service
     *
     * @param methodName         called method name
     * @param response           called service response in json format
     * */
    override fun onSuccessResponse(methodName: String?, response: String?) {
        try {
            if (methodName.equals(
                    IotSDKUrls.DISCOVERY_SERVICE,
                    ignoreCase = true
                ) && response != null
            ) {
                val discoveryApiResponse =
                    Gson().fromJson(response, DiscoveryApiResponse::class.java)
                if (discoveryApiResponse != null) {
                    //BaseUrl received to sync the device information.
                    iotSDKLogUtils!!.log(
                        false,
                        isDebug,
                        "INFO_IN07",
                        context.getString(R.string.INFO_IN07)
                    )
                    if (!validationUtils!!.validateBaseUrl(discoveryApiResponse)) return
                    val baseUrl: String = discoveryApiResponse.d.bu + SYNC
                    IotSDKPreferences.getInstance(context)!!
                        .putStringData(IotSDKPreferences.SYNC_API, baseUrl)
                    callSyncService()
                }
            } else if (methodName.equals(
                    IotSDKUrls.SYNC_SERVICE,
                    ignoreCase = true
                ) && response != null
            ) {
                val syncServiceResponseData =
                    Gson().fromJson(response, SyncServiceResponse::class.java)
                if (syncServiceResponseData != null && syncServiceResponseData.d != null) {

                    //save sync response to shared pref
                    if (commandType == null) {

                        //Device information not found. While sync the device when get the response code 'rc' not equal to '0'
                        val rc = syncServiceResponseData.d.rc
                        validationUtils!!.responseCodeMessage(rc)
                        if (rc == 1 || rc == 3 || rc == 4 || rc == 6) {
                            if (reCheckingCountTime <= 3) {
                                reChecking()
                            } else {
                                timerStop(reCheckingTimer)
                                iotSDKLogUtils!!.log(
                                    true,
                                    isDebug,
                                    "ERR_IN10",
                                    context.getString(R.string.ERR_IN10)
                                )
                            }
                            return
                        } else if (rc != 0) {
//                            onConnectionStateChange(false);
                            iotSDKLogUtils!!.log(
                                true,
                                isDebug,
                                "ERR_IN10",
                                context.getString(R.string.ERR_IN10)
                            )
                            return
                        } else {
                            timerStop(reCheckingTimer)
                        }

                        //got perfect sync response for the device.
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_IN01",
                            context.getString(R.string.INFO_IN01)
                        )

                        //check edge enable
                        if (syncServiceResponseData.d.ee == 1) {
                            idEdgeDevice = true
                            try {
                                processEdgeDeviceTWTimer(syncServiceResponseData)
                            } catch (e: Exception) {
                                iotSDKLogUtils!!.log(true, isDebug, "ERR_EE01", e.message!!)
                            }
                        }

                        //save the sync response to sahred pref.
                        IotSDKPreferences.getInstance(context)!!
                            .putStringData(IotSDKPreferences.SYNC_RESPONSE, response)
                    }
                    iotSDKLogUtils!!.log(
                        false,
                        isDebug,
                        "INFO_IN01",
                        context.getString(R.string.INFO_IN01)
                    )
                    if (commandType == null && syncServiceResponseData.d.att != null) {
//                        deviceCallback.getAttributes(getAttributes(syncServiceResponseData.getD()));
                    }
                    if (commandType == null && syncServiceResponseData.d.p != null && syncServiceResponseData.d.p.h != null) {
                        callMQTTService()
                    } else if (commandType != null && commandType.equals(
                            ATTRIBUTE_INFO_UPDATE,
                            ignoreCase = true
                        )
                    ) {
                        if (idEdgeDevice) edgeDeviceTimerStop()
                        updateAttribute(syncServiceResponseData)
                    } else if (commandType != null && commandType.equals(
                            DEVICE_INFO_UPDATE,
                            ignoreCase = true
                        )
                    ) {
                        updateDeviceInfo(syncServiceResponseData)
                    } else if (commandType != null && commandType.equals(
                            PASSWORD_INFO_UPDATE,
                            ignoreCase = true
                        )
                    ) {
                        dispose()
                        updatePasswordInfo(syncServiceResponseData)
                    } else if (commandType != null && commandType.equals(
                            SETTING_INFO_UPDATE,
                            ignoreCase = true
                        )
                    ) {
                        updateSettings(syncServiceResponseData)
                    } else if (commandType != null && commandType.equals(
                            RULE_INFO_UPDATE,
                            ignoreCase = true
                        )
                    ) {
                        updateRule(syncServiceResponseData)
                    }
                } else {
//                    onConnectionStateChange(false);
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*Fail call back method called on service response.
     * methods : discovery service
     *           sync service
     *
     * @param methodName         called method name
     * @param ERRCode            error code
     * @param message            error message
     * */
    override fun onFailedResponse(methodName: String?, ERRCode: Int, message: String?) {
        if (methodName.equals(IotSDKUrls.DISCOVERY_SERVICE, ignoreCase = true)) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_IN09", context.getString(R.string.ERR_IN09))
        } else if (methodName.equals(IotSDKUrls.SYNC_SERVICE, ignoreCase = true)) {
        }
    }

    /*process data for edge device timer start.
     *
     * @param response      Sync service response.
     * */
    private fun processEdgeDeviceTWTimer(response: SyncServiceResponse?) {
        val attributeList = response!!.d.att
        edgeDeviceAttributeMap = ConcurrentHashMap()
        edgeDeviceAttributeGyroMap = ConcurrentHashMap()
        edgeDeviceTimersList = ArrayList()
        for (bean in attributeList) {
            if (bean.p != null && !bean.p.isEmpty()) {
                // if for not empty "p":"gyro"
                val gyroAttributeList: MutableList<TumblingWindowBean?> = ArrayList()
                val listD = bean.d
                for (beanX in listD) {
                    val attributeLn = beanX.ln

                    //check attribute input type is numeric or not, ignore the attribute if it is not numeric.
                    if (beanX.dt != 1) {
                        val twb = TumblingWindowBean()
                        twb.attributeName = attributeLn
                        gyroAttributeList.add(twb)
                    }
                }
                (edgeDeviceAttributeGyroMap as ConcurrentHashMap<String?, List<TumblingWindowBean?>?>)[bean.p] =
                    gyroAttributeList
                edgeDeviceTWTimerStart(bean.tw, bean.p, bean.tg)
            } else {
                val listD = bean.d
                for (beanX in listD) {
                    val ln = beanX.ln
                    val tag = beanX.tg

                    //check attribute input type is numeric or not, ignore the attribute if it is not numeric.
                    if (beanX.dt != 1) edgeDeviceTWTimerStart(beanX.tw, ln, tag)
                }
            }
        }
    }

    /*Start timer for Edge device attributes (humidity,temp,gyro etc...), each attribute has it's own timer.
     *with delay of Tumbling window ("tw":"10s") time in seconds.
     *
     * @param  twTime       Tumbling window ("tw":"10s") time in seconds
     * @param  ln           attribute name humidity,temp,gyro etc...
     * @param  tag          attribute tag
     */
    private fun edgeDeviceTWTimerStart(twTime: String, ln: String, tag: String) {
        val tw = twTime.replace("[^\\d.]".toRegex(), "").toInt()
        edgeDeviceAttributeMap!![ln] = TumblingWindowBean()
        val timerTumblingWindow = Timer()
        edgeDeviceTimersList!!.add(timerTumblingWindow)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                val dtg: String = syncResponse?.getD()!!.getDtg()
                val publishObj = publishEdgeDeviceInputData(
                    ln,
                    tag,
                    edgeDeviceAttributeGyroMap!!,
                    edgeDeviceAttributeMap!!,
                    uniqueId!!,
                    cpId,
                    environment,
                    appVersion,
                    dtg
                )

                //check publish object is not empty of data. check to inner "d":[] object. Example below json string inner "d":[] object is empty.
                //{"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2021-01-11T02:36:19.644Z","mt":2,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2021-01-11T02:36:19.644Z","tg":"","d":[]}]}
                var isPublish = true
                try {
                    val dArray = publishObj!!.getJSONArray(D_OBJ)
                    for (i in 0 until dArray.length()) {
                        val innerDObj = dArray.getJSONObject(i).getJSONArray(D_OBJ)
                        if (innerDObj.length() <= 0) {
                            isPublish = false
                        }
                    }

                    //return on "d":[] object is empty.
                    if (!isPublish) return
                } catch (e: JSONException) {
                    e.printStackTrace()
                    return
                }
                if (publishObj != null) {
                    publishMessage(publishObj.toString(), false)
                }
            }
        }
        timerTumblingWindow.scheduleAtFixedRate(
            timerTask,
            (tw * 1000).toLong(),
            (tw * 1000).toLong()
        )
    }

    /*Stop timer for Edge device attributes (humidity,temp,gyro etc...).
     * Clear the list and map collection.
     * */
    private fun edgeDeviceTimerStop() {
        if (edgeDeviceAttributeMap != null) edgeDeviceAttributeMap!!.clear()
        if (edgeDeviceAttributeGyroMap != null) edgeDeviceAttributeGyroMap!!.clear()
        if (edgeDeviceTimersList != null) for (timer in edgeDeviceTimersList!!) {
            timerStop(timer)
        }
    }

    private fun timerStop(timer: Timer?) {
        if (timer != null) {
            timer.cancel()
            timer.purge()
        }
    }

    /*Update attributes list when there is update on web.
     * Save the updated sync response to shared preference.
     * If edge device is connected than re-start all timers.
     *
     * @param  attributesUpdatedResponse         attribute list response.
     * */
    private fun updateAttribute(attributesUpdatedResponse: SyncServiceResponse) {
        val savedResponse = syncResponse
        savedResponse!!.d.att = attributesUpdatedResponse.d.att
        IotSDKPreferences.getInstance(context)!!
            .putStringData(IotSDKPreferences.SYNC_RESPONSE, Gson().toJson(savedResponse))

        //re-start the edge device timers for all attributes on update of attributes.
        if (idEdgeDevice) processEdgeDeviceTWTimer(savedResponse)
    }

    /*Update password info when there is update on web.
     *
     * */
    private fun updatePasswordInfo(passwordResponse: SyncServiceResponse) {
        val savedResponse = syncResponse
        savedResponse!!.d.p = passwordResponse.d.p
        IotSDKPreferences.getInstance(context)!!
            .putStringData(IotSDKPreferences.SYNC_RESPONSE, Gson().toJson(savedResponse))
        callMQTTService()
    }

    private fun updateSettings(settingResponse: SyncServiceResponse) {
        val savedResponse = syncResponse
        savedResponse!!.d.set = settingResponse.d.set
        IotSDKPreferences.getInstance(context)!!
            .putStringData(IotSDKPreferences.SYNC_RESPONSE, Gson().toJson(savedResponse))
    }

    private fun updateDeviceInfo(deviceResponse: SyncServiceResponse) {
        val savedResponse = syncResponse
        savedResponse!!.d.d = deviceResponse.d.d
        IotSDKPreferences.getInstance(context)!!
            .putStringData(IotSDKPreferences.SYNC_RESPONSE, Gson().toJson(savedResponse))
    }

    private fun updateRule(ruleResponse: SyncServiceResponse) {
        val savedResponse = syncResponse
        savedResponse!!.d.r = ruleResponse.d.r
        IotSDKPreferences.getInstance(context)!!
            .putStringData(IotSDKPreferences.SYNC_RESPONSE, Gson().toJson(savedResponse))
    }

    /*re-checking the device connection after interval of 10 seconds for 3 times.
     * in case of device connect button is clicked and than device creating process is done on web.
     * */
    private fun reChecking() {
        iotSDKLogUtils!!.log(false, isDebug, "INFO_IN06", context.getString(R.string.INFO_IN06))
        startReCheckingTimer()
    }

    private var reCheckingTimer: Timer? = null

    /*start timer for re-checking the device connection.
     * after 3 check of 10 second time interval it will be stop.
     * or it can be stop on device found within time interval.
     * */
    private fun startReCheckingTimer() {
        reCheckingTimer = Timer()
        val timerTaskObj: TimerTask = object : TimerTask() {
            override fun run() {
                (context as Activity).runOnUiThread { callSyncService() }
                reCheckingCountTime++
                timerStop(reCheckingTimer)
            }
        }
        reCheckingTimer!!.schedule(timerTaskObj, 10000, 10000)
    }

    /*MQTT service call to connect device.
     * */
    private fun callMQTTService() {
        val response = syncResponse
        if (response!!.d == null || response.d.p == null) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_IN11", context.getString(R.string.ERR_IN11))
            return
        }
        mqttService = IotSDKMQTTService.getInstance(
            context,
            response.d.p,
            this,
            this,
            iotSDKLogUtils!!,
            isDebug,
            uniqueId!!
        )
        mqttService!!.connectMQTT()
    }

    override fun onFailedResponse(message: String?) {}
    private fun callSyncService() {
        if (!validationUtils!!.networkConnectionCheck()) return
        val baseUrl = IotSDKPreferences.getInstance(
            context
        )!!.getStringData(IotSDKPreferences.SYNC_API)
        if (baseUrl != null) {
            CallWebServices().sync(
                baseUrl, getSyncServiceRequest(
                    cpId, uniqueId, commandType
                ), this
            )
        }
    }//CREATE DEVICE OBJECT, "device":{"id":"dee02","tg":"gateway"}

    //ADD TO MAIN OBJECT

    //ADD MAIN BOJ TO ARRAY.

    //Attributes data not found
    /*Method creates json string to be given to framework.
     * [{"device":{"id":"ddd2","tg":"gateway"},"attributes":[{"agt":0,"dt":0,"dv":"5 to 20, 25","ln":"Temp","sq":1,"tg":"gateway","tw":""},{"agt":0,"dt":0,"dv":"","ln":"humidity","sq":8,"tg":"gateway","tw":""},{"agt":0,"dt":1,"dv":"","ln":"abc","sq":12,"tg":"gateway","tw":""},{"agt":0,"d":[{"agt":0,"dt":0,"dv":"","ln":"x","sq":1,"tg":"gateway","tw":""},{"agt":0,"dt":1,"dv":"red, gray,   blue","ln":"y","sq":2,"tg":"gateway","tw":""},{"agt":0,"dt":0,"dv":"-5 to 5, 10","ln":"z","sq":3,"tg":"gateway","tw":""}],"dt":2,"p":"gyro","tg":"gateway","tw":""}]},{"device":{"id":"c1","tg":"zg1"},"attributes":[{"agt":0,"dt":0,"dv":"","ln":"Temperature","sq":2,"tg":"zg1","tw":""},{"agt":0,"dt":0,"dv":"30 to 50","ln":"Humidity","sq":3,"tg":"zg1","tw":""}]},{"device":{"id":"c2","tg":"zg2"},"attributes":[{"agt":0,"dt":0,"dv":"200 to 500","ln":"pressure","sq":5,"tg":"zg2","tw":""},{"agt":0,"dt":0,"dv":"","ln":"vibration","sq":6,"tg":"zg2","tw":""},{"agt":0,"d":[{"agt":0,"dt":0,"dv":"-1to5","ln":"x","sq":1,"tg":"zg2","tw":""},{"agt":0,"dt":0,"dv":"5to10","ln":"y","sq":2,"tg":"zg2","tw":""}],"dt":2,"p":"gyro","tg":"zg2","tw":""}]}]
     * */
    //    private String getAttributes(SyncServiceResponse.DBeanXX data) {
    val attributes: String
        get() {
            val mainArray = JSONArray()
            val response = syncResponse
            if (response != null) {
                val data = response.d
                try {
                    for (device in data.d) {

                        //CREATE DEVICE OBJECT, "device":{"id":"dee02","tg":"gateway"}
                        val deviceObj = JSONObject()
                        deviceObj.put(DEVICE_ID, device.id)
                        deviceObj.put(DEVICE_TAG, device.tg)

                        //ADD TO MAIN OBJECT
                        val mainObj = JSONObject()
                        mainObj.put(DEVICE, deviceObj)
                        mainObj.put(ATTRIBUTES, getAttributesList(data.att, device.tg))

                        //ADD MAIN BOJ TO ARRAY.
                        mainArray.put(mainObj)

                        //Attributes data not found
                        if (mainArray.length() == 0) {
                            iotSDKLogUtils!!.log(
                                true,
                                isDebug,
                                "ERR_GA02",
                                context.getString(R.string.ERR_GA02)
                            )
                        } else {
                            iotSDKLogUtils!!.log(
                                false,
                                isDebug,
                                "INFO_GA01",
                                context.getString(R.string.INFO_GA01)
                            )
                        }
                    }
                } catch (e: Exception) {
                    iotSDKLogUtils!!.log(true, isDebug, "ERR_GA01", e.message!!)
                    e.printStackTrace()
                }
            }
            return mainArray.toString()
        }

    override fun onReceiveMsg(message: String?) {
        if (message != null) {
            try {
                val mainObject = JSONObject(message)
                val cmd = mainObject.getString(CMD_TYPE)
                when (cmd) {
                    CORE_COMMAND -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM01",
                            context.getString(R.string.INFO_CM01)
                        )
                        deviceCallback.onReceiveMsg(message)
                    }
                    DEVICE_CONNECTION_STATUS -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM16",
                            context.getString(R.string.INFO_CM16)
                        )
                        deviceCallback.onReceiveMsg(message)
                    }
                    FIRMWARE_UPDATE -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM02",
                            context.getString(R.string.INFO_CM02)
                        )
                        deviceCallback.onReceiveMsg(message)
                    }
                    ATTRIBUTE_INFO_UPDATE -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM03",
                            context.getString(R.string.INFO_CM03)
                        )
                        commandType = ATTRIBUTE_INFO_UPDATE
                        callSyncService()
                    }
                    SETTING_INFO_UPDATE -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM04",
                            context.getString(R.string.INFO_CM04)
                        )
                        commandType = SETTING_INFO_UPDATE
                        callSyncService()
                    }
                    PASSWORD_INFO_UPDATE -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM05",
                            context.getString(R.string.INFO_CM05)
                        )
                        commandType = PASSWORD_INFO_UPDATE
                        callSyncService()
                    }
                    DEVICE_INFO_UPDATE -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM06",
                            context.getString(R.string.INFO_CM06)
                        )
                        commandType = DEVICE_INFO_UPDATE
                    }
                    RULE_INFO_UPDATE -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM07",
                            context.getString(R.string.INFO_CM07)
                        )
                        commandType = RULE_INFO_UPDATE
                        callSyncService()
                    }
                    STOP_SDK_CONNECTION -> {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_CM09",
                            context.getString(R.string.INFO_CM09)
                        )
                        dispose()
                    }
                    else -> {}
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    override fun twinUpdateCallback(data: JSONObject?) {
        try {
            data!!.put(UNIQUE_ID, uniqueId)
            twinUpdateCallback.twinUpdateCallback(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onSendMsg(message: String?) {
        if (message != null) {
//            hubToSdkCallback.onSendMsg(message);
            deviceCallback.onReceiveMsg(message)
        }
    }

    override fun onConnectionStateChange(isConnected: Boolean) {
        if (isConnected) {
            iotSDKLogUtils!!.log(false, isDebug, "INFO_IN02", context.getString(R.string.INFO_IN02))
        } else {
            iotSDKLogUtils!!.log(false, isDebug, "INFO_IN03", context.getString(R.string.INFO_IN03))
        }
        this.isConnected = isConnected
        //        deviceCallback.onConnectionStateChange(isConnected);
        onDeviceConnectionStatus(isConnected)
    }

    /*Process the edge device input data from client.
     *
     * @param  jsonData
     * */
    fun processEdgeDeviceInputData(jsonData: String?) {
        val response = syncResponse
        publishObjForRuleMatchEdgeDevice = null
        if (response != null) {
            try {
                val jsonArray = JSONArray(jsonData)
                for (i in 0 until jsonArray.length()) {
                    val dataObj = jsonArray.getJSONObject(i).getJSONObject(DATA)
                    val dataJsonKey = dataObj.keys()
                    val tag = getTag(uniqueId!!, response.d)
                    while (dataJsonKey.hasNext()) {
                        val key = dataJsonKey.next()
                        val value = dataObj.getString(key)
                        if (!value.replace("\\s".toRegex(), "")
                                .isEmpty() && JSONTokener(value).nextValue() is JSONObject
                        ) {
                            val AttObj = JSONObject()

                            // get value for
                            // "gyro": {"x":"7","y":"8","z":"9"}
                            val innerObj = dataObj.getJSONObject(key)
                            val innerJsonKey = innerObj.keys()
                            while (innerJsonKey.hasNext()) {
                                val innerKey = innerJsonKey.next()
                                val innerKValue = innerObj.getString(innerKey)

                                //check for input validation dv=data validation dv="data validation". {"ln":"x","dt":0,"dv":"10to20","tg":"","sq":1,"agt":63,"tw":"40s"}
                                val `val` = compareForInputValidation(
                                    innerKey,
                                    innerKValue,
                                    tag,
                                    response.d
                                )

                                //ignore string value for edge device.
                                if (isDigit(innerKValue) && `val` != 1) {
                                    updateEdgeDeviceGyroObj(
                                        key,
                                        innerKey,
                                        innerKValue,
                                        edgeDeviceAttributeGyroMap!!
                                    )
                                    EvaluateRuleForEdgeDevice(
                                        response.d.r,
                                        key,
                                        innerKey,
                                        innerKValue,
                                        jsonData,
                                        AttObj
                                    )
                                }
                            }
                            //publish
                            publishRuleEvaluatedData()
                        } else {

                            //check for input validation dv="data validation". {"ln":"abc","dt":0,"dv":"10","tg":"","sq":8,"agt":63,"tw":"60s"}
                            val `val` = compareForInputValidation(key, value, tag, response.d)

                            //ignore string value for edge device.
                            if (isDigit(value) && `val` != 1) {
                                updateEdgeDeviceObj(key, value, edgeDeviceAttributeMap!!)
                                EvaluateRuleForEdgeDevice(
                                    response.d.r,
                                    key,
                                    null,
                                    value,
                                    jsonData,
                                    null
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                iotSDKLogUtils!!.log(true, isDebug, "ERR_EE01", e.message!!)
                e.printStackTrace()
            }
        }
    }

    /*Process edge device rule matched attribute and publish.
     *
     * @param    ruleBeansList      array list of rule  ("r":[{"g":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","es":"514076B1-3C21-4849-A777-F423B1821FC7","con":"humidity = 15","att":[{"g":["AF644BEB-C615-4587-AE38-8EAE59248376"]}],"cmd":"reboot"}])
     * @param    parentKey          parent attribute name (temp, gyro etc..)
     * @param    innerKey           gyro object child key (x,y,z etc..)
     * @param    inputValue         attribute input value
     * @param    inputJsonString    json string from client input.
     * @param    attObj             empty json object to collect rule matched gyro child attributes objects.
     * * */
    private fun EvaluateRuleForEdgeDevice(
        ruleBeansList: List<SyncServiceResponse.DBeanXX.RuleBean>?,
        parentKey: String,
        innerKey: String?,
        inputValue: String,
        inputJsonString: String?,
        attObj: JSONObject?
    ) {
        try {
            if (ruleBeansList != null) {
                val value = inputValue.replace("\\s".toRegex(), "").toInt()
                for (bean in ruleBeansList) {
                    val con = bean.con
                    val attKey = getAttributeName(con)
                    //match parent attribute name (eg. temp == temp OR gyro == gyro)
                    if (attKey != null && parentKey == attKey) {

//                        if (innerKey != null) {

                        //for gyro type object. "gyro": {"x":"7","y":"8","z":"9"}
                        if (innerKey != null && con.contains("#") && con.contains("AND")) { //ac1#vibration.x > 5 AND ac1#vibration.y > 10
                            val param = con.split("AND".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            for (i in param.indices) {
                                val att = param[i]
                                if (att.contains(".")) { //gyro#vibration.x > 5
                                    val KeyValue =
                                        att.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                            .toTypedArray()
                                    val parent = KeyValue[0].split("#".toRegex())
                                        .dropLastWhile { it.isEmpty() }
                                        .toTypedArray() //gyro#vibration
                                    val parentAttName = parent[0] //gyro
                                    val childAttName = parent[1] //vibration
                                    setPublishJsonForRuleMatchedEdgeDevice(
                                        KeyValue[1],
                                        innerKey,
                                        value,
                                        attObj,
                                        parentKey,
                                        bean,
                                        inputJsonString
                                    )
                                } else if (con.contains("#")) {
                                    val parent =
                                        att.split("#".toRegex()).dropLastWhile { it.isEmpty() }
                                            .toTypedArray() //gyro#x > 5
                                    val parentAttName = parent[0] //gyro
                                    setPublishJsonForRuleMatchedEdgeDevice(
                                        parent[1],
                                        innerKey,
                                        value,
                                        attObj,
                                        parentKey,
                                        bean,
                                        inputJsonString
                                    )
                                }
                            }
                        } else if (innerKey != null && con.contains("#")) { //gyro#x > 5  //  ac1#vibration.x > 5
                            val parent = con.split("#".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray() //gyro#x > 5
                            val parentAttName = parent[0] //gyro
                            setPublishJsonForRuleMatchedEdgeDevice(
                                parent[1],
                                innerKey,
                                value,
                                attObj,
                                parentKey,
                                bean,
                                inputJsonString
                            )
                        } else if (innerKey != null && con.contains(".") && con.contains("AND")) { //"gyro.x = 10 AND gyro.y > 10",
                            val param = con.split("AND".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            for (i in param.indices) {
                                val KeyValue =
                                    param[i].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                        .toTypedArray() //gyro.x = 10
                                setPublishJsonForRuleMatchedEdgeDevice(
                                    KeyValue[1],
                                    innerKey,
                                    value,
                                    attObj,
                                    parentKey,
                                    bean,
                                    inputJsonString
                                )
                            }
                        } else if (innerKey != null && con.contains(".")) { //gyro.x = 10
                            val KeyValue = con.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray() //gyro.x = 10
                            setPublishJsonForRuleMatchedEdgeDevice(
                                KeyValue[1],
                                innerKey,
                                value,
                                attObj,
                                parentKey,
                                bean,
                                inputJsonString
                            )
                        } else if (innerKey == null) { // simple object like temp = 10. (not gyro type).
                            if (evaluateEdgeDeviceRuleValue(con, value)) {
                                onEdgeDeviceRuleMatched(bean)
                                val cvAttObj = JSONObject()
                                cvAttObj.put(parentKey, inputValue)
                                val mainObj = getEdgeDevicePublishMainObj(
                                    currentDate,
                                    dtg,
                                    cpId,
                                    environment,
                                    appVersion,
                                    EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE
                                )
                                val publishObj = getPublishStringEdgeDevice(
                                    uniqueId,
                                    currentDate,
                                    bean,
                                    inputJsonString!!,
                                    cvAttObj,
                                    mainObj
                                )
                                //publish edge device rule matched data. Publish simple attribute data only. (temp > 10)
                                if (publishObj != null) {
                                    publishMessage(publishObj.toString(), false)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*Get dtg attribute from sync saved response.
     * */
    private val dtg: String
        private get() = syncResponse!!.d.dtg

    /*Set the json data for publish for edge device only for gyro attributes.
     * */
    private fun setPublishJsonForRuleMatchedEdgeDevice(
        childAttNameValue: String,
        innerKey: String,
        value: Int,
        attObj: JSONObject?,
        parentKey: String,
        bean: SyncServiceResponse.DBeanXX.RuleBean,
        inputJsonString: String?
    ) {
        //collect publish data for gyro type object.
        try {

//            String childAttNameValue = KeyValue[1]; //x > 5
            val key = getAttName(childAttNameValue)
            if (innerKey == key) { // compare x with x.
                if (evaluateEdgeDeviceRuleValue(childAttNameValue, value)) {
                    onEdgeDeviceRuleMatched(bean)
                    attObj!!.put(key, value.toString() + "")
                }
            }
            if (attObj!!.length() != 0) {
                val cvAttObj = JSONObject()
                cvAttObj.put(parentKey, attObj)
                val mainObj = getEdgeDevicePublishMainObj(
                    currentDate,
                    dtg,
                    cpId,
                    environment,
                    appVersion,
                    EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE
                )
                publishObjForRuleMatchEdgeDevice = getPublishStringEdgeDevice(
                    uniqueId, currentDate, bean, inputJsonString!!, cvAttObj, mainObj
                )
            }
        } catch (e: Exception) {
        }
    }

    /* 1.Publish edge device rule matched data with bellow json format.
     * 2.This method publish gyro type attributes data.(//"gyro.x = 10 AND gyro.y > 10")
     * {"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"514076B1-3C21-4849-A777-F423B1821FC7","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    private fun publishRuleEvaluatedData() {
        if (publishObjForRuleMatchEdgeDevice != null) publishMessage(
            publishObjForRuleMatchEdgeDevice.toString(),
            false
        )
    }

    /* Publish input data for Device.
     * @param inputJsonStr input json from user.
     * */
    private fun publishDeviceInputData(inputJsonStr: String?, dObj: SyncServiceResponse.DBeanXX) {
        try {
            val jsonArray = JSONArray(inputJsonStr)
            var doFaultyPublish = false
            var doReportingPublish = false
            var time: String? = ""
            val reportingObject_reporting =
                getMainObject("0", dObj, appVersion!!, environment) // 0 for reporting.
            val reportingObject_faulty =
                getMainObject("1", dObj, appVersion, environment) // 1 for faulty.
            val arrayObj_reporting = JSONArray()
            val arrayObj_Faulty = JSONArray()
            var outerD_Obj_reporting: JSONObject? = null
            var outerD_Obj_Faulty: JSONObject? = null
            for (i in 0 until jsonArray.length()) {
                time = jsonArray.getJSONObject(i).optString(TIME)
                val uniqueId = jsonArray.getJSONObject(i).getString(UNIQUE_ID)
                val dataObj = jsonArray.getJSONObject(i).getJSONObject(DATA)
                val dataJsonKey = dataObj.keys()
                val tag = getTag(uniqueId, dObj)
                outerD_Obj_reporting = JSONObject()
                outerD_Obj_reporting.put(ID, uniqueId)
                outerD_Obj_reporting.put(DT, time)
                outerD_Obj_reporting.put(TG, tag)
                outerD_Obj_Faulty = JSONObject()
                outerD_Obj_Faulty.put(ID, uniqueId)
                outerD_Obj_Faulty.put(DT, time)
                outerD_Obj_Faulty.put(TG, tag)
                val innerD_Obj_reporting = JSONObject()
                val innerD_Obj_faulty = JSONObject()

                //getting value for
//                 "d": {"Temp":"66","humidity":"55","abc":"y","gyro":{"x":"7","y":"8","z":"9"}}
                while (dataJsonKey.hasNext()) {
                    val key = dataJsonKey.next()
                    val value = dataObj.getString(key)
                    if (!value.replace("\\s".toRegex(), "")
                            .isEmpty() && JSONTokener(value).nextValue() is JSONObject
                    ) {
                        val gyroObj_reporting = JSONObject()
                        val gyroObj_faulty = JSONObject()
                        val innerObj = dataObj.getJSONObject(key)
                        val innerJsonKey = innerObj.keys()

                        // get value for
//                         "gyro": {"x":"7","y":"8","z":"9"}
                        while (innerJsonKey.hasNext()) {
                            val InnerKey = innerJsonKey.next()
                            val InnerKValue = innerObj.getString(InnerKey)
                            val `val` = compareForInputValidation(InnerKey, InnerKValue, tag, dObj)
                            if (`val` == 0) {
                                gyroObj_reporting.put(InnerKey, InnerKValue)
                            } else {
                                gyroObj_faulty.put(InnerKey, InnerKValue)
                            }
                        }

                        //add gyro object to parent d object.
                        if (gyroObj_reporting.length() != 0) innerD_Obj_reporting.put(
                            key,
                            gyroObj_reporting
                        )
                        if (gyroObj_faulty.length() != 0) innerD_Obj_faulty.put(key, gyroObj_faulty)
                    } else {
                        val `val` = compareForInputValidation(key, value, tag, dObj)
                        if (`val` == 0) {
                            innerD_Obj_reporting.put(key, value)
                        } else {
                            innerD_Obj_faulty.put(key, value)
                        }
                    }
                }
                val arrayObj_attributes_reporting = JSONArray()
                val arrayObj_attributes_faulty = JSONArray()
                if (innerD_Obj_reporting.length() != 0) arrayObj_attributes_reporting.put(
                    innerD_Obj_reporting
                )
                if (innerD_Obj_faulty.length() != 0) arrayObj_attributes_faulty.put(
                    innerD_Obj_faulty
                )
                if (arrayObj_attributes_reporting.length() > 0) doReportingPublish = true
                if (arrayObj_attributes_faulty.length() > 0) doFaultyPublish = true


                //add object of attribute object to parent object.
                outerD_Obj_reporting.put(D_OBJ, arrayObj_attributes_reporting)
                outerD_Obj_Faulty.put(D_OBJ, arrayObj_attributes_faulty)
                arrayObj_reporting.put(outerD_Obj_reporting)
                arrayObj_Faulty.put(outerD_Obj_Faulty)
            }
            reportingObject_reporting.put(CURRENT_DATE, time)
            reportingObject_faulty.put(CURRENT_DATE, time)

            //Reporting json string as below.
//            {"cpId":"uei","dtg":"f76f806a-b0b6-4f34-bb15-11516d1e42ed","mt":"0","sdk":{"e":"qa","l":"M_android","v":"2.0"},"t":"2020-10-05T10:09:27.362Z","d":[{"id":"ddd2","dt":"2020-10-05T10:09:27.350Z","tg":"gateway","d":[{"Temp":"25","humidity":"0","abc":"abc","gyro":{"x":"0","y":"blue"}}]},{"id":"c1","dt":"2020-10-05T10:09:27.357Z","tg":"zg1","d":[{"Temperature":"0","Humidity":"50"}]},{"id":"c2","dt":"2020-10-05T10:09:27.362Z","tg":"zg2","d":[{"pressure":"500","vibration":"0","gyro":{"x":"5"}}]}]}

            //publish reporting data
            if (doReportingPublish) publishMessage(
                reportingObject_reporting.put(
                    D_OBJ,
                    arrayObj_reporting
                ).toString(), false
            )

            //publish faulty data
            if (doFaultyPublish) publishMessage(
                reportingObject_faulty.put(D_OBJ, arrayObj_Faulty).toString(), false
            )
        } catch (e: JSONException) {
            e.printStackTrace()
            iotSDKLogUtils!!.log(true, isDebug, "CM01_SD01", e.message!!)
        }
    }

    /*Call publish method of IotSDKMQTTService class to publish to web.
     * 1.When device is not connected to network and offline storage is true from client, than save all published message to device memory.
     * */
    private fun publishMessage(publishMessage: String, isUpdate: Boolean) {
        try {
            if (validationUtils!!.networkConnectionCheck()) {
                if (!isUpdate) {
                    mqttService!!.publishMessage(publishMessage)
                } else {
                    mqttService!!.updateTwin(publishMessage)
                }
            } else if (!isSaveToOffline) { // save message to offline.
                var fileToWrite: String? = null
                val sdkPreferences = IotSDKPreferences.getInstance(
                    context
                )
                val fileNamesList = sdkPreferences!!.getList(IotSDKPreferences.TEXT_FILE_NAME)
                if (fileNamesList.isEmpty()) { //create new file when file list is empty.
                    fileToWrite = createTextFile()
                } else {

                    /*1.check file with "current" prefix.
                     * 2.get the text file size and compare with defined size.
                     * 3.When text file size is more than defined size than create new file and write to that file.
                     * */
                    if (!fileNamesList.isEmpty()) {
                        for (textFile in fileNamesList) {
                            if (textFile!!.contains(TEXT_FILE_PREFIX)) {
                                fileToWrite = textFile
                                val file =
                                    File(File(context.filesDir, directoryPath), "$textFile.txt")
                                if (fileSizeToCreateInMb != 0 && getFileSizeInKB(file) >= fileSizeToCreateInMb) {
                                    //create new text file.
                                    fileToWrite = createTextFile()
                                }
                                break
                            }
                        }
                    }
                }
                try {
                    iotSDKLogUtils!!.writePublishedMessage(
                        directoryPath!!,
                        fileToWrite!!,
                        publishMessage
                    )
                } catch (e: Exception) {
                    iotSDKLogUtils!!.log(
                        true,
                        isDebug,
                        "ERR_OS02",
                        context.getString(R.string.ERR_OS02) + e.message
                    )
                }
                iotSDKLogUtils!!.log(
                    false,
                    isDebug,
                    "INFO_OS02",
                    context.getString(R.string.INFO_OS02)
                )
            }
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
        }
    }

    private fun createTextFile(): String {

        //rename file to directory
        val directory = File(context.filesDir, directoryPath)
        if (directory.exists()) {
            val contents = directory.listFiles()
            if (contents != null) for (filePath in contents) {
                val path = filePath.toString()
                val textFileName = path.substring(path.lastIndexOf("/") + 1)
                if (textFileName.contains(TEXT_FILE_PREFIX)) {
                    val reNameFile =
                        textFileName.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1]
                    val from = File(directory, textFileName)
                    val to = File(directory, reNameFile)
                    if (from.exists()) from.renameTo(to)
                }
            }
        }
        val textFileName = TEXT_FILE_PREFIX + "_" + System.currentTimeMillis() / 1000 + ""
        val sdkPreferences = IotSDKPreferences.getInstance(
            context
        )
        val fileList = CopyOnWriteArrayList(
            sdkPreferences!!.getList(IotSDKPreferences.TEXT_FILE_NAME)
        )
        //re-name the file to shared preference.
        for (file in fileList) {
            if (file!!.contains(TEXT_FILE_PREFIX)) {
                fileList.remove(file)
                fileList.add(file.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1])
            }
        }
        fileList.add(textFileName)
        val list = ArrayList(fileList)
        sdkPreferences.saveList(IotSDKPreferences.TEXT_FILE_NAME, list)

        //Delete first text file, when more than user defined count.
        if (list.size > fileCount) {
            deleteTextFile(list)
        }
        iotSDKLogUtils!!.log(false, isDebug, "INFO_OS03", context.getString(R.string.INFO_OS03))
        return textFileName
    }

    override fun networkAvailable() {
        try {
            if (!isSaveToOffline) {

                //check is there any text file name.
                if (IotSDKPreferences.getInstance(context)?.getList(
                        IotSDKPreferences.TEXT_FILE_NAME
                    )!!.isEmpty()
                ) {
                    iotSDKLogUtils!!.log(
                        false,
                        isDebug,
                        "INFO_OS05",
                        context.getString(R.string.INFO_OS05)
                    )
                    return
                }

                //check device if there any file stored for offline storage.
                val directory = File(context.filesDir, directoryPath)
                if (directory.exists()) {
                    val contents = directory.listFiles()
                    if (contents.size > 0) {
//                        publishOfflineData();
                        checkIsDeviceOnline()
                    }
                }
            }
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
        }
    }

    /*Check is device got connected on network available time, than publish offline data.
     * */
    private var timerCheckDeviceState: Timer? = null
    private fun checkIsDeviceOnline() {
        timerCheckDeviceState = Timer()
        val timerTaskObj: TimerTask = object : TimerTask() {
            override fun run() {
                if (isConnected) {
                    timerStop(timerCheckDeviceState)
                    publishOfflineData()
                }
            }
        }
        timerCheckDeviceState!!.schedule(timerTaskObj, 0, 2000)
    }

    override fun networkUnavailable() {
        timerStop(timerCheckDeviceState)
        if (!isSaveToOffline) {
            timerStop(timerOfflineSync)
        }
    }

    private var timerOfflineSync: Timer? = null

    /*Method to connect the device
     * @param cpId device cp id
     * @param uniqueId device unique id
     * @param sdkOptions json string containing discovery url, certificate, offlineStorage options. ({"certificate":{"SSLCaPath":"","SSLCertPath":"","SSLKeyPath":""},"discoveryUrl":"https://discovery.iotconnect.io/api/sdk/","isDebug":true,"offlineStorage":{"availSpaceInMb":1,"disabled":false,"fileCount":5}})
     * */
    init {
        this.environment = environment
        this.cpId = cpId
        this.uniqueId = uniqueId
        this.sdkOptions = sdkOptions
        connect()
        registerNetworkState()
    }

    /*Start timer for publish "df" value interval
     * */
    private fun publishOfflineData() {
        try {
            syncOfflineData = true
            val finalOfflineData = CopyOnWriteArrayList<String>()
            finalOfflineData.addAll(readTextFile())
            if (finalOfflineData.isEmpty()) return

            //start timer to sync offline data.
            timerOfflineSync = Timer()
            val timerTaskObj: TimerTask = object : TimerTask() {
                override fun run() {
                    //read next text file, when previous list is done sync.
                    if (finalOfflineData.isEmpty()) {
                        finalOfflineData.addAll(readTextFile())
                    }
                    if (syncOfflineData) {
                        if (!finalOfflineData.isEmpty()) {
                            for (i in finalOfflineData.indices) {
                                val data = finalOfflineData[i]
                                try {
                                    val dataObj = JSONObject(data)
                                    //                                    dataObj.put(OFFLINE_DATA, 1);

                                    //publish offline data.
                                    mqttService!!.publishMessage(dataObj.toString())
                                } catch (e: JSONException) {
                                    iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
                                    e.printStackTrace()
                                }
                                finalOfflineData.removeAt(i)
                            }
                        }
                        syncOfflineData = false
                    } else {
                        syncOfflineData = true
                    }
                }
            }
            timerOfflineSync!!.schedule(timerTaskObj, 0, 10000)
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
        }
    }

    fun readTextFile(): CopyOnWriteArrayList<String> {
        val offlineData = CopyOnWriteArrayList<String>()
        try {
            val preferences = IotSDKPreferences.getInstance(
                context
            )
            val fileNamesList = preferences!!.getList(IotSDKPreferences.TEXT_FILE_NAME)
            if (fileNamesList.isEmpty()) {
                //shared preference is empty than stop sync timer.
                timerStop(timerOfflineSync)
                return offlineData
            }
            val bufferedReader = BufferedReader(
                FileReader(
                    File(
                        File(
                            context.filesDir, directoryPath
                        ), fileNamesList[0] + ".txt"
                    )
                )
            )
            var read: String
            //            StringBuilder builder = new StringBuilder("");
            while (bufferedReader.readLine().also { read = it } != null) {
//                builder.append(read);
                offlineData.add(read)
            }
            bufferedReader.close()

            //delete text file after reading all records.
            if (deleteTextFile(fileNamesList as ArrayList<String?>)) {
                iotSDKLogUtils!!.log(
                    false,
                    isDebug,
                    "INFO_OS04",
                    context.getString(R.string.INFO_OS04)
                )
            }
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(
                true,
                isDebug,
                "ERR_OS03",
                context.getString(R.string.ERR_OS03) + e.message
            )
            e.printStackTrace()
        }
        return offlineData
    }

    private fun deleteTextFile(fileNamesList: ArrayList<String?>): Boolean {
        try {
            //Delete from device
            val file = File(File(context.filesDir, directoryPath), fileNamesList[0] + ".txt")
            if (file.exists()) {
                file.delete()
            }
            fileNamesList.removeAt(0)
            //delete from shared preferences
            IotSDKPreferences.getInstance(context)!!
                .saveList(IotSDKPreferences.TEXT_FILE_NAME, fileNamesList)
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
            return false
        }
        return true
    }

    companion object {
        private val sdkClient: SDKClient? = null
        private const val DEFAULT_DISCOVERY_URL = "https://discovery.iotconnect.io/"
        private const val URL_PATH = "api/sdk/"
        private const val SYNC = "sync"
        private const val CPID = "cpid/"
        private const val LANG_ANDROID_VER = "/lang/android/ver/"
        private const val ENV = "/env/"
        private const val UNIQUE_ID = "uniqueId"
        private const val CP_ID = "cpId"
        private const val CURRENT_DATE = "t"
        private const val MESSAGE_TYPE = "mt"
        private const val SDK_OBJ = "sdk"
        private const val D_OBJ = "d"
        private const val DEVICE_ID = "id"
        private const val DEVICE_TAG = "tg"
        private const val DEVICE = "device"
        private const val ATTRIBUTES = "attributes"
        private const val CMD_TYPE = "cmdType"
        private const val DISCOVERY_URL = "discoveryUrl"
        private const val IS_DEBUG = "isDebug"
        private const val DATA = "data"
        private const val TIME = "time"
        private const val ID = "id"
        private const val DT = "dt"
        private const val TG = "tg"
        private const val DIRECTORY_PATH = "logs/offline/"
        private const val EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE = 3
        private var edgeDeviceAttributeMap: MutableMap<String?, TumblingWindowBean?>? = null
        private var edgeDeviceAttributeGyroMap: MutableMap<String?, List<TumblingWindowBean?>?>? =
            null

        /*return singleton object for this class.
     * */
        fun getInstance(
            context: Context,
            cpId: String,
            uniqueId: String,
            deviceCallback: DeviceCallback,
            twinUpdateCallback: TwinUpdateCallback,
            sdkOptions: String,
            environment: String
        ): SDKClient? {
            return sdkClient
                ?: SDKClient(
                    context,
                    cpId,
                    uniqueId,
                    deviceCallback,
                    twinUpdateCallback,
                    sdkOptions,
                    environment
                )
        }

        private var reCheckingCountTime = 0
        private const val TEXT_FILE_PREFIX = "current"
        private var syncOfflineData = true
        private const val OFFLINE_DATA = "od"
    }
}