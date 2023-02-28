package com.iotconnectsdk

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.URLUtil
import com.google.gson.Gson
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.beans.GetChildDeviceBean
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.enums.C2DMessageEnums
import com.iotconnectsdk.enums.DeviceIdentityMessages
import com.iotconnectsdk.interfaces.DeviceCallback
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.PublishMessageCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.mqtt.IotSDKMQTTService
import com.iotconnectsdk.utils.*
import com.iotconnectsdk.utils.SDKClientUtils.createTextFile
import com.iotconnectsdk.utils.SDKClientUtils.getAttributesList
import com.iotconnectsdk.utils.ValidationTelemetryUtils.compareForInputValidationNew
import com.iotconnectsdk.webservices.CallWebServices
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse
import com.iotconnectsdk.webservices.responsebean.IdentityServiceResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.*

/**
 * class for SDKClient
 */
class SDKClient(
    private val context: Context?,
    private val cpId: String?,
    private val uniqueId: String?,
    private val deviceCallback: DeviceCallback?,
    private val twinUpdateCallback: TwinUpdateCallback?,
    private val sdkOptions: String?,
    private val environment: String?,

    ) : WsResponseInterface, HubToSdkCallback, PublishMessageCallback, TwinUpdateCallback,
    NetworkStateReceiver.NetworkStateReceiverListener {


    private var validationUtils: ValidationUtils? = null

    private var iotSDKLogUtils: IotSDKLogUtils? = null

    private var mqttService: IotSDKMQTTService? = null

    private val TEXT_FILE_PREFIX = "current"

    private var commandType: String? = null

    private var isConnected = false

    private var isDispose = false

    private var isSaveToOffline = false

    private var isDebug = false

    private var idEdgeDevice = false

    private val appVersion = "v2.1"

    private var discoveryUrl = ""

    private val DEFAULT_DISCOVERY_URL = "https://discovery.iotconnect.io/"

    private val URL_PATH = "api/$appVersion/dsdk/"

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

    private val CMD_TYPE = "ct"

    private val DISCOVERY_URL = "discoveryUrl"

    private val IS_DEBUG = "isDebug"

    private val DATA = "data"

    private val TIME = "time"

    private val ID = "id"

    private val DT = "dt"

    private val DF = "df"

    private val TG = "tg"

    private val DIRECTORY_PATH = "logs/offline/"

    private var savedTime: Long = 0

    private val EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE = 3

    //for Edge Device
    private val edgeDeviceTimersList: List<Timer>? = null

    private val edgeDeviceAttributeMap: Map<String, TumblingWindowBean>? = null

    private val edgeDeviceAttributeGyroMap: Map<String, List<TumblingWindowBean>>? = null

    private val publishObjForRuleMatchEdgeDevice: JSONObject? = null

    private var networkStateReceiver: NetworkStateReceiver? = null

    private var fileSizeToCreateInMb = 0

    private var directoryPath: String? = null

    private var fileCount = 0

    private val reCheckingTimer: Timer? = null

    private var reCheckingCountTime = 0


    /*return singleton object for this class.
     * */
    companion object {
        @Volatile
        private var sdkClient: SDKClient? = null

        @JvmStatic
        fun getInstance(
            context: Context?, cpId: String?, uniqueId: String?, deviceCallback: DeviceCallback?,
            twinUpdateCallback: TwinUpdateCallback?, sdkOptions: String?, environment: String?
        ): SDKClient {
            synchronized(this) {
                if (sdkClient == null) {
                    sdkClient = SDKClient(
                        context, cpId, uniqueId, deviceCallback, twinUpdateCallback, sdkOptions,
                        environment
                    )
                }
                sdkClient?.connect()
                sdkClient?.registerNetworkState()
                return sdkClient!!
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
        this.reCheckingCountTime = 0
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
                discoveryUrl =
                    DEFAULT_DISCOVERY_URL //set default discovery url when it is empty from client end.
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
            discoveryUrl = DEFAULT_DISCOVERY_URL //set default discovery url when sdkOption is null.
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
                discoveryUrl + URL_PATH + CPID + cpId /*+ LANG_ANDROID_VER + appVersion*/ + ENV + environment
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
                    IotSDKUrls.DISCOVERY_SERVICE,
                    ignoreCase = true
                ) && response != null
            ) {
                val discoveryApiResponse =
                    Gson().fromJson(response, DiscoveryApiResponse::class.java)
                if (discoveryApiResponse != null && discoveryApiResponse.d.bu != null) {
                    //BaseUrl received to sync the device information.
                    iotSDKLogUtils!!.log(
                        false,
                        isDebug,
                        "INFO_IN07",
                        context!!.getString(R.string.INFO_IN07)
                    )
                    if (!validationUtils!!.validateBaseUrl(discoveryApiResponse)) return
                    val baseUrl: String = discoveryApiResponse.d.bu + UNIQUE_ID + uniqueId
                    IotSDKPreferences.getInstance(context)
                        ?.putStringData(IotSDKPreferences.SYNC_API, baseUrl)
                    callSyncService()
                } else {
                    val responseCodeMessage =
                        validationUtils?.responseCodeMessage(discoveryApiResponse.d.ec)
                    deviceCallback?.onReceiveMsg(responseCodeMessage)
                    sdkClient = null
                }
            } else if (methodName.equals(
                    IotSDKUrls.SYNC_SERVICE,
                    ignoreCase = true
                ) && response != null
            ) {
                val syncServiceResponseData =
                    Gson().fromJson(response, IdentityServiceResponse::class.java)

                if (syncServiceResponseData != null && syncServiceResponseData?.d != null && syncServiceResponseData.d.p != null) {
                    //save the sync response to shared pref
                    IotSDKPreferences.getInstance(context!!)
                        ?.putStringData(IotSDKPreferences.SYNC_RESPONSE, response)
                    callMQTTService()
                } else {
                    val responseCodeMessage =
                        validationUtils?.responseCodeMessage(syncServiceResponseData.d.ec)
                    deviceCallback?.onReceiveMsg(responseCodeMessage)
                    sdkClient = null
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
            context!!, response.d.p, this, this, this, iotSDKLogUtils!!, isDebug, uniqueId!!
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

    /*get the saved Settings response from shared preference.
  * */
    private fun getSettingsResponse(): CommonResponseBean? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getDeviceInformation(IotSDKPreferences.SETTING_TWIN_RESPONSE)
    }

    /*get the saved GatewayChild response from shared preference.
  * */
    private fun getGatewayChildResponse(): CommonResponseBean? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getDeviceInformation(IotSDKPreferences.CHILD_DEVICE_RESPONSE)
    }


    /*
    * Callback to show message in UI
    * */
    override fun onSendMsgUI(message: String?) {
        if (message != null) {
            deviceCallback?.onReceiveMsg(message)
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
            /* publishMessage(
                 response.d.p.topics.di,
                 JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_EDGE_RULE.value)
                     .toString(), false
             )*/
        }

        if ((response?.d?.has?.d == 1)) {
            publishMessage(
                response.d.p.topics.di,
                JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_CHILD_DEVICES.value)
                    .toString(), false
            )
        }

        if ((response?.d?.has?.ota == 1)) {
            /*publishMessage(
                response.d.p.topics.di,
                JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_PENDING_OTA.value)
                    .toString(), false
            )*/
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
                val mainObjectLog = JSONObject(message)
                Log.d("mainObject", "::$mainObjectLog")

                val gson = Gson()
                val commonModel = gson.fromJson(message, CommonResponseBean::class.java)

                if (commonModel?.d != null) {
                    when (commonModel.d.ct) {
                        /*{"d":{"att":[{"p":"","dt":0,"tg":"","d":[{"ln":"Temp","dt":1,"dv":"5 to 10","sq":1,"tg":"p"},{"ln":"Humidity","dt":1,"dv":"5 to 10","sq":2,"tg":"ch"},{"ln":"Lumosity","dt":1,"dv":"","sq":4,"tg":"ch"}]},{"p":"Gyroscope","dt":11,"tg":"p","d":[{"ln":"x","dt":1,"dv":"","sq":1,"tg":"p"},{"ln":"y","dt":1,"dv":"","sq":2,"tg":"p"}]}],"ct":201,"ec":0,"dt":"2023-02-22T10:41:18.6947577Z"}}*/
                        DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value -> {
                            IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                IotSDKPreferences.ATTRIBUTE_RESPONSE, Gson().toJson(commonModel)
                            )

                            onDeviceConnectionStatus(isConnected())
                        }

                        /*
                        * {"d":{"set":[{"ln":"Motor","dt":1,"dv":""}],"ct":202,"ec":0,"dt":"2023-02-22T10:41:18.6948342Z"}}
                        * */
                        DeviceIdentityMessages.GET_DEVICE_TEMPLATE_SETTINGS_TWIN.value -> {
                            IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                IotSDKPreferences.SETTING_TWIN_RESPONSE, Gson().toJson(commonModel)
                            )
                        }

                        DeviceIdentityMessages.GET_EDGE_RULE.value -> {

                        }

                        /*
                        * {"d":{"d":[{"tg":"ch","id":"ch1"}],"ct":204,"ec":0,"dt":"2023-02-22T10:41:18.2683604Z"}}
                        * */
                        DeviceIdentityMessages.GET_CHILD_DEVICES.value -> {
                            IotSDKPreferences.getInstance(context!!)!!.putStringData(
                                IotSDKPreferences.CHILD_DEVICE_RESPONSE, Gson().toJson(commonModel)
                            )
                        }

                        DeviceIdentityMessages.GET_PENDING_OTA.value -> {

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
                                false, isDebug, "INFO_CM01", context!!.getString(R.string.INFO_CM01)
                            )
                            deviceCallback?.onReceiveMsg(message)
                        }

                        /*OTA Command received by the device from the cloud*/
                        C2DMessageEnums.OTA_COMMAND.value -> {

                        }

                        /*Module Command received by the device from the cloud*/
                        C2DMessageEnums.MODULE_COMMAND.value -> {

                        }

                        /*The device must send a message of type 201 to get updated attributes*/
                        C2DMessageEnums.REFRESH_ATTRIBUTE.value -> {
                            val response = getSyncResponse()
                            publishMessage(
                                response?.d?.p?.topics!!.di, JSONObject().put(
                                    MESSAGE_TYPE,
                                    DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value
                                ).toString(), false
                            )
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
                        }

                        /*The device must send a message of type 203 to get updated Edge rules*/
                        C2DMessageEnums.REFRESH_EDGE_RULE.value -> {

                        }

                        /*The device must send a message of type 204 to get updated child devices*/
                        C2DMessageEnums.REFRESH_CHILD_DEVICE.value -> {
                            val response = getSyncResponse()
                            publishMessage(
                                response?.d?.p?.topics!!.di, JSONObject().put(
                                    MESSAGE_TYPE,
                                    DeviceIdentityMessages.GET_CHILD_DEVICES.value
                                ).toString(), false
                            )
                        }

                        /*The device needs to update the frequency received in this message*/

                        C2DMessageEnums.DATA_FREQUENCY_CHANGE.value -> {
                            if (context != null) {
                                val response = getSyncResponse()
                                response?.d?.meta?.df = mainObject.getInt(DF)
                                IotSDKPreferences.getInstance(context)?.putStringData(
                                    IotSDKPreferences.SYNC_RESPONSE,
                                    Gson().toJson(response)
                                )
                            }
                        }

                        /*The device must stop all communication and release the MQTT connection*/
                        C2DMessageEnums.DEVICE_DELETED.value, C2DMessageEnums.DEVICE_DISABLED.value, C2DMessageEnums.DEVICE_RELEASED.value,
                        C2DMessageEnums.STOP_OPERATION.value, C2DMessageEnums.DEVICE_CONNECTION_STATUS.value -> {
                            iotSDKLogUtils?.log(
                                false, isDebug, "INFO_CM16", context!!.getString(R.string.INFO_CM16)
                            )
                            dispose()
                        }

                        /*The device must start sending a heartbeat*/
                        C2DMessageEnums.START_HEARTBEAT.value -> {

                        }

                        /*The device must stop sending a heartbeat*/
                        C2DMessageEnums.STOP_HEARTBEAT.value -> {

                        }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }


    }

    /*Call publish method of IotSDKMQTTService class to publish to web.
   * 1.When device is not connected to network and offline storage is true from client, than save all published message to device memory.
   * */
    private fun publishMessage(topics: String, publishMessage: String, isUpdate: Boolean) {

        Log.d("publishMessage", "::$publishMessage")

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
                                if (fileSizeToCreateInMb != 0 && SDKClientUtils.getFileSizeInKB(
                                        file
                                    ) >= fileSizeToCreateInMb
                                ) {
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
    fun getAttributes(): String? {

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
                        getAttributesList(attributeResponse.d!!.att, childDeviceBean.tg)
                    )

                    //ADD MAIN BOJ TO ARRAY.
                    mainArray.put(mainObj)

                    //Attributes data not found
                    if (mainArray.length() == 0) {
                        iotSDKLogUtils!!.log(
                            true,
                            isDebug,
                            "ERR_GA02",
                            context!!.getString(R.string.ERR_GA02)
                        )
                    } else {
                        iotSDKLogUtils!!.log(
                            false,
                            isDebug,
                            "INFO_GA01",
                            context!!.getString(R.string.INFO_GA01)
                        )
                    }


                }
            }

        } else {
            if (attributeResponse != null) {

                //CREATE DEVICE OBJECT, "device":{"id":"dee02","tg":"gateway"}
                val deviceObj = JSONObject()
                deviceObj.put(DEVICE_ID, uniqueId)
                deviceObj.put(DEVICE_TAG, "")


                //ADD TO MAIN OBJECT
                val mainObj = JSONObject()
                mainObj.put(DEVICE, deviceObj)
                mainObj.put(
                    ATTRIBUTES, getAttributesList(attributeResponse.d!!.att, null)
                )

                //ADD MAIN BOJ TO ARRAY.
                mainArray.put(mainObj)

                //Attributes data not found
                if (mainArray.length() == 0) {
                    iotSDKLogUtils!!.log(
                        true,
                        isDebug,
                        "ERR_GA02",
                        context!!.getString(R.string.ERR_GA02)
                    )
                } else {
                    iotSDKLogUtils!!.log(
                        false,
                        isDebug,
                        "INFO_GA01",
                        context!!.getString(R.string.INFO_GA01)
                    )
                }


            }
        }
        return mainArray.toString()
    }


    fun getAllTwins() {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP04", context!!.getString(R.string.ERR_TP04))
            return
        }
        if (mqttService != null) {
            iotSDKLogUtils!!.log(
                false,
                isDebug,
                "INFO_TP02",
                context!!.getString(R.string.INFO_TP02)
            )
            mqttService?.getAllTwins()
        }
    }

    fun updateTwin(key: String?, value: String?) {
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_TP01", context!!.getString(R.string.ERR_TP01))
            return
        }
        if (!validationUtils!!.validateKeyValue(key!!, value!!)) return
        try {
            if (mqttService != null)
                publishMessage("", JSONObject().put(key, value).toString(), true)
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
    fun sendAck(obj: String?, messageType: String?) {
        var request: JSONObject? = null
        if (isDispose) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_CM04", context!!.getString(R.string.ERR_CM04))
            return
        }
        try {
            request = JSONObject(obj)
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        if (!validationUtils!!.validateAckParameters(request, messageType!!)) return

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
    fun dispose() {
        isDispose = true
        //edgeDeviceTimerStop()

        if (mqttService != null) {
            mqttService?.disconnectClient()
            mqttService?.clearInstance() //destroy singleton object.
        }
        //  timerStop(reCheckingTimer)
        // timerStop(timerCheckDeviceState)
        // timerStop(timerOfflineSync)
        unregisterReceiver()
        sdkClient = null
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
        val strJson = SDKClientUtils.createCommandFormat(
            C2DMessageEnums.DEVICE_CONNECTION_STATUS.value, cpId, "", uniqueId,
            isConnected.toString(), false, ""
        )
        deviceCallback?.onReceiveMsg(strJson)

    }

    override fun twinUpdateCallback(data: JSONObject?) {
        try {
            data?.put(UNIQUE_ID_View, uniqueId)
            twinUpdateCallback?.twinUpdateCallback(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun networkAvailable() {

    }

    override fun networkUnavailable() {

    }


    override fun onFailedResponse(message: String?) {

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
        if (!validationUtils!!.isValidInputFormat(jsonData!!, uniqueId!!))
            return
        if (!idEdgeDevice) { // simple device.
            publishDeviceInputData(jsonData)
        } else { //Edge device
            //  processEdgeDeviceInputData(jsonData)
        }
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
        /*var df = 0
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
        }*/
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
        syncResponse: IdentityServiceResponse?, inputJsonStr: String,
        dObj: CommonResponseBean?
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
                                compareForInputValidationNew(InnerKey, InnerKValue, tag, dObj)
                            if (gyroValidationValue == 0) {
                                gyroObj_reporting.put(InnerKey, InnerKValue)
                            } else {
                                gyroObj_faulty.put(InnerKey, InnerKValue)
                            }
                        }

                        //add gyro object to parent d object.
                        if (gyroObj_reporting.length() != 0)
                            innerD_Obj_reporting.put(key, gyroObj_reporting)
                        if (gyroObj_faulty.length() != 0)
                            innerD_Obj_faulty.put(key, gyroObj_faulty)
                    } else {
                        val othersValidation = compareForInputValidationNew(key, value, tag, dObj)
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


                if (innerD_Obj_reporting.length() != 0)
                    arrayObj_attributes_reporting.put(reportingObject_reporting)

                if (innerD_Obj_faulty.length() != 0)
                    arrayObj_attributes_faulty.put(reportingObject_faulty)

                if (arrayObj_attributes_reporting.length() > 0)
                    doReportingPublish = true
                if (arrayObj_attributes_faulty.length() > 0)
                    doFaultyPublish = true


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

}

