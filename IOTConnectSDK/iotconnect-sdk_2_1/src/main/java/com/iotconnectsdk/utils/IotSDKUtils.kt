package com.iotconnectsdk.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

object IotSDKUtils {
    /*   public static String getAppVersion() {
//            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        String versionName = com.iotconnectsdk.BuildConfig.VERSION_NAME;
        return versionName;
    }*/

    @JvmStatic
    val currentDate: String
        get() {
            @SuppressLint("SimpleDateFormat") val df =
                SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")
            df.timeZone = TimeZone.getTimeZone("gmt")
            return df.format(Date())
        }
}