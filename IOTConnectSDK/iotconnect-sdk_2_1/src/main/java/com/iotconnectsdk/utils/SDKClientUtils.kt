package com.iotconnectsdk.utils

import android.content.Context
import com.google.gson.Gson
import com.iotconnectsdk.R
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.beans.Data
import com.iotconnectsdk.beans.GetAttributeBean
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

object SDKClientUtils {
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

    fun isDigit(value: String): Boolean {
        return value.matches("^-?[0-9]*\\.?[0-9]+\$".toRegex())

    }

    fun isLetter(value: String): Boolean {
        return value.all { char -> char.isLetter() }

    }

    fun isLetterOrDigit(value: String): Boolean {
        return value.matches("[A-Za-z0-9\\!\\\"\\#\\$\\%\\&\\'\\(\\)\\*\\+\\,\\-\\.\\/\\:\\;\\<\\>\\=\\?\\@\\[\\]\\{\\}\\\\\\^\\_\\`\\~]+$".toRegex())

    }

    fun createCommandFormat(
        commandType: Int, cpId: String?, guid: String?, uniqueId: String?, command: String?,
        ack: Boolean, ackId: String?
    ): String {
        val data = Data()
        data.cpid = cpId
        data.guid = guid
        data.uniqueId = uniqueId
        data.command = command
        data.isAck = ack
        data.ackId = ackId
        data.cmdType = commandType
        return Gson().toJson(data)
    }



    fun getFileSizeInKB(file: File): Long {
        val fileSizeInBytes = file.length()
        return fileSizeInBytes / 1024 //KB
    }

    fun createTextFile(
        context: Context, directoryPath: String?, fileCount: Int, iotSDKLogUtils: IotSDKLogUtils?,
        isDebug: Boolean
    ): String {

        //rename file to directory
        val directory: File = File(context.filesDir, directoryPath)
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