# IOT Connect SDK: iotconnect-Android-sdk(Device Message 1.0)

This is Android library to connect with IoTConnect cloud by MQTT
This library only abstract JSON responses from both end D2C and C2D 

## Features:

* The SDK supports to send telemetry data and receive commands from IoTConnect portal.
* User can update firmware Over The Air using "OTA update" Feature supported by SDK.
* SDK support SAS authentication as well as x509 certificate authentication.  
* SDK consists of Gateway device with multiple child devices support.
* SDK supports to receive and update the Twin property. 
* SDK supports device and OTA command acknowledgment.
* Edge device support with data aggregation.
* Provide device connection status receive by command.
* Support hard stop command to stop device client from cloud.
* It allows sending the OTA command acknowledgment for Gateway and child device.
* It manages the sensor data sending flow over the cloud by using data frequency("df") configuration.
* It allows to disconnect the device from firmware.

# Example Usage:
	
-Prerequisite input data *
```java	
  	String uniqueId = <<uniqueId>>;
	String cpId = <<CPID>>; 
	String environment = <<environment>>; 
```

"uniqueId" 	: Your device uniqueId
"cpId" 		: It is the company code. It gets from the IoTConnect UI portal "Settings->Key Vault"
"env" 		: It is the UI platform environment. It gets from the IoTConnect UI portal "Settings->Key Vault"

- SdkOptions is for the SDK configuration and needs to parse in SDK object initialize call. You need to manage the below configuration as per your device authentication type.
```json
	String sdkOptions = {
		"certificate" : {
			"SSLKeyPath"	: "<< SystemPath >>/device.key",
			"SSLCertPath"   : "<< SystemPath >>/device.pem",
			"SSLCaPath"     : "<< SystemPath >>/rootCA.pem"
		},
		"offlineStorage": { 
			"disabled": false, 
			"availSpaceInMb": 1, 
			"fileCount": 5 
		}
	}
```
"certificate": It is indicated to define the path of the certificate file. Mandatory for X.509/SSL device CA signed or self-signed authentication type only.
	- SSLKeyPath: your device key
	- SSLCertPath: your device certificate
	- SSLCaPath : Root CA certificate
"offlineStorage" : Define the configuration related to the off-line data storage 
	- disabled : false = off-line data storing, true = not storing off-line data 
	- availSpaceInMb : Define the file size of off-line data which should be in (MB)
	- fileCount : Number of files need to create for off-line data
Note: sdkOptions is optional but mandatory for SSL/x509 device authentication type only. Define proper setting or leave it NULL. 
If you do not provide off-line storage, it will set the default settings as per defined above. It may harm your device by storing the large data. Once memory gets full may chance to stop the execution.

	
- To Initialize the SDK object and connect to the cloud.
```java		
	SDKClient sdkClient = SDKClient.getInstance(Context, cpId, uniqueId, DeviceCallback, TwinUpdateCallback, sdkOptions, environment);
```

- To receive the command from Cloud to Device(C2D).	
```java		
	@Override
	public void onReceiveMsg(String message) {
        if (!message.isEmpty()) {
				JSONObject mainObject = new JSONObject(message);
                String cmdType = mainObject.getString("cmdType");
                JSONObject dataObj = mainObject.getJSONObject("data");
				
			 switch (cmdType) {
                   case "0x01":
                        // Device Command
                   break;

                   case "0x02":
                        // Firmware Command
                   break;

                   case "0x16":
                       // Device Connection status (command : true [connected] and command : false [disconnected])
                   break;

                   default:
                   break;
             }
		}
	}
```


- To receive the twin from Cloud to Device(C2D)
```java	
	@Override
    public void twinUpdateCallback(JSONObject data) {
		Log.d(TAG, data);
	}
```
	
- To get the list of attributes with respective device.
```java
	String data = sdkClient.getAttributes();
    Log.d("Attribute list device wise :", data);	
```

