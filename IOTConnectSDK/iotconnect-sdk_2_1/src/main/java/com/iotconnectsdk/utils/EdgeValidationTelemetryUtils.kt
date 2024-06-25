package com.iotconnectsdk.utils

import android.text.TextUtils
import android.util.Log
import com.iotconnectsdk.beans.CommonResponseBean
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*
import java.util.regex.Pattern

internal object EdgeValidationTelemetryUtils {

    private const val FAULTY = 1 //"flt";

    private const val REPORTING = 0 //"rpt";

    private const val ERR = 0 //"ERR";

    private const val NEGLECT = -2 //"ERR";

    private const val DATA_TYPE_INTEGER = 1
    private const val DATA_TYPE_LONG = 2
    private const val DATA_TYPE_DECIMAL = 3


    @Synchronized
    fun compareForInputValidationEdge(
        key: String,
        value: String,
        tag: String,
        dObj: CommonResponseBean?,
        isSkipValidation: Boolean
    ): Int {
        val attributesList = dObj?.d?.att
        if (attributesList != null) {
            outerloop@
            for (i in attributesList.indices) {
                val dataBeanList = attributesList[i].d
                for (j in dataBeanList.indices) {
                    val data = dataBeanList[j]
                    val ln = data.ln
                    var tg: String
                    if (data.tg != null) {
                        tg = data.tg
                    } else {
                        tg = ""
                    }

                    val dv = data.dv
                    val dt = data.dt
                    if (key.equals(ln, ignoreCase = true) && tag.equals(tg, ignoreCase = true)) {
                        if (!TextUtils.isEmpty(value.trim())) {
                            return validateDataType(dt, value, dv, isSkipValidation)
                        }else{
                            return NEGLECT
                        }

                    }
                }
            }
        }
        return ERR
    }


    private fun validateDataType(
        dt: Int,
        value: String,
        dv: String,
        isSkipValidation: Boolean
    ): Int {

        if (isSkipValidation) {
            return REPORTING
        }  else if (dt == DATA_TYPE_INTEGER || dt == DATA_TYPE_LONG || dt == DATA_TYPE_DECIMAL) {
            if (TextUtils.isEmpty(value.trim())) {
                return validateNumber(dt, value, dv)
            } else if (!SDKClientUtils.isDigit(value)) {
                return FAULTY
            }

        }

        return ERR
    }

    @Throws(Exception::class)
    private fun validateNumber(dt: Int, value: String, range: String): Int {
        var range = range
        var start: Double
        var end: Double

        if ((value == null || value.isEmpty()) && (range == null || range.isEmpty())) {
            return REPORTING
        }

        range = range.replace(",\\s".toRegex(), ",")
        val array = range.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val list = ArrayList<String>()
        for (s in array.toList()) {
            list.add(s)
        }
        val rngwithnum = ArrayList<Double>()
        val rngwithto = ArrayList<String>()
        for (str in list) {
            if (str.isNotEmpty()) {
                if (str.contains("to")) {
                    rngwithto.add(str)
                } else {
                    try {
                        rngwithnum.add(str.toDouble())
                    } catch (e: Exception) {
                        if (SDKClientUtils.isLetter(value)) {
                            return validateVarchar(value, range)
                        } else {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        list.removeAll(rngwithto)
        //if value is in comma separated value

        try {

            if (SDKClientUtils.isDigit(value)) {
                if (rngwithnum.contains(value.toDouble())) {
                    return REPORTING
                } else {
                    //check value is in range
                    val `val` = value.toDouble()
                    val isRangeChecked = false
                    val availableList = ArrayList<Int>()
                    for (str in rngwithto) {
                        if (str.isNotEmpty()) {
                            val ary = str.split("to".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            start = ary[0].toDouble()
                            end = ary[1].toDouble()
                            if (`val` < start || `val` > end) {
                                availableList.add(FAULTY)
                            } else {
                                availableList.add(REPORTING)
                            }
                        }
                    }
                    if (rngwithto.size == 0 && rngwithnum.size > 0) {
                        return FAULTY
                    } else if (availableList.contains(REPORTING)) {
                        return REPORTING
                    } else {
                        return if (availableList.contains(FAULTY))
                            FAULTY
                        else
                            REPORTING
                    }
                }
            } else {
                return validateVarchar(value, range)
            }
        } catch (e: Exception) {
            return 1 // return faulty (input value is not an int type, so we can not campare with between.)
        }

    }

    @Throws(java.lang.Exception::class)
    private fun validateVarchar(value: String, range: String): Int {
        var range: String? = range
        if (range == null || range.trim { it <= ' ' }.isEmpty()) {
            return REPORTING
        }
        range = range.replace(",\\s".toRegex(), ",").replace("\\s+,".toRegex(), ",")
        val array = range.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val list = array.toList()
        return if (list.contains(value))
            REPORTING
        else
            FAULTY
    }

}