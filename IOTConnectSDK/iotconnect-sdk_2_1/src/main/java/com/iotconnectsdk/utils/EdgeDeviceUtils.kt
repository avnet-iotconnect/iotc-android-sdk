package com.iotconnectsdk.utils

import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

object EdgeDeviceUtils {

    private const val CP_ID = "cpId"
    private const val MESSAGE_TYPE = "mt"
    private const val LANGUAGE = "l"
    private const val VERSION = "v"
    private const val ENVIRONMENT = "e"
    private const val SDK_OBJ = "sdk"
    private const val DTG = "dtg"
    private const val CURRENT_DATE = "dt"
    private const val DEVICE_ID = "id"
    private const val DT = "dt"
    private const val DEVICE_TAG = "tg"
    private const val D_OBJ = "d"

    private const val EDGE_DEVICE_MESSAGE_TYPE = 2
    private const val TEXT_FILE_PREFIX = "current"

    fun updateEdgeDeviceGyroObj(
        key: String, innerKey: String, value: String,
        edgeDeviceAttributeGyroMap: MutableMap<String, List<TumblingWindowBean>>?
    ) {
        if (edgeDeviceAttributeGyroMap != null) {
            for ((key1, tlbList) in edgeDeviceAttributeGyroMap) {
                if (key1 == key) {
                    val inputValue = value.toInt()
                    if (tlbList != null) {
                        for (bean in tlbList) {
                            if (innerKey == bean?.attributeName) {
                                setObjectValue(bean, inputValue)
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateEdgeDeviceObj(
        key: String, value: String, edgeDeviceAttributeMap: MutableMap<String, TumblingWindowBean>?
    ) {
        if (edgeDeviceAttributeMap != null) {
            for ((key1, tlb) in edgeDeviceAttributeMap) {
                if (key1 == key) {
                    val inputValue = value.toInt()
                    if (tlb != null) {
                        setObjectValue(tlb, inputValue)
                    }
                }
            }
        }
    }

    /*Publish Edge Device data based on attributes Tumbling window time in seconds ("tw": "15s")
    *
    * @Param attributeName     Attribute name (humidity etc...)
    * */
    fun publishEdgeDeviceInputData(
        attributeName: String?, tag: String,
        edgeDeviceAttributeGyroMap: MutableMap<String, List<TumblingWindowBean>>?,
        edgeDeviceAttributeMap: MutableMap<String, TumblingWindowBean>?, uniqueId: String?, cpId: String?,
        environment: String?, appVersion: String?, dtg: String?
    ): JSONObject? {
        val currentTime = DateTimeUtils.currentDate
        val mainObj = getEdgeDevicePublishMainObj(
            currentTime/*, dtg, cpId, environment, appVersion, EDGE_DEVICE_MESSAGE_TYPE*/
        )
        val dArray = JSONArray()
        val dArrayObject = getEdgeDevicePublishDObj(currentTime, tag, uniqueId!!)
        val dInnerArray = JSONArray()
        val gyroObj = JSONObject()
        val dInnerArrayObject = JSONObject()
        //for gyro object
        if (edgeDeviceAttributeGyroMap != null) {
            for ((key, twbList) in edgeDeviceAttributeGyroMap) {
                if (attributeName == key) {
                    try {
                        var attributeArray: JSONArray? = null
                        if (twbList != null) {
                            for (twb in twbList) {
                                attributeArray = getEdgeDevicePublishAttributes(twb!!)
                                if (attributeArray.length() > 0) {
                                    dInnerArrayObject.put(twb.attributeName, attributeArray)
                                }
                                clearObject(twb)
                            }
                        }
                        if (dInnerArrayObject.length() > 0) {
                            gyroObj.put(attributeName, dInnerArrayObject)
                           // dInnerArray.put(gyroObj)
                        }
                        dArrayObject.put(D_OBJ, gyroObj)
                        dArray.put(dArrayObject)
                        mainObj.put(D_OBJ, dArray)
                        return mainObj
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        //for simple object
        if (edgeDeviceAttributeMap != null) {
            for ((key, twb) in edgeDeviceAttributeMap) {
                if (key == attributeName) {
                    try {
                        val attributeArray = getEdgeDevicePublishAttributes(twb!!)
                        if (attributeArray.length() > 0) {
                            dInnerArrayObject.put(attributeName, attributeArray)
                           // dInnerArray.put(dInnerArrayObject)
                        }
                        dArrayObject.put(D_OBJ, dInnerArrayObject)
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
        }
        return null
    }

    fun getEdgeDevicePublishMainObj(
        currentTime: String?/*, dtg: String?, cpId: String?, environment: String?,
        appVersion: String?, messageType: Int*/
    ): JSONObject {
        val mainObj = JSONObject()
        try {
          //  mainObj.put(CP_ID, cpId)
          //  mainObj.put(DTG, dtg)
            mainObj.put(DT, currentTime)
           // mainObj.put(MESSAGE_TYPE, messageType)
        //  mainObj.put(SDK_OBJ, SDKClientUtils.getSdk(environment, appVersion))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return mainObj
    }

    private fun getEdgeDevicePublishDObj(
        currentTime: String, tag: String, uniqueId: String
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

    fun getAttributeName(con: String): String? {

        //"ac1#vibration.x > 5 AND ac1#vibration.y > 10",
        //gyro#vibration.x > 5 AND gyro#vibration.y > 10
        try {
            //gyro#vibration.x > 5 AND gyro#vibration.y > 10
            if (con.contains("#") && con.contains("AND")) {
                val param = con.split("AND".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in 0..param.size) {
                    val att = param[i]
                    if (att.contains(".")) {           //gyro#vibration.x > 5
                        val KeyValue =
                            att.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val parent = KeyValue[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray() //gyro#vibration
                        //                        String childAttName = parent[1]; //vibration
//                        String keyAttName = KeyValue[1]; //x > 5
                        return parent[0]
                    } else if (con.contains("#")) {                    //gyro#x > 5
                        val parent = att.split("#".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray() //gyro#x > 5
                        //                        String childAttName = parent[1]; //x > 5
                        return parent[0]
                    }
                }
            } else if (con.contains("#")) {     //ac1#vibration.x > 5
                val KeyValue =
                    con.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val parent = KeyValue[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray() //gyro#vibration
                val parentAttName = parent[0] //gyro
                val childAttName = parent[1] //vibration
                val keyAttName = KeyValue[1] //x > 5
                return getAttName(keyAttName)
            } else if (con.contains(".")) {
                val keyValue =
                    con.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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
        val att = con.split(operator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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
        val att = con.split(operator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return att[1].replace("\\s".toRegex(), "").toInt()
    }

    /*create bellow json and publish on edge device rule matched.
     * {"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"514076B1-3C21-4849-A777-F423B1821FC7","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    fun getPublishStringEdgeDevice(
        uniqueId: String?, currentTime: String?, bean: SyncServiceResponse.DBeanXX.RuleBean,
        inputJsonString: String, cvAttObj: JSONObject?, mainObj: JSONObject
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
                    if (!value.replace("\\s".toRegex(), "").isEmpty() && JSONTokener(
                            value
                        ).nextValue() is JSONObject
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
                            if (SDKClientUtils.isDigit(innerKValue)) {
                                gyro.put(innerKey, innerKValue)
                            }
                        }
                        if (gyro.length() != 0) dObj.put(key, gyro)
                    } else {
                        //ignore string value for edge device.
                        if (SDKClientUtils.isDigit(value)) {
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


}