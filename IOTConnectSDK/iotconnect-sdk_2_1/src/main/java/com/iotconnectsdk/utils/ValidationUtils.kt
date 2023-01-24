package com.iotconnectsdk.utils

import android.content.Context
import android.webkit.URLUtil
import com.iotconnectsdk.R
import com.iotconnectsdk.utils.IotSDKNetworkUtils.isOnline
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal class ValidationUtils private constructor(
    private val iotSDKLogUtils: IotSDKLogUtils,
    private val context: Context,
    private val isDebug: Boolean
) {
    fun networkConnectionCheck(): Boolean {
        if (!isOnline(context)) {
            iotSDKLogUtils.log(true, isDebug, "ERR_IN08", context.getString(R.string.ERR_IN08))
            return false
        }
        return true
    }

    fun isEmptyValidation(id: String, errorCode: String?, message: String?): Boolean {
        var id = id
        id = id.replace("\\s+".toRegex(), "")
        if (id.isEmpty() || id.length == 0) {
            iotSDKLogUtils.log(true, isDebug, errorCode!!, message!!)
            return false
        }
        return true
    }

    /*  Check discovery url is empty or not.
     * @ param discoveryUrl  discovery URL ("discoveryUrl" : "https://discovery.iotconnect.io")
     * */
    fun checkDiscoveryURL(DISCOVERY_URL: String?, sdkObj: JSONObject): Boolean {
        try {
            if (sdkObj.has(DISCOVERY_URL)) {
                val discoveryUrl = sdkObj.getString(DISCOVERY_URL)
                if (discoveryUrl.isEmpty() || discoveryUrl.length == 0) {
                    iotSDKLogUtils.log(
                        true,
                        isDebug,
                        "ERR_IN02",
                        context.getString(R.string.ERR_IN02)
                    )
                    return false
                }
                if (!URLUtil.isValidUrl(discoveryUrl)) {
                    iotSDKLogUtils.log(
                        true,
                        isDebug,
                        "ERR_IN01",
                        context.getString(R.string.ERR_IN01)
                    )
                    return false
                }
            } else {
                iotSDKLogUtils.log(true, isDebug, "ERR_IN03", context.getString(R.string.ERR_IN03))
                return false
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            iotSDKLogUtils.log(true, isDebug, "ERR_IN01", e.message!!)
            return false
        }
        return true
    }

    fun validateBaseUrl(discoveryApiResponse: DiscoveryApiResponse): Boolean {
        if (discoveryApiResponse.d.bu == null || discoveryApiResponse.d.bu.isEmpty()) {
            iotSDKLogUtils.log(true, isDebug, "ERR_IN09", context.getString(R.string.ERR_IN09))
            return false
        }
        return true
    }

    /*Validate user input format.
     * */
    fun isValidInputFormat(jsonData: String, uniqueId: String): Boolean {
        //check is input empty.
        if (jsonData.isEmpty() || jsonData.length == 0) {
            iotSDKLogUtils.log(true, isDebug, "ERR_SD05", context.getString(R.string.ERR_SD05))
            return false
        }

        //check the input json standard as JSONArray or not.
        try {
            val jsonArray = JSONArray(jsonData)

            //compare gateway device uniqueId with input gateway device uniqueId.
            var isValidId = false
            for (i in 0 until jsonArray.length()) {
                val inputUniqueId = jsonArray.getJSONObject(i).getString("uniqueId")
                if (uniqueId == inputUniqueId) {
                    isValidId = true
                    break
                }
            }
            if (!isValidId) {
                iotSDKLogUtils.log(true, isDebug, "ERR_SD02", context.getString(R.string.ERR_SD02))
                return false
            }
            if (jsonArray is JSONArray) {
                if (jsonArray.length() > 0) {
                    for (i in 0 until jsonArray.length()) {
                        if (!jsonArray.getJSONObject(i).has("time")) {
                            iotSDKLogUtils.log(
                                true,
                                isDebug,
                                "ERR_SD07",
                                context.getString(R.string.ERR_SD07)
                            )
                            return false
                        } else {
                            val time = jsonArray.getJSONObject(i).getString("time")
                            val pattern =
                                "^\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01])T([0-9]|0[0-9]|1?[0-9]|2[0-3]):[0-5]?[0-9]:[0-5]?[0-9].\\d{3}Z$"
                            if (!time.matches(pattern.toRegex())) {
                                iotSDKLogUtils.log(
                                    true,
                                    isDebug,
                                    "ERR_SD03",
                                    context.getString(R.string.ERR_SD03)
                                )
                            }
                        }
                        if (!jsonArray.getJSONObject(i).has("uniqueId")) {
                            iotSDKLogUtils.log(
                                true,
                                isDebug,
                                "ERR_SD08",
                                context.getString(R.string.ERR_SD08)
                            )
                            return false
                        }
                        if (!jsonArray.getJSONObject(i).has("data")) {
                            iotSDKLogUtils.log(
                                true,
                                isDebug,
                                "ERR_SD06",
                                context.getString(R.string.ERR_SD06)
                            )
                            return false
                        }
                    }
                } else {
                    iotSDKLogUtils.log(
                        true,
                        isDebug,
                        "ERR_SD05",
                        context.getString(R.string.ERR_SD05)
                    )
                }
            } else {
                iotSDKLogUtils.log(true, isDebug, "ERR_SD05", context.getString(R.string.ERR_SD05))
                return false
            }
        } catch (e: JSONException) {
            iotSDKLogUtils.log(true, isDebug, "ERR_SD04", context.getString(R.string.ERR_SD04))
            return false
        }
        return true
    }

    fun validateKeyValue(key: String, value: String): Boolean {
        var key = key
        var value = value
        key = key.replace("\\s+".toRegex(), "")
        value = key.replace("\\s+".toRegex(), "")
        if (key.length == 0 || value.length == 0) {
            iotSDKLogUtils.log(true, isDebug, "ERR_TP03", context.getString(R.string.ERR_TP03))
            return false
        }
        return true
    }

    fun validateAckParameters(obj: JSONObject?, messageType: String): Boolean {
        if (obj == null || messageType.isEmpty()) {
            iotSDKLogUtils.log(true, isDebug, "ERR_CM02", context.getString(R.string.ERR_CM02))
            return false
        }
        return if (obj is JSONObject) {
            true
        } else {
            iotSDKLogUtils.log(
                true,
                isDebug,
                "ERR_CM03",
                context.getString(R.string.ERR_CM03)
            )
            false
        }
    }

    fun responseCodeMessage(rc: Int) {
        when (rc) {
            0 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN08",
                context.getString(R.string.INFO_IN08)
            )
            1 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN09",
                context.getString(R.string.INFO_IN09)
            )
            2 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN10",
                context.getString(R.string.INFO_IN10)
            )
            3 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN11",
                context.getString(R.string.INFO_IN11)
            )
            4 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN12",
                context.getString(R.string.INFO_IN12)
            )
            5 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN13",
                context.getString(R.string.INFO_IN13)
            )
            6 -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN14",
                context.getString(R.string.INFO_IN14)
            )
            else -> iotSDKLogUtils.log(
                false,
                isDebug,
                "INFO_IN15",
                context.getString(R.string.INFO_IN15)
            )
        }
    }

    companion object {
        private val validationUtils: ValidationUtils? = null
        fun getInstance(
            iotSDKLogUtils: IotSDKLogUtils,
            context: Context,
            debug: Boolean
        ): ValidationUtils? {
            return validationUtils
                ?: ValidationUtils(iotSDKLogUtils, context, debug)
        }
    }
}