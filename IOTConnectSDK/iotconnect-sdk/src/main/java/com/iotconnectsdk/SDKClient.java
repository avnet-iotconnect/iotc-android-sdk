package com.iotconnectsdk;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.webkit.URLUtil;

import com.google.gson.Gson;
import com.iotconnectsdk.beans.TumblingWindowBean;
import com.iotconnectsdk.interfaces.DeviceCallback;
import com.iotconnectsdk.interfaces.HubToSdkCallback;
import com.iotconnectsdk.interfaces.TwinUpdateCallback;
import com.iotconnectsdk.mqtt.IotSDKMQTTService;
import com.iotconnectsdk.utils.IotSDKLogUtils;
import com.iotconnectsdk.utils.IotSDKPreferences;
import com.iotconnectsdk.utils.IotSDKUrls;
import com.iotconnectsdk.utils.IotSDKUtils;
import com.iotconnectsdk.utils.NetworkStateReceiver;
import com.iotconnectsdk.utils.SDKClientUtils;
import com.iotconnectsdk.utils.ValidationUtils;
import com.iotconnectsdk.webservices.CallWebServices;
import com.iotconnectsdk.webservices.interfaces.WsResponseInterface;
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse;
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.iotconnectsdk.utils.IotSDKConstant.ATTRIBUTE_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.CORE_COMMAND;
import static com.iotconnectsdk.utils.IotSDKConstant.DEVICE_CONNECTION_STATUS;
import static com.iotconnectsdk.utils.IotSDKConstant.DEVICE_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.FIRMWARE_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.PASSWORD_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.RULE_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.SETTING_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.STOP_SDK_CONNECTION;
import static com.iotconnectsdk.utils.IotSDKPreferences.SYNC_API;
import static com.iotconnectsdk.utils.IotSDKPreferences.SYNC_RESPONSE;
import static com.iotconnectsdk.utils.IotSDKPreferences.TEXT_FILE_NAME;

/**
 * class for SDKClient
 */

public class SDKClient implements WsResponseInterface, HubToSdkCallback, TwinUpdateCallback, NetworkStateReceiver.NetworkStateReceiverListener {

    private static SDKClient sdkClient;
    private ValidationUtils validationUtils;
    private IotSDKLogUtils iotSDKLogUtils;
    private IotSDKMQTTService mqttService;

    private DeviceCallback deviceCallback;
    private TwinUpdateCallback twinUpdateCallback;

    private final Context context;
    private String cpId, uniqueId, commandType, sdkOptions, environment = "PROD";
    private boolean isConnected, isDispose, isSaveToOffline;
    private boolean isDebug = false;
    private boolean idEdgeDevice;
    private String appVersion = "2.0";
    private String discoveryUrl = "";

    private static final String DEFAULT_DISCOVERY_URL = "https://discovery.iotconnect.io/";
    private static final String URL_PATH = "api/sdk/";
    private static final String SYNC = "sync";
    private static final String CPID = "cpid/";
    private static final String LANG_ANDROID_VER = "/lang/android/ver/";
    private static final String ENV = "/env/";
    private static final String UNIQUE_ID = "uniqueId";
    private static final String CP_ID = "cpId";
    private static final String CURRENT_DATE = "t";
    private static final String MESSAGE_TYPE = "mt";
    private static final String SDK_OBJ = "sdk";
    private static final String D_OBJ = "d";
    private static final String DEVICE_ID = "id";
    private static final String DEVICE_TAG = "tg";
    private static final String DEVICE = "device";
    private static final String ATTRIBUTES = "attributes";
    private static final String CMD_TYPE = "cmdType";
    private static final String DISCOVERY_URL = "discoveryUrl";
    private static final String IS_DEBUG = "isDebug";
    private static final String DATA = "data";
    private static final String TIME = "time";
    private static final String ID = "id";
    private static final String DT = "dt";
    private static final String TG = "tg";
    private static final String DIRECTORY_PATH = "logs/offline/";

    private long savedTime = 0;

    private static final int EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE = 3;

    //for Edge Device
    private List<Timer> edgeDeviceTimersList;
    private static Map<String, TumblingWindowBean> edgeDeviceAttributeMap;
    private static Map<String, List<TumblingWindowBean>> edgeDeviceAttributeGyroMap;

    private JSONObject publishObjForRuleMatchEdgeDevice = null;

    private NetworkStateReceiver networkStateReceiver;

    private int fileSizeToCreateInMb;
    private String directoryPath;

    private int fileCount;

    /*return singleton object for this class.
     * */
    public static SDKClient getInstance(Context context, String cpId, String uniqueId, DeviceCallback deviceCallback, TwinUpdateCallback twinUpdateCallback, String sdkOptions, String environment) {

        if (sdkClient == null)
            return new SDKClient(context, cpId, uniqueId, deviceCallback, twinUpdateCallback, sdkOptions, environment);

        return sdkClient;
    }

    /*Method to connect the device
     * @param cpId device cp id
     * @param uniqueId device unique id
     * @param sdkOptions json string containing discovery url, certificate, offlineStorage options. ({"certificate":{"SSLCaPath":"","SSLCertPath":"","SSLKeyPath":""},"discoveryUrl":"https://discovery.iotconnect.io/api/sdk/","isDebug":true,"offlineStorage":{"availSpaceInMb":1,"disabled":false,"fileCount":5}})
     * */
    private SDKClient(Context context, String cpId, String uniqueId, DeviceCallback deviceCallback, TwinUpdateCallback twinUpdateCallback, String sdkOptions, String environment) {

        this.context = context;
        this.deviceCallback = deviceCallback;
        this.twinUpdateCallback = twinUpdateCallback;
        this.environment = environment;
        this.cpId = cpId;
        this.uniqueId = uniqueId;
        this.sdkOptions = sdkOptions;

        connect();
        registerNetworkState();
    }

