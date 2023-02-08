package com.iotconnectsdk.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

internal object IotSDKUtils {
    /*   public static String getAppVersion() {
//            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        String versionName = com.iotconnectsdk.BuildConfig.VERSION_NAME;
        return versionName;
    }*/

    val currentDate: String
        get() {
            @SuppressLint("SimpleDateFormat") val df =
                SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")
            df.timeZone = TimeZone.getTimeZone("gmt")
            return df.format(Date())
        }

     fun getCurrentTime(): Long {
        return System.currentTimeMillis() / 1000
    }
}