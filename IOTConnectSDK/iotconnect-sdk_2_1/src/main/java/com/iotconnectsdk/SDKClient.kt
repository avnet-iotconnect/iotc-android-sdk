package com.iotconnectsdk

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
import com.iotconnectsdk.webservices.CallWebServices
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse
import org.json.JSONException
import org.json.JSONObject
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
    private val environment: String?
) : WsResponseInterface, HubToSdkCallback, TwinUpdateCallback,
    NetworkStateReceiver.NetworkStateReceiverListener {

    private var validationUtils: ValidationUtils? = null

    private var iotSDKLogUtils: IotSDKLogUtils? = null

    private val mqttService: IotSDKMQTTService? = null

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

    private val appVersion = "2.0"

    private var discoveryUrl = ""

    private val DEFAULT_DISCOVERY_URL = "https://discovery.iotconnect.io/"

    private val URL_PATH = "api/sdk/"

    private val SYNC = "sync"

    private val CPID = "cpid/"

    private val LANG_ANDROID_VER = "/lang/android/ver/"

    private val ENV = "/env/"

    private val UNIQUE_ID = "uniqueId"

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
                }
                return sdkClient
            }

        }
    }

    init {
        connect()
        registerNetworkState()
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
                discoveryUrl + URL_PATH + CPID + cpId + LANG_ANDROID_VER + appVersion + ENV + environment
            CallWebServices().getDiscoveryApi(discoveryApi, this)
        }
    }

    private fun callSyncService() {
        if (!validationUtils!!.networkConnectionCheck()) return
        val baseUrl =
            IotSDKPreferences.getInstance(context!!)?.getStringData(IotSDKPreferences.SYNC_API)
        if (baseUrl != null) {
            CallWebServices().sync(
                baseUrl,
                SDKClientUtils.getSyncServiceRequest(cpId, uniqueId, commandType),
                this
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
                    val baseUrl: String = discoveryApiResponse.d.bu + SYNC
                    IotSDKPreferences.getInstance(context)
                        ?.putStringData(IotSDKPreferences.SYNC_API, baseUrl)
                    callSyncService()
                }
            } else if (methodName.equals(
                    IotSDKUrls.SYNC_SERVICE,
                    ignoreCase = true
                ) && response != null
            ) {

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

    override fun onReceiveMsg(message: String?) {
        TODO("Not yet implemented")
    }

    override fun onSendMsg(message: String?) {
        TODO("Not yet implemented")
    }

    override fun onConnectionStateChange(isConnected: Boolean) {
        TODO("Not yet implemented")
    }

    override fun twinUpdateCallback(data: JSONObject?) {
        TODO("Not yet implemented")
    }

    override fun networkAvailable() {
        TODO("Not yet implemented")
    }

    override fun networkUnavailable() {
        TODO("Not yet implemented")
    }


    override fun onFailedResponse(message: String?) {
        TODO("Not yet implemented")
    }
}

