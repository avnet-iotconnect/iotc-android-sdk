package com.softweb.iotconnectsdk.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.iotconnectsdk.SDKClient;
import com.iotconnectsdk.enums.BrokerType;
import com.iotconnectsdk.interfaces.DeviceCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.iotconnectsdk.iotconnectconfigs.EnvironmentType;
import com.softweb.iotconnectsdk.model.Attribute;
import com.softweb.iotconnectsdk.model.AttributesModel;
import com.iotconnectsdk.iotconnectconfigs.Certificate;
import com.softweb.iotconnectsdk.model.D;
import com.softweb.iotconnectsdk.model.Device;
import com.iotconnectsdk.iotconnectconfigs.OfflineStorage;
import com.iotconnectsdk.iotconnectconfigs.SdkOptions;
import com.softweb.iotconnectsdk.R;


/*
 *****************************************************************************
 * @file : FirmwareActivity.java
 * @author : Softweb Solutions An Avnet Company
 * @modify : 28-June-2022
 * @brief : Firmware part for Android SDK 3.1.2
 * *****************************************************************************
 */

/*
 * Hope you have imported SDK v3.1.2 in build.gradle as guided in README.md file or from documentation portal.
 */

public class FirmwareActivity extends AppCompatActivity implements View.OnClickListener, DeviceCallback {

    private static final String TAG = FirmwareActivity.class.getSimpleName();

    private Button btnConnect;
    private Button btnSendData;
    private Button btnGetAllTwins;
    private Button btnClear;

    private Button btnChildDevices;

    private TextView tvConnStatus;
    private TextView tvStatus;

    private EditText etCpid;
    private EditText etUniqueId;
    private EditText etSubscribe;

    private RadioButton rbtnDev;
    private RadioButton rbtnAvnet;
    private RadioButton rbtnProd;
    private RadioButton rbtnQa;

    private LinearLayout linearLayout;

    private HashMap<String, List<TextInputLayout>> inputMap;

    private ArrayList<String> tagsList;

    private List<TextInputLayout> editTextInputList;
    private final String[] permissions = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private boolean isConnected;

    /*
     * ## Prerequisite params to run this sample code
     * - cpId              :: It need to get from the IoTConnect platform.
     * - uniqueId          :: Its device ID which register on IotConnect platform and also its status has Active and Acquired
     * - environment       :: You need to pass respective environment of IoTConnect platform
     **/
    private String cpId = "";
    private String uniqueId = "";
    private EnvironmentType environment;

