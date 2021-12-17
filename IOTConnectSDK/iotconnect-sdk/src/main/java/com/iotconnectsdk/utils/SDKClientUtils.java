package com.iotconnectsdk.utils;

import static com.iotconnectsdk.utils.IotSDKConstant.ATTRIBUTE_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.DEVICE_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.M_ANDROID;
import static com.iotconnectsdk.utils.IotSDKConstant.PASSWORD_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.RULE_INFO_UPDATE;
import static com.iotconnectsdk.utils.IotSDKConstant.SETTING_INFO_UPDATE;

import com.google.gson.Gson;
import com.iotconnectsdk.beans.CommandFormatJson;
import com.iotconnectsdk.beans.Data;
import com.iotconnectsdk.beans.TumblingWindowBean;
import com.iotconnectsdk.webservices.requestbean.SyncServiceRequest;
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SDKClientUtils {

    private static final String CP_ID = "cpId";
    private static final String MESSAGE_TYPE = "mt";

    private static final String LANGUAGE = "l";
    private static final String VERSION = "v";
    private static final String ENVIRONMENT = "e";
    private static final String SDK_OBJ = "sdk";
    private static final String DTG = "dtg";
    private static final String CURRENT_DATE = "t";
    private static final String DEVICE_ID = "id";
    private static final String DT = "dt";
    private static final String DEVICE_TAG = "tg";
    private static final String D_OBJ = "d";
  /*  private static final String ACK_ID = "ackId";
    private static final String ACK = "ack";
    private static final String CMD_TYPE = "cmdType";
    private static final String GU_ID = "guid";
    private static final String UNIQUE_ID = "uniqueId";
    private static final String DATA = "data";
    private static final String COMMAND = "command";*/

    private static final int EDGE_DEVICE_MESSAGE_TYPE = 2;

    public static SyncServiceRequest getSyncServiceRequest(String cpId, String uniqueId, String cmdType) {
        SyncServiceRequest syncServiceRequest = new SyncServiceRequest();
        syncServiceRequest.setCpId(cpId);
        syncServiceRequest.setUniqueId(uniqueId);
        SyncServiceRequest.OptionBean optionBean = new SyncServiceRequest.OptionBean();
        optionBean.setAttribute(false);
        optionBean.setDevice(false);
        optionBean.setProtocol(false);
        optionBean.setSetting(false);
        optionBean.setSdkConfig(false);
        optionBean.setRule(false);
        if (cmdType == null) {
            optionBean.setAttribute(true);
            optionBean.setDevice(true);
            optionBean.setProtocol(true);
            optionBean.setSetting(true);
            optionBean.setSdkConfig(true);
            optionBean.setRule(true);
        } else if (cmdType.equalsIgnoreCase(ATTRIBUTE_INFO_UPDATE)) {
            optionBean.setAttribute(true);
        } else if (cmdType.equalsIgnoreCase(DEVICE_INFO_UPDATE)) {
            optionBean.setDevice(true);
        } else if (cmdType.equalsIgnoreCase(PASSWORD_INFO_UPDATE)) {
            optionBean.setProtocol(true);
        } else if (cmdType.equalsIgnoreCase(SETTING_INFO_UPDATE)) {
            optionBean.setSetting(true);
        } else if (cmdType.equalsIgnoreCase(RULE_INFO_UPDATE)) {
            optionBean.setRule(true);
        }
        syncServiceRequest.setOption(optionBean);
        return syncServiceRequest;
    }


    public static JSONArray getAttributesList(List<SyncServiceResponse.DBeanXX.AttBean> attributesLists, String tag) {

        //CREATE ATTRIBUTES ARRAY and OBJECT, "attributes":[{"ln":"Temp","dt":"number","dv":"5 to 20, 25","tg":"gateway","tw":"60s"},{"p":"gyro","dt":"object","tg":"gateway","tw":"90s","d":[{"ln":"x","dt":"number","dv":"","tg":"gateway","tw":"90s"},{"ln":"y","dt":"string","dv":"red, gray,   blue","tg":"gateway","tw":"90s"},{"ln":"z","dt":"number","dv":"-5 to 5, 10","tg":"gateway","tw":"90s"}]}]

        JSONArray attributesArray = new JSONArray();
        for (SyncServiceResponse.DBeanXX.AttBean attribute : attributesLists) {

//           if for not empty "p":"gyro"
            if (attribute.getP() != null && !attribute.getP().isEmpty()) {

                if (tag.equals(attribute.getTg())) {
                    try {
                        JSONObject attributeObj = new JSONObject(new Gson().toJson(attribute));
                        attributesArray.put(attributeObj);
                    } catch (Exception e) {
                    }
                }

            } else {
                // "p" : "", is empty.
                List<SyncServiceResponse.DBeanXX.AttBean.DBeanX> attributeValues = attribute.getD();

                for (SyncServiceResponse.DBeanXX.AttBean.DBeanX attributeValue : attributeValues) {

                    if (tag.equals(attributeValue.getTg())) {
                        try {
                            JSONObject attributeObj = new JSONObject(new Gson().toJson(attributeValue));
                            attributesArray.put(attributeObj);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        return attributesArray;
    }

    public static JSONObject getSdk(String environment, String appVersion) {
        //sdk object
        JSONObject objSdk = new JSONObject();
        try {
            objSdk.put(ENVIRONMENT, environment);
            objSdk.put(LANGUAGE, M_ANDROID);
            objSdk.put(VERSION, appVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return objSdk;
    }

    public static JSONObject getMainObject(String reportingORFaulty, SyncServiceResponse.DBeanXX dObj, String appVersion, String environment) {
        JSONObject obj = new JSONObject();

        try {
            obj.put(SDK_OBJ, getSDKObject(appVersion, environment));
            obj.put(CP_ID, dObj.getCpId());
            obj.put(DTG, dObj.getDtg());
            obj.put(MESSAGE_TYPE, reportingORFaulty); // 0 for reporting 1 for faulty.
        } catch (JSONException e) {
            e.printStackTrace();
//            iotSDKLogUtils.log(true, this.isDebug, "SD01", e.getMessage());
        }
        return obj;
    }

    private static JSONObject getSDKObject(String appVersion, String environment) {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(ENVIRONMENT, environment);
            jsonObj.put(LANGUAGE, M_ANDROID);
            jsonObj.put(VERSION, appVersion);
        } catch (JSONException e) {
            e.printStackTrace();
//            iotSDKLogUtils.log(true, this.isDebug, "SD01", e.getMessage());
        }
        return jsonObj;
    }

    /* function to get device tag. by comparing unique id.
     *
     *@param uniqueId  device unique id.
     * */
    public static String getTag(String uniqueId, SyncServiceResponse.DBeanXX dObj) {
        String tag = "";
        List<SyncServiceResponse.DBeanXX.DBean> values = dObj.getD();
        for (SyncServiceResponse.DBeanXX.DBean data : values) {
            if (uniqueId.equalsIgnoreCase(data.getId())) {
                tag = data.getTg();
                break;
            }
        }
        return tag;
    }

    public static synchronized int compareForInputValidation(String key, String value, String tag, SyncServiceResponse.DBeanXX dObj) {

        int result = 0;

        List<SyncServiceResponse.DBeanXX.AttBean> attributesList = dObj.getAtt();

        outerloop:
        for (int i = 0; i < attributesList.size(); i++) {

            List<SyncServiceResponse.DBeanXX.AttBean.DBeanX> dataBeanList = attributesList.get(i).getD();
            for (int j = 0; j < dataBeanList.size(); j++) {

                SyncServiceResponse.DBeanXX.AttBean.DBeanX data = dataBeanList.get(j);
                String ln = data.getLn();
                String tg = data.getTg();
                String dv = data.getDv();
                int dt = data.getDt();

                if (key.equalsIgnoreCase(ln) && tag.equalsIgnoreCase(tg)) {
                    if (dt == 0 && !value.isEmpty() && !isDigit(value)) {
                        result = 1;
                    } else {
                        result = compareWithInput(value, dv);
                    }

                    break outerloop;
                }
            }
        }
        return result;
    }

    /*
     * return 0 = reporting, return 1 = faulty.
     *
     *  {
            "agt": 0,
            "dt": 0,
            "dv": "5 to 20, 25",
            "ln": "Temp",
            "sq": 1,
            "tg": "gateway",
            "tw": ""
          },
     * */
    private static int compareWithInput(String inputValue, String validationValue) {

        validationValue = validationValue.replaceAll("\\s", "");

        // "dv": ""
        if (validationValue.isEmpty())
            return 0;


        //  "dv": "5 to 20, 25",   compare between and value.
        if (validationValue.contains("to") && validationValue.contains(",")) {

            //convert string to integer type to compare between value.
            int comparWith = 0;
            try {
                comparWith = Integer.parseInt(inputValue);
            } catch (Exception e) {
                return 1; // return faulty (input value is not an int type, so we can not campare with between.)
            }

            validationValue = validationValue.replace("to", ",");
            String[] array = validationValue.split(",");

            // "dv": "5 to 20, 25",  compare with 25
            if (array.length == 3 && array[2].equalsIgnoreCase(inputValue))
                return 0;

            // "dv": "5 to 20, 25",  compare between 5 to 20
            int from = Integer.parseInt(array[0]);
            int to = Integer.parseInt(array[1]);
            if (array.length > 1 && comparWith >= from && comparWith <= to) {
                return 0;
            } else {
                return 1;
            }
        }


        // "dv": "30 to 50",
        if (validationValue.contains("to")) {

            //convert string to integer type to compare between value.
            int comparWith = 0;
            try {
                comparWith = Integer.parseInt(inputValue);
            } catch (Exception e) {
                return 1; // return faulty (input value is not an int type, so we can not campare with between.)
            }

            validationValue = validationValue.replace("to", ",");
            String[] array = validationValue.split(",");

            // "dv": "5 to 20",  compare between 5 to 20
            int from = Integer.parseInt(array[0]);
            int to = Integer.parseInt(array[1]);
            if (array.length > 1 && comparWith >= from && comparWith <= to) {
                return 0;
            } else {
                return 1;
            }
        }

        //"dv": "red, gray,   blue",
        if (validationValue.contains(",")) {
            int result = 1;
            String[] array = validationValue.split(",");

            for (int i = 0; i < array.length; i++) {
                if (inputValue.equalsIgnoreCase(array[i])) {
                    result = 0;
                    break;
                }
            }

            return result;
        }

        //"dv": "red",  OR "dv":"5"
        if (inputValue.equalsIgnoreCase(validationValue)) {
            return 0;
        } else {
            return 1;
        }
    }


    public static void updateEdgeDeviceGyroObj(String key, String innerKey, String value, Map<String, List<TumblingWindowBean>> edgeDeviceAttributeGyroMap) {

        for (Map.Entry<String, List<TumblingWindowBean>> entry : edgeDeviceAttributeGyroMap.entrySet()) {
            if (entry.getKey().equals(key)) {

                int inputValue = Integer.parseInt(value);
                List<TumblingWindowBean> tlbList = entry.getValue();

                for (TumblingWindowBean bean : tlbList) {

                    if (innerKey.equals(bean.getAttributeName())) {
                        SDKClientUtils.setObjectValue(bean, inputValue);
                    }
                }
            }
        }
    }

    public static void updateEdgeDeviceObj(String key, String value, Map<String, TumblingWindowBean> edgeDeviceAttributeMap) {
        for (Map.Entry<String, TumblingWindowBean> entry : edgeDeviceAttributeMap.entrySet()) {
            if (entry.getKey().equals(key)) {

                int inputValue = Integer.parseInt(value);
                TumblingWindowBean tlb = entry.getValue();
                SDKClientUtils.setObjectValue(tlb, inputValue);
            }
        }
    }


    /*Publish Edge Device data based on attributes Tumbling window time in seconds ("tw": "15s")
     *
     * @Param attributeName     Attribute name (humidity etc...)
     * */
    public static JSONObject publishEdgeDeviceInputData(String attributeName, String tag, Map<String, List<TumblingWindowBean>> edgeDeviceAttributeGyroMap, Map<String, TumblingWindowBean> edgeDeviceAttributeMap, String uniqueId, String cpId, String environment, String appVersion, String dtg) {


        String currentTime = IotSDKUtils.getCurrentDate();

        JSONObject mainObj = SDKClientUtils.getEdgeDevicePublishMainObj(currentTime, dtg, cpId, environment, appVersion, EDGE_DEVICE_MESSAGE_TYPE);
        JSONArray dArray = new JSONArray();
        JSONObject dArrayObject = SDKClientUtils.getEdgeDevicePublishDObj(currentTime, tag, uniqueId);
        JSONArray dInnerArray = new JSONArray();
        JSONObject gyroObj = new JSONObject();
        JSONObject dInnerArrayObject = new JSONObject();
        //for gyro object
        for (Map.Entry<String, List<TumblingWindowBean>> entry : edgeDeviceAttributeGyroMap.entrySet()) {

            String key = entry.getKey();
            List<TumblingWindowBean> twbList = entry.getValue();

            if (attributeName.equals(key)) {
                try {
                    JSONArray attributeArray = null;
                    for (TumblingWindowBean twb : twbList) {
                        attributeArray = SDKClientUtils.getEdgeDevicePublishAttributes(twb);
                        if (attributeArray.length() > 0) {
                            dInnerArrayObject.put(twb.getAttributeName(), attributeArray);
                        }
                        SDKClientUtils.clearObject(twb);
                    }

                    if (dInnerArrayObject.length() > 0) {
                        gyroObj.put(attributeName, dInnerArrayObject);
                        dInnerArray.put(gyroObj);
                    }
                    dArrayObject.put(D_OBJ, dInnerArray);
                    dArray.put(dArrayObject);
                    mainObj.put(D_OBJ, dArray);
                    return mainObj;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        //for simple object
        for (Map.Entry<String, TumblingWindowBean> entry : edgeDeviceAttributeMap.entrySet()) {

            if (entry.getKey().equals(attributeName)) {
                TumblingWindowBean twb = entry.getValue();

                try {

                    JSONArray attributeArray = SDKClientUtils.getEdgeDevicePublishAttributes(twb);
                    if (attributeArray.length() > 0) {
                        dInnerArrayObject.put(attributeName, attributeArray);
                        dInnerArray.put(dInnerArrayObject);
                    }

                    dArrayObject.put(D_OBJ, dInnerArray);
                    dArray.put(dArrayObject);
                    mainObj.put(D_OBJ, dArray);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SDKClientUtils.clearObject(twb);
                return mainObj;
            }
        }
        return null;
    }

    public static JSONObject getEdgeDevicePublishMainObj(String currentTime, String dtg, String cpId, String environment, String appVersion, int messageType) {

        JSONObject mainObj = new JSONObject();
        try {
            mainObj.put(CP_ID, cpId);
            mainObj.put(DTG, dtg);
            mainObj.put(CURRENT_DATE, currentTime);
            mainObj.put(MESSAGE_TYPE, messageType);
            mainObj.put(SDK_OBJ, getSdk(environment, appVersion));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mainObj;
    }

    private static JSONObject getEdgeDevicePublishDObj(String currentTime, String tag, String uniqueId) {
        JSONObject dArrayObject = new JSONObject();
        try {
            dArrayObject.put(DEVICE_ID, uniqueId);
            dArrayObject.put(DT, currentTime);
            dArrayObject.put(DEVICE_TAG, tag);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return dArrayObject;
    }

    private static JSONArray getEdgeDevicePublishAttributes(TumblingWindowBean twb) {
        JSONArray attributeArray = new JSONArray();
        try {

            if (twb.getMin() != 0 || twb.getMax() != 0 || twb.getSum() != 0 || twb.getAvg() != 0 || twb.getCount() != 0 || twb.getLv() != 0) {
                attributeArray.put(twb.getMin());
                attributeArray.put(twb.getMax());
                attributeArray.put(twb.getSum());
                attributeArray.put(twb.getAvg());
                attributeArray.put(twb.getCount());
                attributeArray.put(twb.getLv());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return attributeArray;
    }

    public static void setObjectValue(TumblingWindowBean bean, int inputValue) {
        int oldMin = bean.getMin();
        if (oldMin == 0 || inputValue < oldMin)
            bean.setMin(inputValue);

        int oldMax = bean.getMax();
        if (inputValue > oldMax)
            bean.setMax(inputValue);

        int sum = inputValue + bean.getSum();
        bean.setSum(sum);

        int count = bean.getCount();
        count++;

        int avg = (int) (sum / count);
        bean.setAvg(avg);

        bean.setCount(count);

        bean.setLv(inputValue);
    }

    private static void clearObject(TumblingWindowBean twb) {
        //clear object on publish success.
        twb.setMin(0);
        twb.setMax(0);
        twb.setSum(0);
        twb.setAvg(0);
        twb.setCount(0);
        twb.setLv(0);
    }

    public static boolean isDigit(String value) {
        value = value.replaceAll("\\s", "");
        return value.matches("\\d+(?:\\.\\d+)?");
    }

    public static String createCommandFormat(String commandType, String cpId, String guid, String uniqueId, String command, boolean ack, String ackId) {

        CommandFormatJson cfj = new CommandFormatJson();
        cfj.setCmdType(commandType);
        Data data = new Data();
        data.setCpid(cpId);
        data.setGuid(guid);
        data.setUniqueId(uniqueId);
        data.setCommand(command);
        data.setAck(ack);
        data.setAckId(ackId);
        data.setCmdType(commandType);
        cfj.setData(data);

        return new Gson().toJson(cfj);
    }


    public static String getAttributeName(String con) {

        //"ac1#vibration.x > 5 AND ac1#vibration.y > 10",
        //gyro#vibration.x > 5 AND gyro#vibration.y > 10
        try {
            //gyro#vibration.x > 5 AND gyro#vibration.y > 10
            if (con.contains("#") && con.contains("AND")) {
                String[] param = con.split("AND");
                for (int i = 0; i <= param.length; i++) {
                    String att = param[i];
                    if (att.contains(".")) {           //gyro#vibration.x > 5
                        String KeyValue[] = att.split("\\.");
                        String parent[] = KeyValue[0].split("#"); //gyro#vibration
                        String parentAttName = parent[0]; //gyro
//                        String childAttName = parent[1]; //vibration
//                        String keyAttName = KeyValue[1]; //x > 5

                        return parentAttName;
                    } else if (con.contains("#")) {                    //gyro#x > 5

                        String parent[] = att.split("#"); //gyro#x > 5
                        String parentAttName = parent[0]; //gyro
//                        String childAttName = parent[1]; //x > 5
                        return parentAttName;

                    }
                }
            } else if (con.contains("#")) {     //ac1#vibration.x > 5

                String KeyValue[] = con.split("\\.");
                String parent[] = KeyValue[0].split("#"); //gyro#vibration
                String parentAttName = parent[0]; //gyro
                String childAttName = parent[1]; //vibration

                String keyAttName = KeyValue[1]; //x > 5
                return SDKClientUtils.getAttName(keyAttName);

            } else if (con.contains(".")) {
                String keyValue[] = con.split("\\.");
                return keyValue[0];
            } else {    //x > 5
                return SDKClientUtils.getAttName(con);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /*
     * */
    // temp != 15
    // temp >= 15
    // temp <= 15
    // temp = 15
    // temp > 15
    // temp < 15

    private static final String EQUAL_TO = "=";
    private static final String NOT_EQUAL_TO = "!=";
    private static final String GREATER_THAN = ">";
    private static final String GREATER_THAN_OR_EQUAL_TO = ">=";
    private static final String LESS_THAN = "<";
    private static final String LESS_THAN_OR_EQUAL_TO = "<=";

    public static String getAttName(String con) {
        try {
            if (con.contains(NOT_EQUAL_TO)) {
                return getRuleAttName(con, NOT_EQUAL_TO);

            } else if (con.contains(GREATER_THAN_OR_EQUAL_TO)) {
                return getRuleAttName(con, GREATER_THAN_OR_EQUAL_TO);

            } else if (con.contains(LESS_THAN_OR_EQUAL_TO)) {
                return getRuleAttName(con, LESS_THAN_OR_EQUAL_TO);

            } else if (con.contains(EQUAL_TO)) {
                return getRuleAttName(con, EQUAL_TO);

            } else if (con.contains(GREATER_THAN)) {
                return getRuleAttName(con, GREATER_THAN);

            } else if (con.contains(LESS_THAN)) {
                return getRuleAttName(con, LESS_THAN);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private static String getRuleAttName(String con, String operator) {
        String att[] = con.split(operator);
        return (att[0].replaceAll("\\s", ""));
    }

    public static boolean evaluateEdgeDeviceRuleValue(String con, int inputValue) {

        try {
            if (con.contains(NOT_EQUAL_TO)) {
                if (inputValue != SDKClientUtils.getRuleValue(con, NOT_EQUAL_TO)) {
                    return true;
                }

            } else if (con.contains(GREATER_THAN_OR_EQUAL_TO)) {
                if (inputValue >= SDKClientUtils.getRuleValue(con, GREATER_THAN_OR_EQUAL_TO)) {
                    return true;
                }

            } else if (con.contains(LESS_THAN_OR_EQUAL_TO)) {
                if (inputValue <= SDKClientUtils.getRuleValue(con, LESS_THAN_OR_EQUAL_TO)) {
                    return true;
                }

            } else if (con.contains(EQUAL_TO)) {
                if (inputValue == SDKClientUtils.getRuleValue(con, EQUAL_TO)) {
                    return true;
                }

            } else if (con.contains(GREATER_THAN)) {
                if (inputValue > SDKClientUtils.getRuleValue(con, GREATER_THAN)) {
                    return true;
                }

            } else if (con.contains(LESS_THAN)) {
                if (inputValue < SDKClientUtils.getRuleValue(con, LESS_THAN)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    private static int getRuleValue(String con, String operator) {
        String att[] = con.split(operator);
        return Integer.parseInt(att[1].replaceAll("\\s", ""));
    }


    /*create bellow json and publish on edge device rule matched.
     * {"cpId":"uei","dtg":"b55d6d86-5320-4b26-8df2-b65e3221385e","t":"2020-11-25T12:56:34.487Z","mt":3,"sdk":{"e":"qa","l":"M_android","v":"2.0"},"d":[{"id":"AAA02","dt":"2020-11-25T12:56:34.487Z","rg":"3A171114-4CC4-4A1C-924C-D3FCF84E4BD1","ct":"gyro.x = 10 AND gyro.y > 10 AND gyro.z < 10","sg":"514076B1-3C21-4849-A777-F423B1821FC7","d":[{"temp":"10","gyro":{"x":"10","y":"11","z":"9"}}],"cv":{"gyro":{"x":"10","y":"11","z":"9"}}}]}
     * */
    public static JSONObject getPublishStringEdgeDevice(String uniqueId, String currentTime, SyncServiceResponse.DBeanXX.RuleBean bean, String inputJsonString, JSONObject cvAttObj, JSONObject mainObj) {

        try {

            JSONArray dArray = new JSONArray();
            JSONObject dObj = new JSONObject();
            dObj.put("id", uniqueId);
            dObj.put("dt", currentTime);
            dObj.put("rg", bean.getG());
            dObj.put("ct", bean.getCon());
            dObj.put("sg", bean.getEs());

            JSONArray innerDArray = new JSONArray();
            innerDArray.put(getAttFromInput(inputJsonString));

            dObj.put("d", innerDArray);

            dObj.put("cv", cvAttObj);

            dArray.put(dObj);
            mainObj.put("d", dArray);

            return mainObj;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
//        return null;
    }


    private static JSONObject getAttFromInput(String jsonData) {
        JSONObject dObj = new JSONObject();
        try {

            //parse input json
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject dataObj = jsonArray.getJSONObject(i).getJSONObject("data");
                Iterator<String> dataJsonKey = dataObj.keys();

                while (dataJsonKey.hasNext()) {
                    String key = dataJsonKey.next();
                    String value = dataObj.getString(key);

                    if (!value.replaceAll("\\s", "").isEmpty() && (new JSONTokener(value).nextValue()) instanceof JSONObject) {

                        JSONObject gyro = new JSONObject();

                        // get value for
                        // "gyro": {"x":"7","y":"8","z":"9"}
                        JSONObject innerObj = dataObj.getJSONObject(key);
                        Iterator<String> innerJsonKey = innerObj.keys();
                        while (innerJsonKey.hasNext()) {

                            String innerKey = innerJsonKey.next();
                            String innerKValue = innerObj.getString(innerKey);
                            //ignore string value for edge device.
                            if (SDKClientUtils.isDigit(innerKValue)) {
                                gyro.put(innerKey, innerKValue);
                            }
                        }

                        if (gyro.length() != 0)
                            dObj.put(key, gyro);

                    } else {
                        //ignore string value for edge device.
                        if (SDKClientUtils.isDigit(value)) {

                            dObj.put(key, value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return dObj;
    }

    public static long getFileSizeInKB(File file) {
        long fileSizeInBytes = file.length();
        return (fileSizeInBytes / 1024); //KB

        //        return (fileSizeInBytes / (1024 * 1024)); //MB
    }
}
