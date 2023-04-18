package com.iotconnectsdk.mqtt;

import android.content.Context;

import com.iotconnectsdk.R;
import com.iotconnectsdk.interfaces.HubToSdkCallback;
import com.iotconnectsdk.interfaces.TwinUpdateCallback;
import com.iotconnectsdk.utils.IotSDKLogUtils;
import com.iotconnectsdk.utils.IotSDKUtils;
import com.iotconnectsdk.webservices.responsebean.SyncServiceResponse;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

/**
 * Service class for MQTT data upload to cloud
 */
public class IotSDKMQTTService {

    private static final String TAG = IotSDKMQTTService.class.getSimpleName();

    private MqttAndroidClient mqttAndroidClient;
    private final HubToSdkCallback mHubToSdkCallback;
    private final TwinUpdateCallback twinCallbackMessage;

    //- To receive the desired property and update the Reported Property
    private final String TWIN_PUB_TOPIC = "$iothub/twin/PATCH/properties/reported/?$rid=1";
    private final String TWIN_SUB_TOPIC = "$iothub/twin/PATCH/properties/desired/#";


    // - To Publish the blank message on publish and Subscribe message with desired and reported property
    private final String TWIN_PUB_TOPIC_BLANK_MSG = "$iothub/twin/GET/?$rid=0";
    private final String TWIN_SUB_TOPIC_BLANK_MSG = "$iothub/twin/res/#";

    private final String DESIRED = "desired";
    private final String UNIQUE_ID = "uniqueId";

    private final String TWIN_SUBTOPIC_CONTAINT = "$iothub/twin/";

    private String subscriptionTopic;// = "devices/520uta-sdk003/messages/devicebound/#";
    private String publishTopic;// = "devices/520uta-sdk003/messages/events/";

    private final IotSDKLogUtils iotSDKLogUtils;
    private final boolean isDebug;
    private final Context context;
    private final String uniqueId;
    private final SyncServiceResponse.DBeanXX.PBean protocolBean;


    private static IotSDKMQTTService iotSDKMQTTService;

    public static IotSDKMQTTService getInstance(Context context, SyncServiceResponse.DBeanXX.PBean protocolBean, HubToSdkCallback hubToSdkCallback, final TwinUpdateCallback twinCallbackMessage, IotSDKLogUtils iotSDKLogUtils, boolean isDebug, String uniqueId) {

        if (iotSDKMQTTService == null) {
            return iotSDKMQTTService = new IotSDKMQTTService(context, protocolBean, hubToSdkCallback, twinCallbackMessage, iotSDKLogUtils, isDebug, uniqueId);
        }
        return iotSDKMQTTService;
    }

    public void clearInstance() {
        iotSDKMQTTService = null;
    }

    private IotSDKMQTTService(Context context, SyncServiceResponse.DBeanXX.PBean protocolBean, HubToSdkCallback hubToSdkCallback, final TwinUpdateCallback twinCallbackMessage, IotSDKLogUtils iotSDKLogUtils, boolean isDebug, String uniqueId) {
        this.context = context;
        this.uniqueId = uniqueId;
        this.protocolBean = protocolBean;
        this.mHubToSdkCallback = hubToSdkCallback;
        this.twinCallbackMessage = twinCallbackMessage;
        this.iotSDKLogUtils = iotSDKLogUtils;
        this.isDebug = isDebug;
    }

    public void connectMQTT() {
        //init log.
        iotSDKLogUtils.log(false, this.isDebug, "INFO_IN04", context.getString(R.string.INFO_IN04));

        subscriptionTopic = protocolBean.getSub();
        publishTopic = protocolBean.getPub();

        mqttAndroidClient = new MqttAndroidClient(context, "ssl://" + this.protocolBean.getH() + ":" + this.protocolBean.getP(), this.protocolBean.getId());

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                //if (reconnect) {
                    // Because Clean Session is true, we need to re-subscribe
                    if (subscriptionTopic != null) {
                        subscribeToTopic();
                    }
                /*} else {
//                    addToHistory("Connected to: " + serverURI);
                }*/
            }

