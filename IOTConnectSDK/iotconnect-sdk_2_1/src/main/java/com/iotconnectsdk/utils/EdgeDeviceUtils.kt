package com.iotconnectsdk.utils


import android.content.Context
import com.google.common.collect.ListMultimap
import com.iotconnectsdk.IoTCEnvironment
import com.iotconnectsdk.beans.GetEdgeRuleBean
import com.iotconnectsdk.beans.TumblingWindowBean

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.text.DecimalFormat

internal object EdgeDeviceUtils {

    private const val DEVICE_ID = "id"
    private const val DT = "dt"
    private const val DEVICE_TAG = "tg"
    private const val D_OBJ = "d"


    /*Update edge device object with object as datatype*/
    fun updateEdgeDeviceGyroObj(
        key: String,
        innerKey: String,
        value: String,
        uniqueId: String,
        edgeDeviceAttributeGyroMap: ListMultimap<String, List<TumblingWindowBean>>?,
        context: Context?
    ) {
        if (edgeDeviceAttributeGyroMap != null) {
            for ((key1, tlbList) in edgeDeviceAttributeGyroMap.entries()) {
                val keyLatest = key1.split(",").toTypedArray()
                if (keyLatest[0] == key) {
                    val inputValue = value.toDouble()
                    if (tlbList != null) {
                        for (bean in tlbList) {
                            if (uniqueId == bean.uniqueId) {
                                if (innerKey == bean.attributeName) {
                                    setObjectValue(bean, inputValue)
                                }
                            }
                        }
                    }
                }
            }
            ValidationTelemetryUtils.isBit = false
        }
    }


    /*Update edge device object for all other datatype*/
    fun updateEdgeDeviceObj(
        key: String,
        value: String,
        uniqueId: String,
        edgeDeviceAttributeMap: ListMultimap<String, TumblingWindowBean>?,
        context: Context?
    ) {
        if (edgeDeviceAttributeMap != null) {
            for ((key1, tlb) in edgeDeviceAttributeMap.entries()) {
                if (key1 == key) {
                    if (uniqueId == tlb.uniqueId) {
                        val inputValue = value.toDouble()
                        if (tlb != null) {
                            setObjectValue(tlb, inputValue)
                        }
                    }
                }
            }
            ValidationTelemetryUtils.isBit = false
        }
    }

    /*Publish Edge Device data based on attributes Tumbling window time in seconds ("tw": "15s")
    *
    * @Param attributeName     Attribute name (humidity etc...)
    * */
    fun publishEdgeDeviceInputData(
        attributeName: String?,
        tag: String,
        edgeDeviceAttributeGyroMap: ListMultimap<String, List<TumblingWindowBean>>?,
        edgeDeviceAttributeMap: ListMultimap<String, TumblingWindowBean>?,
        uniqueId: String?,
        cpId: String?,
        environment: IoTCEnvironment,
        appVersion: String?,
        dtg: String?
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
            for ((key, twbList) in edgeDeviceAttributeGyroMap.entries()) {

                val keyLatest = key.split(",").toTypedArray()
                val attributeNameLatest = attributeName?.split(",")?.toTypedArray()

                if (attributeNameLatest!![0] == keyLatest[0]) {
                    if (attributeNameLatest[1] == keyLatest[1]) {
                        try {
                            var attributeArray: JSONArray? = null
                            if (twbList != null) {
                                for (twb in twbList) {
                                    if (uniqueId == twb.uniqueId) {
                                        attributeArray = getEdgeDevicePublishAttributes(twb)
                                        if (attributeArray.length() > 0) {
                                            dInnerArrayObject.put(twb.attributeName, attributeArray)
                                        }
                                        clearObject(twb)
                                    }
                                }
                            }
                            if (dInnerArrayObject.length() > 0) {
                                gyroObj.put(attributeNameLatest[0], dInnerArrayObject)
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
        }

        //for simple object
        if (edgeDeviceAttributeMap != null) {
            for ((key, twb) in edgeDeviceAttributeMap.entries()) {
                if (key == attributeName) {
                    if (uniqueId == twb.uniqueId) {
                        try {
                            val attributeArray = getEdgeDevicePublishAttributes(twb)
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
        }
        return null
    }

    fun getEdgeDevicePublishMainObj(
        currentTime: String?
    ): JSONObject {
        val mainObj = JSONObject()
        try {
            mainObj.put(DT, currentTime)
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
            if (twb.min !== 0.0 || twb.max !== 0.0 || twb.sum !== 0.0 || twb.avg !== 0.0 || twb.count !== 0 || twb.lv !== 0.0) {
                attributeArray.put(twb.min)
                attributeArray.put(twb.max)
                attributeArray.put(twb.sum)
                attributeArray.put(twb.avg)
                attributeArray.put(twb.count)
                attributeArray.put(twb.lv)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return attributeArray
    }

    fun setObjectValue(bean: TumblingWindowBean, inputValue: Double) {

        val oldMin = bean.min

        if (inputValue < 0.0) {
            if (oldMin == 0.0 || inputValue < oldMin) {
                bean.isMinSet=true
                bean.min = inputValue

            }

        } else {
            if (inputValue == 0.0) {
                if (!bean.isMinSet) {
                    bean.min = 0.0
                    bean.isMinSet=true
                }

            } else if (oldMin == 0.0 || inputValue < oldMin) {
                if (!bean.isMinSet) {
                    bean.min = inputValue
                }

            }
        }

        val oldMax = bean.max

        if (inputValue < 0.0) {
            if (oldMax == 0.0 || inputValue > oldMax) {
                if (!bean.isMaxSet) {
                    bean.max = inputValue
                }
            }
        } else {
            if (inputValue == 0.0) {
                if (!bean.isMaxSet) {
                    bean.max = 0.0
                    bean.isMaxSet=true
                }
            } else if (oldMax == 0.0 || inputValue > oldMax) {
                bean.isMaxSet=true
                bean.max = inputValue

            }
        }

        val sum: Double = inputValue + bean.sum
        val df_obj = DecimalFormat("#.####")

        bean.sum = df_obj.format(sum).toDouble()
        var count: Int = bean.count
        count++
        bean.avg = df_obj.format(sum / count).toDouble()
        bean.count = count
        bean.lv = inputValue
    }

    private fun clearObject(twb: TumblingWindowBean) {
        //clear object on publish success.
        twb.min = 0.0
        twb.max = 0.0
        twb.sum = 0.0
        twb.avg = 0.0
        twb.count = 0
        twb.lv = 0.0
        twb.isMaxSet=false
        twb.isMinSet=false
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

                        return parent[1]
                    } else if (con.contains("#")) {                    //gyro#x > 5
                        val parent = att.split("#".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray() //gyro#x > 5
                        return parent[1]
                    }
                }
            } else if (con.contains("#")) {     //ac1#vibration.x > 5
                val KeyValue =
                    con.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val parent = KeyValue[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray() //gyro#vibration
                val parentAttName = parent[0] //gyro
                val childAttName = parent[1] //vibration
                return getAttName(childAttName)
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

    fun evaluateEdgeDeviceRuleValue(con: String, inputValue: Double): Boolean {
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

    private fun getRuleValue(con: String, operator: String): Double {
        val att = con.split(operator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return att[1].replace("\\s".toRegex(), "").toDouble()
    }

    /*create bellow json and publish on edge device rule matched.
     * {"cpId":"","dtg":"","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    fun getPublishStringEdgeDevice(
        uniqueId: String?, currentTime: String?, bean: GetEdgeRuleBean,
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