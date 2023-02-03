package com.iotconnectsdk.utils

import android.content.Context
import com.google.gson.Gson
import com.iotconnectsdk.R
import com.iotconnectsdk.beans.CommandFormatJson
import com.iotconnectsdk.beans.Data
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.utils.IotSDKConstant.ATTRIBUTE_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.DEVICE_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.M_ANDROID
import com.iotconnectsdk.utils.IotSDKConstant.PASSWORD_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.RULE_INFO_UPDATE
import com.iotconnectsdk.utils.IotSDKConstant.SETTING_INFO_UPDATE
import com.iotconnectsdk.webservices.requestbean.SyncServiceRequest
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

object SDKClientUtils {
    private const val CP_ID = "cpId"
    private const val MESSAGE_TYPE = "mt"
    private const val LANGUAGE = "l"
    private const val VERSION = "v"
    private const val ENVIRONMENT = "e"
    private const val SDK_OBJ = "sdk"
    private const val DTG = "dtg"
    private const val CURRENT_DATE = "t"
    private const val DEVICE_ID = "id"
    private const val DT = "dt"
    private const val DEVICE_TAG = "tg"
    private const val D_OBJ = "d"

    /*  private static final String ACK_ID = "ackId";
    private static final String ACK = "ack";
    private static final String CMD_TYPE = "cmdType";
    private static final String GU_ID = "guid";
    private static final String UNIQUE_ID = "uniqueId";
    private static final String DATA = "data";
    private static final String COMMAND = "command";*/
    private const val EDGE_DEVICE_MESSAGE_TYPE = 2
    private const val TEXT_FILE_PREFIX = "current"

    fun getSyncServiceRequest(
        cpId: String?,
        uniqueId: String?,
        cmdType: String?
    ): SyncServiceRequest {
        val syncServiceRequest = SyncServiceRequest()
        syncServiceRequest.cpId = cpId
        syncServiceRequest.uniqueId = uniqueId
        val optionBean = SyncServiceRequest.OptionBean()
        optionBean.isAttribute = false
        optionBean.isDevice = false
        optionBean.isProtocol = false
        optionBean.isSetting = false
        optionBean.isSdkConfig = false
        optionBean.isRule = false
        if (cmdType == null) {
            optionBean.isAttribute = true
            optionBean.isDevice = true
            optionBean.isProtocol = true
            optionBean.isSetting = true
            optionBean.isSdkConfig = true
            optionBean.isRule = true
        } else if (cmdType.equals(ATTRIBUTE_INFO_UPDATE, ignoreCase = true)) {
            optionBean.isAttribute = true
        } else if (cmdType.equals(DEVICE_INFO_UPDATE, ignoreCase = true)) {
            optionBean.isDevice = true
        } else if (cmdType.equals(PASSWORD_INFO_UPDATE, ignoreCase = true)) {
            optionBean.isProtocol = true
        } else if (cmdType.equals(SETTING_INFO_UPDATE, ignoreCase = true)) {
            optionBean.isSetting = true
        } else if (cmdType.equals(RULE_INFO_UPDATE, ignoreCase = true)) {
            optionBean.isRule = true
        }
        syncServiceRequest.option = optionBean
        return syncServiceRequest
    }