            @Override
            public void connectionLost(Throwable cause) {
                mHubToSdkCallback.onConnectionStateChange(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                try {
                    if (message.getPayload().toString().isEmpty()) {
                        return;
                    }

                    if (topic.contains(TWIN_SUB_TOPIC.substring(0, TWIN_SUB_TOPIC.length() - 1))) {
                        JSONObject mainObj = new JSONObject();
                        mainObj.put(DESIRED, new JSONObject(new String(message.getPayload())));
                        mainObj.put(UNIQUE_ID, uniqueId);

                        twinCallbackMessage.twinUpdateCallback(mainObj);

                    } else if (topic.contains(TWIN_SUB_TOPIC_BLANK_MSG.substring(0, TWIN_SUB_TOPIC_BLANK_MSG.length() - 1))) {

                        JSONObject mainObj = new JSONObject(new String(message.getPayload()));
                        mainObj.put(UNIQUE_ID, uniqueId);

                        twinCallbackMessage.twinUpdateCallback(mainObj);
                    } else {
                        mHubToSdkCallback.onReceiveMsg(new String(message.getPayload()));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(protocolBean.getUn());
        if (protocolBean.getPwd() != null) {
            mqttConnectOptions.setPassword(protocolBean.getPwd().toCharArray());
        }

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    mHubToSdkCallback.onConnectionStateChange(false);
                    iotSDKLogUtils.log(true, isDebug, "ERR_IN13", context.getString(R.string.ERR_IN13));
                }
            });

        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic() {
        iotSDKLogUtils.log(false, this.isDebug, "INFO_IN05", context.getString(R.string.INFO_IN05));
        try {
            String[] topics = {subscriptionTopic, TWIN_SUB_TOPIC, TWIN_SUB_TOPIC_BLANK_MSG};
            mqttAndroidClient.subscribe(topics, new int[]{0, 0, 0}, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mHubToSdkCallback.onConnectionStateChange(true);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    mHubToSdkCallback.onConnectionStateChange(false);
                }
            });

        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    public void disconnectClient() {
        if (mqttAndroidClient != null) {
            try {
                mqttAndroidClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }

            mHubToSdkCallback.onConnectionStateChange(false);
            mqttAndroidClient.unregisterResources();
//            mqttAndroidClient.close();
            mqttAndroidClient = null;
        }
    }

    public void getAllTwins() {
        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload("".getBytes());
                mqttAndroidClient.publish(TWIN_PUB_TOPIC_BLANK_MSG, message);


            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateTwin(String msgPublish) {

        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(msgPublish.getBytes());
                mqttAndroidClient.publish(TWIN_PUB_TOPIC, message);


                iotSDKLogUtils.log(false, this.isDebug, "INFO_TP01", context.getString(R.string.INFO_TP01) + " " + IotSDKUtils.getCurrentDate());
            } catch (MqttException e) {
                e.printStackTrace();
            }

        }
    }

    public void publishMessage(String msgPublish) {
        try {
            if (mqttAndroidClient != null && mqttAndroidClient.isConnected() && msgPublish != null) {
                MqttMessage message = new MqttMessage();
                message.setPayload(msgPublish.getBytes());
                mqttAndroidClient.publish(publishTopic, message);
                mHubToSdkCallback.onSendMsg(msgPublish);

                iotSDKLogUtils.log(false, this.isDebug, "INFO_SD01", context.getString(R.string.INFO_SD01) + " " + IotSDKUtils.getCurrentDate());
            } else {
                iotSDKLogUtils.log(true, this.isDebug, "ERR_SD10", context.getString(R.string.ERR_SD10) + " : " + IotSDKUtils.getCurrentDate());
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
