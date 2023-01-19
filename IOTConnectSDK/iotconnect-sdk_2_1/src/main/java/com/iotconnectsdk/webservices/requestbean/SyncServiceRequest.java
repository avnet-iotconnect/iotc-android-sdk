package com.iotconnectsdk.webservices.requestbean;

public class SyncServiceRequest {

    /**
     * cpId : ""
     * uniqueId : ""
     * option : {"attribute":true,"setting":true,"protocol":true,"device":true}
     */

    private String cpId;
    private String uniqueId;
    private OptionBean option;

    public String getCpId() {
        return cpId;
    }

    public void setCpId(String cpId) {
        this.cpId = cpId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public OptionBean getOption() {
        return option;
    }

    public void setOption(OptionBean option) {
        this.option = option;
    }

    public static class OptionBean {
        /**
         * attribute : true
         * setting : true
         * protocol : true
         * device : true
         */

        private boolean attribute;
        private boolean setting;
        private boolean protocol;
        private boolean device;
        private boolean sdkConfig;
        private boolean rule;

        public boolean isAttribute() {
            return attribute;
        }

        public void setAttribute(boolean attribute) {
            this.attribute = attribute;
        }

        public boolean isSetting() {
            return setting;
        }

        public void setSetting(boolean setting) {
            this.setting = setting;
        }

        public boolean isProtocol() {
            return protocol;
        }

        public void setProtocol(boolean protocol) {
            this.protocol = protocol;
        }

        public boolean isDevice() {
            return device;
        }

        public void setDevice(boolean device) {
            this.device = device;
        }

        public boolean isSdkConfig() {
            return sdkConfig;
        }

        public void setSdkConfig(boolean sdkConfig) {
            this.sdkConfig = sdkConfig;
        }

        public boolean isRule() {
            return rule;
        }

        public void setRule(boolean rule) {
            this.rule = rule;
        }
    }
}
