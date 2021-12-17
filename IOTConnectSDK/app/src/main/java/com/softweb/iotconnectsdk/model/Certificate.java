package com.softweb.iotconnectsdk.model;

import com.google.gson.annotations.SerializedName;

public class Certificate{

	@SerializedName("SSLCaPath")
	private String sSLCaPath;

	@SerializedName("SSLKeyPath")
	private String sSLKeyPath;

	@SerializedName("SSLCertPath")
	private String sSLCertPath;

	public String getSSLCaPath(){
		return sSLCaPath;
	}

	public String getSSLKeyPath(){
		return sSLKeyPath;
	}

	public String getSSLCertPath(){
		return sSLCertPath;
	}

	public void setsSLCaPath(String sSLCaPath) {
		this.sSLCaPath = sSLCaPath;
	}

	public void setsSLKeyPath(String sSLKeyPath) {
		this.sSLKeyPath = sSLKeyPath;
	}

	public void setsSLCertPath(String sSLCertPath) {
		this.sSLCertPath = sSLCertPath;
	}
}