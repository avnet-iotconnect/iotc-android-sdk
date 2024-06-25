package com.iotconnectsdk.utils

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.iotconnectsdk.R
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.beans.Data
import com.iotconnectsdk.beans.GetAttributeBean
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


internal object SDKClientUtils {
    private const val TEXT_FILE_PREFIX = "current"
    private const val YEARS = 31536000 // 1 year

    fun getAttributesList(
        attributesLists: List<GetAttributeBean>, tag: String?
    ): JSONArray {


        val attributesArray = JSONArray()
        for (attribute in attributesLists) {

//           if for not empty "p":"gyro"
            if (attribute.p != null && attribute.p.isNotEmpty()) {
                if (tag == attribute.tg) {
                    try {
                       attribute.d.map {
                           it.faultyTime="60"
                           it
                       }
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
                            attributeValue.faultyTime="60"
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


    fun getTagsList(attributesLists: List<GetAttributeBean>): JSONArray {


        val attributesArray = JSONArray()
        for (attribute in attributesLists) {

//           if for not empty "p":"gyro"
            if (attribute.p != null && attribute.p.isNotEmpty()) {
                try {
                    val attributeObj = JSONObject(Gson().toJson(attribute))
                    attributesArray.put(attributeObj)
                } catch (e: Exception) {
                }

            } else {
                // "p" : "", is empty.
                val attributeValues = attribute.d
                for (attributeValue in attributeValues) {
                    try {
                        val attributeObj = JSONObject(Gson().toJson(attributeValue))
                        attributesArray.put(attributeObj)
                    } catch (e: Exception) {
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


    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(Exception::class)
    fun generateSasTokenLatest(resourceUri: String?, key: String?): String? {
        // Token will expire in one year
        val expiry = Instant.now().epochSecond + YEARS
        val stringToSign = URLEncoder.encode(
            resourceUri,
            "UTF-8"
        ) + "\n" + expiry
        val decodedKey = java.util.Base64.getDecoder().decode(key)
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey =
            SecretKeySpec(decodedKey, "HmacSHA256")
        sha256HMAC.init(secretKey)
        val encoder = java.util.Base64.getEncoder()
        val signature = String(
            encoder.encode(
                sha256HMAC.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
            ), StandardCharsets.UTF_8
        )
        return ("SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, "UTF-8")
                + "&sig=" + URLEncoder.encode(
            signature,
            StandardCharsets.UTF_8.name()
        ) + "&se=" + expiry)
    }

    @Throws(java.lang.Exception::class)
    fun generateSasToken(resourceUri: String?, key: String?): String? {
        // Token will expire in one year
        // val expiry = Instant.now().epochSecond + YEARS

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val yearsInSeconds = TimeUnit.DAYS.toSeconds(365 * YEARS.toLong())
        val expiry = currentTimeSeconds + yearsInSeconds

        val stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry

        val decodedKey = Base64.decode(key, Base64.DEFAULT);
        val sha256HMAC = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(decodedKey, "HmacSHA256")
        sha256HMAC.init(secretKey)

        val base64String = Base64.encode(
            sha256HMAC.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8)),
            Base64.NO_WRAP
        )

        val signature = String(base64String, StandardCharsets.UTF_8)

        return ("SharedAccessSignature sr=" + URLEncoder.encode(
            resourceUri,
            "UTF-8"
        ) + "&sig=" + URLEncoder.encode(signature, StandardCharsets.UTF_8.name()) + "&se=" + expiry)
    }

    fun String.ensureEndsWithSlash(): String {
        return if (this.endsWith("/")) this else "$this/"
    }


}