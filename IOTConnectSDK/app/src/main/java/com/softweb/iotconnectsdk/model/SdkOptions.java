package com.softweb.iotconnectsdk.model;

import com.google.gson.annotations.SerializedName;

public class SdkOptions {

    @SerializedName("offlineStorage")
    private OfflineStorage offlineStorage;

    @SerializedName("certificate")
    private Certificate certificate;

    @SerializedName("discoveryUrl")
    private String discoveryUrl;

    @SerializedName("isDebug")
    private boolean isDebug = false;

    @SerializedName("skipValidation")
    private boolean skipValidation = false;

    public OfflineStorage getOfflineStorage() {
        return offlineStorage;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String discoveryUrl) {
        this.discoveryUrl = discoveryUrl;
    }

    public void setOfflineStorage(OfflineStorage offlineStorage) {
        this.offlineStorage = offlineStorage;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }


    public boolean isSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
    }
}