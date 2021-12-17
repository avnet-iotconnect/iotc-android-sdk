package com.iotconnectsdk.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class IotSDKUtils {

    public static String getCurrentDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("gmt"));
        return df.format(new Date());
    }

 /*   public static String getAppVersion() {
//            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        String versionName = com.iotconnectsdk.BuildConfig.VERSION_NAME;
        return versionName;
    }*/
}
