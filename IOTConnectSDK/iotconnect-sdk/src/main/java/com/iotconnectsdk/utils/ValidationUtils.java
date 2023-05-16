package com.iotconnectsdk.utils;

import android.content.Context;
import android.webkit.URLUtil;
import com.iotconnectsdk.R;
import com.iotconnectsdk.webservices.responsebean.DiscoveryApiResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ValidationUtils {

    private final Context context;
    private final boolean isDebug;
    private final IotSDKLogUtils iotSDKLogUtils;

    private static final ValidationUtils validationUtils = null;

    public static ValidationUtils getInstance(IotSDKLogUtils iotSDKLogUtils, Context context, boolean debug) {
        if (validationUtils == null)
            return new ValidationUtils(iotSDKLogUtils, context, debug);

        return validationUtils;
    }

    private ValidationUtils(IotSDKLogUtils iotSDKLogUtils, Context context, boolean debug) {
        this.iotSDKLogUtils = iotSDKLogUtils;
        this.context = context;
        this.isDebug = debug;
    }

    public boolean networkConnectionCheck() {
        if (!IotSDKNetworkUtils.isOnline(context)) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_IN08", context.getString(R.string.ERR_IN08));
            return false;
        }
        return true;
    }

    public boolean isEmptyValidation(String id, String errorCode, String message) {
        id = id.replaceAll("\\s+", "");
        if (id.isEmpty() || id.length() == 0) {
            iotSDKLogUtils.log(true, this.isDebug, errorCode, message);
            return false;
        }
        return true;
    }

    /*  Check discovery url is empty or not.
     * @ param discoveryUrl  discovery URL ("discoveryUrl" : "https://discovery.iotconnect.io")
     * */
    public boolean checkDiscoveryURL(String DISCOVERY_URL, JSONObject sdkObj) {

        try {

            if (sdkObj.has(DISCOVERY_URL)) {
                String discoveryUrl = sdkObj.getString(DISCOVERY_URL);

                if (discoveryUrl.isEmpty() || discoveryUrl.length() == 0) {
                    iotSDKLogUtils.log(true, this.isDebug, "ERR_IN02", context.getString(R.string.ERR_IN02));
                    return false;
                }
                if(!URLUtil.isValidUrl(discoveryUrl)){
                    iotSDKLogUtils.log(true, this.isDebug, "ERR_IN01", context.getString(R.string.ERR_IN01));
                    return false;
                }

            } else {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_IN03", context.getString(R.string.ERR_IN03));
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            iotSDKLogUtils.log(true, this.isDebug, "ERR_IN01", e.getMessage());
            return false;
        }

        return true;
    }

    public boolean validateBaseUrl(DiscoveryApiResponse discoveryApiResponse) {

        if (discoveryApiResponse.getBaseUrl() == null || discoveryApiResponse.getBaseUrl().length() == 0) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_IN09", context.getString(R.string.ERR_IN09));
            return false;
        }

        return true;
    }

    /*Validate user input format.
     * */
    public boolean isValidInputFormat(String jsonData, String uniqueId) {
        //check is input empty.
        if (jsonData.isEmpty() || jsonData.length() == 0) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_SD05", context.getString(R.string.ERR_SD05));
            return false;
        }

        //check the input json standard as JSONArray or not.
        try {

            JSONArray jsonArray = new JSONArray(jsonData);

            //compare gateway device uniqueId with input gateway device uniqueId.
            boolean isValidId = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                String inputUniqueId =  jsonArray.getJSONObject(i).getString("uniqueId");
                if(uniqueId.equals(inputUniqueId)){
                    isValidId = true;
                    break;
                }
            }

            if(!isValidId) {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_SD02", context.getString(R.string.ERR_SD02));
                return false;
            }

            if (jsonArray instanceof JSONArray) {

                if (jsonArray.length() > 0) {

                    for (int i = 0; i < jsonArray.length(); i++) {

                        if (!jsonArray.getJSONObject(i).has("time")) {
                            iotSDKLogUtils.log(true, this.isDebug, "ERR_SD07", context.getString(R.string.ERR_SD07));
                            return false;
                        } else {
                            String time = jsonArray.getJSONObject(i).getString("time");
                            String pattern = "^\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01])T([0-9]|0[0-9]|1?[0-9]|2[0-3]):[0-5]?[0-9]:[0-5]?[0-9].\\d{3}Z$";

                            if (!time.matches(pattern)) {
                                iotSDKLogUtils.log(true, this.isDebug, "ERR_SD03", context.getString(R.string.ERR_SD03));
                            }
                        }

                        if (!jsonArray.getJSONObject(i).has("uniqueId")) {
                            iotSDKLogUtils.log(true, this.isDebug, "ERR_SD08", context.getString(R.string.ERR_SD08));
                            return false;
                        }

                        if (!jsonArray.getJSONObject(i).has("data")) {
                            iotSDKLogUtils.log(true, this.isDebug, "ERR_SD06", context.getString(R.string.ERR_SD06));
                            return false;
                        }
                    }

                } else {
                    iotSDKLogUtils.log(true, this.isDebug, "ERR_SD05", context.getString(R.string.ERR_SD05));
                }


            } else {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_SD05", context.getString(R.string.ERR_SD05));
                return false;
            }
        } catch (JSONException e) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_SD04", context.getString(R.string.ERR_SD04));
            return false;
        }

        return true;
    }

    public boolean validateKeyValue(String key, String value) {

        key = key.replaceAll("\\s+", "");
        value = key.replaceAll("\\s+", "");
        if (key.length() == 0 || value.length() == 0) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_TP03", context.getString(R.string.ERR_TP03));
            return false;
        }

        return true;
    }


    public boolean validateAckParameters(JSONObject obj, String messageType) {
        if (obj == null || messageType.isEmpty()) {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_CM02", context.getString(R.string.ERR_CM02));
            return false;
        }

        if (obj instanceof JSONObject) {
            return true;
        } else {
            iotSDKLogUtils.log(true, this.isDebug, "ERR_CM03", context.getString(R.string.ERR_CM03));
            return false;
        }
    }

    public void responseCodeMessage(int rc) {
        switch (rc) {
            case 0:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN08", context.getString(R.string.INFO_IN08));
                break;
            case 1:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN09", context.getString(R.string.INFO_IN09));
                break;
            case 2:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN10", context.getString(R.string.INFO_IN10));
                break;
            case 3:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN11", context.getString(R.string.INFO_IN11));
                break;
            case 4:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN12", context.getString(R.string.INFO_IN12));
                break;
            case 5:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN13", context.getString(R.string.INFO_IN13));
                break;
            case 6:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN14", context.getString(R.string.INFO_IN14));
                break;
            default:
                iotSDKLogUtils.log(false, this.isDebug, "INFO_IN15", context.getString(R.string.INFO_IN15));
                break;

        }
    }
}
