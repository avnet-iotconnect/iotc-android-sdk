package com.iotconnectsdk.beans;

import com.google.gson.annotations.SerializedName;

public class CommandFormatJson{

	@SerializedName("data")
	private Data data;

	@SerializedName("cmdType")
	private String cmdType;

	public void setData(Data data){
		this.data = data;
	}

	public Data getData(){
		return data;
	}

	public void setCmdType(String cmdType){
		this.cmdType = cmdType;
	}

	public String getCmdType(){
		return cmdType;
	}
}