package com.iotconnectsdk.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iotconnectsdk.beans.CommonResponseBean
import com.iotconnectsdk.webservices.responsebean.IdentityServiceResponse

/**
 * Stores Application Preference Data
 */
internal class IotSDKPreferences private constructor(context: Context) {

    private val APPLICATION = "IotConnectSDK"
    private var mSharedPreferences = context.getSharedPreferences(APPLICATION, Context.MODE_PRIVATE)

    companion object {

        val SYNC_API = "SYNC_API"
        val SYNC_RESPONSE = "sync_response"
        val ATTRIBUTE_RESPONSE = "att_response"
        val SETTING_TWIN_RESPONSE = "setting_response"
        val EDGE_RULE_RESPONSE = "edge_rule_response"
        val CHILD_DEVICE_RESPONSE = "child_device_response"
        val PENDING_OTA_RESPONSE = "ota_response"
        val DATA_FREQUENCY_CHANGE = "data_freq_change"

        val TEXT_FILE_NAME = "text_file"

        @Volatile
        private var iotSDKPreferences: IotSDKPreferences? = null
        fun getInstance(context: Context): IotSDKPreferences? {
            synchronized(this) {
                if (iotSDKPreferences == null) iotSDKPreferences = IotSDKPreferences(context)
                return iotSDKPreferences
            }

        }
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

    fun putBooleanData(key: String?, value: Boolean) {
        val mEditor = mSharedPreferences.edit()
        mEditor.putBoolean(key, value)
        mEditor.commit()
    }


    fun getBooleanData(key: String?): Boolean {
        return try {
            mSharedPreferences.getBoolean(key, false)
        } catch (ex: Exception) {
            ex.printStackTrace()
            true
        }
    }

    fun clearSharedPreferences(key:String) {
        val mEditor = mSharedPreferences.edit()
        mEditor.remove(key)
        mEditor.commit()
    }

    fun getSyncResponse(key: String?): IdentityServiceResponse? {
        val syncServiceResponse = try {
            val jsonString = getStringData(key)
            Gson().fromJson(jsonString, IdentityServiceResponse::class.java)
        } catch (e: Exception) {
            return null
        }
        return syncServiceResponse
    }

    fun getDeviceInformation(key: String?): CommonResponseBean? {
        val attributeResponse = try {
            val jsonString = getStringData(key)
            Gson().fromJson(jsonString, CommonResponseBean::class.java)
        } catch (e: Exception) {
            return null
        }
        return attributeResponse
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


}