package com.softweb.iotconnectsdk.activity

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
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
import com.softweb.iotconnectsdk.R
import com.softweb.iotconnectsdk.model.*
import kotlinx.android.synthetic.main.activity_firmware.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/*
*****************************************************************************
* @file : FirmwareActivityKotlin
* @author : Softweb Solutions An Avnet Company
* @modify : 28-June-2022
* @brief : Firmware part for Android SDK 3.1.2
* *****************************************************************************
*/
/*
 * Hope you have imported SDK v3.1.2 in build.gradle as guided in README.md file or from documentation portal.
 */
class FirmwareActivityKotlin : AppCompatActivity(), View.OnClickListener,
    DeviceCallback {
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

    private var tagsList: ArrayList<String>? = null

    private val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    var ackId = ""
    var childId = ""
    var cmdType = -1
    var mainObject: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firmware)


        btnConnect.setOnClickListener(this)
        btnSendData.setOnClickListener(this)
        btnGetAllTwins.setOnClickListener(this)
        btnClear.setOnClickListener(this)
        btnChildDevices.setOnClickListener(this)

        checkPermissions()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnConnect) {
            if (sdkClient != null && isConnected) {
                sdkClient!!.dispose()
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
                    btnSendData!!.isEnabled = false
                    btnGetAllTwins!!.isEnabled = false
                    cpId = etCpid!!.text.toString()
                    uniqueId = etUniqueId!!.text.toString()

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
                        sdkOptions,
                        environment
                    )
                    showDialog(this@FirmwareActivityKotlin)
                }
            }
        } else if (v.id == R.id.btnSendData) {
            // showDialog(FirmwareActivityKotlin.this);
            try {
                sendInputData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (v.id == R.id.btnGetAllTwins) {
            if (sdkClient != null) {
                if (isConnected) {
                    /*
                     * Type    : Public Method "getAllTwins()"
                     * Usage   : Send request to get all the twin properties Desired and Reported
                     * Input   :
                     * Output  :
                     */
                    sdkClient!!.getTwins()
                } else {
                    Toast.makeText(
                        this@FirmwareActivityKotlin,
                        getString(R.string.string_connection_not_found),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else if (v.id == R.id.btnChildDevices) {
            val intent = Intent(this, GatewayChildDevicesActivity::class.java)
            intent.putExtra("tagsList", tagsList)
            startActivity(intent)
        } else if (v.id == R.id.btnClear) {
            etSubscribe!!.setText("")
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
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }

    /*
     * Check here that permission is granted or not and do accordingly
     * */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                // do something
            }
        }
    }//        String sdkOptions = {
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

    //put certificate file in asset folder


    //For using symmetric key authentication type
    //default value false
    //This will be in MB. mean total available space is 1 MB.
    //5 files can be created.
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
    private val sdkOptions: String
        private get() {
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

            //put certificate file in asset folder
            certificate.setsSLKeyPath(getRobotCacheFile(this, "")?.absolutePath)
            certificate.setsSLCertPath(getRobotCacheFile(this, "")?.absolutePath)
            certificate.setsSLCaPath(getRobotCacheFile(this, "")?.absolutePath)


            //For using symmetric key authentication type
            sdkOptions.devicePK = ""
            val offlineStorage = OfflineStorage()
            offlineStorage.isDisabled = false //default value false
            offlineStorage.availSpaceInMb =
                1 //This will be in MB. mean total available space is 1 MB.
            offlineStorage.fileCount = 5 //5 files can be created.
            sdkOptions.certificate = certificate
            sdkOptions.offlineStorage = offlineStorage
            sdkOptions.isSkipValidation = false
            val sdkOptionsJsonStr = Gson().toJson(sdkOptions)
            Log.d(
                TAG,
                "getSdkOptions => $sdkOptionsJsonStr"
            )
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
                valueObj.put("time", currentTime)
                val dObj = JSONObject()
                var gyroObj = JSONObject()
                var objectKeyName = ""
                for (inValue in inputValue) {
                    val label = inValue.hint.toString()
                    val value = inValue.editText!!.text.toString()
                    if (label.contains(":")) {
                        //for object type values.
                        objectKeyName = label.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        val lab = label.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1]
                        val keyExist = keyExists(dObj, objectKeyName)
                        if (keyExist) {
                            gyroObj.put(lab, value)
                            dObj.putOpt(objectKeyName, gyroObj)
                        } else {
                            gyroObj = JSONObject()
                            gyroObj.put(lab, value)
                            dObj.putOpt(objectKeyName, gyroObj)
                        }
                    } else {
                        //for simple key values.
                        dObj.put(label, value)
                    }
                }
                valueObj.put("data", dObj)
                inputArray.put(valueObj)
            } catch (e: Exception) {
                Log.d(TAG, e.message!!)
            }
        }
        // Device to Cloud data publish.
        if (isConnected) /*
             * Type    : Public data Method "sendData()"
             * Usage   : To publish the data on cloud D2C
             * Input   : Predefined data object
             * Output  :
             */ sdkClient!!.sendData(inputArray.toString()) else Toast.makeText(
            this@FirmwareActivityKotlin,
            getString(R.string.string_connection_not_found),
            Toast.LENGTH_LONG
        ).show()
    }

    @Throws(JSONException::class)
    private fun keyExists(`object`: JSONObject, searchedKey: String): Boolean {
        var exists = `object`.has(searchedKey)
        if (!exists) {
            val keys: Iterator<*> = `object`.keys()
            while (keys.hasNext()) {
                val key = keys.next() as String
                if (`object`[key] is JSONObject) {
                    exists = keyExists(`object`[key] as JSONObject, searchedKey)
                }
            }
        }
        return exists
    }

    /*
     * Type    : Callback Function "onReceiveMsg()"
     * Usage   : Firmware will receive commands from cloud. You can manage your business logic as per received command.
     * Input   :
     * Output  : Receive device command, firmware command and other device initialize error response
     */
    override fun onReceiveMsg(message: String?) {
        try {
            getJsonMessage(message)
            if (cmdType == 0 || cmdType == 1 || cmdType == 2) {
            } else if (cmdType == 116) {
                /*command type "116" for Device "Connection Status"
                      true = connected, false = disconnected*/
                Log.d(FirmwareActivityKotlin.TAG, "--- Device connection status ---")
                // JSONObject dataObj = mainObject.getJSONObject("data");
                //  Log.d(TAG, "DeviceId ::: [" + mainObject.getString("uniqueId") + "] :: Device status :: " + mainObject.getString("command") + "," + new Date());
                if (mainObject!!.has("command")) {
                    isConnected = mainObject!!.getBoolean("command")
                    onConnectionStateChange(isConnected)
                }
            } else if (cmdType == -1) {
                hideDialog(this@FirmwareActivityKotlin)
                setStatusText(R.string.device_disconnected)
                Toast.makeText(this@FirmwareActivityKotlin, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onDeviceCommand(message: String?) {
        getJsonMessage(message)

        /*
         * Type    : Public Method "sendAck()"
         * Usage   : Send device command received acknowledgment to cloud
         *
         * - status Type
         *     st = 6; // Device command Ack status
         *     st = 4; // Failed Ack
         *
         */if (isConnected) {
            if (TextUtils.isEmpty(childId)) {
                FirmwareActivity.sdkClient.sendAckCmd(ackId, 6, "")
            } else {
                FirmwareActivity.sdkClient.sendAckCmd(ackId, 6, "", childId)
            }
        } else {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.string_connection_not_found),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onOTACommand(message: String?) {
        getJsonMessage(message)

        /*
         * Type    : Public Method "sendAck()"
         * Usage   : Send firmware command received acknowledgement to cloud
         * - status Type
         *     st = 0; // firmware OTA command Ack status
         *     st = 4; // Failed Ack
         *
         */if (isConnected) {
            if (TextUtils.isEmpty(childId)) {
                FirmwareActivity.sdkClient.sendOTAAckCmd(ackId, 0, "")
            } else {
                FirmwareActivity.sdkClient.sendOTAAckCmd(ackId, 0, "", childId)
            }
        } else {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.string_connection_not_found),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onModuleCommand(message: String?) {
        getJsonMessage(message)
        if (isConnected) {
            if (TextUtils.isEmpty(childId)) {
                FirmwareActivity.sdkClient.sendAckModule(ackId, 0, "")
            } else {
                FirmwareActivity.sdkClient.sendAckModule(ackId, 0, "", childId)
            }
        } else {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.string_connection_not_found),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onAttrChangeCommand(message: String?) {
        getJsonMessage(message)
    }

    override fun onTwinChangeCommand(message: String?) {
        getJsonMessage(message)
    }

    override fun onRuleChangeCommand(message: String?) {
        getJsonMessage(message)
    }

    override fun onDeviceChangeCommand(message: String?) {
        getJsonMessage(message)
    }


    private fun getJsonMessage(message: String?) {
        btnClear.isEnabled = true
        Log.d(FirmwareActivityKotlin.TAG, "onReceiveMsg => $message")
        runOnUiThread { etSubscribe.setText(message) }
        val jsonValid = isJSONValid(message)
        if (jsonValid) {
            try {
                mainObject = JSONObject(message)
                //Publish message call back received.
                if (!mainObject!!.has("ct")) {
                    cmdType = -2
                    return
                }
                cmdType = mainObject!!.getInt("ct")
                if (mainObject!!.has("ack")) {
                    ackId = mainObject!!.getString("ack")
                }
                if (mainObject!!.has("id")) {
                    childId = mainObject!!.getString("id")
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } else {
            cmdType = -1
        }
    }

    /*
     * Type    : Function "onConnectionStateChange()"
     */
    private fun onConnectionStateChange(isConnected: Boolean) {
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
            if (data != null && !data.equals("[]", ignoreCase = true)) {
                btnSendData!!.isEnabled = true
                btnGetAllTwins!!.isEnabled = true
                createDynamicViews(data)
            } else {
                return
            }
        } else {
            setStatusText(R.string.device_disconnected)
            tvConnStatus!!.isSelected = false
            btnConnect!!.text = "Connect"
            btnSendData!!.isEnabled = false
            btnGetAllTwins!!.isEnabled = false
            btnChildDevices!!.isEnabled = false
        }
        hideDialog(this@FirmwareActivityKotlin)
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
                if (model.tags != null && model.tags.size > 0) {
                    tagsList = ArrayList()
                    tagsList!!.addAll(model.tags)
                    btnChildDevices!!.isEnabled = true
                } else {
                    btnChildDevices!!.isEnabled = false
                }
                val textViewTitle = TextView(this)
                textViewTitle.text = "TAG : : " + device.tg + " : " + device.id
                llTemp!!.addView(textViewTitle)
                editTextInputList = ArrayList()
                val attributeList = model.attributes
                for (attribute in attributeList) {

                    // if for not empty "p":"gyro"
                    if (attribute.p != null && !attribute.p.isEmpty()) {
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
                inputMap!![device.id] = editTextInputList!!
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
            ).show()
            return false
        } else if (etUniqueId!!.text.toString().isEmpty()) {
            Toast.makeText(
                this@FirmwareActivityKotlin,
                getString(R.string.alert_enter_unique_id),
                Toast.LENGTH_SHORT
            ).show()
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
            sdkClient!!.dispose()
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
        Log.d(
            TAG,
            "twinUpdateCallback => $data"
        )
        etSubscribe!!.append("\n\n---------twinUpdateCallback----------\n\n")
        etSubscribe!!.append(data.toString() + "")
        try {
            if (data!!.has("desired")) {
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
                            sdkClient!!.updateTwin(key, "" + value)
                        }
                        break
                    } catch (e: JSONException) {
                        // Something went wrong!
                    }
                }
            }
        } catch (e: Exception) {
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

    private fun getRobotCacheFile(context: Context, fileName: String): File? {
        if (!TextUtils.isEmpty(fileName)) {
            val cacheFile = File(context.cacheDir, fileName)
            try {
                context.assets.open(fileName).use { inputStream ->
                    FileOutputStream(cacheFile).use { outputStream ->
                        val buf = ByteArray(1024)
                        var len: Int
                        while (inputStream.read(buf).also { len = it } > 0) {
                            outputStream.write(buf, 0, len)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return cacheFile
        }
        return File("")
    }

    companion object {
        private val TAG = FirmwareActivity::class.java.simpleName
        var sdkClient: SDKClient? = null
        private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        /*
       * Type    : private function "getCurrentTime()"
       * Usage   : To get current time with format of "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".
       * Input   :
       * Output  : current time.
       */
        private val currentTime: String
            private get() {
                val df = SimpleDateFormat(DATE_TIME_FORMAT)
                df.timeZone = TimeZone.getTimeZone("gmt")
                return df.format(Date())
            }
    }
}