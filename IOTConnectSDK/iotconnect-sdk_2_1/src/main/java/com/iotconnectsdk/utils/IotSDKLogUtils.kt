package com.iotconnectsdk.utils

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

internal class IotSDKLogUtils private constructor(
    private val activity: Context,
    private val cpId: String,
    private val uniqueId: String
) {
    fun log(isError: Boolean, isDebug: Boolean, code: String, msg: String) {
        if (isDebug) {
            val currentDate: String = DateTimeUtils.currentDate
            val LOG = "[" + code + "] " + currentDate + " [" + cpId + "_" + uniqueId + "]: " + msg
            writeLogFile(isError, LOG)
        }
    }

    private fun writeLogFile(isError: Boolean, log: String) {
        try {
            val directoryPath = "logs/debug"
            val directory = getChildrenFolder(directoryPath)
            val errorFile = File(directory, "error.txt")
            val infoFile = File(directory, "info.txt")
            if (isError) checkFile(errorFile) else checkFile(infoFile)
            if (isError) writeToFile(errorFile, log) else writeToFile(infoFile, log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writePublishedMessage(directoryPath: String, textFile: String, message: String) {
        val directory = getChildrenFolder(directoryPath)
        val file = File(directory, "$textFile.txt")
        checkFile(file)
        writeToFile(file, message)
    }

    private fun getChildrenFolder(path: String): File {
        var dir = activity.filesDir
        val dirs: List<String> =
            ArrayList(Arrays.asList(*path.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()))
        for (i in dirs.indices) {
            dir = File(dir, dirs[i]) //Getting a file within the dir.
            if (!dir.exists()) {
                dir.mkdir()
            }
        }
        return dir
    }

    private fun checkFile(file: File) {
        if (!file.exists()) {
            try {
                file.parentFile.mkdirs()
                file.createNewFile()
            } catch (e: IOException) {
                // log(false, true, "0000", e.getMessage());
                e.printStackTrace()
            }
        }
    }

    private fun writeToFile(file: File, log: String) {
        try {
            // Adds a line to the file
            val writer = BufferedWriter(FileWriter(file, true))
            writer.write(log)
            writer.newLine()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val iotSDKLogUtils: IotSDKLogUtils? = null
        fun getInstance(act: Context, cpId: String, uniqueId: String): IotSDKLogUtils {
            return iotSDKLogUtils
                ?: IotSDKLogUtils(
                    act,
                    cpId,
                    uniqueId
                )
        }
    }
}