package com.iotconnectsdk.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse

/**
 * Stores Application Preference Data
 */
internal class IotSDKPreferences private constructor(context: Context) {
    init {
        mSharedPreferences = context.getSharedPreferences(APPLICATION, Context.MODE_PRIVATE)
    }

    fun putStringData(key: String?, value: String?): Boolean {
        val mEditor = mSharedPreferences.edit()
        mEditor.putString(key, value)
        mEditor.commit()
        return true
    }

    fun getStringData(key: String?): String? {
        return try {
            mSharedPreferences.getString(key, null)
        } catch (e: Exception) {
            null
        }
    }

    fun getSyncResponse(key: String?): SyncServiceResponse? {
        var syncServiceResponse: SyncServiceResponse? = null
        syncServiceResponse = try {
            val jsonString = getStringData(key)
            Gson().fromJson(jsonString, SyncServiceResponse::class.java)
        } catch (e: Exception) {
            return null
        }
        return syncServiceResponse
    }

    fun saveList(key: String?, valueList: List<String?>?): Boolean {
        val value = Gson().toJson(valueList)
        val mEditor = mSharedPreferences.edit()
        mEditor.putString(key, value)
        mEditor.commit()
        return true
    }

    fun getList(key: String?): List<String?> {
        return try {
            var list: List<String?> = ArrayList()
            val value = mSharedPreferences.getString(key, null)
            if (value != null) {
                val gson = Gson()
                val type = object : TypeToken<List<String?>?>() {}.type
                list = gson.fromJson(value, type)
            }
            list
        } catch (e: Exception) {
            ArrayList()
        }
    }

    companion object {
        private const val APPLICATION = "IotConnectSDK"
        const val SYNC_API = "SYNC_API"
        const val SYNC_RESPONSE = "sync_response"
        const val TEXT_FILE_NAME = "text_file"
        private lateinit var mSharedPreferences: SharedPreferences
        private var iotSDKPreferences: IotSDKPreferences? = null
        fun getInstance(context: Context): IotSDKPreferences? {
            if (iotSDKPreferences == null) iotSDKPreferences = IotSDKPreferences(context)
            return iotSDKPreferences
        }
    }
}