package com.iotconnectsdk

import EnvironmentType
import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.text.TextUtils
import android.webkit.URLUtil
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.gson.Gson
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.beans.GetChildDeviceBean
import com.iotconnectsdk.beans.GetEdgeRuleBean
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.enums.BrokerType
import com.iotconnectsdk.enums.C2DMessageEnums
import com.iotconnectsdk.enums.DeviceIdentityMessages
import com.iotconnectsdk.interfaces.DeviceCallback
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.PublishMessageCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.mqtt.IotSDKMQTTService
import com.iotconnectsdk.utils.*
import com.iotconnectsdk.utils.DateTimeUtils.getCurrentTime
import com.iotconnectsdk.utils.EdgeDeviceUtils.evaluateEdgeDeviceRuleValue
import com.iotconnectsdk.utils.EdgeDeviceUtils.getAttName
import com.iotconnectsdk.utils.EdgeDeviceUtils.getAttributeName
import com.iotconnectsdk.utils.EdgeDeviceUtils.getEdgeDevicePublishMainObj
import com.iotconnectsdk.utils.EdgeDeviceUtils.getPublishStringEdgeDevice
import com.iotconnectsdk.utils.EdgeDeviceUtils.publishEdgeDeviceInputData
import com.iotconnectsdk.utils.EdgeDeviceUtils.updateEdgeDeviceGyroObj
import com.iotconnectsdk.utils.EdgeDeviceUtils.updateEdgeDeviceObj
import com.iotconnectsdk.utils.SDKClientUtils.createTextFile
import com.iotconnectsdk.utils.SDKClientUtils.deleteTextFile
import com.iotconnectsdk.utils.SDKClientUtils.getAttributesList
import com.iotconnectsdk.utils.ValidationTelemetryUtils.compareForInputValidationNew
import com.iotconnectsdk.webservices.CallWebServices
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse
import com.iotconnectsdk.webservices.responsebean.IdentityServiceResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


/**
 * class for SDKClient
 */