    fun getAttributesList(
        attributesLists: List<SyncServiceResponse.DBeanXX.AttBean>,
        tag: String
    ): JSONArray {

        //CREATE ATTRIBUTES ARRAY and OBJECT, "attributes":[{"ln":"Temp","dt":"number","dv":"5 to 20, 25","tg":"gateway","tw":"60s"},{"p":"gyro","dt":"object","tg":"gateway","tw":"90s","d":[{"ln":"x","dt":"number","dv":"","tg":"gateway","tw":"90s"},{"ln":"y","dt":"string","dv":"red, gray,   blue","tg":"gateway","tw":"90s"},{"ln":"z","dt":"number","dv":"-5 to 5, 10","tg":"gateway","tw":"90s"}]}]
        val attributesArray = JSONArray()
        for (attribute in attributesLists) {

//           if for not empty "p":"gyro"
            if (attribute.p != null && !attribute.p.isEmpty()) {
                if (tag == attribute.tg) {
                    try {
                        val attributeObj = JSONObject(Gson().toJson(attribute))
                        attributesArray.put(attributeObj)
                    } catch (e: Exception) {
                    }
                }
            } else {
                // "p" : "", is empty.
                val attributeValues = attribute.d
                for (attributeValue in attributeValues) {
                    if (tag == attributeValue.tg) {
                        try {
                            val attributeObj = JSONObject(Gson().toJson(attributeValue))
                            attributesArray.put(attributeObj)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
        return attributesArray
    }

    fun getSdk(environment: String?, appVersion: String?): JSONObject {
        //sdk object
        val objSdk = JSONObject()
        try {
            objSdk.put(ENVIRONMENT, environment)
            objSdk.put(LANGUAGE, M_ANDROID)
            objSdk.put(VERSION, appVersion)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return objSdk
    }

    fun getMainObject(
        reportingORFaulty: String?,
        dObj: SyncServiceResponse.DBeanXX,
        appVersion: String,
        environment: String
    ): JSONObject {
        val obj = JSONObject()
        try {
            obj.put(SDK_OBJ, getSDKObject(appVersion, environment))
            obj.put(CP_ID, dObj.cpId)
            obj.put(DTG, dObj.dtg)
            obj.put(MESSAGE_TYPE, reportingORFaulty) // 0 for reporting 1 for faulty.
        } catch (e: JSONException) {
            e.printStackTrace()
            //            iotSDKLogUtils.log(true, this.isDebug, "SD01", e.getMessage());
        }
        return obj
    }

    private fun getSDKObject(appVersion: String, environment: String): JSONObject {
        val jsonObj = JSONObject()
        try {
            jsonObj.put(ENVIRONMENT, environment)
            jsonObj.put(LANGUAGE, M_ANDROID)
            jsonObj.put(VERSION, appVersion)
        } catch (e: JSONException) {
            e.printStackTrace()
            //            iotSDKLogUtils.log(true, this.isDebug, "SD01", e.getMessage());
        }
        return jsonObj
    }

    /* function to get device tag. by comparing unique id.
     *
     *@param uniqueId  device unique id.
     * */
    fun getTag(uniqueId: String, dObj: SyncServiceResponse.DBeanXX): String {
        var tag = ""
        val values = dObj.d
        for (data in values) {
            if (uniqueId.equals(data.id, ignoreCase = true)) {
                tag = data.tg
                break
            }
        }
        return tag
    }

    @Synchronized
    fun compareForInputValidation(
        key: String,
        value: String,
        tag: String,
        dObj: SyncServiceResponse.DBeanXX
    ): Int {
        var result = 0
        val attributesList = dObj.att
        outerloop@ for (i in attributesList.indices) {
            val dataBeanList = attributesList[i].d
            for (j in dataBeanList.indices) {
                val data = dataBeanList[j]
                val ln = data.ln
                val tg = data.tg
                val dv = data.dv
                val dt = data.dt
                if (key.equals(ln, ignoreCase = true) && tag.equals(tg, ignoreCase = true)) {
                    result =
                        if (dt == 0 && !value.isEmpty() && !isDigit(value)) {
                            1
                        } else {
                            compareWithInput(value, dv)
                        }
                    break@outerloop
                }
            }
        }
        return result
    }

    /*
     * return 0 = reporting, return 1 = faulty.
     *
     *  {
            "agt": 0,
            "dt": 0,
            "dv": "5 to 20, 25",
            "ln": "Temp",
            "sq": 1,
            "tg": "gateway",
            "tw": ""
          },
     * */
    private fun compareWithInput(inputValue: String, validationValue: String): Int {
        var validationValue = validationValue
        validationValue = validationValue.replace("\\s".toRegex(), "")

        // "dv": ""
        if (validationValue.isEmpty()) return 0


        //  "dv": "5 to 20, 25",   compare between and value.
        if (validationValue.contains("to") && validationValue.contains(",")) {

            //convert string to integer type to compare between value.
            var comparWith = 0
            comparWith = try {
                inputValue.toInt()
            } catch (e: Exception) {
                return 1 // return faulty (input value is not an int type, so we can not campare with between.)
            }
            validationValue = validationValue.replace("to", ",")
            val array = validationValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()

            // "dv": "5 to 20, 25",  compare with 25
            if (array.size == 3 && array[2].equals(inputValue, ignoreCase = true)) return 0

            // "dv": "5 to 20, 25",  compare between 5 to 20
            val from = array[0].toInt()
            val to = array[1].toInt()
            return if (array.size > 1 && comparWith >= from && comparWith <= to) {
                0
            } else {
                1
            }
        }


        // "dv": "30 to 50",
        if (validationValue.contains("to")) {

            //convert string to integer type to compare between value.
            var comparWith = 0
            comparWith = try {
                inputValue.toInt()
            } catch (e: Exception) {
                return 1 // return faulty (input value is not an int type, so we can not campare with between.)
            }
            validationValue = validationValue.replace("to", ",")
            val array = validationValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()

            // "dv": "5 to 20",  compare between 5 to 20
            val from = array[0].toInt()
            val to = array[1].toInt()
            return if (array.size > 1 && comparWith >= from && comparWith <= to) {
                0
            } else {
                1
            }
        }

        //"dv": "red, gray,   blue",
        if (validationValue.contains(",")) {
            var result = 1
            val array = validationValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (i in array.indices) {
                if (inputValue.equals(array[i], ignoreCase = true)) {
                    result = 0
                    break
                }
            }
            return result
        }

        //"dv": "red",  OR "dv":"5"
        return if (inputValue.equals(validationValue, ignoreCase = true)) {
            0
        } else {
            1
        }
    }

    fun updateEdgeDeviceGyroObj(
        key: String,
        innerKey: String,
        value: String,
        edgeDeviceAttributeGyroMap: Map<String?, List<TumblingWindowBean?>?>
    ) {
        for ((key1, tlbList) in edgeDeviceAttributeGyroMap) {
            if (key1 == key) {
                val inputValue = value.toInt()
                if (tlbList != null) {
                    for (bean in tlbList) {
                        if (innerKey == bean?.getAttributeName()) {
                            setObjectValue(bean, inputValue)
                        }
                    }
                }
            }
        }
    }

    fun updateEdgeDeviceObj(
        key: String,
        value: String,
        edgeDeviceAttributeMap: Map<String?, TumblingWindowBean?>
    ) {
        for ((key1, tlb) in edgeDeviceAttributeMap) {
            if (key1 == key) {
                val inputValue = value.toInt()
                if (tlb != null) {
                    setObjectValue(tlb, inputValue)
                }
            }
        }
    }

    /*Publish Edge Device data based on attributes Tumbling window time in seconds ("tw": "15s")
     *
     * @Param attributeName     Attribute name (humidity etc...)
     * */
    fun publishEdgeDeviceInputData(
        attributeName: String,
        tag: String,
        edgeDeviceAttributeGyroMap: Map<String?, List<TumblingWindowBean?>?>,
        edgeDeviceAttributeMap: Map<String?, TumblingWindowBean?>,
        uniqueId: String,
        cpId: String?,
        environment: String?,
        appVersion: String?,
        dtg: String?
    ): JSONObject? {
        val currentTime = IotSDKUtils.currentDate
        val mainObj = getEdgeDevicePublishMainObj(
            currentTime,
            dtg,
            cpId,
            environment,
            appVersion,
            EDGE_DEVICE_MESSAGE_TYPE
        )
        val dArray = JSONArray()
        val dArrayObject = getEdgeDevicePublishDObj(currentTime, tag, uniqueId)
        val dInnerArray = JSONArray()
        val gyroObj = JSONObject()
        val dInnerArrayObject = JSONObject()
        //for gyro object
        for ((key, twbList) in edgeDeviceAttributeGyroMap) {
            if (attributeName == key) {
                try {
                    var attributeArray: JSONArray? = null
                    if (twbList != null) {
                        for (twb in twbList) {
                            attributeArray = getEdgeDevicePublishAttributes(twb!!)
                            if (attributeArray.length() > 0) {
                                dInnerArrayObject.put(twb.getAttributeName(), attributeArray)
                            }
                            clearObject(twb)
                        }
                    }
                    if (dInnerArrayObject.length() > 0) {
                        gyroObj.put(attributeName, dInnerArrayObject)
                        dInnerArray.put(gyroObj)
                    }
                    dArrayObject.put(D_OBJ, dInnerArray)
                    dArray.put(dArrayObject)
                    mainObj.put(D_OBJ, dArray)
                    return mainObj
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }

        //for simple object
        for ((key, twb) in edgeDeviceAttributeMap) {
            if (key == attributeName) {
                try {
                    val attributeArray = getEdgeDevicePublishAttributes(twb!!)
                    if (attributeArray.length() > 0) {
                        dInnerArrayObject.put(attributeName, attributeArray)
                        dInnerArray.put(dInnerArrayObject)
                    }
                    dArrayObject.put(D_OBJ, dInnerArray)
                    dArray.put(dArrayObject)
                    mainObj.put(D_OBJ, dArray)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                if (twb != null) {
                    clearObject(twb)
                }
                return mainObj
            }
        }
        return null
    }

    fun getEdgeDevicePublishMainObj(
        currentTime: String?,
        dtg: String?,
        cpId: String?,
        environment: String?,
        appVersion: String?,
        messageType: Int
    ): JSONObject {
        val mainObj = JSONObject()
        try {
            mainObj.put(CP_ID, cpId)
            mainObj.put(DTG, dtg)
            mainObj.put(CURRENT_DATE, currentTime)
            mainObj.put(MESSAGE_TYPE, messageType)
            mainObj.put(SDK_OBJ, getSdk(environment, appVersion))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return mainObj
    }

    private fun getEdgeDevicePublishDObj(
        currentTime: String,
        tag: String,
        uniqueId: String
    ): JSONObject {
        val dArrayObject = JSONObject()
        try {
            dArrayObject.put(DEVICE_ID, uniqueId)
            dArrayObject.put(DT, currentTime)
            dArrayObject.put(DEVICE_TAG, tag)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return dArrayObject
    }

    private fun getEdgeDevicePublishAttributes(twb: TumblingWindowBean): JSONArray {
        val attributeArray = JSONArray()
        try {
            if (twb.getMin() !== 0 || twb.getMax() !== 0 || twb.getSum() !== 0 || twb.avg !== 0 || twb.getCount() !== 0 || twb.getLv() !== 0) {
                attributeArray.put(twb.getMin())
                attributeArray.put(twb.getMax())
                attributeArray.put(twb.getSum())
                attributeArray.put(twb.getAvg())
                attributeArray.put(twb.getCount())
                attributeArray.put(twb.getLv())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return attributeArray
    }

    fun setObjectValue(bean: TumblingWindowBean, inputValue: Int) {
        val oldMin: Int = bean.getMin()
        if (oldMin == 0 || inputValue < oldMin) bean.setMin(inputValue)
        val oldMax: Int = bean.getMax()
        if (inputValue > oldMax) bean.setMax(inputValue)
        val sum: Int = inputValue + bean.getSum()
        bean.setSum(sum)
        var count: Int = bean.getCount()
        count++
        bean.setAvg((sum / count))
        bean.setCount(count)
        bean.setLv(inputValue)
    }

    private fun clearObject(twb: TumblingWindowBean) {
        //clear object on publish success.
        twb.setMin(0)
        twb.setMax(0)
        twb.setSum(0)
        twb.setAvg(0)
        twb.setCount(0)
        twb.setLv(0)
    }

    fun isDigit(value: String): Boolean {
        var value = value
        value = value.replace("\\s".toRegex(), "")
        return value.matches("\\d+(?:\\.\\d+)?".toRegex())
    }

    fun createCommandFormat(
        commandType: String?,
        cpId: String?,
        guid: String?,
        uniqueId: String?,
        command: String?,
        ack: Boolean,
        ackId: String?
    ): String {
        val cfj = CommandFormatJson()
        cfj.setCmdType(commandType)
        val data = Data()
        data.setCpid(cpId)
        data.setGuid(guid)
        data.setUniqueId(uniqueId)
        data.setCommand(command)
        data.setAck(ack)
        data.setAckId(ackId)
        data.setCmdType(commandType)
        cfj.setData(data)
        return Gson().toJson(cfj)
    }

    fun getAttributeName(con: String): String? {

        //"ac1#vibration.x > 5 AND ac1#vibration.y > 10",
        //gyro#vibration.x > 5 AND gyro#vibration.y > 10
        try {
            //gyro#vibration.x > 5 AND gyro#vibration.y > 10
            if (con.contains("#") && con.contains("AND")) {
                val param = con.split("AND".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (i in 0..param.size) {
                    val att = param[i]
                    if (att.contains(".")) {           //gyro#vibration.x > 5
                        val KeyValue =
                            att.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val parent =
                            KeyValue[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray() //gyro#vibration
                        //                        String childAttName = parent[1]; //vibration
//                        String keyAttName = KeyValue[1]; //x > 5
                        return parent[0]
                    } else if (con.contains("#")) {                    //gyro#x > 5
                        val parent =
                            att.split("#".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray() //gyro#x > 5
                        //                        String childAttName = parent[1]; //x > 5
                        return parent[0]
                    }
                }
            } else if (con.contains("#")) {     //ac1#vibration.x > 5
                val KeyValue = con.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val parent = KeyValue[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray() //gyro#vibration
                val parentAttName = parent[0] //gyro
                val childAttName = parent[1] //vibration
                val keyAttName = KeyValue[1] //x > 5
                return getAttName(keyAttName)
            } else if (con.contains(".")) {
                val keyValue = con.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                return keyValue[0]
            } else {    //x > 5
                return getAttName(con)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return null
    }

    /*
     * */
    // temp != 15
    // temp >= 15
    // temp <= 15
    // temp = 15
    // temp > 15
    // temp < 15
    private const val EQUAL_TO = "="
    private const val NOT_EQUAL_TO = "!="
    private const val GREATER_THAN = ">"
    private const val GREATER_THAN_OR_EQUAL_TO = ">="
    private const val LESS_THAN = "<"
    private const val LESS_THAN_OR_EQUAL_TO = "<="
    fun getAttName(con: String): String? {
        try {
            if (con.contains(NOT_EQUAL_TO)) {
                return getRuleAttName(con, NOT_EQUAL_TO)
            } else if (con.contains(GREATER_THAN_OR_EQUAL_TO)) {
                return getRuleAttName(con, GREATER_THAN_OR_EQUAL_TO)
            } else if (con.contains(LESS_THAN_OR_EQUAL_TO)) {
                return getRuleAttName(con, LESS_THAN_OR_EQUAL_TO)
            } else if (con.contains(EQUAL_TO)) {
                return getRuleAttName(con, EQUAL_TO)
            } else if (con.contains(GREATER_THAN)) {
                return getRuleAttName(con, GREATER_THAN)
            } else if (con.contains(LESS_THAN)) {
                return getRuleAttName(con, LESS_THAN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return null
    }

    private fun getRuleAttName(con: String, operator: String): String {
        val att = con.split(operator.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        return att[0].replace("\\s".toRegex(), "")
    }

    fun evaluateEdgeDeviceRuleValue(con: String, inputValue: Int): Boolean {
        try {
            if (con.contains(NOT_EQUAL_TO)) {
                if (inputValue != getRuleValue(con, NOT_EQUAL_TO)) {
                    return true
                }
            } else if (con.contains(GREATER_THAN_OR_EQUAL_TO)) {
                if (inputValue >= getRuleValue(con, GREATER_THAN_OR_EQUAL_TO)) {
                    return true
                }
            } else if (con.contains(LESS_THAN_OR_EQUAL_TO)) {
                if (inputValue <= getRuleValue(con, LESS_THAN_OR_EQUAL_TO)) {
                    return true
                }
            } else if (con.contains(EQUAL_TO)) {
                if (inputValue == getRuleValue(con, EQUAL_TO)) {
                    return true
                }
            } else if (con.contains(GREATER_THAN)) {
                if (inputValue > getRuleValue(con, GREATER_THAN)) {
                    return true
                }
            } else if (con.contains(LESS_THAN)) {
                if (inputValue < getRuleValue(con, LESS_THAN)) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun getRuleValue(con: String, operator: String): Int {
        val att = con.split(operator.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        return att[1].replace("\\s".toRegex(), "").toInt()
    }

    /*create bellow json and publish on edge device rule matched.
     * {"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"514076B1-3C21-4849-A777-F423B1821FC7","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    fun getPublishStringEdgeDevice(
        uniqueId: String?,
        currentTime: String?,
        bean: SyncServiceResponse.DBeanXX.RuleBean,
        inputJsonString: String,
        cvAttObj: JSONObject?,
        mainObj: JSONObject
    ): JSONObject? {
        return try {
            val dArray = JSONArray()
            val dObj = JSONObject()
            dObj.put("id", uniqueId)
            dObj.put("dt", currentTime)
            dObj.put("rg", bean.g)
            dObj.put("ct", bean.con)
            dObj.put("sg", bean.es)
            val innerDArray = JSONArray()
            innerDArray.put(getAttFromInput(inputJsonString))
            dObj.put("d", innerDArray)
            dObj.put("cv", cvAttObj)
            dArray.put(dObj)
            mainObj.put("d", dArray)
            mainObj
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        //        return null;
    }

    private fun getAttFromInput(jsonData: String): JSONObject {
        val dObj = JSONObject()
        try {

            //parse input json
            val jsonArray = JSONArray(jsonData)
            for (i in 0 until jsonArray.length()) {
                val dataObj = jsonArray.getJSONObject(i).getJSONObject("data")
                val dataJsonKey = dataObj.keys()
                while (dataJsonKey.hasNext()) {
                    val key = dataJsonKey.next()
                    val value = dataObj.getString(key)
                    if (!value.replace("\\s".toRegex(), "")
                            .isEmpty() && JSONTokener(value).nextValue() is JSONObject
                    ) {
                        val gyro = JSONObject()

                        // get value for
                        // "gyro": {"x":"7","y":"8","z":"9"}
                        val innerObj = dataObj.getJSONObject(key)
                        val innerJsonKey = innerObj.keys()
                        while (innerJsonKey.hasNext()) {
                            val innerKey = innerJsonKey.next()
                            val innerKValue = innerObj.getString(innerKey)
                            //ignore string value for edge device.
                            if (isDigit(innerKValue)) {
                                gyro.put(innerKey, innerKValue)
                            }
                        }
                        if (gyro.length() != 0) dObj.put(key, gyro)
                    } else {
                        //ignore string value for edge device.
                        if (isDigit(value)) {
                            dObj.put(key, value)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dObj
    }

    fun getFileSizeInKB(file: File): Long {
        val fileSizeInBytes = file.length()
        return fileSizeInBytes / 1024 //KB

        //        return (fileSizeInBytes / (1024 * 1024)); //MB
    }

     fun createTextFile(
         context: Context,
         directoryPath: String?,
         fileCount: Int,
         iotSDKLogUtils: IotSDKLogUtils?,
         isDebug: Boolean
     ): String {

        //rename file to directory
        val directory: File = File(context.getFilesDir(), directoryPath)
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
        val textFileName: String =
           TEXT_FILE_PREFIX + "_" + System.currentTimeMillis() / 1000 + ""
        val sdkPreferences = IotSDKPreferences.getInstance(context)
        val fileList = CopyOnWriteArrayList(sdkPreferences!!.getList(IotSDKPreferences.TEXT_FILE_NAME))
        //re-name the file to shared preference.
        for (file in fileList) {
            if (file!!.contains(TEXT_FILE_PREFIX)) {
                fileList.remove(file)
                fileList.add(file.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[1])
            }
        }
        fileList.add(textFileName)
        val list= ArrayList(fileList)
        IotSDKPreferences.getInstance(context)!!
            .saveList(IotSDKPreferences.TEXT_FILE_NAME, list)

        //Delete first text file, when more than user defined count.
        if (list.size > fileCount) {
            if (directoryPath != null) {
                deleteTextFile(list,context,directoryPath,iotSDKLogUtils,isDebug)
            }
        }
        iotSDKLogUtils?.log(false, isDebug, "INFO_OS03", context.getString(R.string.INFO_OS03))
        return textFileName
    }

    private fun deleteTextFile(fileNamesList: ArrayList<String?>,context: Context,directoryPath: String, iotSDKLogUtils: IotSDKLogUtils?,
                               isDebug: Boolean): Boolean {
        try {
            //Delete from device
            val file: File =
                File(File(context.filesDir,directoryPath), fileNamesList[0] + ".txt")
            if (file.exists()) {
                file.delete()
            }
            fileNamesList.removeAt(0)
            //delete from shared preferences
            IotSDKPreferences.getInstance(context)?.saveList(IotSDKPreferences.TEXT_FILE_NAME, fileNamesList)
        } catch (e: java.lang.Exception) {
            iotSDKLogUtils?.log(true,isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
            return false
        }
        return true
    }
}