- This is the standard data input format for Gateway and non Gateway device to send the data on IoTConnect cloud(D2C).
```json
	// For Non Gateway Device 
	String data = [{
		"uniqueId": "<< Device UniqueId >>",
		"time" : "<< date >>",
		"data": {}
	}];

	// For Gateway and multiple child device 
	String data = [{
		"uniqueId": "<< Gateway Device UniqueId >>", // It should be must first object of the array
		"time": "<< date >>",
		"data": {}
	},
	{
		"uniqueId":"<< Child DeviceId >>", //Child device
		"time": "<< date >>",
		"data": {}
	}]
	sdkClient.sendData(String data);
```
"time" : Date format should be as defined //"2021-01-24T10:06:17.857Z" 
"data" : JSON data type format // {"temperature": 15.55, "gyroscope" : { 'x' : -1.2 }}

- To send the command acknowledgment from device to cloud.
```java
	JSONObject obj = new JSONObject("{
		"ackId": "",
		"st": "",
		"msg": "",
		"childId": ""
	}");
	
	String messageType = "";
	sdkClient.sendAck(JSONObject obj, String messageType)	
```

"ackId(*)" 	: Command Acknowledgment GUID which will receive from command payload (data.ackId)
"st(*)"		: Acknowledgment status sent to cloud (4 = Fail, 6 = Device command[0x01], 7 = Firmware OTA command[0x02])
"msg" 		: It is used to send your custom message
"childId" 	: It is used for Gateway's child device OTA update only
				0x01 : null or "" for Device command
			  	0x02 : null or "" for Gateway device and mandatory for Gateway child device's OTA update.
		   		How to get the "childId" .?
		   		- You will get child uniqueId for child device OTA command from payload "data.urls[~].uniqueId"
"msgType" 	: Message type (5 = "0x01" device command, 11 = "0x02" Firmware OTA command)
Note : (*) indicates the mandatory element of the object.

- To update the Twin Property.
```java
	String key = "<< Desired property key >>";
	String value = "<< Desired Property value >>";
	sdkClient.updateTwin(key,value)
```
"key" 	:	Desired property key received from Twin callback message
"value"	:	Value of the respective desired property

- To disconnect the device from the cloud
```java
	sdkClient.dispose()
```

- To get the all twin property Desired and Reported
```java
	sdkClient.getAllTwins();
```

- Disconnect iotConnect on Activity onDestroy
```java
	@Override
    protected void onDestroy() {
        super.onDestroy();
        if (sdkClient != null) {
            sdkClient.dispose();
        }
    }
```

# Dependencies:

1.Verify jCenter in your root build.gradle at the end of repositories.

```java
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

2.Add below dependency in build.gradle file.

```java	
	dependencies {
	        implementation 'com.iotconnectsdk:iotconnectpoc:3.1.4'		
	}
```	

## Prerequisite tools

- Java 8 ( or above )
- Android software development kit
- Android Studio


## Release Note :

** Improvements **
1. We have updated the below methods name:
   To Initialize the SDK object:
	- Old : new sdk(cpid, uniqueId, callbackMessage, twinCallbackMessage, env, sdkOptions);
	- New : SDKClient.getInstance(Activity.this, cpId, uniqueId, DeviceCallback, TwinUpdateCallback, sdkOptions, environment);
   To send the data :
    - Old : SendData(data)
    - New : sendData(data)
   To update the Twin Reported Property :
    - Old : UpdateTwin(key, value)
    - New : updateTwin(key, value)
   To receive Device command callback :
    - Old : callbackMessage(data);
	- New : deviceCallback(data);
   To receive OTA command callback :
    - Old : twinCallbackMessage(data);
	- New : twinUpdateCallback(data);
2. Update the OTA command receiver payload for multiple OTA files
3. Use the "df" Data Frequency feature to control the flow of data which publish on cloud (For Non-Edge device only).
