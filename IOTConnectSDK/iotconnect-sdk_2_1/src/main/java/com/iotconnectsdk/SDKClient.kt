package com.iotconnectsdk

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.URLUtil
import com.google.gson.Gson
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.beans.SendAttributeBean
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.interfaces.DeviceCallback
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.PublishMessageCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.mqtt.IotSDKMQTTService
import com.iotconnectsdk.utils.*
import com.iotconnectsdk.utils.IotSDKUtils.getCurrentTime
import com.iotconnectsdk.utils.SDKClientUtils.compareForInputValidation
import com.iotconnectsdk.utils.SDKClientUtils.createTextFile
import com.iotconnectsdk.webservices.CallWebServices
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse
import com.iotconnectsdk.webservices.responsebean.IdentityServiceResponse
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * class for SDKClient
 */
class SDKClient private constructor(
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

    // private val deviceCallback: DeviceCallback? = null

    // private val twinUpdateCallback: TwinUpdateCallback? = null

    //  private val context: Context? = null

    //  private val cpId: String? = null

    // private val uniqueId: String? = null

    private var commandType: String? = null

    //  private val sdkOptions: String? = null

    //  private val environment: String = "PROD"

    private var isConnected = false

    private var isDispose = false

    private var isSaveToOffline = false

    private var isDebug = false

    private var idEdgeDevice = false

    private val appVersion = "v2.1"

    private var discoveryUrl = ""

    private val DEFAULT_DISCOVERY_URL = "https://discovery.iotconnect.io/"

    private val URL_PATH = "api/$appVersion/dsdk/"

    private val SYNC = "sync"

    private val CPID = "cpid/"

    private val LANG_ANDROID_VER = "/lang/android/ver/"

    private val ENV = "/env/"

    private val UNIQUE_ID = "/uid/"

    private val CP_ID = "cpId"

    private val CURRENT_DATE = "t"

    private val MESSAGE_TYPE = "mt"

    private val SDK_OBJ = "sdk"

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
        private lateinit var sdkClient: SDKClient

        @JvmStatic
        fun getInstance(
            context: Context?,
            cpId: String?,
            uniqueId: String?,
            deviceCallback: DeviceCallback?,
            twinUpdateCallback: TwinUpdateCallback?,
            sdkOptions: String?,
            environment: String?
        ): SDKClient {
            synchronized(this) {
                if (!::sdkClient.isInitialized) {
                    sdkClient = SDKClient(
                        context,
                        cpId,
                        uniqueId,
                        deviceCallback,
                        twinUpdateCallback,
                        sdkOptions,
                        environment
                    )
                }
                sdkClient.connect()
                sdkClient.registerNetworkState()
                return sdkClient
            }

        }

    }


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

