package com.iotconnectsdk.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.iotconnectsdk.R
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.beans.Data
import com.iotconnectsdk.beans.GetAttributeBean
import com.iotconnectsdk.beans.TumblingWindowBean
import com.iotconnectsdk.utils.ValidationTelemetryUtils.M_ANDROID

import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.*
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

    fun getAttributesList(
        attributesLists: List<GetAttributeBean>, tag: String?
    ): JSONArray {

        //CREATE ATTRIBUTES ARRAY and OBJECT, "attributes":[{"ln":"Temp","dt":"number","dv":"5 to 20, 25","tg":"gateway","tw":"60s"},{"p":"gyro","dt":"object","tg":"gateway","tw":"90s","d":[{"ln":"x","dt":"number","dv":"","tg":"gateway","tw":"90s"},{"ln":"y","dt":"string","dv":"red, gray,   blue","tg":"gateway","tw":"90s"},{"ln":"z","dt":"number","dv":"-5 to 5, 10","tg":"gateway","tw":"90s"}]}]
        val attributesArray = JSONArray()
        for (attribute in attributesLists) {

//           if for not empty "p":"gyro"
            if (attribute.p != null && attribute.p.isNotEmpty()) {
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
        reportingORFaulty: String?, dObj: SyncServiceResponse.DBeanXX, appVersion: String,
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
    fun getTag(uniqueId: String, dObj: CommonResponseBean.D?): String {
        var tag = ""
        val values = dObj?.childDevice
        if (values != null) {
            for (data in values) {
                if (uniqueId.equals(data.id, ignoreCase = true)) {
                    if (data.tg != null) {
                        tag = data.tg!!
                    } else {
                        tag = ""
                    }
                    break
                }
            }
        }
        return tag
    }

  /*  @Synchronized
    fun compareForInputValidation(
        key: String, value: String, tag: String, dObj: CommonResponseBean?
    ): Int {
        var result = 0
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
                        result = validateDataType(dt, value, dv)

                        break@outerloop
                    }
                }
            }
        }
        return result
    }*/

/*    private fun validateDataType(dt: Int, value: String, dv: String): Int {
        var result = 0

        Log.d("dtValue", "::" + dt)

        if (dt == 1 || dt == 2 || dt == 3 || dt == 8) {
            if (value.isNotEmpty() && !isDigit(value)) {
                result = 1
            } else {
                result = compareWithInput(value, dv)
            }
        } else {
            result = compareWithInput(value, dv)
        }

        return result
    }*/

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
    /*private fun compareWithInput(inputValue: String, validationValue: String): Int {
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
            val array =
                validationValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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
            val array =
                validationValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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
            val array =
                validationValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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
    }*/


    fun isDigit(value: String): Boolean {
       // return value.all { char -> char.isDigit() }
        return value.matches("^-?[0-9]*\\.?[0-9]+\$".toRegex())

         /*  var value = value
           value = value.replace("\\s".toRegex(), "")
           return value.matches("\\d+(?:\\.\\d+)?".toRegex())*/
    }

    fun isLetter(value: String): Boolean {
        return value.all { char -> char.isLetter() }

        /*  var value = value
          value = value.replace("\\s".toRegex(), "")
          return value.matches("\\d+(?:\\.\\d+)?".toRegex())*/
    }

    fun isLetterOrDigit(value: String): Boolean {
      //  return value.matches("^[a-zA-Z0-9!@#$&()\\-`.+,/\"]*$".toRegex())
        return value.matches("[A-Za-z0-9\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\>\\=\\?\\@\\[\\]\\{\\}\\\\\\^\\_\\`\\~]+$".toRegex())

        /*  var value = value
          value = value.replace("\\s".toRegex(), "")
          return value.matches("\\d+(?:\\.\\d+)?".toRegex())*/
    }

    fun createCommandFormat(
        commandType: Int, cpId: String?, guid: String?, uniqueId: String?, command: String?,
        ack: Boolean, ackId: String?
    ): String {
        // val cfj = CommandFormatJson()
        // cfj.cmdType = commandType
        val data = Data()
        data.cpid = cpId
        data.guid = guid
        data.uniqueId = uniqueId
        data.command = command
        data.isAck = ack
        data.ackId = ackId
        data.cmdType = commandType
        // cfj.data = data
        return Gson().toJson(data)
    }



    fun getFileSizeInKB(file: File): Long {
        val fileSizeInBytes = file.length()
        return fileSizeInBytes / 1024 //KB

        //        return (fileSizeInBytes / (1024 * 1024)); //MB
    }

    fun createTextFile(
        context: Context, directoryPath: String?, fileCount: Int, iotSDKLogUtils: IotSDKLogUtils?,
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
        val textFileName: String = TEXT_FILE_PREFIX + "_" + System.currentTimeMillis() / 1000 + ""
        val sdkPreferences = IotSDKPreferences.getInstance(context)
        val fileList =
            CopyOnWriteArrayList(sdkPreferences!!.getList(IotSDKPreferences.TEXT_FILE_NAME))
        //re-name the file to shared preference.
        for (file in fileList) {
            if (file!!.contains(TEXT_FILE_PREFIX)) {
                fileList.remove(file)
                fileList.add(
                    file.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                )
            }
        }
        fileList.add(textFileName)
        val list = ArrayList(fileList)
        IotSDKPreferences.getInstance(context)!!.saveList(IotSDKPreferences.TEXT_FILE_NAME, list)

        //Delete first text file, when more than user defined count.
        if (list.size > fileCount) {
            if (directoryPath != null) {
                deleteTextFile(list, context, directoryPath, iotSDKLogUtils, isDebug)
            }
        }
        iotSDKLogUtils?.log(false, isDebug, "INFO_OS03", context.getString(R.string.INFO_OS03))
        return textFileName
    }

     fun deleteTextFile(
         fileNamesList: ArrayList<String?>, context: Context, directoryPath: String?,
         iotSDKLogUtils: IotSDKLogUtils?, isDebug: Boolean
    ): Boolean {
        try {
            //Delete from device
            val file: File = File(File(context.filesDir, directoryPath), fileNamesList[0] + ".txt")
            if (file.exists()) {
                file.delete()
            }
            fileNamesList.removeAt(0)
            //delete from shared preferences
            IotSDKPreferences.getInstance(context)?.saveList(
                IotSDKPreferences.TEXT_FILE_NAME, fileNamesList
            )
        } catch (e: java.lang.Exception) {
            iotSDKLogUtils?.log(true, isDebug, "ERR_OS01", e.message!!)
            e.printStackTrace()
            return false
        }
        return true
    }
}