internal class SDKClientManager(
    private val context: Context?,
    private val cpId: String?,
    private val uniqueId: String?,
    private val deviceCallback: DeviceCallback?,
    private val sdkOptions: String?,
    private val environment: EnvironmentType,

    ) : WsResponseInterface, HubToSdkCallback, PublishMessageCallback, TwinUpdateCallback,
    NetworkStateReceiver.NetworkStateReceiverListener {


    private var validationUtils: ValidationUtils? = null

    private var iotSDKLogUtils: IotSDKLogUtils? = null

    private var mqttService: IotSDKMQTTService? = null

    private val TEXT_FILE_PREFIX = "current"

    private var commandType: String? = null

    private var isConnected = false


    private var isSaveToOffline = false

    private var isDebug = false

    private var idEdgeDevice = false

    private val appVersion = "v2.1"

    private var discoveryUrl = ""

    private val DEFAULT_DISCOVERY_URL_AZ = "https://discovery.iotconnect.io/"

    private val DEFAULT_DISCOVERY_URL_AWS = "http://54.160.162.148:219/"

    private val URL_PATH = "api/$appVersion/dsdk/"

    private val END_POINT_AWS = "?pf=aws"

    private val CPID = "cpid/"

    private val ENV = "/env/"

    private val UNIQUE_ID = "/uid/"

    private val UNIQUE_ID_View = "uniqueId"

    private val MESSAGE_TYPE = "mt"

    private val D_OBJ = "d"

    private val DEVICE_ID = "id"

    private val DEVICE_TAG = "tg"

    private val DEVICE = "device"

    private val ATTRIBUTES = "attributes"

    private val TAGS = "tags"

    private val CMD_TYPE = "ct"

    private val DISCOVERY_URL = "discoveryUrl"

    private val IS_DEBUG = "isDebug"

    private val DATA = "data"

    private val ID = "id"

    private val DT = "dt"

    private val DF = "df"

    private val FREQUENCY_HEART_BEAT = "f"

    private val TG = "tg"

    private val DIRECTORY_PATH = "logs/offline/"

    private var savedTime: Long = 0

    //for Edge Device
    private var edgeDeviceTimersList: ArrayList<Timer>? = null

    private var edgeDeviceAttributeMap: ListMultimap<String, TumblingWindowBean>? = null

    private var edgeDeviceAttributeGyroMap: ListMultimap<String, List<TumblingWindowBean>>? = null

    private var publishObjForRuleMatchEdgeDevice: JSONObject? = null

    private var networkStateReceiver: NetworkStateReceiver? = null

    private var fileSizeToCreateInMb = 0

    private var directoryPath: String? = null

    private var fileCount = 0

    private var reCheckingTimer: Timer? = null

    private var reCheckingCountTime = 0

    private var isRefreshAttribute = false

    val scope = MainScope()
    var job: Job? = null

    var isSkipValidation = false

    var brokerType = ""


    /*return singleton object for this class.
     * */
    companion object {
        @Volatile
        private var sdkClientManger: SDKClientManager? = null

        @JvmStatic
        @JvmSynthetic
        fun getInstance(
            context: Context?,
            cpId: String?,
            uniqueId: String?,
            deviceCallback: DeviceCallback?,
            sdkOptions: String?,
            environment: EnvironmentType
        ): SDKClientManager {
            synchronized(this) {
                if (sdkClientManger == null) {
                    sdkClientManger = SDKClientManager(
                        context,
                        cpId,
                        uniqueId,
                        deviceCallback,
                        sdkOptions,
                        environment
                    )
                }
                sdkClientManger?.connect()
                sdkClientManger?.registerNetworkState()
                return sdkClientManger!!
            }

        }
    }

    /**
     * Register Broadcast Receiver  for network state changes
     */

    private fun registerNetworkState() {
        try {
            networkStateReceiver = NetworkStateReceiver()
            networkStateReceiver?.addListener(this)
            context?.registerReceiver(
                networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connect() {

        directoryPath = ""
        reCheckingCountTime = 0
        commandType = null
        idEdgeDevice = false
        isSaveToOffline = false
        isDebug = false
        isSkipValidation = false
        brokerType = ""
        fileCount = 0

        //get is debug option.
        var sdkObj: JSONObject? = null
        try {
            if (sdkOptions != null) {
                sdkObj = JSONObject(sdkOptions)
                if (sdkObj.has(IS_DEBUG)) {
                    isDebug = sdkObj.getBoolean(IS_DEBUG)
                }

                if (sdkObj.has("skipValidation")) {
                    isSkipValidation = sdkObj.getBoolean("skipValidation")
                }

                if (sdkObj.has("brokerType")) {
                    brokerType = sdkObj.getString("brokerType")
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
        iotSDKLogUtils = IotSDKLogUtils.getInstance(
            context!!, cpId!!, uniqueId!!
        )
        validationUtils = ValidationUtils.getInstance(
            iotSDKLogUtils!!, context, isDebug
        )
        if (sdkOptions != null) {

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
                if (brokerType == BrokerType.AZ.value) {
                    discoveryUrl =
                        DEFAULT_DISCOVERY_URL_AZ //set default discovery url when it is empty from client end.
                } else if (brokerType == BrokerType.AWS.value) {
                    discoveryUrl =
                        DEFAULT_DISCOVERY_URL_AWS //set default discovery url when it is empty from client end.
                } else {
                    discoveryUrl =
                        DEFAULT_DISCOVERY_URL_AZ //set default discovery url when it is empty from client end.
                }

            } else {
                discoveryUrl = try {
                    sdkObj.getString(DISCOVERY_URL)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    iotSDKLogUtils!!.log(true, isDebug, "ERR_IN01", e.message!!)
                    return
                }
            }
        } else {

            if (brokerType == BrokerType.AZ.value) {
                discoveryUrl =
                    DEFAULT_DISCOVERY_URL_AZ //set default discovery url when sdkOption is null.
            } else if (brokerType == BrokerType.AWS.value) {
                discoveryUrl =
                    DEFAULT_DISCOVERY_URL_AWS //set default discovery url when sdkOption is null.
            } else {
                discoveryUrl =
                    DEFAULT_DISCOVERY_URL_AZ //set default discovery url when sdkOption is null.
            }
        }
        if (!validationUtils!!.isEmptyValidation(
                cpId, "ERR_IN04", context.getString(R.string.ERR_IN04)
            )
        ) return
        if (!validationUtils!!.isEmptyValidation(
                uniqueId, "ERR_IN05", context.getString(R.string.ERR_IN05)
            )
        ) return
        callDiscoveryService()
    }

    /*API call for discovery.
     *@param discoveryUrl  discovery URL ("discoveryUrl" : "https://discovery.iotconnect.io")
     * */
    private fun callDiscoveryService() {
        if (!validationUtils!!.networkConnectionCheck()) return

        val discoveryApi: String

        if (appVersion != null) {

            if (brokerType == BrokerType.AZ.value) {
                discoveryApi = discoveryUrl + URL_PATH + CPID + cpId + ENV + environment.value
            } else if (brokerType == BrokerType.AWS.value) {
                discoveryApi =
                    discoveryUrl + URL_PATH + CPID + cpId + ENV + environment.value + END_POINT_AWS
            } else {
                discoveryApi = discoveryUrl + URL_PATH + CPID + cpId + ENV + environment.value
            }

            CallWebServices().getDiscoveryApi(discoveryApi, this)
        }
    }

    /*
    *  API call for Identity Api
    */
    private fun callSyncService() {
        if (!validationUtils!!.networkConnectionCheck()) return
        val baseUrl =
            IotSDKPreferences.getInstance(context!!)?.getStringData(IotSDKPreferences.SYNC_API)
        if (baseUrl != null) {
            CallWebServices().sync(baseUrl, this)
        }
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
                    IotSDKUrls.DISCOVERY_SERVICE, ignoreCase = true
                ) && response != null
            ) {
                val discoveryApiResponse =
                    Gson().fromJson(response, DiscoveryApiResponse::class.java)
                if (discoveryApiResponse != null && discoveryApiResponse.d.bu != null) {
                    //BaseUrl received to sync the device information.
                    iotSDKLogUtils!!.log(
                        false, isDebug, "INFO_IN07", context!!.getString(R.string.INFO_IN07)
                    )
                    if (!validationUtils!!.validateBaseUrl(discoveryApiResponse)) return
                    val baseUrl: String = discoveryApiResponse.d.bu + UNIQUE_ID + uniqueId
                    IotSDKPreferences.getInstance(context)
                        ?.putStringData(IotSDKPreferences.SYNC_API, baseUrl)
                    callSyncService()
                } else {
                    val responseCodeMessage =
                        validationUtils?.responseCodeMessage(discoveryApiResponse.d.ec)

                    try {
                        deviceCallback?.onReceiveMsg(responseCodeMessage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    sdkClientManger = null
                }
            } else if (methodName.equals(
                    IotSDKUrls.SYNC_SERVICE, ignoreCase = true
                ) && response != null
            ) {
                val syncServiceResponseData =
                    Gson().fromJson(response, IdentityServiceResponse::class.java)

                if (syncServiceResponseData != null && syncServiceResponseData.d != null && syncServiceResponseData.d.p != null) {
                    //save the sync response to shared pref
                    IotSDKPreferences.getInstance(context!!)
                        ?.putStringData(IotSDKPreferences.SYNC_RESPONSE, response)
                    callMQTTService()
                } else {


                    //Device information not found. While sync the device when get the response code 'rc' not equal to '0'
                    val rc: Int = syncServiceResponseData.d.ec

                    val responseCodeMessage =
                        validationUtils?.responseCodeMessage(syncServiceResponseData.d.ec)

                    try {
                        deviceCallback?.onReceiveMsg(responseCodeMessage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    sdkClientManger = null

                    if (rc == 1 || rc == 3 || rc == 4 || rc == 6) {
                        if (reCheckingCountTime <= 3) {
                            reChecking()
                        } else {
                            timerStop(reCheckingTimer)
                            iotSDKLogUtils!!.log(
                                true, isDebug, "ERR_IN10", context!!.getString(R.string.ERR_IN10)
                            )
                        }
                        return
                    } else if (rc != 0) {
//                            onConnectionStateChange(false);
                        iotSDKLogUtils!!.log(
                            true, isDebug, "ERR_IN10", context!!.getString(R.string.ERR_IN10)
                        )
                        return
                    } else {
                        timerStop(reCheckingTimer)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
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
    override fun onFailedResponse(methodName: String?, errorCode: Int, message: String?) {
        if (methodName.equals(IotSDKUrls.DISCOVERY_SERVICE, ignoreCase = true)) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_IN09", context!!.getString(R.string.ERR_IN09))
        } else if (methodName.equals(IotSDKUrls.SYNC_SERVICE, ignoreCase = true)) {
        }
    }


    /*MQTT service call to connect device.
     * */
    private fun callMQTTService() {
        val response = getSyncResponse()
        if (response!!.d == null || response.d.p == null) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_IN11", context!!.getString(R.string.ERR_IN11))
            return
        }
        mqttService = IotSDKMQTTService.getInstance(
            context!!,
            sdkOptions,
            response.d.p,
            response.d.meta.at,
            this,
            this,
            this,
            iotSDKLogUtils!!,
            isDebug,
            uniqueId!!,
            cpId!!
        )
        mqttService!!.connectMQTT()

    }


    /*get the saved sync response from shared preference.
     * */
    private fun getSyncResponse(): IdentityServiceResponse? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getSyncResponse(IotSDKPreferences.SYNC_RESPONSE)
    }

    /*get the saved Attribute response from shared preference.
   * */
    private fun getAttributeResponse(): CommonResponseBean? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getDeviceInformation(IotSDKPreferences.ATTRIBUTE_RESPONSE)
    }

    /*get the saved Edge response from shared preference.
  * */
    private fun getEdgeRuleResponse(): CommonResponseBean? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getDeviceInformation(IotSDKPreferences.EDGE_RULE_RESPONSE)
    }

    /*get the saved Settings response from shared preference.
 * */
    private fun getSettingsResponse(): CommonResponseBean? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getDeviceInformation(IotSDKPreferences.SETTING_TWIN_RESPONSE)
    }

    /*get the saved GatewayChild response from shared preference.
  * */
    private fun getGatewayChildResponse(): CommonResponseBean {
        return IotSDKPreferences.getInstance(context!!)
            ?.getDeviceInformation(IotSDKPreferences.CHILD_DEVICE_RESPONSE) ?: CommonResponseBean()
    }


    /*
    * Callback to show message in UI
    * */
    override fun onSendMsgUI(message: String?) {
        if (message != null) {
            try {
                deviceCallback?.onReceiveMsg(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    /*
    * To retrieve more information on attributes, settings/shadow, rules, etc
    * device must send JSON with a specific message type by publishing on di topic
    * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-identity-messages/
    * */
    override fun onSendMsg() {
        val response = getSyncResponse()

        if (response?.d?.has?.attr == 1) {
            publishMessage(
                response.d.p.topics.di, JSONObject().put(
                    MESSAGE_TYPE, DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value
                ).toString(), false
            )
        }

        if ((response?.d?.has?.set == 1)) {
            publishMessage(
                response.d.p.topics.di, JSONObject().put(
                    MESSAGE_TYPE, DeviceIdentityMessages.GET_DEVICE_TEMPLATE_SETTINGS_TWIN.value
                ).toString(), false
            )
        }

        if ((response?.d?.has?.r == 1)) {
            publishMessage(
                response.d.p.topics.di,
                JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_EDGE_RULE.value)
                    .toString(),
                false
            )
        }

        if ((response?.d?.has?.d == 1)) {
            publishMessage(
                response.d.p.topics.di,
                JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_CHILD_DEVICES.value)
                    .toString(),
                false
            )
        }

        if ((response?.d?.has?.ota == 1)) {
            publishMessage(
                response.d.p.topics.di,
                JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_PENDING_OTA.value)
                    .toString(), false
            )
        }

    }


    /*
    * Data will come accordingly of templates,settings/twins etc.after publishing on di topic
    * and saved in preference
    * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-identity-messages/
    * */
    override fun onReceiveMsg(message: String?) {

        if (message != null) {
            try {
                val mainObject = JSONObject(message)
                var cmdType: Int? = -1
                var responseCode: Int? = -1
                if (mainObject.has("d")) {
                    val innerObject = mainObject.getJSONObject("d")
                    cmdType = innerObject.getInt(CMD_TYPE)
                    responseCode = innerObject.getInt("ec")
                }


                if (cmdType == DeviceIdentityMessages.CREATE_CHILD_DEVICE.value) {
                    /*{"d":{"ec":0,"ct":221,"d":{"tg":"werw","id":"dfsfd","s":0}}}*/

                    val responseCodeMessage =
                        validationUtils?.rcMessageChildDevice(responseCode!!)

                    try {
                        if (responseCode == 0) {
                            val response = getSyncResponse()
                            publishMessage(
                                response?.d?.p?.topics!!.di, JSONObject().put(
                                    MESSAGE_TYPE, DeviceIdentityMessages.GET_CHILD_DEVICES.value
                                ).toString(), false
                            )
                        }


                        deviceCallback?.onReceiveMsg(responseCodeMessage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else if (cmdType == DeviceIdentityMessages.DELETE_CHILD_DEVICE.value) {
                    /*{"d":{"ec":0,"ct":222,"d":{"tg":"werw","id":"dfsfd","s":0}}}*/

                    val responseCodeMessage =
                        validationUtils?.rcMessageDelChildDevice(responseCode!!)

                    try {

                        if (responseCode == 0) {
                            val response = getSyncResponse()
                            publishMessage(
                                response?.d?.p?.topics!!.di, JSONObject().put(
                                    MESSAGE_TYPE, DeviceIdentityMessages.GET_CHILD_DEVICES.value
                                ).toString(), false
                            )
                        }
                        deviceCallback?.onReceiveMsg(responseCodeMessage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    val gson = Gson()
                    val commonModel = gson.fromJson(message, CommonResponseBean::class.java)

                    if (commonModel?.d?.ct != null) {
                        when (commonModel.d.ct) {
                            /*{"d":{"att":[{"p":"","dt":0,"tg":"","d":[{"ln":"Temp","dt":1,"dv":"5 to 10","sq":1,"tg":"p","tw":"60s"},{"ln":"Humidity","dt":1,"dv":"5 to 10","sq":2,"tg":"ch","tw":"60s"},{"ln":"Lumosity","dt":1,"dv":"","sq":4,"tg":"ch","tw":"60s"}]},{"p":"Gyroscope","dt":11,"tg":"p","d":[{"ln":"x","dt":1,"dv":"","sq":1,"tg":"p","tw":"60s"},{"ln":"y","dt":1,"dv":"","sq":2,"tg":"p","tw":"60s"}]}],"ct":201,"ec":0,"dt":"2023-02-22T10:41:18.6947577Z"}}*/
                            DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value -> {
                                val response = getSyncResponse()

                                IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                    IotSDKPreferences.ATTRIBUTE_RESPONSE, Gson().toJson(commonModel)
                                )
                                if (response?.d?.meta?.edge == 1) {
                                    idEdgeDevice = true

                                    try {
                                        processEdgeDeviceTWTimer(response, commonModel)
                                    } catch (e: Exception) {
                                        iotSDKLogUtils!!.log(true, isDebug, "ERR_EE01", e.message!!)
                                    }
                                }
                                onDeviceConnectionStatus(isConnected())

                            }

                            /*
                            * {"d":{"set":[{"ln":"Motor","dt":1,"dv":""}],"ct":202,"ec":0,"dt":"2023-02-22T10:41:18.6948342Z"}}
                            * */
                            DeviceIdentityMessages.GET_DEVICE_TEMPLATE_SETTINGS_TWIN.value -> {
                                IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                    IotSDKPreferences.SETTING_TWIN_RESPONSE,
                                    Gson().toJson(commonModel)
                                )
                            }

                            DeviceIdentityMessages.GET_EDGE_RULE.value -> {
                                IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                    IotSDKPreferences.EDGE_RULE_RESPONSE, Gson().toJson(commonModel)
                                )
                            }

                            /*
                            * {"d":{"d":[{"tg":"ch","id":"ch1"}],"ct":204,"ec":0,"dt":"2023-02-22T10:41:18.2683604Z"}}
                            * */
                            DeviceIdentityMessages.GET_CHILD_DEVICES.value -> {
                                IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                    IotSDKPreferences.CHILD_DEVICE_RESPONSE,
                                    Gson().toJson(commonModel)
                                )
                                val response = getSyncResponse()
                                if (idEdgeDevice) edgeDeviceTimerStop()
                                if (response?.d?.meta?.edge == 1) {
                                    idEdgeDevice = true

                                    try {
                                        processEdgeDeviceTWTimer(response, getAttributeResponse()!!)
                                    } catch (e: Exception) {
                                        iotSDKLogUtils!!.log(true, isDebug, "ERR_EE01", e.message!!)
                                    }
                                }

                                onDeviceConnectionStatus(isConnected())

                            }

                            DeviceIdentityMessages.GET_PENDING_OTA.value -> {
                                iotSDKLogUtils!!.log(
                                    false,
                                    isDebug,
                                    "INFO_CM02",
                                    context!!.getString(R.string.INFO_CM02)
                                )
                                deviceCallback!!.onReceiveMsg(message)
                            }

                            else -> {
                                deviceCallback!!.onReceiveMsg(message)
                            }
                        }
                    } else {

                        /*
                        *For receiving Cloud to Device (C2D) messages
                        * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/cloud-to-device-c2d-messages/
                        * */
                        val mainObject = JSONObject(message)

                        when (mainObject.getInt(CMD_TYPE)) {

                            /*Device command received by the device from the cloud
                            *
                            * {"v":"2.1","ct":0,"cmd":"ON ON","ack":"6198c520-1ebc-4556-b12c-dde9d790decc"}
                            * */

                            C2DMessageEnums.DEVICE_COMMAND.value -> {
                                iotSDKLogUtils!!.log(
                                    false,
                                    isDebug,
                                    "INFO_CM01",
                                    context!!.getString(R.string.INFO_CM01)
                                )

                                try {
                                    deviceCallback?.onDeviceCommand(message)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            }

                            /*OTA Command received by the device from the cloud*/
                            C2DMessageEnums.OTA_COMMAND.value -> {

                                iotSDKLogUtils!!.log(
                                    false,
                                    isDebug,
                                    "INFO_CM02",
                                    context!!.getString(R.string.INFO_CM02)
                                )
                                deviceCallback!!.onOTACommand(message)
                            }

                            /*Module Command received by the device from the cloud*/
                            C2DMessageEnums.MODULE_COMMAND.value -> {
                                iotSDKLogUtils!!.log(
                                    false,
                                    isDebug,
                                    "INFO_CM02",
                                    context!!.getString(R.string.INFO_CM02Module)
                                )
                                deviceCallback!!.onModuleCommand(message)
                            }

                            /*The device must send a message of type 201 to get updated attributes*/
                            C2DMessageEnums.REFRESH_ATTRIBUTE.value -> {
                                isRefreshAttribute = true
                                val response = getSyncResponse()

                                if (idEdgeDevice) edgeDeviceTimerStop()

                                publishMessage(
                                    response?.d?.p?.topics!!.di, JSONObject().put(
                                        MESSAGE_TYPE,
                                        DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value
                                    ).toString(), false
                                )

                                deviceCallback!!.onAttrChangeCommand(message)
                            }

                            /*The device must send a message of type 202 to get updated settings or twin*/
                            C2DMessageEnums.REFRESH_SETTING_TWIN.value -> {
                                val response = getSyncResponse()
                                publishMessage(
                                    response?.d?.p?.topics!!.di, JSONObject().put(
                                        MESSAGE_TYPE,
                                        DeviceIdentityMessages.GET_DEVICE_TEMPLATE_SETTINGS_TWIN.value
                                    ).toString(), false
                                )

                                deviceCallback!!.onTwinChangeCommand(message)
                            }

                            /*The device must send a message of type 203 to get updated Edge rules*/
                            C2DMessageEnums.REFRESH_EDGE_RULE.value -> {
                                val response = getSyncResponse()
                                publishMessage(
                                    response?.d?.p?.topics!!.di, JSONObject().put(
                                        MESSAGE_TYPE, DeviceIdentityMessages.GET_EDGE_RULE.value
                                    ).toString(), false
                                )

                                deviceCallback!!.onRuleChangeCommand(message)
                            }

                            /*The device must send a message of type 204 to get updated child devices*/
                            C2DMessageEnums.REFRESH_CHILD_DEVICE.value -> {
                                val response = getSyncResponse()
                                publishMessage(
                                    response?.d?.p?.topics!!.di, JSONObject().put(
                                        MESSAGE_TYPE, DeviceIdentityMessages.GET_CHILD_DEVICES.value
                                    ).toString(), false
                                )
                                deviceCallback!!.onDeviceChangeCommand(message)
                            }

                            /*The device needs to update the frequency received in this message*/

                            C2DMessageEnums.DATA_FREQUENCY_CHANGE.value -> {
                                if (context != null) {
                                    onFrequencyChangeCommand(mainObject.getInt(DF))
                                }
                            }

                            /*The device must stop all communication and release the MQTT connection*/
                            C2DMessageEnums.DEVICE_DELETED.value, C2DMessageEnums.DEVICE_DISABLED.value, C2DMessageEnums.DEVICE_RELEASED.value, C2DMessageEnums.STOP_OPERATION.value, C2DMessageEnums.DEVICE_CONNECTION_STATUS.value -> {
                                iotSDKLogUtils?.log(
                                    false,
                                    isDebug,
                                    "INFO_CM16",
                                    context!!.getString(R.string.INFO_CM16)
                                )
                                dispose()
                            }

                            /*The device must start sending a heartbeat*/
                            C2DMessageEnums.START_HEARTBEAT.value -> {
                                val frequencyHeartBeat = mainObject.getInt(FREQUENCY_HEART_BEAT)
                                onHeartbeatCommand(frequencyHeartBeat)
                            }

                            /*The device must stop sending a heartbeat*/
                            C2DMessageEnums.STOP_HEARTBEAT.value -> {
                                onHeartbeatCommand()
                            }

                            C2DMessageEnums.VALIDATION_SKIP.value -> {
                                onValidationSkipCommand()
                            }

                            else -> {
                                deviceCallback!!.onReceiveMsg(message)
                            }
                        }
                    }
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun onValidationSkipCommand() {

    }

    private fun onFrequencyChangeCommand(frequency: Int) {
        if (context != null) {
            val response = getSyncResponse()
            response?.d?.meta?.df = frequency
            IotSDKPreferences.getInstance(context)?.putStringData(
                IotSDKPreferences.SYNC_RESPONSE, Gson().toJson(response)
            )
        }
    }

    /*Call publish method of IotSDKMQTTService class to publish to web.
    * 1.When device is not connected to network and offline storage is true from client, than save all published message to device memory.
    * */
    private fun publishMessage(topics: String, publishMessage: String, isUpdate: Boolean) {

        try {
            if (validationUtils!!.networkConnectionCheck()) {
                if (!isUpdate) {
                    mqttService!!.publishMessage(topics, publishMessage)
                } else {
                    mqttService!!.updateTwin(publishMessage)
                }
            } else if (!isSaveToOffline) { // save message to offline.
                var fileToWrite: String? = null
                val sdkPreferences = IotSDKPreferences.getInstance(
                    context!!
                )
                val fileNamesList = sdkPreferences!!.getList(IotSDKPreferences.TEXT_FILE_NAME)
                if (fileNamesList.isEmpty()) { //create new file when file list is empty.
                    fileToWrite =
                        createTextFile(context, directoryPath, fileCount, iotSDKLogUtils, isDebug)
                } else {

                    /*1.check file with "current" prefix.
                     * 2.get the text file size and compare with defined size.
                     * 3.When text file size is more than defined size than create new file and write to that file.
                     * */
                    if (!fileNamesList.isEmpty()) {
                        for (textFile in fileNamesList) {
                            if (textFile!!.contains(TEXT_FILE_PREFIX)) {
                                fileToWrite = textFile
                                val file = File(
                                    File(context.filesDir, directoryPath), "$textFile.txt"
                                )
                                if (fileSizeToCreateInMb != 0 && SDKClientUtils.getFileSizeInKB(file) >= fileSizeToCreateInMb) {
                                    //create new text file.
                                    fileToWrite = createTextFile(
                                        context, directoryPath, fileCount, iotSDKLogUtils, isDebug
                                    )
                                }
                                break
                            }
                        }
                    }
                }
                try {
                    iotSDKLogUtils!!.writePublishedMessage(
                        directoryPath!!, fileToWrite!!, publishMessage
                    )
                } catch (e: java.lang.Exception) {
                    iotSDKLogUtils!!.log(
                        true, isDebug, "ERR_OS02", context.getString(R.string.ERR_OS02) + e.message
                    )
                }
                iotSDKLogUtils!!.log(
                    false, isDebug, "INFO_OS020", context.getString(R.string.INFO_OS02)
                )
            }
        } catch (e: java.lang.Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
        }
    }


    /*Method creates json string to be given to framework.
    *[{"device":{"id":"ch1","tg":"ch"},"attributes":[{"dt":1,"dv":"5 to 10","ln":"Humidity","sq":2,"tg":"ch"},{"dt":1,"dv":"","ln":"Lumosity","sq":4,"tg":"ch"}]},{"device":{"id":"","tg":"p"},"attributes":[{"dt":1,"dv":"5 to 10","ln":"Temp","sq":1,"tg":"p"},{"d":[{"dt":1,"dv":"","ln":"x","sq":1,"tg":"p"},{"dt":1,"dv":"","ln":"y","sq":2,"tg":"p"}],"dt":11,"p":"Gyroscope","tg":"p"}]}]
    * */
    @JvmSynthetic
    fun getAttributes(): String {

        val syncResponse = getSyncResponse()
        val attributeResponse = getAttributeResponse()
        val mainArray = JSONArray()

        /*
        * gtw will be null if the device is not Gateway
        * */
        if (syncResponse?.d?.meta?.gtw != null) {
            val gatewayChildResponse = getGatewayChildResponse()
            val getChildDeviceBean = GetChildDeviceBean()

            getChildDeviceBean.tg = syncResponse.d.meta.gtw.tg
            getChildDeviceBean.id = uniqueId
            gatewayChildResponse?.d?.childDevice?.add(getChildDeviceBean)

            val tagsList = ArrayList<String>()

            attributeResponse?.d?.att?.forEach { attBean ->
                attBean.d.forEach {
                    if (syncResponse.d.meta.gtw.tg != it.tg) {
                        if (!tagsList.contains(it.tg)) {
                            tagsList.add(it.tg)
                        }
                    }
                }
            }

            gatewayChildResponse?.d?.childDevice?.forEach { childDeviceBean ->
                if (attributeResponse != null) {


                    //CREATE DEVICE OBJECT, "device":{"id":"dee02","tg":"gateway"}
                    val deviceObj = JSONObject()
                    deviceObj.put(DEVICE_ID, childDeviceBean.id)
                    deviceObj.put(DEVICE_TAG, childDeviceBean.tg)


                    //ADD TO MAIN OBJECT
                    val mainObj = JSONObject()
                    mainObj.put(DEVICE, deviceObj)
                    mainObj.put(
                        ATTRIBUTES,
                        getAttributesList(attributeResponse.d?.att!!, childDeviceBean.tg)
                    )

                    mainObj.put(TAGS, JSONArray(tagsList))

                    //ADD MAIN BOJ TO ARRAY.
                    mainArray.put(mainObj)

                    //Attributes data not found
                    if (mainArray.length() == 0) {
                        iotSDKLogUtils!!.log(
                            true, isDebug, "ERR_GA02", context!!.getString(R.string.ERR_GA02)
                        )
                    } else {
                        iotSDKLogUtils!!.log(
                            false, isDebug, "INFO_GA01", context!!.getString(R.string.INFO_GA01)
                        )
                    }


                }
            }

        } else {
            if (attributeResponse != null) {

                val deviceObj = JSONObject()
                deviceObj.put(DEVICE_ID, uniqueId)
                deviceObj.put(DEVICE_TAG, "")


                //ADD TO MAIN OBJECT
                val mainObj = JSONObject()
                mainObj.put(DEVICE, deviceObj)
                mainObj.put(
                    ATTRIBUTES, getAttributesList(attributeResponse.d!!.att!!, null)
                )

                //ADD MAIN BOJ TO ARRAY.
                mainArray.put(mainObj)

                //Attributes data not found
                if (mainArray.length() == 0) {
                    iotSDKLogUtils!!.log(
                        true, isDebug, "ERR_GA02", context!!.getString(R.string.ERR_GA02)
                    )
                } else {
                    iotSDKLogUtils!!.log(
                        false, isDebug, "INFO_GA01", context!!.getString(R.string.INFO_GA01)
                    )
                }


            }
        }
        return mainArray.toString()
    }


    @JvmSynthetic
    fun getTwins() {
        if (mqttService != null) {
            iotSDKLogUtils!!.log(
                false, isDebug, "INFO_TP02", context!!.getString(R.string.INFO_TP02)
            )
            mqttService?.getAllTwins()
        }
    }

    @JvmSynthetic
    fun updateTwin(key: String?, value: String?) {
        if (!validationUtils!!.validateKeyValue(key!!, value!!)) return
        try {
            if (mqttService != null) publishMessage(
                "",
                JSONObject().put(key, value).toString(),
                true
            )
        } catch (e: JSONException) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP01", e.message!!)
            e.printStackTrace()
        }
    }


    /**
     * send acknowledgment to IOT connect portal
     *
     * @param obj         JSONObject object for "d"
     * @param messageType Message Type
     *
     * https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-to-cloud-d2c-messages/#Device_Acknowledgement
     */
    @JvmSynthetic
    fun sendAck(obj: String?) {
        var request: JSONObject? = null

        try {
            request = JSONObject(obj)
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        if (!validationUtils!!.validateAckParameters(request)) return

        if (obj != null) {
            val response = getSyncResponse()
            if (response != null) {
                publishMessage(response.d.p.topics.ack, obj, false)
            }
        }
    }


    /*Disconnect the device from MQTT connection.
     * stop all timers and change the device connection status.
     * */

    @JvmSynthetic
    fun dispose() {
        edgeDeviceTimerStop()
        timerStop(reCheckingTimer)
        timerStop(timerCheckDeviceState)
        timerStop(timerOfflineSync)

        if (context != null) {
            IotSDKPreferences.getInstance(context)?.clearSharedPreferences()
        }
        if (mqttService != null) {
            mqttService?.disconnectClient()
            mqttService?.clearInstance() //destroy singleton object.
        }
        unregisterReceiver()
        onHeartbeatCommand()
        sdkClientManger = null
    }

    /* Unregister network receiver.
     *
     * */
    private fun unregisterReceiver() {

        try {
            networkStateReceiver!!.removeListener(this)
            context?.unregisterReceiver(networkStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onConnectionStateChange(isConnected: Boolean) {
        if (isConnected) {
            iotSDKLogUtils!!.log(
                false, isDebug, "INFO_IN02", context!!.getString(R.string.INFO_IN02)
            )

        } else {
            iotSDKLogUtils!!.log(
                false, isDebug, "INFO_IN03", context!!.getString(R.string.INFO_IN03)
            )
            dispose()
        }

        setConnected(isConnected)
        onDeviceConnectionStatus(isConnected)

    }

    private fun isConnected(): Boolean {
        return isConnected
    }

    private fun setConnected(connected: Boolean) {
        isConnected = connected
    }


    /* ON DEVICE CONNECTION STATUS CHANGE command create json with bellow format and provide to framework.
        *
        * {"ackb":false,"ack":"","ct":116,"command":"false","cpid":"","guid":"","uniqueId":""}
        *
        * command = (true = connected, false = disconnected)
        * */
    private fun onDeviceConnectionStatus(isConnected: Boolean) {

        try {
            val strJson = SDKClientUtils.createCommandFormat(
                C2DMessageEnums.DEVICE_CONNECTION_STATUS.value,
                cpId,
                "",
                uniqueId,
                isConnected.toString(),
                false,
                ""
            )
            deviceCallback?.onReceiveMsg(strJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun twinUpdateCallback(data: JSONObject?) {
        try {
            data?.put(UNIQUE_ID_View, uniqueId)
            deviceCallback?.twinUpdateCallback(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun networkAvailable() {
        try {
            if (!isSaveToOffline) {

                //check is there any text file name.
                if (IotSDKPreferences.getInstance(context!!)!!
                        .getList(IotSDKPreferences.TEXT_FILE_NAME).isEmpty()
                ) {
                    iotSDKLogUtils!!.log(
                        false, isDebug, "INFO_OS05", context.getString(R.string.INFO_OS05)
                    )
                    return
                }

                //check device if there any file stored for offline storage.
                val directory = File(context.filesDir, directoryPath)
                if (directory.exists()) {
                    val contents = directory.listFiles()
                    if (contents != null) {
                        if (contents.isNotEmpty()) {
                            checkIsDeviceOnline()
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
        }
    }

    override fun networkUnavailable() {
        timerStop(timerCheckDeviceState)
        if (!isSaveToOffline) {
            timerStop(timerOfflineSync)
        }
    }

    private var timerOfflineSync: Timer? = null
    private var syncOfflineData = true
    private val OFFLINE_DATA = "od"

    /*Start timer for publish "df" value interval
     * */
    private fun publishOfflineData() {
        try {
            syncOfflineData = true
            val response = getSyncResponse()
            val finalOfflineData = CopyOnWriteArrayList<String>()
            finalOfflineData.addAll(readTextFile())
            if (finalOfflineData.isEmpty()) return

            Thread.sleep(5000)
            //start timer to sync offline data.
            timerOfflineSync = Timer()
            val timerTaskObj: TimerTask = object : TimerTask() {
                override fun run() {
                    //read next text file, when previous list is done sync.
                    if (finalOfflineData.isEmpty()) {
                        finalOfflineData.addAll(readTextFile())
                    }
                    syncOfflineData = if (syncOfflineData) {
                        if (!finalOfflineData.isEmpty()) {
                            for (i in finalOfflineData.indices) {
                                val data = finalOfflineData[i]
                                try {
                                    val dataObj = JSONObject(data)
//                                    dataObj.put(OFFLINE_DATA, 1);

                                    //publish offline data.

                                    mqttService!!.publishMessage(
                                        response?.d?.p?.topics?.od!!,
                                        dataObj.toString()
                                    )
                                } catch (e: JSONException) {
                                    iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
                                    e.printStackTrace()
                                }
                                //finalOfflineData.removeAt(i)
                            }
                            finalOfflineData.removeAll(readTextFile())
                        }
                        false
                    } else {
                        true
                    }
                }
            }
            timerOfflineSync!!.schedule(timerTaskObj, 0, 10000)
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
        }
    }

    private fun readTextFile(): CopyOnWriteArrayList<String> {
        val offlineData = CopyOnWriteArrayList<String>()
        try {
            val preferences = IotSDKPreferences.getInstance(
                context!!
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
                        File(context.filesDir, directoryPath),
                        fileNamesList[0] + ".txt"
                    )
                )
            )

            bufferedReader.useLines { lines ->
                lines.forEach {
                    offlineData.add(it)
                }
            }


            bufferedReader.close()

            //delete text file after reading all records.
            if (deleteTextFile(
                    fileNamesList as ArrayList<String?>,
                    context,
                    directoryPath,
                    iotSDKLogUtils,
                    isDebug
                )
            ) {
                iotSDKLogUtils!!.log(
                    false, isDebug, "INFO_OS04", context.getString(R.string.INFO_OS04)
                )
            }
        } catch (e: Exception) {
            iotSDKLogUtils!!.log(
                true, isDebug, "ERR_OS03", context!!.getString(R.string.ERR_OS03) + e.message
            )
            e.printStackTrace()
        }
        return offlineData
    }


    override fun onFailedResponse(message: String?) {

    }

    /*Check is device got connected on network available time, than publish offline data.
     * */
    private var timerCheckDeviceState: Timer? = null

    private fun checkIsDeviceOnline() {
        timerCheckDeviceState = Timer()
        val timerTaskObj: TimerTask = object : TimerTask() {
            override fun run() {
                if (isConnected()) {
                    timerStop(timerCheckDeviceState)
                    publishOfflineData()
                }
            }
        }
        timerCheckDeviceState!!.schedule(timerTaskObj, 0, 2000)
    }


    /**
     * Send device data to server by calling publish method.
     *
     * @param jsonData json data from client as below.
     *
     */
    @JvmSynthetic
    fun sendData(jsonData: String?) {
        if (!validationUtils!!.isValidInputFormat(jsonData!!, uniqueId!!)) return
        if (!idEdgeDevice) { // simple device.
            publishDeviceInputData(jsonData)
        } else { //Edge device
            try {
                processEdgeDeviceInputData(jsonData)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    /*
    *https://docs.iotconnect.io/iotconnect/resources/device-message-2-1-2/device-identity-messages/#devices
    *
    * If device is of gateway type then below function will get child device from IOT connect portal
    * {"d": {"d": [{"tg": "","id": ""}],"ct": 204,"ec": 0 }}
    *
    */

    @JvmSynthetic
    fun getChildDevices() {
        val response = getSyncResponse()
        publishMessage(
            response?.d?.p?.topics!!.di,
            JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_CHILD_DEVICES.value)
                .toString(),
            false
        )
    }

    /*Create child Device
    *
    *{"mt":221,"d":{"dn":"adasdad","id":"asdasd","tg":"qwe","g":"xvxcvx"}}
    * */
    @JvmSynthetic
    fun createChildDevice(innerObject: JSONObject) {
        val response = getSyncResponse()
        val mainObject = JSONObject()
        innerObject.put("g", response?.d?.meta?.gtw?.g)
        mainObject.put("mt", DeviceIdentityMessages.CREATE_CHILD_DEVICE.value)
        mainObject.put("d", innerObject)
        publishMessage(
            response?.d?.p?.topics?.di!!, mainObject.toString(), false
        )

    }

    /*Delete child device
    *
    *{"mt":222,"d":{"id":"asdasd"}}
    *
    * */
    @JvmSynthetic
    fun deleteChildDevice(innerObject: JSONObject) {
        val response = getSyncResponse()
        val mainObject = JSONObject()
        mainObject.put("mt", DeviceIdentityMessages.DELETE_CHILD_DEVICE.value)
        mainObject.put("d", innerObject)
        publishMessage(
            response?.d?.p?.topics?.di!!, mainObject.toString(), false
        )

    }

    /*process input data to publish.
     * 1.Publish input data based on interval of "df" value.
     * "df"="60" than data is published in interval of 60 seconds. If data is publish lass than 60 second time than data is ignored.
     * 2.If "df" = 0, input data can be published on button click.
     *
     * @param jsonData       input data from framework.
     * */
    private fun publishDeviceInputData(jsonData: String?) {

        val response = getSyncResponse()
        var df = 0
        if (response != null) {
            df = response.d.meta.df
        }
        if (savedTime == 0L) {
            savedTime = getCurrentTime()
            savedTime = savedTime + df
        } else {
            val currentTime: Long = getCurrentTime()
            if (currentTime <= savedTime) {
                return
            } else {
                savedTime = savedTime + df
            }
        }
        if (response != null) {
            if (jsonData != null) {
                publishDeviceInputData(response, jsonData, getAttributeResponse())
            }
        }
    }

    /* Publish input data for Device.
     * @param inputJsonStr input json from user.
     * */
    private fun publishDeviceInputData(
        syncResponse: IdentityServiceResponse?, inputJsonStr: String, dObj: CommonResponseBean?
    ) {
        try {

            val jsonArray = JSONArray(inputJsonStr)

            var doFaultyPublish = false
            var doReportingPublish = false

            var reportingObject_reporting: JSONObject? = null
            // 0 for reporting.
            var reportingObject_faulty: JSONObject? = null
            // 1 for faulty.

            val outerD_Obj_reporting = JSONObject()
            val outerD_Obj_Faulty = JSONObject()

            val arrayObj_attributes_reporting = JSONArray()
            val arrayObj_attributes_faulty = JSONArray()

            val gatewayChildResponse = getGatewayChildResponse()
            val getChildDeviceBean = GetChildDeviceBean()

            getChildDeviceBean.tg = syncResponse?.d?.meta?.gtw?.tg
            getChildDeviceBean.id = uniqueId
            gatewayChildResponse?.d?.childDevice?.add(getChildDeviceBean)


            for (i in 0 until jsonArray.length()) {

                val uniqueId = jsonArray.getJSONObject(i).getString(UNIQUE_ID_View)
                val dataObj = jsonArray.getJSONObject(i).getJSONObject(DATA)
                val dataJsonKey = dataObj.keys()

                val tag = SDKClientUtils.getTag(uniqueId, gatewayChildResponse?.d)

                reportingObject_reporting = JSONObject()

                reportingObject_faulty = JSONObject()


                val innerD_Obj_reporting = JSONObject()
                val innerD_Obj_faulty = JSONObject()

                //getting value for
//                 "d": {"Temp":"66","humidity":"55","abc":"y","gyro":{"x":"7","y":"8","z":"9"}}
                while (dataJsonKey.hasNext()) {
                    val key = dataJsonKey.next()
                    val value = dataObj.getString(key)
                    if (value.replace("\\s".toRegex(), "")
                            .isNotEmpty() && JSONTokener(value).nextValue() is JSONObject
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
                            val gyroValidationValue =
                                compareForInputValidationNew(
                                    InnerKey,
                                    InnerKValue,
                                    tag,
                                    dObj,
                                    isSkipValidation
                                )
                            if (gyroValidationValue == 0) {
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
                        val othersValidation =
                            compareForInputValidationNew(key, value, tag, dObj, isSkipValidation)
                        if (othersValidation == 0) {
                            innerD_Obj_reporting.put(key, value)
                        } else {
                            innerD_Obj_faulty.put(key, value)
                        }
                    }
                }

                reportingObject_reporting.put(DT, DateTimeUtils.currentDate)
                reportingObject_reporting.put(ID, uniqueId)
                reportingObject_reporting.put(TG, tag)

                reportingObject_reporting.put(D_OBJ, innerD_Obj_reporting)

                reportingObject_faulty.put(DT, DateTimeUtils.currentDate)
                reportingObject_faulty.put(ID, uniqueId)
                reportingObject_faulty.put(TG, tag)

                reportingObject_faulty.put(D_OBJ, innerD_Obj_faulty)


                if (innerD_Obj_reporting.length() != 0) arrayObj_attributes_reporting.put(
                    reportingObject_reporting
                )

                if (innerD_Obj_faulty.length() != 0) arrayObj_attributes_faulty.put(
                    reportingObject_faulty
                )

                if (arrayObj_attributes_reporting.length() > 0) doReportingPublish = true
                if (arrayObj_attributes_faulty.length() > 0) doFaultyPublish = true


            }
            //add object of attribute object to parent object.

            outerD_Obj_reporting.put(DT, DateTimeUtils.currentDate)
            outerD_Obj_reporting.put(D_OBJ, arrayObj_attributes_reporting)

            outerD_Obj_Faulty.put(DT, DateTimeUtils.currentDate)
            outerD_Obj_Faulty.put(D_OBJ, arrayObj_attributes_faulty)


            //Reporting json string as below.
//{"dt":"2023-02-22T11:27:24.870Z","d":[{"dt":"2023-02-22T11:27:24.866Z","id":"ch1","tg":"ch","d":{"Humidity":"6","Lumosity":"6"}},{"dt":"2023-02-22T11:27:24.868Z","id":"AndroidGateway","tg":"p","d":{"Temp":"6","Gyroscope":{"x":"7","y":"8"}}}]}

            //publish reporting data
            if (doReportingPublish) publishMessage(
                syncResponse?.d?.p?.topics!!.rpt, outerD_Obj_reporting.toString(), false
            )

            //publish faulty data
            if (doFaultyPublish) publishMessage(
                syncResponse?.d?.p?.topics!!.flt, outerD_Obj_Faulty.toString(), false
            )
        } catch (e: JSONException) {
            e.printStackTrace()
            iotSDKLogUtils!!.log(true, isDebug, "CM01_SD01", e.message!!)
        }
    }


    /*Process the edge device input data from client.
     *
     * @param  jsonData
     * */
    private fun processEdgeDeviceInputData(jsonData: String?) {
        val syncResponse = getSyncResponse()
        val attributeResponse = getAttributeResponse()
        val edgeResponse = getEdgeRuleResponse()
        publishObjForRuleMatchEdgeDevice = null
        if (syncResponse != null) {
            try {
                val jsonArray = JSONArray(jsonData)

                val gatewayChildResponse = getGatewayChildResponse()
                val getChildDeviceBean = GetChildDeviceBean()

                getChildDeviceBean.tg = syncResponse.d.meta.gtw?.tg
                getChildDeviceBean.id = uniqueId
                gatewayChildResponse?.d?.childDevice?.add(getChildDeviceBean)

                for (i in 0 until jsonArray.length()) {
                    val uniqueId = jsonArray.getJSONObject(i).getString(UNIQUE_ID_View)
                    val dataObj = jsonArray.getJSONObject(i).getJSONObject(DATA)
                    val dataJsonKey = dataObj.keys()
                    val tag = SDKClientUtils.getTag(uniqueId, gatewayChildResponse?.d)
                    while (dataJsonKey.hasNext()) {
                        val key = dataJsonKey.next()
                        val value = dataObj.getString(key)
                        if (value.replace("\\s".toRegex(), "")
                                .isNotEmpty() && JSONTokener(value).nextValue() is JSONObject
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
                                val validation: Int = compareForInputValidationNew(
                                    innerKey, innerKValue, tag, attributeResponse, isSkipValidation
                                )

                                //ignore string value for edge device.
                                if (SDKClientUtils.isDigit(innerKValue) && validation != 1) {
                                    updateEdgeDeviceGyroObj(
                                        key,
                                        innerKey,
                                        innerKValue,
                                        uniqueId,
                                        edgeDeviceAttributeGyroMap,
                                        context
                                    )
                                    if (edgeResponse != null) {
                                        if (jsonData != null) {
                                            EvaluateRuleForEdgeDevice(
                                                edgeResponse.d!!.edge!!,
                                                key,
                                                innerKey,
                                                innerKValue,
                                                jsonData,
                                                AttObj
                                            )
                                        }
                                    }
                                }
                            }
                            //publish
                            publishRuleEvaluatedData()
                        } else {

                            //check for input validation dv="data validation". {"ln":"abc","dt":0,"dv":"10","tg":"","sq":8,"agt":63,"tw":"60s"}
                            val validation: Int = compareForInputValidationNew(
                                key, value, tag, attributeResponse, isSkipValidation
                            )

                            //ignore string value for edge device.
                            if (SDKClientUtils.isDigit(value) && validation != 1) {
                                updateEdgeDeviceObj(
                                    key, value, uniqueId, edgeDeviceAttributeMap, context
                                )
                                if (edgeResponse != null) {
                                    if (jsonData != null) {
                                        EvaluateRuleForEdgeDevice(
                                            edgeResponse.d!!.edge!!,
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
                    }
                }
            } catch (e: Exception) {
                iotSDKLogUtils!!.log(true, isDebug, "ERR_EE01", e.message!!)
                e.printStackTrace()
            }
        }
    }


    /*process data for edge device timer start.
     *
     * @param response      Sync service response.
     * */
    private fun processEdgeDeviceTWTimer(
        syncResponse: IdentityServiceResponse, response: CommonResponseBean
    ) {
        val attributeList = response.d?.att
        edgeDeviceAttributeMap = ArrayListMultimap.create()
        edgeDeviceAttributeGyroMap = ArrayListMultimap.create()
        edgeDeviceTimersList = ArrayList()
        if (attributeList != null) {
            for (bean in attributeList) {
                if (bean.p != null && bean.p.isNotEmpty()) {
                    // if for not empty "p":"gyro"
                    val gyroAttributeList: MutableList<TumblingWindowBean> = ArrayList()
                    val listD = bean.d
                    for (beanX in listD) {
                        val attributeLn = beanX.ln

                        val twb = TumblingWindowBean()
                        twb.attributeName = attributeLn
                        twb.uniqueId = uniqueId
                        twb.tag = ""

                        var tg: String
                        var id: String
                        if (bean.tg != null) {
                            tg = bean.tg

                            val gatewayChildResponse = getGatewayChildResponse()
                            val getChildDeviceBean = GetChildDeviceBean()

                            getChildDeviceBean.tg = syncResponse.d.meta.gtw.tg
                            getChildDeviceBean.id = uniqueId
                            gatewayChildResponse?.d?.childDevice?.add(getChildDeviceBean)

                            gatewayChildResponse?.d?.childDevice?.forEach {

                                if (it.tg == bean.tg) {
                                    val twb = TumblingWindowBean()
                                    twb.attributeName = attributeLn
                                    twb.uniqueId = it.id
                                    twb.tag = it.tg
                                    gyroAttributeList.add(twb)
                                    edgeDeviceTWTimerStart(
                                        syncResponse,
                                        beanX.tw,
                                        bean.p + "," + bean.tg,
                                        it.tg!!,
                                        it.id!!,
                                        gyroAttributeList,
                                        "isGyro"
                                    )
                                }

                            }

                        } else {
                            tg = ""
                            id = ""
                            gyroAttributeList.add(twb)
                            edgeDeviceTWTimerStart(
                                syncResponse,
                                beanX.tw,
                                bean.p + "," + bean.tg,
                                tg,
                                id,
                                gyroAttributeList,
                                "isGyro"
                            )
                        }


                    }


                } else {
                    val listD = bean.d
                    for (beanX in listD) {
                        val ln = beanX.ln
                        val tag: String
                        var id: String

                        if (beanX.tg != null) {
                            tag = beanX.tg
                            val gatewayChildResponse = getGatewayChildResponse()
                            val getChildDeviceBean = GetChildDeviceBean()

                            getChildDeviceBean.tg = syncResponse.d.meta.gtw.tg
                            getChildDeviceBean.id = uniqueId
                            gatewayChildResponse?.d?.childDevice?.add(getChildDeviceBean)

                            gatewayChildResponse?.d?.childDevice?.forEach {

                                if (it.tg == tag) {
                                    id = it.id!!
                                    edgeDeviceTWTimerStart(
                                        syncResponse,
                                        beanX.tw,
                                        ln,
                                        it.tg!!,
                                        id,
                                        null,
                                        "isSimple"
                                    )
                                }

                            }

                        } else {
                            tag = ""
                            id = ""
                            edgeDeviceTWTimerStart(
                                syncResponse,
                                beanX.tw,
                                ln,
                                tag,
                                id,
                                null,
                                "isSimple"
                            )
                        }
                    }
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
    private fun edgeDeviceTWTimerStart(
        syncResponse: IdentityServiceResponse,
        twTime: String?,
        ln: String?,
        tag: String,
        uniqueId1: String,
        gyroAttributeList: MutableList<TumblingWindowBean>?,
        checkAttType: String
    ) {
        var id = uniqueId1
        val tw = twTime?.replace("[^\\d.]".toRegex(), "")?.toDouble()

        if (syncResponse.d.meta.gtw != null) {

        } else {
            if (uniqueId != null) {
                id = uniqueId
            }
        }


        if (ln != null) {
            if (!TextUtils.isEmpty(id)) {
                if (checkAttType == "isSimple") {
                    val tumblingWindowBean = TumblingWindowBean()
                    tumblingWindowBean.uniqueId = id
                    edgeDeviceAttributeMap?.put(ln, tumblingWindowBean)
                }
            }

        }

        if (ln != null) {
            if (gyroAttributeList != null) {

                if (checkAttType == "isGyro") {
                    if (!edgeDeviceAttributeGyroMap!!.containsEntry(ln, gyroAttributeList)) {
                        (edgeDeviceAttributeGyroMap as ListMultimap<String, List<TumblingWindowBean>>).put(
                            ln,
                            gyroAttributeList
                        )
                    }
                }
            }
        }

        val timerTumblingWindow = Timer()
        edgeDeviceTimersList?.add(timerTumblingWindow)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {

                val publishObj = publishEdgeDeviceInputData(
                    ln,
                    tag,
                    edgeDeviceAttributeGyroMap,
                    edgeDeviceAttributeMap,
                    id,
                    cpId,
                    environment,
                    appVersion,
                    ""
                )

                //check publish object is not empty of data. check to inner "d":[] object. Example below json string inner "d":[] object is empty.
                //{"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2021-01-11T02:36:19.644Z","mt":2,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2021-01-11T02:36:19.644Z","tg":"","d":[]}]}
                var isPublish = true
                try {
                    val dArray = publishObj?.getJSONArray(D_OBJ)
                    if (dArray != null) {
                        for (i in 0 until dArray.length()) {
                            val innerDObj = dArray.getJSONObject(i).getJSONObject(D_OBJ)
                            if (innerDObj.length() <= 0) {
                                isPublish = false
                            }
                        }
                    }


                    //return on "d":[] object is empty.
                    if (!isPublish) return
                } catch (e: JSONException) {
                    e.printStackTrace()
                    return
                }
                if (publishObj != null) {
                    publishMessage(syncResponse.d.p.topics.erpt, publishObj.toString(), false)
                }
            }
        }
        if (tw != null) {
            timerTumblingWindow.scheduleAtFixedRate(
                timerTask, (tw * 1000).toLong(), (tw * 1000).toLong()
            )
        }
    }


    /*Stop timer for Edge device attributes (humidity,temp,gyro etc...).
     * Clear the list and map collection.
     * */

    private fun edgeDeviceTimerStop() {
        edgeDeviceAttributeMap?.clear()
        edgeDeviceAttributeGyroMap?.clear()
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

    /*re-checking the device connection after interval of 10 seconds for 3 times.
     * in case of device connect button is clicked and than device creating process is done on web.
     * */
    private fun reChecking() {
        iotSDKLogUtils!!.log(false, isDebug, "INFO_IN06", context!!.getString(R.string.INFO_IN06))
        startReCheckingTimer()
    }

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

    /* On edge device rule match, send below json format to firmware.
    *{"cmdType":"0x01","data":{"cpid":"deviceData.cpId","guid":"deviceData.company","uniqueId":"device uniqueId","command":"json.cmd","ack":true,"ackId":null,"cmdType":"config.commandType.CORE_COMMAND, 0x01"}}
    *
    * */
    private fun onEdgeDeviceRuleMatched(bean: GetEdgeRuleBean) {
        val strJson = SDKClientUtils.createCommandFormat(
            C2DMessageEnums.DEVICE_COMMAND.value, cpId, bean.g, uniqueId, bean.cmd, true, ""
        )

        try {
            deviceCallback!!.onReceiveMsg(strJson)
        } catch (e: Exception) {
            e.printStackTrace()
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
        ruleBeansList: List<GetEdgeRuleBean>,
        parentKey: String,
        innerKey: String?,
        inputValue: String,
        inputJsonString: String,
        attObj: JSONObject?
    ) {
        try {
            if (ruleBeansList != null) {
                val value = inputValue.replace("\\s".toRegex(), "").toDouble()
                for (bean in ruleBeansList) {
                    val con = bean.con
                    val attKey: String? = getAttributeName(con)
                    //match parent attribute name (eg. temp == temp OR gyro == gyro)
                    if (attKey != null && parentKey == attKey) {


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

                            setPublishJsonForRuleMatchedEdgeDevice(
                                parent[1], innerKey, value, attObj, parentKey, bean, inputJsonString
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
                                val mainObj: JSONObject = getEdgeDevicePublishMainObj(
                                    DateTimeUtils.currentDate
                                )
                                val publishObj: JSONObject = getPublishStringEdgeDevice(
                                    uniqueId,
                                    DateTimeUtils.currentDate,
                                    bean,
                                    inputJsonString,
                                    cvAttObj,
                                    mainObj
                                )!!
                                //publish edge device rule matched data. Publish simple attribute data only. (temp > 10)
                                if (publishObj != null) {
                                    val syncResponse = getSyncResponse()
                                    publishMessage(
                                        syncResponse?.d?.p?.topics?.erm!!,
                                        publishObj.toString(),
                                        false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    /*Set the json data for publish for edge device only for gyro attributes.
     * */
    private fun setPublishJsonForRuleMatchedEdgeDevice(
        childAttNameValue: String,
        innerKey: String,
        value: Double,
        attObj: JSONObject?,
        parentKey: String,
        bean: GetEdgeRuleBean,
        inputJsonString: String
    ) {
        //collect publish data for gyro type object.
        try {
            val key: String = getAttName(childAttNameValue)!!
            if (innerKey == key) { // compare x with x.
                if (evaluateEdgeDeviceRuleValue(childAttNameValue, value)) {
                    onEdgeDeviceRuleMatched(bean)
                    attObj!!.put(key, value.toString() + "")
                }
            }
            if (attObj!!.length() != 0) {
                val cvAttObj = JSONObject()
                cvAttObj.put(parentKey, attObj)
                val mainObj: JSONObject = getEdgeDevicePublishMainObj(
                    DateTimeUtils.currentDate
                )
                publishObjForRuleMatchEdgeDevice = getPublishStringEdgeDevice(
                    uniqueId, DateTimeUtils.currentDate, bean, inputJsonString, cvAttObj, mainObj
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /* 1.Publish edge device rule matched data with bellow json format.
     * 2.This method publish gyro type attributes data.(//"gyro.x = 10 AND gyro.y > 10")
     * {"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"514076B1-3C21-4849-A777-F423B1821FC7","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    private fun publishRuleEvaluatedData() {
        val syncResponse = getSyncResponse()
        if (publishObjForRuleMatchEdgeDevice != null) publishMessage(
            syncResponse?.d?.p?.topics?.erm!!, publishObjForRuleMatchEdgeDevice.toString(), false
        )
    }

    private fun onHeartbeatCommand(frequencyHeartBeat: Int) {
        onHeartbeatCommand()
        val response = getSyncResponse()
        job = scope.launch {
            while (true) {
                // the function that should be ran every second
                publishMessage(
                    response?.d?.p?.topics?.hb!!,
                    JSONObject().toString(),
                    false
                )
                delay(frequencyHeartBeat.toLong() * 1000)
            }
        }
    }

    private fun onHeartbeatCommand() {
        job?.cancel()
        job = null
    }

}

