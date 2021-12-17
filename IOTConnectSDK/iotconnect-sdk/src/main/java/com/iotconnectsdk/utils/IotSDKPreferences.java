package com.iotconnectsdk.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores Application Preference Data
 */

public class IotSDKPreferences {

    private static final String APPLICATION = "IotConnectSDK";
    public static final String SYNC_API = "SYNC_API";
    public static final String SYNC_RESPONSE = "sync_response";
    public static final String TEXT_FILE_NAME = "text_file";

    private static SharedPreferences mSharedPreferences;
    private static IotSDKPreferences iotSDKPreferences;

    public static IotSDKPreferences getInstance(Context context) {
        if (iotSDKPreferences == null)
            iotSDKPreferences = new IotSDKPreferences(context);
        return iotSDKPreferences;
    }

    private IotSDKPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(APPLICATION, Context.MODE_PRIVATE);
    }

    public boolean putStringData(String key, String value) {
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString(key, value);
        mEditor.commit();
        return true;
    }

    public String getStringData(String key) {
        try {
            return mSharedPreferences.getString(key, null);
        } catch (Exception e) {
            return null;
        }
    }

    public SyncServiceResponse getSyncResponse(String key) {
        SyncServiceResponse syncServiceResponse = null;
        try {
            String jsonString = getStringData(key);
            syncServiceResponse = new Gson().fromJson(jsonString, SyncServiceResponse.class);
        } catch (Exception e) {
            return null;
        }
        return syncServiceResponse;
    }

    public boolean saveList(String key, List<String> valueList) {

        String value = new Gson().toJson(valueList);

        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString(key, value);
        mEditor.commit();
        return true;
    }

    public List<String> getList(String key) {
        try {
            List<String> list = new ArrayList<>();

            String value = mSharedPreferences.getString(key, null);
            if (value != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>() {
                }.getType();
                list = gson.fromJson(value, type);
            }

            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
