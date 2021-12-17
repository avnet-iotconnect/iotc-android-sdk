package com.iotconnectsdk.interfaces;

public interface HubToSdkCallback {
    void onReceiveMsg(String message);

    void onSendMsg(String message);

    void onConnectionStateChange(boolean isConnected);
}