    static SDKClient sdkClient;

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    String ackId = "";
    String childId = "";
    int cmdType = -1;
    JSONObject mainObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware);

        linearLayout = (LinearLayout) findViewById(R.id.llTemp);

        tvConnStatus = findViewById(R.id.tvConnStatus);
        tvStatus = findViewById(R.id.tvStatus);

        etCpid = findViewById(R.id.etCpid);
        etUniqueId = findViewById(R.id.etUniqueId);
        etSubscribe = findViewById(R.id.etSubscribe);

        btnConnect = findViewById(R.id.btnConnect);
        btnSendData = findViewById(R.id.btnSendData);
        btnGetAllTwins = findViewById(R.id.btnGetAllTwins);
        btnClear = findViewById(R.id.btnClear);
        btnChildDevices = findViewById(R.id.btnChildDevices);

        btnConnect.setOnClickListener(this);
        btnSendData.setOnClickListener(this);
        btnGetAllTwins.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnChildDevices.setOnClickListener(this);

        rbtnDev = findViewById(R.id.rbtnDev);
        rbtnAvnet = findViewById(R.id.rbtnAvnet);
        rbtnProd = findViewById(R.id.rbtnProd);
        rbtnQa = findViewById(R.id.rbtnQa);

        checkPermissions();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnConnect) {
            if (sdkClient != null && isConnected) {
                sdkClient.dispose();
            } else {
                if (environment == null) {
                    Toast.makeText(FirmwareActivity.this, getString(R.string.string_select_environment), Toast.LENGTH_LONG).show();
                    return;
                }

                if (checkValidation()) {
                    setStatusText(R.string.initializing_sdk);
                    btnSendData.setEnabled(false);
                    btnGetAllTwins.setEnabled(false);

                    cpId = etCpid.getText().toString();
                    uniqueId = etUniqueId.getText().toString();

                    /*
                     * Type    : Object Initialization "new SDKClient()"
                     * Usage   : To Initialize SDK and Device connection
                     * Input   : context, cpId, uniqueId, deviceCallback, twinUpdateCallback, sdkOptions, env.
                     * Output  : Callback methods for device command and twin properties
                     */
                    sdkClient = SDKClient.getInstance(FirmwareActivity.this, cpId, uniqueId, FirmwareActivity.this, getSdkOptions(), environment);

                    showDialog(FirmwareActivity.this);
                }
            }
        } else if (v.getId() == R.id.btnSendData) {
            try {
                sendInputData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.btnGetAllTwins) {
            if (sdkClient != null) {
                if (isConnected) {
                    /*
                     * Type    : Public Method "getAllTwins()"
                     * Usage   : Send request to get all the twin properties Desired and Reported
                     * Input   :
                     * Output  :
                     */
                    sdkClient.getTwins();
                } else {
                    Toast.makeText(FirmwareActivity.this, getString(R.string.string_connection_not_found), Toast.LENGTH_LONG).show();
                }
            }
        } else if (v.getId() == R.id.btnChildDevices) {

            Intent intent = new Intent(this, GatewayChildDevicesActivity.class);
            intent.putExtra("tagsList", tagsList);
            startActivity(intent);

        } else if (v.getId() == R.id.btnClear) {
            etSubscribe.setText("");
        }
    }

    /*
     * Type    : Function "checkPermissions()"
     * Usage   : To check user permissions.
     * Input   :
     * Output  :
     */
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    /*
     * Check here that permission is granted or not and do accordingly
     * */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
        }
    }


    /*
     * It helps to define the path of self signed and CA signed certificate as well as define the offline storage params.
     * <p>
     * Type    : Function "checkPermissions()"
     * Usage   : To check user permissions.
     * Input   :
     * Output  : returns json of sdk options as below mentioned
     * <p>
     * sdkOptions is optional. Mandatory for "certificate" X.509 device authentication type
     * "certificate" : It indicated to define the path of the certificate file. Mandatory for X.509/SSL device CA signed or self-signed authentication type only.
     * - SSLKeyPath: your device key
     * - SSLCertPath: your device certificate
     * - SSLCaPath : Root CA certificate
     * "offlineStorage" : Define the configuration related to the offline data storage
     * - disabled : false = offline data storing, true = not storing offline data
     * - availSpaceInMb : Define the file size of offline data which should be in (MB)
     * - fileCount : Number of files need to create for offline data
     * Note: sdkOptions is optional but mandatory for SSL/x509 device authentication type only. Define proper setting or leave it NULL. If you not provide the off-line storage it will set the default settings as per defined above. It may harm your device by storing the large data. Once memory get full may chance to stop the execution.
     */
    private String getSdkOptions() {
//        String sdkOptions = {
//                "certificate": {
//                    "SSLKeyPath"	: "<< Certificate file path >>",
//                    "SSLCertPath"   : "<< Certificate file path >>",
//                    "SSLCaPath"     : "<< Certificate file path >>"
//                },
//                "offlineStorage": {
//                    "disabled": false, //default value = false, false = store data, true = not store data
//                    "availSpaceInMb": 1, //in MB Default value = unlimited
//                    "fileCount": 5 // Default value = 1
//                },
//            }

        SdkOptions sdkOptions = new SdkOptions();

        Certificate certificate = new Certificate();

        //put certificate file in asset folder
        certificate.setsSLKeyPath(getRobotCacheFile(this, "").getAbsolutePath());
        certificate.setsSLCertPath(getRobotCacheFile(this, "").getAbsolutePath());
        certificate.setsSLCaPath(getRobotCacheFile(this, "").getAbsolutePath());


        //For using symmetric key authentication type
        sdkOptions.devicePK = "";

        OfflineStorage offlineStorage = new OfflineStorage();
        offlineStorage.setDisabled(false); //default value false
        offlineStorage.availSpaceInMb = 1; //This will be in MB. mean total available space is 1 MB.
        offlineStorage.fileCount = 5; //5 files can be created.

        sdkOptions.certificate = certificate;
        sdkOptions.offlineStorage = offlineStorage;
        sdkOptions.setSkipValidation(false);
        sdkOptions.brokerType = BrokerType.AZ;    //pass broker type either AZ or AWS from mentioned enums

        String sdkOptionsJsonStr = new Gson().toJson(sdkOptions);

        Log.d(TAG, "getSdkOptions => " + sdkOptionsJsonStr);

        return sdkOptionsJsonStr;
    }


    /*
     * Type    : Private function sendInputData()"
     * Usage   : Collect user input and send to cloud.
     * Input   :
     * Output  :
     */
    private void sendInputData() {

        JSONArray inputArray = new JSONArray();

        for (Map.Entry<String, List<TextInputLayout>> entry : inputMap.entrySet()) {

            try {
                JSONObject valueObj = new JSONObject();

                String keyValue = entry.getKey();
                valueObj.put("uniqueId", keyValue);
                valueObj.put("time", getCurrentTime());

                JSONObject dObj = new JSONObject();
                JSONObject gyroObj = new JSONObject();

                String objectKeyName = "";

                List<TextInputLayout> inputValue = entry.getValue();
                for (TextInputLayout inValue : inputValue) {
                    String label = inValue.getHint().toString();
                    String value = inValue.getEditText().getText().toString();

                    if (label.contains(":")) {
                        //for object type values.
                        objectKeyName = label.split(":")[0];
                        String lab = label.split(":")[1];

                        boolean keyExist = keyExists(dObj, objectKeyName);
                        if (keyExist) {
                            gyroObj.put(lab, value);
                            dObj.putOpt(objectKeyName, gyroObj);
                        } else {
                            gyroObj = new JSONObject();
                            gyroObj.put(lab, value);
                            dObj.putOpt(objectKeyName, gyroObj);
                        }


                    } else {
                        //for simple key values.
                        dObj.put(label, value);
                    }
                }

                valueObj.put("data", dObj);

                inputArray.put(valueObj);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
        // Device to Cloud data publish.
        if (isConnected)
            /*
             * Type    : Public data Method "sendData()"
             * Usage   : To publish the data on cloud D2C
             * Input   : Predefined data object
             * Output  :
             */
            sdkClient.sendData(inputArray.toString());
        else
            Toast.makeText(FirmwareActivity.this, getString(R.string.string_connection_not_found), Toast.LENGTH_LONG).show();
    }


    private boolean keyExists(JSONObject object, String searchedKey) throws JSONException {
        boolean exists = object.has(searchedKey);
        if (!exists) {
            Iterator<?> keys = object.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (object.get(key) instanceof JSONObject) {
                    exists = keyExists((JSONObject) object.get(key), searchedKey);
                }
            }
        }
        return exists;
    }

    /*
     * Type    : private function "getCurrentTime()"
     * Usage   : To get current time with format of "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".
     * Input   :
     * Output  : current time.
     */
    private static String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("gmt"));
        return df.format(new Date());
    }

    /*
     * Type    : Callback Function "onReceiveMsg()"
     * Usage   : Firmware will receive commands from cloud. You can manage your business logic as per received command.
     * Input   :
     * Output  : Receive device command, firmware command and other device initialize error response
     */
    @Override
    public void onReceiveMsg(String message) {

        try {
            getJsonMessage(message);

            if (cmdType == 0 || cmdType == 1 || cmdType == 2) {
            } else if (cmdType == 116) {
                /*command type "116" for Device "Connection Status"
                      true = connected, false = disconnected*/

                Log.d(TAG, "--- Device connection status ---");
                // JSONObject dataObj = mainObject.getJSONObject("data");
                //  Log.d(TAG, "DeviceId ::: [" + mainObject.getString("uniqueId") + "] :: Device status :: " + mainObject.getString("command") + "," + new Date());

                if (mainObject.has("command")) {
                    isConnected = mainObject.getBoolean("command");
                    onConnectionStateChange(isConnected);
                }
            } else if (cmdType == -1) {
                hideDialog(FirmwareActivity.this);
                setStatusText(R.string.device_disconnected);
                Toast.makeText(FirmwareActivity.this, message, Toast.LENGTH_LONG).show();
            } else {
                hideDialog(FirmwareActivity.this);
                // Toast.makeText(FirmwareActivity.this, message, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDeviceCommand(@Nullable String message) {

        getJsonMessage(message);

        /*
         * Type    : Public Method "sendAck()"
         * Usage   : Send device command received acknowledgment to cloud
         *
         * - status Type
         *     st = 6; // Device command Ack status
         *     st = 4; // Failed Ack
         *
         */

        if (isConnected) {
            if (TextUtils.isEmpty(childId)) {
                sdkClient.sendAckCmd(ackId, 6, "");
            } else {
                sdkClient.sendAckCmd(ackId, 6, "", childId);
            }
        } else {
            Toast.makeText(FirmwareActivity.this, getString(R.string.string_connection_not_found), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onOTACommand(@Nullable String message) {

        getJsonMessage(message);

        /*
         * Type    : Public Method "sendAck()"
         * Usage   : Send firmware command received acknowledgement to cloud
         * - status Type
         *     st = 0; // firmware OTA command Ack status
         *     st = 4; // Failed Ack
         *
         */

        if (isConnected) {

            if (TextUtils.isEmpty(childId)) {
                sdkClient.sendOTAAckCmd(ackId, 0, "");
            } else {
                sdkClient.sendOTAAckCmd(ackId, 0, "", childId);
            }
        } else {
            Toast.makeText(FirmwareActivity.this, getString(R.string.string_connection_not_found), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onModuleCommand(@Nullable String message) {
        getJsonMessage(message);
        if (isConnected) {

            if (TextUtils.isEmpty(childId)) {
                sdkClient.sendAckModule(ackId, 0, "");
            } else {
                sdkClient.sendAckModule(ackId, 0, "", childId);
            }
        } else {
            Toast.makeText(FirmwareActivity.this, getString(R.string.string_connection_not_found), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAttrChangeCommand(@Nullable String message) {
        getJsonMessage(message);
    }

    @Override
    public void onTwinChangeCommand(@Nullable String message) {
        getJsonMessage(message);
    }

    @Override
    public void onRuleChangeCommand(@Nullable String message) {
        getJsonMessage(message);
    }

    @Override
    public void onDeviceChangeCommand(@Nullable String message) {
        getJsonMessage(message);
    }


    private void getJsonMessage(String message) {
        btnClear.setEnabled(true);
        Log.d(TAG, "onReceiveMsg => " + message);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                etSubscribe.setText(message);

            }
        });

        boolean jsonValid = isJSONValid(message);

        if (jsonValid) {
            try {
                mainObject = new JSONObject(message);
                //Publish message call back received.
                if (!mainObject.has("ct")) {
                    cmdType = -2;
                    return;
                }

                cmdType = mainObject.getInt("ct");

                if (mainObject.has("ack")) {
                    ackId = mainObject.getString("ack");
                }

                if (mainObject.has("id")) {
                    childId = mainObject.getString("id");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            cmdType = -1;
        }
    }

    /*
     * Type    : Function "onConnectionStateChange()"
     */
    private void onConnectionStateChange(boolean isConnected) {

        linearLayout.removeAllViews();
        if (isConnected) {

            setStatusText(R.string.device_connected);
            tvConnStatus.setSelected(true);
            btnConnect.setText("Disconnect");

            /*
             * Type    : Function "getAttributes()"
             * Usage   : Firmware will receive device attributes.
             * Input   :
             * Output  :
             */

            String data = sdkClient.getAttributes();
            Log.d("attdata", "::" + data);
            if (data != null && !data.equalsIgnoreCase("[]")) {
                btnSendData.setEnabled(true);
                btnGetAllTwins.setEnabled(true);
                createDynamicViews(data);
            } else {
                return;
            }

        } else {
            setStatusText(R.string.device_disconnected);
            tvConnStatus.setSelected(false);
            btnConnect.setText("Connect");
            btnSendData.setEnabled(false);
            btnGetAllTwins.setEnabled(false);
            btnChildDevices.setEnabled(false);
        }
        hideDialog(FirmwareActivity.this);
    }


    /*
     * Type    : private function "createDynamicViews()"
     * Usage   : To create views according to device attributes.
     * Input   : Device attribute object.
     * Output  :
     */
    private void createDynamicViews(String data) {

        inputMap = new HashMap<String, List<TextInputLayout>>();
        editTextInputList = new ArrayList<>();


        try {
            Gson gson = new Gson();
            AttributesModel[] attributesModelList = gson.fromJson(data, AttributesModel[].class);

            for (AttributesModel model : attributesModelList) {
                Device device = model.getDevice();

                if (model.getTags() != null && model.getTags().size() > 0) {
                    tagsList = new ArrayList<>();
                    tagsList.addAll(model.getTags());
                    btnChildDevices.setEnabled(true);
                } else {
                    btnChildDevices.setEnabled(false);
                }
                TextView textViewTitle = new TextView(this);
                textViewTitle.setText("TAG : : " + device.getTg() + " : " + device.getId());
                linearLayout.addView(textViewTitle);

                editTextInputList = new ArrayList<>();
                List<Attribute> attributeList = model.getAttributes();
                for (Attribute attribute : attributeList) {

                    // if for not empty "p":"gyro"
                    if (attribute.getP() != null && !attribute.getP().isEmpty()) {
                        List<D> d = attribute.getD();

                        for (D dObj : d) {
                            TextInputLayout textInputLayout = (TextInputLayout) LayoutInflater.from(FirmwareActivity.this).inflate(R.layout.attribute_layout, null);
                            linearLayout.addView(textInputLayout);
                            editTextInputList.add(textInputLayout);
                            textInputLayout.setHint(attribute.getP() + ":" + dObj.getLn());
                        }

                    } else {
                        TextInputLayout textInputLayout = (TextInputLayout) LayoutInflater.from(FirmwareActivity.this).inflate(R.layout.attribute_layout, null);
                        linearLayout.addView(textInputLayout);
                        editTextInputList.add(textInputLayout);
                        textInputLayout.setHint(attribute.getLn());
                    }
                }
                inputMap.put(device.getId(), editTextInputList);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * ## Function to check prerequisite configuration to run this sample code
     * cpId               : It need to get from the IoTConnect platform "Settings->Key Vault".
     * uniqueId           : Its device ID which register on IotConnect platform and also its status has Active and Acquired
     */
    private boolean checkValidation() {
        if (etCpid.getText().toString().isEmpty()) {
            Toast.makeText(FirmwareActivity.this, getString(R.string.alert_enter_cpid), Toast.LENGTH_SHORT).show();
            return false;
        } else if (etUniqueId.getText().toString().isEmpty()) {
            Toast.makeText(FirmwareActivity.this, getString(R.string.alert_enter_unique_id), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideDialog(FirmwareActivity.this);
        if (sdkClient != null) {

            /*
             * Type    : Public Method "dispose()"
             * Usage   : Disconnect the device from cloud
             * Input   :
             * Output  :
             */
            sdkClient.dispose();
        }
    }

    /*
     * Type    : Public Method "onRadioButtonClicked()"
     * Usage   : Radio button click event handled.
     * Input   :
     * Output  :
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked

        switch (view.getId()) {
            case R.id.rbtnDev:
                if (checked)
                    environment = EnvironmentType.DEV;
                break;
            case R.id.rbtnProd:
                if (checked)
                    environment = EnvironmentType.PROD;
                break;
            case R.id.rbtnAvnet:
                if (checked)
                    environment = EnvironmentType.AVNET;
                break;
            case R.id.rbtnQa:
                if (checked)
                    environment = EnvironmentType.QA;
                break;
        }
    }

    /*
     * Type    : private function "setStatusText()"
     * Usage   : To set the text to textView.
     * Input   : String.xml value for specific text.
     * Output  :
     */
    private void setStatusText(int stringId) {
        tvStatus.setText(getString(stringId));
    }

    /*
     * Type    : Callback Function "twinUpdateCallback()"
     * Usage   : Manage twin properties as per business logic to update the twin reported property
     * Input   :
     * Output  : Receive twin Desired and twin Reported properties
     */
    @Override
    public void twinUpdateCallback(JSONObject data) {
        Log.d(TAG, "twinUpdateCallback => " + data.toString());
        etSubscribe.append("\n\n---------twinUpdateCallback----------\n\n");
        etSubscribe.append(data + "");

        try {
            if (data.has("desired")) {
                JSONObject jsonObject = data.getJSONObject("desired");
                Iterator<String> iter = jsonObject.keys();

                while (iter.hasNext()) {
                    String key = iter.next();
                    if (key.equalsIgnoreCase("$version")) {
                        break;
                    }

                    try {
                        Object value = jsonObject.get(key);

                        if (sdkClient != null && isConnected) {
                            sdkClient.updateTwin(key, "" + value);
                        }

                        break;

                    } catch (JSONException e) {
                        // Something went wrong!
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ProgressDialog progressDialog;

    private void showDialog(Activity activity) {
        try {
            progressDialog = new ProgressDialog(activity);
            if (activity != null && !activity.isFinishing()) {
                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideDialog(Activity activity) {
        if (activity != null && !activity.isFinishing() && progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public boolean isJSONValid(String message) {
        try {
            new JSONObject(message);
        } catch (JSONException ex) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                new JSONArray(message);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private File getRobotCacheFile(Context context, String fileName) {

        if (!TextUtils.isEmpty(fileName)) {
            File cacheFile = new File(context.getCacheDir(), fileName);
            try {
                try (InputStream inputStream = context.getAssets().open(fileName)) {
                    try (FileOutputStream outputStream = new FileOutputStream(cacheFile)) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

            }
            return cacheFile;
        }

        return new File("");
    }
}
