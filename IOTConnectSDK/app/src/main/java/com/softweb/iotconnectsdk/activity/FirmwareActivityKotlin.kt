package com.softweb.iotconnectsdk.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.iotconnectsdk.SDKClient
import com.iotconnectsdk.interfaces.DeviceCallback
import com.iotconnectsdk.interfaces.TwinUpdateCallback
import com.softweb.iotconnectsdk.R
import com.softweb.iotconnectsdk.model.*
import kotlinx.android.synthetic.main.activity_firmware.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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

class FirmwareActivityKotlin : AppCompatActivity(), View.OnClickListener, DeviceCallback,
    TwinUpdateCallback {

    private val TAG = FirmwareActivityKotlin::class.java.simpleName

    private var inputMap: MutableMap<String, List<TextInputLayout>>? = null

    private var editTextInputList: MutableList<TextInputLayout>? = null
    private val permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var isConnected = false

    /*
     * ## Prerequisite params to run this sample code
     * - cpId              :: It need to get from the IoTConnect platform.
     * - uniqueId          :: Its device ID which register on IotConnect platform and also its status has Active and Acquired
     * - environment       :: You need to pass respective environment of IoTConnect platform
     **/
    private var cpId = ""
    private var uniqueId = ""
    private var environment = ""

    private var sdkClient: SDKClient? = null

    private val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firmware)

        btnConnect.setOnClickListener(this)
        btnSendData.setOnClickListener(this)
        btnGetAllTwins.setOnClickListener(this)
        btnClear.setOnClickListener(this)

        checkPermissions()
    }


    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnConnect -> {
                if (sdkClient != null && isConnected) {
                    sdkClient?.dispose()
                } else {
                    if (environment.isEmpty()) {
                        Toast.makeText(
                            this@FirmwareActivityKotlin,
                            getString(R.string.string_select_environment),
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    if (checkValidation()) {
                        setStatusText(R.string.initializing_sdk)
                        btnSendData.isEnabled = false
                        btnGetAllTwins.isEnabled = false
                        cpId = etCpid.text.toString()
                        uniqueId = etUniqueId.text.toString()

                        /*
                                 * Type    : Object Initialization "new SDKClient()"
                                 * Usage   : To Initialize SDK and Device connection
                                 * Input   : context, cpId, uniqueId, deviceCallback, twinUpdateCallback, sdkOptions, env.
                                 * Output  : Callback methods for device command and twin properties
                                 */sdkClient = SDKClient.getInstance(
                            this@FirmwareActivityKotlin,
                            cpId,
                            uniqueId,
                            this@FirmwareActivityKotlin,
                            this@FirmwareActivityKotlin,
                            getSdkOptions(),
                            environment
                        )
                        showDialog(this@FirmwareActivityKotlin)
                    }
                }
            }
            R.id.btnSendData -> {
                // showDialog(this@FirmwareActivityKotlin)
                sendInputData()
            }
            R.id.btnGetAllTwins -> {
                if (sdkClient != null) {
                    if (isConnected) {
                        /*
                             * Type    : Public Method "getAllTwins()"
                             * Usage   : Send request to get all the twin properties Desired and Reported
                             * Input   :
                             * Output  :
                             */
                        //sdkClient!!.getAllTwins()
                    } else {
                        Toast.makeText(
                            this@FirmwareActivityKotlin,
                            getString(R.string.string_connection_not_found),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            R.id.btnClear -> {
                etSubscribe!!.setText("")
            }
        }
    }


    /*
     * Type    : Function "checkPermissions()"
     * Usage   : To check user permissions.
     * Input   :
     * Output  :
     */
    private fun checkPermissions(): Boolean {
        var result: Int
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (p in permissions) {
            result = ContextCompat.checkSelfPermission(this, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                // do something
            }
            return
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
    private fun getSdkOptions(): String? {
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
        val sdkOptions = SdkOptions()
        val certificate = Certificate()
        certificate.setsSLKeyPath("")
        certificate.setsSLCertPath("")
        certificate.setsSLCaPath("")
        val offlineStorage = OfflineStorage()
        offlineStorage.isDisabled = false //default value false
        offlineStorage.availSpaceInMb = 1 //This will be in MB. mean total available space is 1 MB.
        offlineStorage.fileCount = 5 //5 files can be created.
        sdkOptions.certificate = certificate
        sdkOptions.offlineStorage = offlineStorage
        val sdkOptionsJsonStr = Gson().toJson(sdkOptions)
        Log.d(TAG, "getSdkOptions = $sdkOptionsJsonStr")
        return sdkOptionsJsonStr
    }

    /*
     * Type    : Private function sendInputData()"
     * Usage   : Collect user input and send to cloud.
     * Input   :
     * Output  :
     */
    private fun sendInputData() {
        val inputArray = JSONArray()
        for ((keyValue, inputValue) in inputMap!!) {
            try {
                val valueObj = JSONObject()
                valueObj.put("uniqueId", keyValue)
                valueObj.put("time", getCurrentTime())
                val dObj = JSONObject()
                val gyroObj = JSONObject()
                var objectKeyName = ""
                for (inValue in inputValue) {
                    val label = inValue.hint.toString()
                    val value = inValue.editText!!.text.toString()
                    if (label.contains(":")) {
                        //for object type values.
                        objectKeyName = label.split(":").toTypedArray()[0]
                        val lab = label.split(":").toTypedArray()[1]
                        gyroObj.put(lab, value)
                    } else {
                        //for simple key values.
                        dObj.put(label, value)
                    }
                }

                // add object type values.
                if (gyroObj.length() != 0) {
                    dObj.putOpt(objectKeyName, gyroObj)
                }
                valueObj.put("data", dObj)
                inputArray.put(valueObj)
            } catch (e: Exception) {
                Log.d(TAG, e.message!!)
            }
        }
        // Device to Cloud data publish.
        if (isConnected) {
            /*
             * Type    : Public data Method "sendData()"
             * Usage   : To publish the data on cloud D2C
             * Input   : Predefined data object
             * Output  :
             */
            sdkClient!!.sendData(inputArray.toString())
        } else {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.string_connection_not_found),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * Type    : private function "getCurrentTime()"
     * Usage   : To get current time with format of "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".
     * Input   :
     * Output  : current time.
     */
    private fun getCurrentTime(): String? {
        val df = SimpleDateFormat(DATE_TIME_FORMAT)
        df.timeZone = TimeZone.getTimeZone("gmt")
        return df.format(Date())
    }

    /*
     * Type    : Callback Function "onReceiveMsg()"
     * Usage   : Firmware will receive commands from cloud. You can manage your business logic as per received command.
     * Input   :
     * Output  : Receive device command, firmware command and other device initialize error response
     */
    override fun onReceiveMsg(message: String?) {
        hideDialog(this@FirmwareActivityKotlin)
        Log.d(TAG, "onReceiveMsg => $message")
        if (!message?.isEmpty()!!) {
            btnClear!!.isEnabled = true
            etSubscribe!!.append("\n--- Device Command Received ---\n")
            etSubscribe!!.append(message + "")
            var messageType = ""
            try {

                var messageType = ""
                var ackId = ""
                var cmdType = -1
                var mainObject: JSONObject? = null

                val jsonValid: Boolean = isJSONValid(message)

                if (jsonValid) {
                    mainObject = JSONObject(message)
                    //Publish message call back received.
                    if (!mainObject.has("ct")) {
                        return
                    }
                    cmdType = mainObject.getInt("ct")
                    if (mainObject.has("ack")) {
                        ackId = mainObject.getString("ack")
                    }
                }
                when (cmdType) {
                    0 -> {
                        Log.d(TAG, "--- Device Command Received ---")
                        if (ackId != null && ackId.isNotEmpty()) {
                            messageType = "5"


                            /*
                             * Type    : Public Method "sendAck()"
                             * Usage   : Send device command received acknowledgment to cloud
                             *
                             * - status Type
                             *     st = 6; // Device command Ack status
                             *     st = 4; // Failed Ack
                             * - Message Type
                             *     msgType = 5; // for "0x01" device command
                             */


                            val d2CSendAckBean = D2CSendAckBean(
                                getCurrentTime()!!, D2CSendAckBean.Data(ackId, 0, 6, "", null)
                            )
                            val gson = Gson()
                            val jsonString = gson.toJson(d2CSendAckBean)

                            if (isConnected)
                                sdkClient?.sendAck(jsonString, messageType)
                            else
                                Toast.makeText(
                                    this@FirmwareActivityKotlin,
                                    getString(R.string.string_connection_not_found),
                                    Toast.LENGTH_LONG
                                ).show()

                        }
                    }
                    0x02 -> {
                        Log.d(TAG, "--- Firmware OTA Command Received ---")
                        if (ackId != null && ackId.isNotEmpty()) {
                            messageType = "11"
                            val obj = getAckObject(mainObject!!)
                            obj!!.put("st", 7)

                            /*
                             * Type    : Public Method "sendAck()"
                             * Usage   : Send firmware command received acknowledgement to cloud
                             * - status Type
                             *     st = 7; // firmware OTA command Ack status
                             *     st = 4; // Failed Ack
                             * - Message Type
                             *     msgType = 11; // for "0x02" Firmware command
                             *//*if (isConnected) sdkClient!!.sendAck(
                                obj,
                                messageType
                            ) else Toast.makeText(
                                this@FirmwareActivityKotlin,
                                getString(R.string.string_connection_not_found),
                                Toast.LENGTH_LONG
                            ).show()*/
                        }
                    }
                    116 -> {
                        /*command type "0x16" for Device "Connection Status"
                          true = connected, false = disconnected*/Log.d(
                            TAG,
                            "--- Device connection status ---"
                        )
                        Log.d(
                            TAG,
                            "DeviceId ::: [" + mainObject?.getString(
                                "uniqueId"
                            ) + "] :: Device status :: " + mainObject?.getString(
                                "command"
                            ) + "," + Date()
                        )
                        if (mainObject != null) {
                            if (mainObject.has("command")) {
                                isConnected = mainObject.getBoolean("command")
                                onConnectionStateChange(isConnected)
                            }
                        }
                    }
                    else -> {
                        hideDialog(this@FirmwareActivityKotlin)
                        setStatusText(R.string.device_disconnected)
                        Toast.makeText(this@FirmwareActivityKotlin, message, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    /*
     * Type    : Function "onConnectionStateChange()"
     */
    private fun onConnectionStateChange(isConnected: Boolean) {
        hideDialog(this@FirmwareActivityKotlin)
        llTemp!!.removeAllViews()
        if (isConnected) {
            setStatusText(R.string.device_connected)
            tvConnStatus!!.isSelected = true
            btnConnect!!.text = "Disconnect"

            /*
             * Type    : Function "getAttributes()"
             * Usage   : Firmware will receive device attributes.
             * Input   :
             * Output  :
             */

            val data = sdkClient!!.getAttributes()
            Log.d("attdata", "::$data")
            if (data != null) {
                btnSendData!!.isEnabled = true
                btnGetAllTwins!!.isEnabled = true
                createDynamicViews(data)
            }
        } else {
            setStatusText(R.string.device_disconnected)
            tvConnStatus.isSelected = false
            btnConnect.text = "Connect"
            btnSendData.isEnabled = false
            btnGetAllTwins.isEnabled = false
        }
    }


    private fun getAckObject(mainObject: JSONObject): JSONObject? {
        var objD: JSONObject? = null
        var childId = ""
        try {
            val dataObj = mainObject.getJSONObject("data")
            val ackId = dataObj.getString("ackId")

//                var obj = {
//                        "ackId": command.ackId,
//                        "st": st, // 6 (Device '0x01'), 7 (Firmware '0x02' )
//                        "msg": "", //Leave it blank
//                        "childId": "" //Leave it blank
//                 }
            try {
                if (dataObj.has("urls")) {
                    val urlsObj = dataObj.getJSONArray("urls")
                    val arObject = urlsObj[0] as JSONObject
                    if (arObject.has("uniqueId")) childId = arObject.getString("uniqueId")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }


            //create json object.
            objD = JSONObject()
            objD.put("ackId", ackId)
            objD.put("msg", "OTA updated successfully..!!")
            objD.put("childId", childId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return objD
    }


    /*
     * Type    : private function "createDynamicViews()"
     * Usage   : To create views according to device attributes.
     * Input   : Device attribute object.
     * Output  :
     */
    @SuppressLint("SetTextI18n", "InflateParams")
    private fun createDynamicViews(data: String) {
        inputMap = HashMap()
        editTextInputList = ArrayList()
        try {
            val gson = Gson()
            val attributesModelList = gson.fromJson(
                data,
                Array<AttributesModel>::class.java
            )
            for (model in attributesModelList) {
                val device = model.device
                val textViewTitle = TextView(this)
                textViewTitle.text = "$TAG : : " + device.tg + " : " + device.id
                llTemp!!.addView(textViewTitle)
                editTextInputList = ArrayList()
                val attributeList = model.attributes
                for (attribute in attributeList) {

                    // if for not empty "p":"gyro"
                    if (attribute.p != null && attribute.p.isNotEmpty()) {
                        val d = attribute.d
                        for (dObj in d) {
                            val textInputLayout = LayoutInflater.from(this@FirmwareActivityKotlin)
                                .inflate(R.layout.attribute_layout, null) as TextInputLayout
                            llTemp!!.addView(textInputLayout)
                            editTextInputList!!.add(textInputLayout)
                            textInputLayout.hint = attribute.p + ":" + dObj.ln
                        }
                    } else {
                        val textInputLayout = LayoutInflater.from(this@FirmwareActivityKotlin)
                            .inflate(R.layout.attribute_layout, null) as TextInputLayout
                        llTemp!!.addView(textInputLayout)
                        editTextInputList!!.add(textInputLayout)
                        textInputLayout.hint = attribute.ln
                    }
                }
                inputMap!!.put(device.id, editTextInputList!!)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /*
     * ## Function to check prerequisite configuration to run this sample code
     * cpId               : It need to get from the IoTConnect platform "Settings->Key Vault".
     * uniqueId           : Its device ID which register on IotConnect platform and also its status has Active and Acquired
     */
    private fun checkValidation(): Boolean {
        if (etCpid!!.text.toString().isEmpty()) {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.alert_enter_cpid),
                Toast.LENGTH_SHORT
            )
                .show()
            return false
        } else if (etUniqueId!!.text.toString().isEmpty()) {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.alert_enter_unique_id),
                Toast.LENGTH_SHORT
            )
                .show()
            return false
        }
        return true
    }


    override fun onDestroy() {
        super.onDestroy()
        hideDialog(this@FirmwareActivityKotlin)
        if (sdkClient != null) {

            /*
             * Type    : Public Method "dispose()"
             * Usage   : Disconnect the device from cloud
             * Input   :
             * Output  :
             */

             sdkClient?.dispose()
        }
    }

    /*
     * Type    : Public Method "onRadioButtonClicked()"
     * Usage   : Radio button click event handled.
     * Input   :
     * Output  :
     */
    fun onRadioButtonClicked(view: View) {
        // Is the button now checked?
        val checked = (view as RadioButton).isChecked
        when (view.getId()) {
            R.id.rbtnDev -> if (checked) environment = rbtnDev!!.text.toString()
            R.id.rbtnStage -> if (checked) environment = rbtnStage!!.text.toString()
            R.id.rbtnAvnet -> if (checked) environment = rbtnAvnet!!.text.toString()
            R.id.rbtnQa -> if (checked) environment = rbtnQa!!.text.toString()
        }
    }

    /*
     * Type    : private function "setStatusText()"
     * Usage   : To set the text to textView.
     * Input   : String.xml value for specific text.
     * Output  :
     */
    private fun setStatusText(stringId: Int) {
        tvStatus!!.text = getString(stringId)
    }

    /*
     * Type    : Callback Function "twinUpdateCallback()"
     * Usage   : Manage twin properties as per business logic to update the twin reported property
     * Input   :
     * Output  : Receive twin Desired and twin Reported properties
     */
    override fun twinUpdateCallback(data: JSONObject?) {
        Log.d(TAG, "twinUpdateCallback => $data")
        etSubscribe!!.append("\n\n---------twinUpdateCallback----------\n\n")
        etSubscribe!!.append(data.toString() + "")
        try {
            if (data?.has("desired")!!) {
                val jsonObject = data.getJSONObject("desired")
                val iter = jsonObject.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    if (key.equals("\$version", ignoreCase = true)) {
                        break
                    }
                    try {
                        val value = jsonObject[key]
                        if (sdkClient != null && isConnected) {
                            // sdkClient!!.updateTwin(key, "" + value)
                        }
                        break
                    } catch (e: JSONException) {
                        // Something went wrong!
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private var progressDialog: ProgressDialog? = null

    private fun showDialog(activity: Activity?) {
        try {
            progressDialog = ProgressDialog(activity)
            if (activity != null && !activity.isFinishing) {
                progressDialog!!.setMessage("Please wait...")
                progressDialog!!.setCancelable(false)
                progressDialog!!.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideDialog(activity: Activity?) {
        if (activity != null && !activity.isFinishing && progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }

    fun isJSONValid(message: String?): Boolean {
        try {
            JSONObject(message)
        } catch (ex: JSONException) {
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                JSONArray(message)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }
}
