package com.iotconnectsdk

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import android.webkit.URLUtil
import com.google.gson.Gson
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.interfaces.DeviceCallback
import com.iotconnectsdk.interfaces.HubToSdkCallback
import com.iotconnectsdk.interfaces.PublishMessageCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.iotconnectsdk.mqtt.IotSDKMQTTService
import com.iotconnectsdk.utils.*
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

    private val isConnected = false

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

    private val CMD_TYPE = "cmdType"

    private val DISCOVERY_URL = "discoveryUrl"

    private val IS_DEBUG = "isDebug"

    private val DATA = "data"

    private val TIME = "time"

    private val ID = "id"

    private val DT = "dt"

    private val TG = "tg"

    private val DIRECTORY_PATH = "logs/offline/"

    private val savedTime: Long = 0

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
            context: Context?, cpId: String?, uniqueId: String?, deviceCallback: DeviceCallback?,
            twinUpdateCallback: TwinUpdateCallback?, sdkOptions: String?, environment: String?
        ): SDKClient {
            synchronized(this) {
                if (!::sdkClient.isInitialized) {
                    sdkClient = SDKClient(
                        context, cpId, uniqueId, deviceCallback,
                        twinUpdateCallback, sdkOptions, environment
                    )
                    sdkClient.connect()
                    sdkClient.registerNetworkState()
                }
                return sdkClient
            }

        }

    }


    private fun registerNetworkState() {
        try {
            networkStateReceiver = NetworkStateReceiver()
            networkStateReceiver?.addListener(this)
            context?.registerReceiver(
                networkStateReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
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
            context!!,
            cpId!!, uniqueId!!
        )
        validationUtils = ValidationUtils.getInstance(
            iotSDKLogUtils!!,
            context, isDebug
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
            discoveryUrl =
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
            if (methodName.equals(IotSDKUrls.DISCOVERY_SERVICE, ignoreCase = true)
                && response != null
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
                    IotSDKUrls.SYNC_SERVICE,
                    ignoreCase = true
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
            context!!, response.d.p, this, this, this, iotSDKLogUtils!!, isDebug,
            uniqueId!!
        )
        mqttService!!.connectMQTT()

    }


    /*get the saved sync response from shared preference.
     * */
    private fun getSyncResponse(): IdentityServiceResponse? {
        return IotSDKPreferences.getInstance(context!!)
            ?.getSyncResponse(IotSDKPreferences.SYNC_RESPONSE)
    }


    /*Call publish method of IotSDKMQTTService class to publish to web.
     * 1.When device is not connected to network and offline storage is true from client, than save all published message to device memory.
     * */
    private fun publishMessage(topics:String,publishMessage: String, isUpdate: Boolean) {
        try {
            if (validationUtils!!.networkConnectionCheck()) {
                if (!isUpdate) {
                    mqttService!!.publishMessage(topics,publishMessage)
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
                                    File(context.filesDir, directoryPath),
                                    "$textFile.txt"
                                )
                                if (fileSizeToCreateInMb != 0 && SDKClientUtils.getFileSizeInKB(file) >= fileSizeToCreateInMb) {
                                    //create new text file.
                                    fileToWrite = createTextFile(
                                        context,
                                        directoryPath,
                                        fileCount,
                                        iotSDKLogUtils,
                                        isDebug
                                    )
                                }
                                break
                            }
                        }
                    }
                }
                try {
                    iotSDKLogUtils!!.writePublishedMessage(
                        directoryPath!!,
                        fileToWrite!!, publishMessage
                    )
                } catch (e: java.lang.Exception) {
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
                    "INFO_OS020",
                    context.getString(R.string.INFO_OS02)
                )
            }
        } catch (e: java.lang.Exception) {
            iotSDKLogUtils!!.log(true, isDebug, "ERR_OS01", e.message!!)
        }
    }

    override fun onReceiveMsg(message: String?) {

        if (message != null) {
            try {
                val mainObject = JSONObject(message)
                Log.d("mainObject", "::$mainObject")
            } catch (e: JSONException) {
                e.printStackTrace();
            }
        }


    }

    override fun onSendMsg(message: String?) {


        if (message != null) {
//            hubToSdkCallback.onSendMsg(message);
            deviceCallback?.onReceiveMsg(message)
        }
    }


    /*Disconnect the device from MQTT connection.
     * stop all timers and change the device connection status.
     * */
    fun dispose() {
        isDispose = true
        //edgeDeviceTimerStop()

        if (mqttService != null) {
            mqttService!!.disconnectClient()
            mqttService!!.clearInstance() //destroy singleton object.
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
        context!!.unregisterReceiver(networkStateReceiver)
    }

    override fun onConnectionStateChange(isConnected: Boolean) {

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
            publishMessage(response.d.p.topics.di,JSONObject().put(MESSAGE_TYPE, DeviceIdentityMessages.GET_DEVICE_TEMPLATE_ATTRIBUTES.value).toString(), false
            )
        }

        if ((response?.d?.has?.set == 1)) {

        }

        if ((response?.d?.has?.r == 1)) {

        }

        if ((response?.d?.has?.ota == 1)) {

        }

    }

}