//        appVersion = VERSION_NAME;
        if (appVersion != null) {
            val discoveryApi =
                discoveryUrl + URL_PATH + CPID + cpId /*+ LANG_ANDROID_VER + appVersion*/ + ENV + environment
            CallWebServices().getDiscoveryApi(discoveryApi, this)
        }
    }

    private fun callSyncService() {
        if (!validationUtils!!.networkConnectionCheck()) return
        val baseUrl =
            IotSDKPreferences.getInstance(context!!)?.getStringData(IotSDKPreferences.SYNC_API)
        if (baseUrl != null) {
            CallWebServices().sync(
                baseUrl, this
            )
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
                val discoveryApiResponse = Gson().fromJson(
                    response, DiscoveryApiResponse::class.java
                )
                if (discoveryApiResponse != null) {
                    //BaseUrl received to sync the device information.
                    iotSDKLogUtils!!.log(
                        false, isDebug, "INFO_IN07", context!!.getString(R.string.INFO_IN07)
                    )
                    if (!validationUtils!!.validateBaseUrl(discoveryApiResponse)) return
                    val baseUrl: String = discoveryApiResponse.d.bu + UNIQUE_ID + uniqueId
                    IotSDKPreferences.getInstance(context)
                        ?.putStringData(IotSDKPreferences.SYNC_API, baseUrl)
                    callSyncService()
                }
            } else if (methodName.equals(
                    IotSDKUrls.SYNC_SERVICE, ignoreCase = true
                ) && response != null
            ) {
                val syncServiceResponseData =
                    Gson().fromJson(response, IdentityServiceResponse::class.java)

                if (syncServiceResponseData?.d != null) {
                    //save the sync response to shared pref
                    IotSDKPreferences.getInstance(context!!)
                        ?.putStringData(IotSDKPreferences.SYNC_RESPONSE, response)
                    callMQTTService()
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

    private fun getAttributeResponse(): CommonResponseBean? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getAttributes(IotSDKPreferences.ATTRIBUTE_RESPONSE)
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

    override fun onReceiveMsg(message: String?) {

        if (message != null) {
            try {
                val mainObjectLog = JSONObject(message)
                Log.d("mainObject", "::$mainObjectLog")

                val gson = Gson()
                val commonModel = gson.fromJson(message, CommonResponseBean::class.java)

                if (commonModel?.d != null) {
                    if (commonModel.d.ct == 201) {
                        IotSDKPreferences.getInstance(context!!)!!.putStringData(
                            IotSDKPreferences.ATTRIBUTE_RESPONSE, Gson().toJson(commonModel)
                        )
                    }

                    if (commonModel.d.ct == 202) {

                    }

                    if (commonModel.d.ct == 203) {

                    }

                    if (commonModel.d.ct == 204) {

                    }

                    if (commonModel.d.ct == 205) {

                    }
                } else {
                    val mainObject = JSONObject(message)

                    when (mainObject.getInt(CMD_TYPE)) {
                        0 -> {
                            iotSDKLogUtils!!.log(
                                false, isDebug, "INFO_CM01", context!!.getString(R.string.INFO_CM01)
                            )
                            deviceCallback?.onReceiveMsg(message)
                        }
                        1 -> {

                        }
                        2 -> {

                        }
                        3 -> {

                        }
                        116 -> {
                            iotSDKLogUtils?.log(
                                false, isDebug, "INFO_CM16", context!!.getString(R.string.INFO_CM16)
                            )
                            deviceCallback?.onReceiveMsg(message)
                        }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }


    }

    override fun onSendMsg(message: String?) {


        if (message != null) {
//            hubToSdkCallback.onSendMsg(message);
            deviceCallback?.onReceiveMsg(message)
        }
    }


    fun getAttributes(): String? {
        var jsonString = ""
        val response = getAttributeResponse()
        if (response != null) {
            // val data = response.att
            try {

                val gson = Gson()
                jsonString = gson.toJson(response)


                /*  for (device in response.att) {

                      //CREATE DEVICE OBJECT, "device":{"id":"dee02","tg":"gateway"}
                      val deviceObj = JSONObject()
                      deviceObj.put(SDKClient.DEVICE_ID, device.id)
                      deviceObj.put(SDKClient.DEVICE_TAG, device.tg)

                      //ADD TO MAIN OBJECT
                      val mainObj = JSONObject()
                      mainObj.put(SDKClient.DEVICE, deviceObj)
                      mainObj.put(
                          SDKClient.ATTRIBUTES,
                          SDKClientUtils.getAttributesList(data.att, device.tg)
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
                  }*/
            } catch (e: java.lang.Exception) {
                iotSDKLogUtils!!.log(true, isDebug, "ERR_GA01", e.message!!)
                e.printStackTrace()
            }
        }
        return jsonString
    }


    /**
     * send acknowledgment
     *
     * @param obj         JSONObject object for "d"
     * @param messageType Message Type
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
        /* val objMain = JSONObject()
         try {
             objMain.put(SDKClient.UNIQUE_ID, uniqueId)
             objMain.put(SDKClient.CP_ID, cpId)
             objMain.put(SDKClient.CURRENT_DATE, IotSDKUtils.getCurrentDate())
             objMain.put(SDKClient.MESSAGE_TYPE, messageType)
             objMain.putOpt(
                 SDKClient.SDK_OBJ, SDKClientUtils.getSdk(
                     environment, appVersion
                 )
             )
             objMain.putOpt(SDKClient.D_OBJ, obj)
             publishMessage(objMain.toString(), false)
             iotSDKLogUtils!!.log(
                 false,
                 isDebug,
                 "INFO_CM10",
                 context!!.getString(R.string.INFO_CM10) + " " + IotSDKUtils.getCurrentDate()
             )
         } catch (e: JSONException) {
             iotSDKLogUtils!!.log(true, isDebug, "CM01_CM01", e.message!!)
             e.printStackTrace()
         }*/
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
    }

    /* Unregister network receiver.
     *
     * */
    private fun unregisterReceiver() {
        networkStateReceiver!!.removeListener(this)
        context?.unregisterReceiver(networkStateReceiver)
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


    private fun onDeviceConnectionStatus(isConnected: Boolean) {
        val strJson = SDKClientUtils.createCommandFormat(
            IotSDKConstant.DEVICE_CONNECTION_STATUS,
            cpId,
            "",
            uniqueId,
            isConnected.toString(),
            false,
            ""
        )
        deviceCallback?.onReceiveMsg(strJson)
    }

    override fun twinUpdateCallback(data: JSONObject?) {

    }

    override fun networkAvailable() {

    }

    override fun networkUnavailable() {

    }


    override fun onFailedResponse(message: String?) {

    }

    override fun onSendMsg() {
        val response = getSyncResponse()
        if ((response?.d?.has?.d == 1)) {

        }

        if (response?.d?.has?.attr == 1) {
            publishMessage(
                response.d.p.topics.di, JSONObject().put(
                    MESSAGE_TYPE, DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value
                ).toString(), false
            )
        }

        if ((response?.d?.has?.set == 1)) {

        }

        if ((response?.d?.has?.r == 1)) {

        }

        if ((response?.d?.has?.ota == 1)) {

        }

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
        /* if (!validationUtils!!.isValidInputFormat(jsonData!!, uniqueId!!))
             return*/
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
        if (response?.d != null) {
            if (jsonData != null) {
                publishMessage(response.d.p.topics.rpt, jsonData, false)
            }
            val gson = Gson()
            val sendAttributeBean = gson.fromJson(jsonData, SendAttributeBean::class.java)
           compareForInputValidation(getAttributeResponse(),sendAttributeBean)
            // publishDeviceInputData(jsonData, response.d)
        }
    }

}

