package com.softweb.iotconnectsdk.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AttributesModel {

    @SerializedName("device")
    @Expose
    private Device device;
    @SerializedName("attributes")
    @Expose
    private List<Attribute> attributes = null;

    @SerializedName("tags")
    @Expose
    private List<String> tags = null;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
