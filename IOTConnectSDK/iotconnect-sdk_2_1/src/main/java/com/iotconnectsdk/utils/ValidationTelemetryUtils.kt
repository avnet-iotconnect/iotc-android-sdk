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

internal object ValidationTelemetryUtils {

    private const val FAULTY = 1 //"flt";

    private const val REPORTING = 0 //"rpt";

    private const val ERR = 0 //"ERR";

    private const val DATA_TYPE_INTEGER = 1
    private const val DATA_TYPE_LONG = 2
    private const val DATA_TYPE_DECIMAL = 3
    private const val DATA_TYPE_STRING = 4
    private const val DATA_TYPE_TIME = 5
    private const val DATA_TYPE_DATE = 6
    private const val DATA_TYPE_DATETIME = 7
    private const val DATA_TYPE_BIT = 8 // [0 / 1]

    private const val DATA_TYPE_BOOLEAN = 9 // [true / false | True/False]

    private const val DATA_TYPE_LATLONG = 10 // [Decimal Array, Decimal (10,8), Decimal (11,8)]

    var isBit = false


    @Synchronized
    fun compareForInputValidationNew(
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
                        return validateDataType(dt, value, dv, isSkipValidation)

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
        } else if (dt == DATA_TYPE_STRING) {
            return validateNumber(dt, value, dv)
        } /*else if (TextUtils.isEmpty(value.trim())) {
            return FAULTY
        }*/ else if (dt == DATA_TYPE_INTEGER || dt == DATA_TYPE_LONG || dt == DATA_TYPE_DECIMAL) {
            if (TextUtils.isEmpty(value.trim())) {
                return validateNumber(dt, value, dv)
            } else if (!SDKClientUtils.isDigit(value)) {
                return FAULTY
            } /*else {

            }*/

        } else if (dt == DATA_TYPE_BIT) {
            isBit = true
            return validateBit(value, dv)
        } else if (dt == DATA_TYPE_BOOLEAN) {
            return validateBoolean(value, dv)
        } else if (dt == DATA_TYPE_TIME) {
            return validateTime(value, dv)
        } else if (dt == DATA_TYPE_DATE) {
            return validateDate(value, dv, dt)
        } else if (dt == DATA_TYPE_DATETIME) {
            return validateDate(value, dv, dt)
        } else if (dt == DATA_TYPE_LATLONG) {
            return validateLatLong(value, dv)
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

    private fun validateBit(value: String?, dataValidation: String?): Int {
        return try {
            if ((value == null || value.isEmpty()) && (dataValidation == null || dataValidation.isEmpty())) {
                return REPORTING
            }
            val `val` = value!!.toInt()
            if (`val` != 0 && `val` != 1) {
                return FAULTY
            }
            if (dataValidation != null && !dataValidation.isEmpty()) {
                return if (dataValidation.toInt() == `val`) {
                    REPORTING
                } else {
                    FAULTY
                }
            }
            REPORTING
        } catch (e: java.lang.Exception) {
            FAULTY
        }
    }

    private fun validateBoolean(value: String, dataValidation: String?): Int {
        return try {

            if (dataValidation != null && !dataValidation.isEmpty()) {
                if (dataValidation == value) {
                    REPORTING
                } else {
                    return FAULTY
                }
            } else if (value.equals("true", true) || (value.equals("false", true))) {
                return REPORTING
            } else {
                return FAULTY
            }
        } catch (e: java.lang.Exception) {
            return FAULTY
        }

    }


    private fun validateTime(value: String?, range: String): Int {
        var range: String? = range
        return try {
            if ((value == null || value.isEmpty()) && (range == null || range.isEmpty())) {
                return REPORTING
            }
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            val time = LocalTime.parse(value, formatter)
            var start: LocalTime?
            var end: LocalTime?
            range = range!!.replace(",\\s".toRegex(), ",")
            val array = range.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val list = ArrayList<String>()
            for (s in Arrays.asList(*array)) {
                list.add(s)
            }
            val rngwithnum = ArrayList<LocalTime>()
            val rngwithto = ArrayList<String>()
            for (str in list) {
                if (str.length > 0) {
                    if (str.contains("to")) {
                        rngwithto.add(str)
                    } else {
                        rngwithnum.add(LocalTime.parse(str, formatter))
                    }
                }
            }
            list.removeAll(rngwithto)
            //if value is in comma separated value
            if (rngwithnum.contains(time)) {
                REPORTING
            } else {
                //check value is in range
                val isRangeChecked = false
                val availableList = ArrayList<Int>()
                for (str in rngwithto) {
                    if (str.length > 0) {
                        val ary = str.split("to".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        start = LocalTime.parse(ary[0].trim { it <= ' ' }, formatter)
                        end = LocalTime.parse(ary[1].trim { it <= ' ' }, formatter)
                        if (time.isBefore(start) || time.isAfter(end)) {
                            availableList.add(FAULTY)
                        } else {
                            availableList.add(REPORTING)
                        }
                    }
                }
                if (rngwithto.size == 0 && rngwithnum.size > 0) {
                    FAULTY
                } else if (availableList.contains(REPORTING)) {
                    REPORTING
                } else {
                    if (availableList.contains(FAULTY)) FAULTY else REPORTING
                }
            }
        } catch (e: java.lang.Exception) {
            FAULTY
        }
    }

    private fun validateDate(value: String?, range: String, dt: Int): Int {
        var range: String? = range
        var formatter: DateTimeFormatter? = null

        return try {
            if ((value == null || value.isEmpty()) && (range == null || range.isEmpty())) {
                return REPORTING
            }

            if (dt == DATA_TYPE_DATE) {
                formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT)
            } else if (dt == DATA_TYPE_DATETIME) {
                formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'hh:mm:ss.SSS'Z'")
                    .withResolverStyle(ResolverStyle.STRICT)
            }

            val date = LocalDate.parse(value, formatter)
            var start: LocalDate?
            var end: LocalDate?
            range = range!!.replace(",\\s".toRegex(), ",")
            val array = range.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val list = ArrayList<String>()
            for (s in Arrays.asList(*array)) {
                list.add(s)
            }
            val rngwithnum = ArrayList<LocalDate>()
            val rngwithto = ArrayList<String>()
            for (str in list) {
                if (str.length > 0) {
                    if (str.contains("to")) {
                        rngwithto.add(str)
                    } else {
                        rngwithnum.add(LocalDate.parse(str, formatter))
                    }
                }
            }
            list.removeAll(rngwithto)
            //if value is in comma separated value
            if (rngwithnum.contains(date)) {
                REPORTING
            } else {
                //check value is in range
                val isRangeChecked = false
                val availableList = ArrayList<Int>()
                for (str in rngwithto) {
                    if (str.length > 0) {
                        val ary = str.split("to".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        start = LocalDate.parse(ary[0].trim { it <= ' ' }, formatter)
                        end = LocalDate.parse(ary[1].trim { it <= ' ' }, formatter)
                        if (date.isBefore(start) || date.isAfter(end)) {
                            availableList.add(FAULTY)
                        } else {
                            availableList.add(REPORTING)
                        }
                    }
                }
                if (rngwithto.size == 0 && rngwithnum.size > 0) {
                    FAULTY
                } else if (availableList.contains(REPORTING)) {
                    REPORTING
                } else {
                    if (availableList.contains(FAULTY)) FAULTY else REPORTING
                }
            }
        } catch (e: java.lang.Exception) {
            FAULTY
        }
    }

    private fun validateLatLong(value: String, dataValidation: String): Int {
        return try {
            if (Pattern.compile("\\[\\d*\\.?\\d*\\,\\d*\\.?\\d*\\]", Pattern.MULTILINE)
                    .matcher(value).find()
            ) {
                REPORTING
            } else {
                FAULTY
            }
        } catch (e: java.lang.Exception) {
            FAULTY
        }
    }
}