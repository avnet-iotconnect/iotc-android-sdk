package com.iotconnectsdk.beans;

import com.google.gson.annotations.SerializedName;

public class Data{

	@SerializedName("ack")
	private Object ackId;

	@SerializedName("cpid")
	private String cpid;

	@SerializedName("ct")
	private String cmdType;

	@SerializedName("ackb")
	private boolean ack;

	@SerializedName("guid")
	private String guid;

	@SerializedName("uniqueId")
	private String uniqueId;

	@SerializedName("command")
	private String command;


	public void setAckId(Object ackId){
		this.ackId = ackId;
	}

	public Object getAckId(){
		return ackId;
	}

	public void setCpid(String cpid){
		this.cpid = cpid;
	}

	public String getCpid(){
		return cpid;
	}

	public void setCmdType(String cmdType){
		this.cmdType = cmdType;
	}

	public String getCmdType(){
		return cmdType;
	}

	public void setAck(boolean ack){
		this.ack = ack;
	}

	public boolean isAck(){
		return ack;
	}

	public void setGuid(String guid){
		this.guid = guid;
	}

	public String getGuid(){
		return guid;
	}

	public void setUniqueId(String uniqueId){
		this.uniqueId = uniqueId;
	}

	public String getUniqueId(){
		return uniqueId;
	}

	public void setCommand(String command){
		this.command = command;
	}

	public String getCommand(){
		return command;
	}
}