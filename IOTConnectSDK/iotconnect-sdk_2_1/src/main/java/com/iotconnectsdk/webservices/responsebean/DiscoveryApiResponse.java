package com.iotconnectsdk.webservices.responsebean;

public class DiscoveryApiResponse {

    /**
     * baseUrl : http://agent.iotconnect.io/api/v1.1/agent/
     * logInfo : <logInfo><hostName/><user/><password/><topic/></logInfo>
     */

    private String baseUrl;
    private String logInfo;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getLogInfo() {
        return logInfo;
    }

    public void setLogInfo(String logInfo) {
        this.logInfo = logInfo;
    }
}