    private void registerNetworkState() {
        try {
            networkStateReceiver = new NetworkStateReceiver();
            networkStateReceiver.addListener(this);
            context.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect() {

        this.directoryPath = "";
        this.reCheckingCountTime = 0;
        this.commandType = null;
        this.isDispose = false;
        this.idEdgeDevice = false;
        this.isSaveToOffline = false;
        this.isDebug = false;
        fileCount = 0;

        //get is debug option.
        JSONObject sdkObj = null;
        try {

            if (this.sdkOptions != null) {

                sdkObj = new JSONObject(this.sdkOptions);
                if (sdkObj.has(IS_DEBUG)) {
                    this.isDebug = sdkObj.getBoolean(IS_DEBUG);
                }

                if (sdkObj.has("offlineStorage")) {
                    JSONObject offlineStorage = sdkObj.getJSONObject("offlineStorage");
                    if (offlineStorage.has("disabled")) {
                        isSaveToOffline = offlineStorage.getBoolean("disabled");

                        if (!isSaveToOffline) { // false = offline data storing, true = not storing offline data
                            this.isSaveToOffline = isSaveToOffline;

                            //Add below configuration in respective sdk configuration. We want this setting to be done form firmware. default fileCount 1 and availeSpaceInMb is unlimited.
                            if (offlineStorage.has("fileCount") && offlineStorage.getInt("fileCount") > 0) {
                                fileCount = offlineStorage.getInt("fileCount");
                            } else {
                                fileCount = 1;
                            }

                            if (offlineStorage.has("availSpaceInMb") && offlineStorage.getInt("availSpaceInMb") > 0) {
                                int availSpaceInMb = offlineStorage.getInt("availSpaceInMb");
                                this.fileSizeToCreateInMb = ((availSpaceInMb * 1000) / fileCount);
                            } else {
                                this.fileSizeToCreateInMb = 0;
                            }

                            this.directoryPath = DIRECTORY_PATH + this.cpId + "_" + this.uniqueId;

                        }
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            iotSDKLogUtils.log(true, this.isDebug, "IN01", e.getMessage());
            return;
        }

        iotSDKLogUtils = IotSDKLogUtils.getInstance(context, this.cpId, this.uniqueId);
        validationUtils = ValidationUtils.getInstance(iotSDKLogUtils, context, this.isDebug);

        if (this.sdkOptions != null) {

            //get discovery url by checking validations.
            if (!validationUtils.checkDiscoveryURL(DISCOVERY_URL, sdkObj)) {
                if (sdkObj.has(DISCOVERY_URL)) {
                    try {
                        String discovery_Url = sdkObj.getString(DISCOVERY_URL);
                        if (!URLUtil.isValidUrl(discovery_Url)) {
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                discoveryUrl = DEFAULT_DISCOVERY_URL; //set default discovery url when it is empty from client end.
            } else {
                try {
                    discoveryUrl = sdkObj.getString(DISCOVERY_URL);
                } catch (JSONException e) {
                    e.printStackTrace();
                    iotSDKLogUtils.log(true, this.isDebug, "ERR_IN01", e.getMessage());
                    return;
                }
            }
        } else {
            discoveryUrl = DEFAULT_DISCOVERY_URL; //set default discovery url when sdkOption is null.
        }

        if (!validationUtils.isEmptyValidation(this.cpId, "ERR_IN04", context.getString(R.string.ERR_IN04)))
            return;

        if (!validationUtils.isEmptyValidation(this.uniqueId, "ERR_IN05", context.getString(R.string.ERR_IN05)))
            return;

        callDiscoveryService();
    }

    /*API call for discovery.
     *@param discoveryUrl  discovery URL ("discoveryUrl" : "https://discovery.iotconnect.io")
     * */
    private void callDiscoveryService() {

        if (!validationUtils.networkConnectionCheck())
            return;

//        appVersion = VERSION_NAME;
        if (appVersion != null) {
            String discoveryApi = this.discoveryUrl + URL_PATH + CPID + this.cpId + LANG_ANDROID_VER + appVersion + ENV + environment;
            new CallWebServices().getDiscoveryApi(discoveryApi, this);
        }
    }

    /*get the saved sync response from shared preference.
     * */
    private SyncServiceResponse getSyncResponse() {
        return IotSDKPreferences.getInstance(context).getSyncResponse(SYNC_RESPONSE);
    }

    /**
     * Send device data to server by calling publish method.
     *
     * @param jsonData json data from client as bellow.
     *                 [{"uniqueId":"ddd2","time":"2020-10-05T08:22:50.698Z","data":{"Temp":"25","humidity":"0","abc":"abc","gyro":{"x":"0","y":"blue","z":"5"}}},{"uniqueId":"c1","time":"2020-10-05T08:22:50.704Z","data":{"Temperature":"0","Humidity":"50"}},{"uniqueId":"c2","time":"2020-10-05T08:22:50.709Z","data":{"pressure":"500","vibration":"0","gyro":{"x":"5","y":"10"}}}]
     */
    public void sendData(String jsonData) {
        if (isDispose) {
            if (this.uniqueId != null) {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_SD04", context.getString(R.string.ERR_SD04));
            }
            return;
        }

        if (!validationUtils.isValidInputFormat(jsonData, this.uniqueId))
            return;

        if (!idEdgeDevice) { // simple device.
            publishDeviceInputData(jsonData);
        } else { //Edge device
            processEdgeDeviceInputData(jsonData);
        }
    }

    /*process input data to publish.
     * 1.Publish input data based on interval of "df" value.
     * "df"="60" than data is published in interval of 60 seconds. If data is publish lass than 60 second time than data is ignored.
     * 2.If "df" = 0, input data can be published on button click.
     *
     * @param jsonData       input data from framework.
     * */
    public void publishDeviceInputData(String jsonData) {

        SyncServiceResponse response = getSyncResponse();
        int df = 0;
        if (response != null) {
            df = response.getD().getSc().getDf();
        }

        if (savedTime == 0) {
            savedTime = getCurrentTime();
            savedTime = (savedTime + df);
        } else {
            long currentTime = getCurrentTime();
            if (currentTime <= savedTime) {
                return;
            } else {
                savedTime = (savedTime + df);
            }
        }

        if (response != null && response.getD() != null) {
            publishDeviceInputData(jsonData, response.getD());
        }
    }


    private long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }

    public void getAllTwins() {

        if (isDispose) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_TP04", context.getString(R.string.ERR_TP04));
            return;
        }

        if (mqttService != null) {
            iotSDKLogUtils.log(false, this.isDebug, "INFO_TP02", context.getString(R.string.INFO_TP02));
            mqttService.getAllTwins();
        }
    }

    public void updateTwin(String key, String value) {

        if (isDispose) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_TP01", context.getString(R.string.ERR_TP01));
            return;
        }

        if (!validationUtils.validateKeyValue(key, value)) return;

        try {
            if (mqttService != null)
                publishMessage(new JSONObject().put(key, value).toString(), true);

        } catch (JSONException e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_TP01", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * send acknowledgment
     *
     * @param obj         JSONObject object for "d"
     * @param messageType Message Type
     */
    public void sendAck(JSONObject obj, String messageType) {

        if (isDispose) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_CM04", context.getString(R.string.ERR_CM04));
            return;
        }

        if (!validationUtils.validateAckParameters(obj, messageType)) return;

        JSONObject objMain = new JSONObject();
        try {

            objMain.put(UNIQUE_ID, this.uniqueId);
            objMain.put(CP_ID, this.cpId);
            objMain.put(CURRENT_DATE, IotSDKUtils.getCurrentDate());
            objMain.put(MESSAGE_TYPE, messageType);

            objMain.putOpt(SDK_OBJ, SDKClientUtils.getSdk(this.environment, appVersion));
            objMain.putOpt(D_OBJ, obj);

            publishMessage(objMain.toString(), false);


            iotSDKLogUtils.log(false, this.isDebug, "INFO_CM10", context.getString(R.string.INFO_CM10) + " " + IotSDKUtils.getCurrentDate());

        } catch (JSONException e) {
            iotSDKLogUtils.log(true, this.isDebug, "CM01_CM01", e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        this.isConnected = connected;
    }

    /*Disconnect the device from MQTT connection.
     * stop all timers and change the device connection status.
     * */
    public void dispose() {
        isDispose = true;
       /* if (!isDispose) {
            dispose();
        } else {
            iotSDKLogUtils.log(false, this.isDebug, "INFO_DC01", context.getString(R.string.INFO_DC01));
        }*/

        edgeDeviceTimerStop();
//        onDeviceConnectionStatus(false);

        if (mqttService != null) {
            mqttService.disconnectClient();
            mqttService.clearInstance(); //destroy single ton object.
        }

        timerStop(reCheckingTimer);
        timerStop(timerCheckDeviceState);
        timerStop(timerOfflineSync);

        unregisterReceiver();
    }

    /* Unregister network receiver.
     *
     * */
    private void unregisterReceiver() {
        networkStateReceiver.removeListener(this);
        context.unregisterReceiver(networkStateReceiver);
    }

    /* ON DEVICE CONNECTION STATUS CHANGE command create json with bellow format and provide to framework.
     *
     * {"cmdType":"0x16","data":{"cpid":"","guid":"","uniqueId":"","command":true,"ack":false,"ackId":"","cmdType":"0x16"}}
     *
     * command = (true = connected, false = disconnected)
     * */
    private void onDeviceConnectionStatus(boolean isConnected) {

        String strJson = SDKClientUtils.createCommandFormat(DEVICE_CONNECTION_STATUS, this.cpId, "", this.uniqueId, isConnected + "", false, "");
        deviceCallback.onReceiveMsg(strJson);

    }

    /* On edge device rule match, send below json format to firmware.
     *{"cmdType":"0x01","data":{"cpid":"deviceData.cpId","guid":"deviceData.company","uniqueId":"device uniqueId","command":"json.cmd","ack":true,"ackId":null,"cmdType":"config.commandType.CORE_COMMAND, 0x01"}}
     *
     * */
    private void onEdgeDeviceRuleMatched(SyncServiceResponse.DBeanXX.RuleBean bean) {
        String strJson = SDKClientUtils.createCommandFormat(CORE_COMMAND, this.cpId, bean.getG(), this.uniqueId, bean.getCmd(), true, "");
        deviceCallback.onReceiveMsg(strJson);
    }

    /*Success call back method called on service response.
     * methods : discovery service
     *           sync service
     *
     * @param methodName         called method name
     * @param response           called service response in json format
     * */
    @Override
    public void onSuccessResponse(String methodName, String response) {

        try {
            if (methodName.equalsIgnoreCase(IotSDKUrls.DISCOVERY_SERVICE) && response != null) {

                DiscoveryApiResponse discoveryApiResponse = new Gson().fromJson(response, DiscoveryApiResponse.class);
                if (discoveryApiResponse != null) {
                    //BaseUrl received to sync the device information.
                    iotSDKLogUtils.log(false, this.isDebug, "INFO_IN07", context.getString(R.string.INFO_IN07));

                    if (!validationUtils.validateBaseUrl(discoveryApiResponse))
                        return;

                    String baseUrl = discoveryApiResponse.getBaseUrl() + SYNC;
                    IotSDKPreferences.getInstance(context).putStringData(SYNC_API, baseUrl);

                    callSyncService();

                }
            } else if (methodName.equalsIgnoreCase(IotSDKUrls.SYNC_SERVICE) && response != null) {

                SyncServiceResponse syncServiceResponseData = new Gson().fromJson(response, SyncServiceResponse.class);
                if (syncServiceResponseData != null && syncServiceResponseData.getD() != null) {

                    //save sync response to shared pref
                    if (commandType == null) {

                        //Device information not found. While sync the device when get the response code 'rc' not equal to '0'
                        int rc = syncServiceResponseData.getD().getRc();
                        validationUtils.responseCodeMessage(rc);
                        if (rc == 1 || rc == 3 || rc == 4 || rc == 6) {

                            if (reCheckingCountTime <= 3) {
                                reChecking();
                            } else {
                                timerStop(reCheckingTimer);
                                iotSDKLogUtils.log(true, this.isDebug, "ERR_IN10", context.getString(R.string.ERR_IN10));
                            }
                            return;
                        } else if (rc != 0) {
//                            onConnectionStateChange(false);
                            iotSDKLogUtils.log(true, this.isDebug, "ERR_IN10", context.getString(R.string.ERR_IN10));
                            return;
                        } else {
                            timerStop(reCheckingTimer);
                        }

                        //got perfect sync response for the device.
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_IN01", context.getString(R.string.INFO_IN01));

                        //check edge enable
                        if (syncServiceResponseData.getD().getEe() == 1) {
                            idEdgeDevice = true;
                            try {
                                processEdgeDeviceTWTimer(syncServiceResponseData);
                            } catch (Exception e) {
                                iotSDKLogUtils.log(true, this.isDebug, "ERR_EE01", e.getMessage());
                            }
                        }

                        //save the sync response to sahred pref.
                        IotSDKPreferences.getInstance(context).putStringData(SYNC_RESPONSE, response);
                    }
                    iotSDKLogUtils.log(false, this.isDebug, "INFO_IN01", context.getString(R.string.INFO_IN01));
                    if (commandType == null && syncServiceResponseData.getD().getAtt() != null) {
//                        deviceCallback.getAttributes(getAttributes(syncServiceResponseData.getD()));
                    }

                    if (commandType == null && syncServiceResponseData.getD().getP() != null && syncServiceResponseData.getD().getP().getH() != null) {
                        callMQTTService();
                    } else if (commandType != null && commandType.equalsIgnoreCase(ATTRIBUTE_INFO_UPDATE)) {

                        if (idEdgeDevice) edgeDeviceTimerStop();

                        updateAttribute(syncServiceResponseData);


                    } else if (commandType != null && commandType.equalsIgnoreCase(DEVICE_INFO_UPDATE)) {
                        updateDeviceInfo(syncServiceResponseData);
                    } else if (commandType != null && commandType.equalsIgnoreCase(PASSWORD_INFO_UPDATE)) {

                        dispose();
                        updatePasswordInfo(syncServiceResponseData);

                    } else if (commandType != null && commandType.equalsIgnoreCase(SETTING_INFO_UPDATE)) {

                        updateSettings(syncServiceResponseData);

                    } else if (commandType != null && commandType.equalsIgnoreCase(RULE_INFO_UPDATE)) {
                        updateRule(syncServiceResponseData);
                    }

                } else {
//                    onConnectionStateChange(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    @Override
    public void onFailedResponse(String methodName, int ERRCode, String message) {

        if (methodName.equalsIgnoreCase(IotSDKUrls.DISCOVERY_SERVICE)) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_IN09", context.getString(R.string.ERR_IN09));
        } else if (methodName.equalsIgnoreCase(IotSDKUrls.SYNC_SERVICE)) {

        }
    }

    /*process data for edge device timer start.
     *
     * @param response      Sync service response.
     * */
    private void processEdgeDeviceTWTimer(SyncServiceResponse response) {
        List<SyncServiceResponse.DBeanXX.AttBean> attributeList = response.getD().getAtt();

        edgeDeviceAttributeMap = new ConcurrentHashMap();
        edgeDeviceAttributeGyroMap = new ConcurrentHashMap<>();
        edgeDeviceTimersList = new ArrayList<>();

        for (SyncServiceResponse.DBeanXX.AttBean bean : attributeList) {

            if (bean.getP() != null && !bean.getP().isEmpty()) {
                // if for not empty "p":"gyro"

                List<TumblingWindowBean> gyroAttributeList = new ArrayList<>();

                List<SyncServiceResponse.DBeanXX.AttBean.DBeanX> listD = bean.getD();
                for (SyncServiceResponse.DBeanXX.AttBean.DBeanX beanX : listD) {
                    final String attributeLn = beanX.getLn();

                    //check attribute input type is numeric or not, ignore the attribute if it is not numeric.
                    if (beanX.getDt() != 1) {
                        TumblingWindowBean twb = new TumblingWindowBean();
                        twb.setAttributeName(attributeLn);
                        gyroAttributeList.add(twb);
                    }
                }

                edgeDeviceAttributeGyroMap.put(bean.getP(), gyroAttributeList);

                edgeDeviceTWTimerStart(bean.getTw(), bean.getP(), bean.getTg());

            } else {
                List<SyncServiceResponse.DBeanXX.AttBean.DBeanX> listD = bean.getD();
                for (SyncServiceResponse.DBeanXX.AttBean.DBeanX beanX : listD) {
                    final String ln = beanX.getLn();
                    final String tag = beanX.getTg();

                    //check attribute input type is numeric or not, ignore the attribute if it is not numeric.
                    if (beanX.getDt() != 1)
                        edgeDeviceTWTimerStart(beanX.getTw(), ln, tag);
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
    private void edgeDeviceTWTimerStart(String twTime, final String ln, final String tag) {

        int tw = Integer.parseInt(twTime.replaceAll("[^\\d.]", ""));

        edgeDeviceAttributeMap.put(ln, new TumblingWindowBean());
        Timer timerTumblingWindow = new Timer();
        edgeDeviceTimersList.add(timerTumblingWindow);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {


                String dtg = getSyncResponse().getD().getDtg();
                JSONObject publishObj = SDKClientUtils.publishEdgeDeviceInputData(ln, tag, edgeDeviceAttributeGyroMap, edgeDeviceAttributeMap, uniqueId, cpId, environment, appVersion, dtg);

                //check publish object is not empty of data. check to inner "d":[] object. Example below json string inner "d":[] object is empty.
                //{"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2021-01-11T02:36:19.644Z","mt":2,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2021-01-11T02:36:19.644Z","tg":"","d":[]}]}

                boolean isPublish = true;
                try {
                    JSONArray dArray = publishObj.getJSONArray(D_OBJ);
                    for (int i = 0; i < dArray.length(); i++) {
                        JSONArray innerDObj = dArray.getJSONObject(i).getJSONArray(D_OBJ);
                        if (innerDObj.length() <= 0) {
                            isPublish = false;
                        }
                    }

                    //return on "d":[] object is empty.
                    if (!isPublish)
                        return;

                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                if (publishObj != null) {
                    publishMessage(publishObj.toString(), false);
                }
            }
        };
        timerTumblingWindow.scheduleAtFixedRate(timerTask, tw * 1000, tw * 1000);
    }


    /*Stop timer for Edge device attributes (humidity,temp,gyro etc...).
     * Clear the list and map collection.
     * */
    private void edgeDeviceTimerStop() {
        if (edgeDeviceAttributeMap != null)
            edgeDeviceAttributeMap.clear();

        if (edgeDeviceAttributeGyroMap != null)
            edgeDeviceAttributeGyroMap.clear();

        if (edgeDeviceTimersList != null)
            for (Timer timer : edgeDeviceTimersList) {
                timerStop(timer);
            }
    }

    private void timerStop(Timer timer) {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    /*Update attributes list when there is update on web.
     * Save the updated sync response to shared preference.
     * If edge device is connected than re-start all timers.
     *
     * @param  attributesUpdatedResponse         attribute list response.
     * */
    private void updateAttribute(SyncServiceResponse attributesUpdatedResponse) {
        SyncServiceResponse savedResponse = getSyncResponse();
        savedResponse.getD().setAtt(attributesUpdatedResponse.getD().getAtt());

        IotSDKPreferences.getInstance(context).putStringData(SYNC_RESPONSE, new Gson().toJson(savedResponse));

        //re-start the edge device timers for all attributes on update of attributes.
        if (idEdgeDevice) processEdgeDeviceTWTimer(savedResponse);
    }

    /*Update password info when there is update on web.
     *
     * */
    private void updatePasswordInfo(SyncServiceResponse passwordResponse) {
        SyncServiceResponse savedResponse = getSyncResponse();

        savedResponse.getD().setP(passwordResponse.getD().getP());
        IotSDKPreferences.getInstance(context).putStringData(SYNC_RESPONSE, new Gson().toJson(savedResponse));

        callMQTTService();
    }

    private void updateSettings(SyncServiceResponse settingResponse) {
        SyncServiceResponse savedResponse = getSyncResponse();
        savedResponse.getD().setSet(settingResponse.getD().getSet());
        IotSDKPreferences.getInstance(context).putStringData(SYNC_RESPONSE, new Gson().toJson(savedResponse));
    }

    private void updateDeviceInfo(SyncServiceResponse deviceResponse) {
        SyncServiceResponse savedResponse = getSyncResponse();
        savedResponse.getD().setD(deviceResponse.getD().getD());
        IotSDKPreferences.getInstance(context).putStringData(SYNC_RESPONSE, new Gson().toJson(savedResponse));
    }

    private void updateRule(SyncServiceResponse ruleResponse) {
        SyncServiceResponse savedResponse = getSyncResponse();
        savedResponse.getD().setR(ruleResponse.getD().getR());
        IotSDKPreferences.getInstance(context).putStringData(SYNC_RESPONSE, new Gson().toJson(savedResponse));
    }

    /*re-checking the device connection after interval of 10 seconds for 3 times.
     * in case of device connect button is clicked and than device creating process is done on web.
     * */
    private void reChecking() {
        iotSDKLogUtils.log(false, this.isDebug, "INFO_IN06", context.getString(R.string.INFO_IN06));
        startReCheckingTimer();
    }

    private Timer reCheckingTimer = null;
    private static int reCheckingCountTime = 0;

    /*start timer for re-checking the device connection.
     * after 3 check of 10 second time interval it will be stop.
     * or it can be stop on device found within time interval.
     * */
    private void startReCheckingTimer() {
        reCheckingTimer = new Timer();
        final TimerTask timerTaskObj = new TimerTask() {
            public void run() {

                ((Activity) context).runOnUiThread(new Runnable() {
                    public void run() {
                        callSyncService();
                    }
                });

                reCheckingCountTime++;
                timerStop(reCheckingTimer);
            }
        };
        reCheckingTimer.schedule(timerTaskObj, 10000, 10000);
    }

    /*MQTT service call to connect device.
     * */
    private void callMQTTService() {

        SyncServiceResponse response = getSyncResponse();

        if (response.getD() == null || response.getD().getP() == null) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_IN11", context.getString(R.string.ERR_IN11));
            return;
        }

        mqttService = IotSDKMQTTService.getInstance(context, response.getD().getP(), this, this, iotSDKLogUtils, this.isDebug, this.uniqueId);
        mqttService.connectMQTT();
    }


    @Override
    public void onFailedResponse(String message) {

    }

    private void callSyncService() {

        if (!validationUtils.networkConnectionCheck())
            return;

        String baseUrl = IotSDKPreferences.getInstance(context).getStringData(SYNC_API);

        if (baseUrl != null) {
            new CallWebServices().sync(baseUrl, SDKClientUtils.getSyncServiceRequest(this.cpId, this.uniqueId, commandType), this);
        }
    }

    /*Method creates json string to be given to framework.
     * [{"device":{"id":"ddd2","tg":"gateway"},"attributes":[{"agt":0,"dt":0,"dv":"5 to 20, 25","ln":"Temp","sq":1,"tg":"gateway","tw":""},{"agt":0,"dt":0,"dv":"","ln":"humidity","sq":8,"tg":"gateway","tw":""},{"agt":0,"dt":1,"dv":"","ln":"abc","sq":12,"tg":"gateway","tw":""},{"agt":0,"d":[{"agt":0,"dt":0,"dv":"","ln":"x","sq":1,"tg":"gateway","tw":""},{"agt":0,"dt":1,"dv":"red, gray,   blue","ln":"y","sq":2,"tg":"gateway","tw":""},{"agt":0,"dt":0,"dv":"-5 to 5, 10","ln":"z","sq":3,"tg":"gateway","tw":""}],"dt":2,"p":"gyro","tg":"gateway","tw":""}]},{"device":{"id":"c1","tg":"zg1"},"attributes":[{"agt":0,"dt":0,"dv":"","ln":"Temperature","sq":2,"tg":"zg1","tw":""},{"agt":0,"dt":0,"dv":"30 to 50","ln":"Humidity","sq":3,"tg":"zg1","tw":""}]},{"device":{"id":"c2","tg":"zg2"},"attributes":[{"agt":0,"dt":0,"dv":"200 to 500","ln":"pressure","sq":5,"tg":"zg2","tw":""},{"agt":0,"dt":0,"dv":"","ln":"vibration","sq":6,"tg":"zg2","tw":""},{"agt":0,"d":[{"agt":0,"dt":0,"dv":"-1to5","ln":"x","sq":1,"tg":"zg2","tw":""},{"agt":0,"dt":0,"dv":"5to10","ln":"y","sq":2,"tg":"zg2","tw":""}],"dt":2,"p":"gyro","tg":"zg2","tw":""}]}]
     * */
//    private String getAttributes(SyncServiceResponse.DBeanXX data) {
    public String getAttributes() {

        JSONArray mainArray = new JSONArray();

        SyncServiceResponse response = getSyncResponse();

        if (response != null) {
            SyncServiceResponse.DBeanXX data = response.getD();

            try {
                for (SyncServiceResponse.DBeanXX.DBean device : data.getD()) {

                    //CREATE DEVICE OBJECT, "device":{"id":"dee02","tg":"gateway"}
                    JSONObject deviceObj = new JSONObject();
                    deviceObj.put(DEVICE_ID, device.getId());
                    deviceObj.put(DEVICE_TAG, device.getTg());

                    //ADD TO MAIN OBJECT
                    JSONObject mainObj = new JSONObject();
                    mainObj.put(DEVICE, deviceObj);
                    mainObj.put(ATTRIBUTES, SDKClientUtils.getAttributesList(data.getAtt(), device.getTg()));

                    //ADD MAIN BOJ TO ARRAY.
                    mainArray.put(mainObj);

                    //Attributes data not found
                    if (mainArray.length() == 0) {
                        iotSDKLogUtils.log(true, this.isDebug, "ERR_GA02", context.getString(R.string.ERR_GA02));
                    } else {
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_GA01", context.getString(R.string.INFO_GA01));
                    }

                }
            } catch (Exception e) {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_GA01", e.getMessage());
                e.printStackTrace();
            }
        }
        return (mainArray.toString());
    }


    @Override
    public void onReceiveMsg(String message) {

        if (message != null) {

            try {
                JSONObject mainObject = new JSONObject(message);
                String cmd = mainObject.getString(CMD_TYPE);

                switch (cmd) {
                    case CORE_COMMAND: // 0x01
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM01", context.getString(R.string.INFO_CM01));
                        deviceCallback.onReceiveMsg(message);
                        break;
                    case DEVICE_CONNECTION_STATUS: // 0x16
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM16", context.getString(R.string.INFO_CM16));
                        deviceCallback.onReceiveMsg(message);
                        break;

                    case FIRMWARE_UPDATE: // 0x02
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM02", context.getString(R.string.INFO_CM02));
                        deviceCallback.onReceiveMsg(message);
                        break;

                    case ATTRIBUTE_INFO_UPDATE: // 0x10
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM03", context.getString(R.string.INFO_CM03));
                        commandType = ATTRIBUTE_INFO_UPDATE;
                        callSyncService();
                        break;
                    case SETTING_INFO_UPDATE: // 0x11
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM04", context.getString(R.string.INFO_CM04));
                        commandType = SETTING_INFO_UPDATE;
                        callSyncService();
                        break;
                    case PASSWORD_INFO_UPDATE: // 0x12
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM05", context.getString(R.string.INFO_CM05));
                        commandType = PASSWORD_INFO_UPDATE;
                        callSyncService();
                        break;
                    case DEVICE_INFO_UPDATE: // 0x13
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM06", context.getString(R.string.INFO_CM06));
                        commandType = DEVICE_INFO_UPDATE;
                        break;
                    case RULE_INFO_UPDATE: // 0x15
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM07", context.getString(R.string.INFO_CM07));
                        commandType = RULE_INFO_UPDATE;
                        callSyncService();
                        break;
                    case STOP_SDK_CONNECTION: // 0x99
                        iotSDKLogUtils.log(false, this.isDebug, "INFO_CM09", context.getString(R.string.INFO_CM09));
                        dispose();
                        break;

                    default:
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void twinUpdateCallback(JSONObject data) {
        try {
            data.put(UNIQUE_ID, this.uniqueId);
            twinUpdateCallback.twinUpdateCallback(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSendMsg(String message) {
        if (message != null) {
//            hubToSdkCallback.onSendMsg(message);
            deviceCallback.onReceiveMsg(message);
        }
    }

    @Override
    public void onConnectionStateChange(boolean isConnected) {

        if (isConnected) {
            iotSDKLogUtils.log(false, this.isDebug, "INFO_IN02", context.getString(R.string.INFO_IN02));
        } else {
            iotSDKLogUtils.log(false, this.isDebug, "INFO_IN03", context.getString(R.string.INFO_IN03));
        }

        setConnected(isConnected);
//        deviceCallback.onConnectionStateChange(isConnected);

        onDeviceConnectionStatus(isConnected);
    }

    /*Process the edge device input data from client.
     *
     * @param  jsonData
     * */
    public void processEdgeDeviceInputData(String jsonData) {
        SyncServiceResponse response = getSyncResponse();
        publishObjForRuleMatchEdgeDevice = null;

        if (response != null) {

            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject dataObj = jsonArray.getJSONObject(i).getJSONObject(DATA);
                    Iterator<String> dataJsonKey = dataObj.keys();

                    String tag = SDKClientUtils.getTag(uniqueId, response.getD());

                    while (dataJsonKey.hasNext()) {
                        String key = dataJsonKey.next();
                        String value = dataObj.getString(key);


                        if (!value.replaceAll("\\s", "").isEmpty() && (new JSONTokener(value).nextValue()) instanceof JSONObject) {

                            JSONObject AttObj = new JSONObject();

                            // get value for
                            // "gyro": {"x":"7","y":"8","z":"9"}
                            JSONObject innerObj = dataObj.getJSONObject(key);
                            Iterator<String> innerJsonKey = innerObj.keys();
                            while (innerJsonKey.hasNext()) {

                                String innerKey = innerJsonKey.next();
                                String innerKValue = innerObj.getString(innerKey);

                                //check for input validation dv=data validation dv="data validation". {"ln":"x","dt":0,"dv":"10to20","tg":"","sq":1,"agt":63,"tw":"40s"}
                                int val = SDKClientUtils.compareForInputValidation(innerKey, innerKValue, tag, response.getD());

                                //ignore string value for edge device.
                                if (SDKClientUtils.isDigit(innerKValue) && val != 1) {
                                    SDKClientUtils.updateEdgeDeviceGyroObj(key, innerKey, innerKValue, edgeDeviceAttributeGyroMap);
                                    EvaluateRuleForEdgeDevice(response.getD().getR(), key, innerKey, innerKValue, jsonData, AttObj);
                                }
                            }
                            //publish
                            publishRuleEvaluatedData();

                        } else {

                            //check for input validation dv="data validation". {"ln":"abc","dt":0,"dv":"10","tg":"","sq":8,"agt":63,"tw":"60s"}
                            int val = SDKClientUtils.compareForInputValidation(key, value, tag, response.getD());

                            //ignore string value for edge device.
                            if (SDKClientUtils.isDigit(value) && val != 1) {
                                SDKClientUtils.updateEdgeDeviceObj(key, value, edgeDeviceAttributeMap);
                                EvaluateRuleForEdgeDevice(response.getD().getR(), key, null, value, jsonData, null);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_EE01", e.getMessage());
                e.printStackTrace();
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
    private void EvaluateRuleForEdgeDevice(List<SyncServiceResponse.DBeanXX.RuleBean> ruleBeansList, String parentKey, String innerKey, String inputValue, String inputJsonString, JSONObject attObj) {

        try {

            if (ruleBeansList != null) {

                int value = Integer.parseInt(inputValue.replaceAll("\\s", ""));

                for (SyncServiceResponse.DBeanXX.RuleBean bean : ruleBeansList) {

                    String con = bean.getCon();

                    String attKey = SDKClientUtils.getAttributeName(con);
                    //match parent attribute name (eg. temp == temp OR gyro == gyro)
                    if (attKey != null && parentKey.equals(attKey)) {

//                        if (innerKey != null) {

                        //for gyro type object. "gyro": {"x":"7","y":"8","z":"9"}
                        if (innerKey != null && con.contains("#") && con.contains("AND")) { //ac1#vibration.x > 5 AND ac1#vibration.y > 10
                            String[] param = con.split("AND");
                            for (int i = 0; i < param.length; i++) {
                                String att = param[i];
                                if (att.contains(".")) { //gyro#vibration.x > 5
                                    String KeyValue[] = att.split("\\.");
                                    String parent[] = KeyValue[0].split("#"); //gyro#vibration
                                    String parentAttName = parent[0]; //gyro
                                    String childAttName = parent[1]; //vibration

                                    setPublishJsonForRuleMatchedEdgeDevice(KeyValue[1], innerKey, value, attObj, parentKey, bean, inputJsonString);

                                } else if (con.contains("#")) {
                                    String parent[] = att.split("#"); //gyro#x > 5
                                    String parentAttName = parent[0]; //gyro
                                    setPublishJsonForRuleMatchedEdgeDevice(parent[1], innerKey, value, attObj, parentKey, bean, inputJsonString);
                                }
                            }
                        } else if (innerKey != null && con.contains("#")) { //gyro#x > 5  //  ac1#vibration.x > 5

                            String parent[] = con.split("#"); //gyro#x > 5
                            String parentAttName = parent[0]; //gyro
                            setPublishJsonForRuleMatchedEdgeDevice(parent[1], innerKey, value, attObj, parentKey, bean, inputJsonString);

                        } else if (innerKey != null && con.contains(".") && con.contains("AND")) { //"gyro.x = 10 AND gyro.y > 10",
                            String[] param = con.split("AND");

                            for (int i = 0; i < param.length; i++) {
                                String KeyValue[] = param[i].split("\\.");  //gyro.x = 10
                                setPublishJsonForRuleMatchedEdgeDevice(KeyValue[1], innerKey, value, attObj, parentKey, bean, inputJsonString);
                            }

                        } else if (innerKey != null && con.contains(".")) { //gyro.x = 10
                            String KeyValue[] = con.split("\\."); //gyro.x = 10
                            setPublishJsonForRuleMatchedEdgeDevice(KeyValue[1], innerKey, value, attObj, parentKey, bean, inputJsonString);
                        } else if (innerKey == null) { // simple object like temp = 10. (not gyro type).
                            if (SDKClientUtils.evaluateEdgeDeviceRuleValue(con, value)) {
                                onEdgeDeviceRuleMatched(bean);

                                JSONObject cvAttObj = new JSONObject();
                                cvAttObj.put(parentKey, inputValue);
                                JSONObject mainObj = SDKClientUtils.getEdgeDevicePublishMainObj(IotSDKUtils.getCurrentDate(), getDtg(), cpId, environment, appVersion, EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE);

                                JSONObject publishObj = SDKClientUtils.getPublishStringEdgeDevice(this.uniqueId, IotSDKUtils.getCurrentDate(), bean, inputJsonString, cvAttObj, mainObj);
                                //publish edge device rule matched data. Publish simple attribute data only. (temp > 10)
                                if (publishObj != null) {
                                    publishMessage(publishObj.toString(), false);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*Get dtg attribute from sync saved response.
     * */
    private String getDtg() {
        return getSyncResponse().getD().getDtg();
    }

    /*Set the json data for publish for edge device only for gyro attributes.
     * */
    private void setPublishJsonForRuleMatchedEdgeDevice(String childAttNameValue, String innerKey, int value, JSONObject attObj, String parentKey, SyncServiceResponse.DBeanXX.RuleBean bean, String inputJsonString) {
        //collect publish data for gyro type object.
        try {

//            String childAttNameValue = KeyValue[1]; //x > 5

            String key = SDKClientUtils.getAttName(childAttNameValue);
            if (innerKey.equals(key)) { // compare x with x.
                if (SDKClientUtils.evaluateEdgeDeviceRuleValue(childAttNameValue, value)) {
                    onEdgeDeviceRuleMatched(bean);
                    attObj.put(key, value + "");
                }
            }

            if (attObj.length() != 0) {

                JSONObject cvAttObj = new JSONObject();
                cvAttObj.put(parentKey, attObj);
                JSONObject mainObj = SDKClientUtils.getEdgeDevicePublishMainObj(IotSDKUtils.getCurrentDate(), getDtg(), cpId, environment, appVersion, EDGE_DEVICE_RULE_MATCH_MESSAGE_TYPE);
                publishObjForRuleMatchEdgeDevice = SDKClientUtils.getPublishStringEdgeDevice(this.uniqueId, IotSDKUtils.getCurrentDate(), bean, inputJsonString, cvAttObj, mainObj);
            }

        } catch (Exception e) {

        }
    }


    /* 1.Publish edge device rule matched data with bellow json format.
     * 2.This method publish gyro type attributes data.(//"gyro.x = 10 AND gyro.y > 10")
     * {"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"514076B1-3C21-4849-A777-F423B1821FC7","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    private void publishRuleEvaluatedData() {
        if (publishObjForRuleMatchEdgeDevice != null)
            publishMessage(publishObjForRuleMatchEdgeDevice.toString(), false);
    }


    /* Publish input data for Device.
     * @param inputJsonStr input json from user.
     * */
    private void publishDeviceInputData(String inputJsonStr, SyncServiceResponse.DBeanXX dObj) {
        try {


            JSONArray jsonArray = new JSONArray(inputJsonStr);

            boolean doFaultyPublish = false;
            boolean doReportingPublish = false;
            String time = "";

            JSONObject reportingObject_reporting = SDKClientUtils.getMainObject("0", dObj, appVersion, this.environment); // 0 for reporting.
            JSONObject reportingObject_faulty = SDKClientUtils.getMainObject("1", dObj, appVersion, this.environment); // 1 for faulty.

            JSONArray arrayObj_reporting = new JSONArray();
            JSONArray arrayObj_Faulty = new JSONArray();

            JSONObject outerD_Obj_reporting = null;
            JSONObject outerD_Obj_Faulty = null;


            for (int i = 0; i < jsonArray.length(); i++) {

                time = jsonArray.getJSONObject(i).optString(TIME);
                String uniqueId = jsonArray.getJSONObject(i).getString(UNIQUE_ID);

                JSONObject dataObj = jsonArray.getJSONObject(i).getJSONObject(DATA);
                Iterator<String> dataJsonKey = dataObj.keys();

                String tag = SDKClientUtils.getTag(uniqueId, dObj);

                outerD_Obj_reporting = new JSONObject();
                outerD_Obj_reporting.put(ID, uniqueId);
                outerD_Obj_reporting.put(DT, time);
                outerD_Obj_reporting.put(TG, tag);

                outerD_Obj_Faulty = new JSONObject();
                outerD_Obj_Faulty.put(ID, uniqueId);
                outerD_Obj_Faulty.put(DT, time);
                outerD_Obj_Faulty.put(TG, tag);

                JSONObject innerD_Obj_reporting = new JSONObject();
                JSONObject innerD_Obj_faulty = new JSONObject();

                //getting value for
//                 "d": {"Temp":"66","humidity":"55","abc":"y","gyro":{"x":"7","y":"8","z":"9"}}
                while (dataJsonKey.hasNext()) {
                    String key = dataJsonKey.next();
                    String value = dataObj.getString(key);

                    if (!value.replaceAll("\\s", "").isEmpty() && (new JSONTokener(value).nextValue()) instanceof JSONObject) {

                        JSONObject gyroObj_reporting = new JSONObject();
                        JSONObject gyroObj_faulty = new JSONObject();

                        JSONObject innerObj = dataObj.getJSONObject(key);
                        Iterator<String> innerJsonKey = innerObj.keys();

                        // get value for
//                         "gyro": {"x":"7","y":"8","z":"9"}

                        while (innerJsonKey.hasNext()) {
                            String InnerKey = innerJsonKey.next();
                            String InnerKValue = innerObj.getString(InnerKey);

                            int val = SDKClientUtils.compareForInputValidation(InnerKey, InnerKValue, tag, dObj);
                            if (val == 0) {
                                gyroObj_reporting.put(InnerKey, InnerKValue);
                            } else {
                                gyroObj_faulty.put(InnerKey, InnerKValue);
                            }
                        }

                        //add gyro object to parent d object.
                        if (gyroObj_reporting.length() != 0)
                            innerD_Obj_reporting.put(key, gyroObj_reporting);
                        if (gyroObj_faulty.length() != 0)
                            innerD_Obj_faulty.put(key, gyroObj_faulty);


                    } else {

                        int val = SDKClientUtils.compareForInputValidation(key, value, tag, dObj);
                        if (val == 0) {
                            innerD_Obj_reporting.put(key, value);
                        } else {
                            innerD_Obj_faulty.put(key, value);
                        }
                    }
                }

                JSONArray arrayObj_attributes_reporting = new JSONArray();
                JSONArray arrayObj_attributes_faulty = new JSONArray();

                if (innerD_Obj_reporting.length() != 0)
                    arrayObj_attributes_reporting.put(innerD_Obj_reporting);

                if (innerD_Obj_faulty.length() != 0)
                    arrayObj_attributes_faulty.put(innerD_Obj_faulty);


                if (arrayObj_attributes_reporting.length() > 0)
                    doReportingPublish = true;
                if (arrayObj_attributes_faulty.length() > 0)
                    doFaultyPublish = true;


                //add object of attribute object to parent object.
                outerD_Obj_reporting.put(D_OBJ, arrayObj_attributes_reporting);
                outerD_Obj_Faulty.put(D_OBJ, arrayObj_attributes_faulty);


                arrayObj_reporting.put(outerD_Obj_reporting);
                arrayObj_Faulty.put(outerD_Obj_Faulty);
            }

            reportingObject_reporting.put(CURRENT_DATE, time);
            reportingObject_faulty.put(CURRENT_DATE, time);

            //Reporting json string as below.
//            {"cpId":"uei","dtg":"f76f806a-b0b6-4f34-bb15-11516d1e42ed","mt":"0","sdk":{"e":"qa","l":"M_android","v":"2.0"},"t":"2020-10-05T10:09:27.362Z","d":[{"id":"ddd2","dt":"2020-10-05T10:09:27.350Z","tg":"gateway","d":[{"Temp":"25","humidity":"0","abc":"abc","gyro":{"x":"0","y":"blue"}}]},{"id":"c1","dt":"2020-10-05T10:09:27.357Z","tg":"zg1","d":[{"Temperature":"0","Humidity":"50"}]},{"id":"c2","dt":"2020-10-05T10:09:27.362Z","tg":"zg2","d":[{"pressure":"500","vibration":"0","gyro":{"x":"5"}}]}]}

            //publish reporting data
            if (doReportingPublish)
                publishMessage(reportingObject_reporting.put(D_OBJ, arrayObj_reporting).toString(), false);

            //publish faulty data
            if (doFaultyPublish)
                publishMessage(reportingObject_faulty.put(D_OBJ, arrayObj_Faulty).toString(), false);


        } catch (JSONException e) {
            e.printStackTrace();
            iotSDKLogUtils.log(true, this.isDebug, "CM01_SD01", e.getMessage());
        }
    }


    /*Call publish method of IotSDKMQTTService class to publish to web.
     * 1.When device is not connected to network and offline storage is true from client, than save all published message to device memory.
     * */
    private void publishMessage(String publishMessage, boolean isUpdate) {

        try {
            if (validationUtils.networkConnectionCheck()) {
                if (!isUpdate) {
                    mqttService.publishMessage(publishMessage);
                } else {
                    mqttService.updateTwin(publishMessage);
                }

            } else if (!isSaveToOffline) { // save message to offline.

                String fileToWrite = null;

                IotSDKPreferences sdkPreferences = IotSDKPreferences.getInstance(context);
                List<String> fileNamesList = sdkPreferences.getList(TEXT_FILE_NAME);

                if (fileNamesList.isEmpty()) { //create new file when file list is empty.
                    fileToWrite = createTextFile();
                } else {

                    /*1.check file with "current" prefix.
                     * 2.get the text file size and compare with defined size.
                     * 3.When text file size is more than defined size than create new file and write to that file.
                     * */
                    if (!fileNamesList.isEmpty()) {
                        for (String textFile : fileNamesList) {
                            if (textFile.contains(TEXT_FILE_PREFIX)) {
                                fileToWrite = textFile;
                                File file = new File(new File(context.getFilesDir(), directoryPath), textFile + ".txt");
                                if (this.fileSizeToCreateInMb != 0 && SDKClientUtils.getFileSizeInKB(file) >= this.fileSizeToCreateInMb) {
                                    //create new text file.
                                    fileToWrite = createTextFile();
                                }
                                break;
                            }
                        }
                    }
                }

                try {
                    iotSDKLogUtils.writePublishedMessage(directoryPath, fileToWrite, publishMessage);
                } catch (Exception e) {
                    iotSDKLogUtils.log(true, this.isDebug, "ERR_OS02", context.getString(R.string.ERR_OS02) + e.getMessage());
                }

                iotSDKLogUtils.log(false, this.isDebug, "INFO_OS02", context.getString(R.string.INFO_OS02));
            }
        } catch (Exception e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_OS01", e.getMessage());
        }
    }

    private static final String TEXT_FILE_PREFIX = "current";

    private String createTextFile() {

        //rename file to directory

        File directory = new File(context.getFilesDir(), directoryPath);
        if (directory.exists()) {
            File[] contents = directory.listFiles();
            if (contents != null)
                for (File filePath : contents) {

                    String path = filePath.toString();
                    String textFileName = path.substring(path.lastIndexOf("/") + 1);
                    if (textFileName.contains(TEXT_FILE_PREFIX)) {
                        String reNameFile = textFileName.split("_")[1];

                        File from = new File(directory, textFileName);
                        File to = new File(directory, reNameFile);
                        if (from.exists())
                            from.renameTo(to);
                    }
                }
        }

        String textFileName = TEXT_FILE_PREFIX + "_" + (System.currentTimeMillis() / 1000) + "";
        IotSDKPreferences sdkPreferences = IotSDKPreferences.getInstance(context);

        CopyOnWriteArrayList<String> fileList = new CopyOnWriteArrayList<>(sdkPreferences.getList(TEXT_FILE_NAME));
        //re-name the file to shared preference.
        for (String file : fileList) {
            if (file.contains(TEXT_FILE_PREFIX)) {
                fileList.remove(file);
                fileList.add(file.split("_")[1]);
            }
        }
        fileList.add(textFileName);

        List<String> list = new ArrayList<>(fileList);
        sdkPreferences.getInstance(context).saveList(TEXT_FILE_NAME, list);

        //Delete first text file, when more than user defined count.
        if (list.size() > fileCount) {
            deleteTextFile(list);
        }

        iotSDKLogUtils.log(false, this.isDebug, "INFO_OS03", context.getString(R.string.INFO_OS03));

        return textFileName;
    }


    @Override
    public void networkAvailable() {

        try {

            if (!isSaveToOffline) {

                //check is there any text file name.
                if (IotSDKPreferences.getInstance(context).getInstance(context).getList(TEXT_FILE_NAME).isEmpty()) {
                    iotSDKLogUtils.log(false, this.isDebug, "INFO_OS05", context.getString(R.string.INFO_OS05));
                    return;
                }

                //check device if there any file stored for offline storage.
                File directory = new File(context.getFilesDir(), directoryPath);
                if (directory.exists()) {
                    File[] contents = directory.listFiles();
                    if (contents.length > 0) {
//                        publishOfflineData();
                        checkIsDeviceOnline();
                    }
                }
            }
        } catch (Exception e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_OS01", e.getMessage());
            e.printStackTrace();
        }
    }

    /*Check is device got connected on network available time, than publish offline data.
     * */
    private Timer timerCheckDeviceState = null;

    private void checkIsDeviceOnline() {
        timerCheckDeviceState = new Timer();
        final TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                if (isConnected()) {
                    timerStop(timerCheckDeviceState);
                    publishOfflineData();
                }
            }
        };
        timerCheckDeviceState.schedule(timerTaskObj, 0, 2000);
    }

    @Override
    public void networkUnavailable() {
        timerStop(timerCheckDeviceState);
        if (!isSaveToOffline) {
            timerStop(timerOfflineSync);
        }
    }

    private Timer timerOfflineSync = null;
    private static boolean syncOfflineData = true;
    private static final String OFFLINE_DATA = "od";

    /*Start timer for publish "df" value interval
     * */
    private void publishOfflineData() {
        try {
            syncOfflineData = true;

            final CopyOnWriteArrayList<String> finalOfflineData = new CopyOnWriteArrayList<>();
            finalOfflineData.addAll(readTextFile());
            if (finalOfflineData.isEmpty())
                return;

            //start timer to sync offline data.
            timerOfflineSync = new Timer();
            final TimerTask timerTaskObj = new TimerTask() {
                public void run() {
                    //read next text file, when previous list is done sync.
                    if (finalOfflineData.isEmpty()) {
                        finalOfflineData.addAll(readTextFile());
                    }

                    if (syncOfflineData) {
                        if (!finalOfflineData.isEmpty()) {
                            for (int i = 0; i < finalOfflineData.size(); i++) {

                                String data = finalOfflineData.get(i);

                                try {
                                    JSONObject dataObj = new JSONObject(data);
//                                    dataObj.put(OFFLINE_DATA, 1);

                                    //publish offline data.
                                    mqttService.publishMessage(dataObj.toString());

                                } catch (JSONException e) {
                                    iotSDKLogUtils.log(true, isDebug, "ERR_OS01", e.getMessage());
                                    e.printStackTrace();
                                }

                                finalOfflineData.remove(i);
                            }
                        }
                        syncOfflineData = false;
                    } else {
                        syncOfflineData = true;
                    }
                }
            };
            timerOfflineSync.schedule(timerTaskObj, 0, 10000);

        } catch (Exception e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_OS01", e.getMessage());
            e.printStackTrace();
        }
    }

    public CopyOnWriteArrayList<String> readTextFile() {
        CopyOnWriteArrayList<String> offlineData = new CopyOnWriteArrayList<>();

        try {
            IotSDKPreferences preferences = IotSDKPreferences.getInstance(context);
            List<String> fileNamesList = preferences.getList(TEXT_FILE_NAME);
            if (fileNamesList.isEmpty()) {
                //shared preference is empty than stop sync timer.
                timerStop(timerOfflineSync);
                return offlineData;
            }

            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(new File(context.getFilesDir(), this.directoryPath), fileNamesList.get(0) + ".txt")));
            String read;
//            StringBuilder builder = new StringBuilder("");

            while ((read = bufferedReader.readLine()) != null) {
//                builder.append(read);
                offlineData.add(read);
            }

            bufferedReader.close();

            //delete text file after reading all records.
            if (deleteTextFile(fileNamesList)) {
                iotSDKLogUtils.log(false, this.isDebug, "INFO_OS04", context.getString(R.string.INFO_OS04));
            }

        } catch (Exception e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_OS03", context.getString(R.string.ERR_OS03) + e.getMessage());
            e.printStackTrace();
        }

        return offlineData;
    }

    private boolean deleteTextFile(List<String> fileNamesList) {
        try {
            //Delete from device
            File file = new File(new File(context.getFilesDir(), this.directoryPath), fileNamesList.get(0) + ".txt");
            if (file.exists()) {
                file.delete();
            }

            fileNamesList.remove(0);
            //delete from shared preferences
            IotSDKPreferences.getInstance(context).saveList(TEXT_FILE_NAME, fileNamesList);

        } catch (Exception e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_OS01", e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }
}