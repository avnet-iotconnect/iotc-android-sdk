package com.softweb.iotconnectsdk.model;

import com.google.gson.annotations.SerializedName;

public class OfflineStorage{

	@SerializedName("availSpaceInMb")
	private int availSpaceInMb;

	@SerializedName("disabled")
	private boolean disabled = false;

	@SerializedName("fileCount")
	private int fileCount = 1;

	public int getAvailSpaceInMb(){
		return availSpaceInMb;
	}

	public boolean isDisabled(){
		return disabled;
	}

	public int getFileCount(){
		return fileCount;
	}

	public void setAvailSpaceInMb(int availSpaceInMb) {
		this.availSpaceInMb = availSpaceInMb;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void setFileCount(int fileCount) {
		this.fileCount = fileCount;
	}
}