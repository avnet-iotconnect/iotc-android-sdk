# IOT Connect SDK: iotconnect-Android-sdk(Device Message 2.1)

This is Android library to connect with IoTConnect cloud by MQTT
This library only abstract JSON responses from both end D2C and C2D 

# Features:

* The SDK supports to send telemetry data and receive commands from IoTConnect portal.
* User can update firmware Over The Air using "OTA update" Feature supported by SDK.
* SDK support SAS authentication as well as Certificate Authority(CA) Signed and Self Signed authentication.  
* SDK consists of Gateway device with multiple child devices support.
* SDK supports to receive and update the Twin property. 
* SDK supports device and OTA command acknowledgment.
* Edge device support with data aggregation.
* Provide device connection status receive by command.
* Support hard stop command to stop device client from cloud.
* It allows sending the OTA command acknowledgment for Gateway and child device.
* It manages the sensor data sending flow over the cloud by using data frequency("df") configuration.
* It allows to disconnect the device from firmware.
* Offline Storage is used to store device data in a text file when the internet is not available.


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
- For AZ Platform
  
```java	
	dependencies {
	       implementation 'com.github.avnet-iotconnect:iotc-android-sdk:12.2.1-azTest'
	}
```

- For AWS Platform

```java	
	dependencies {
	      implementation 'com.github.avnet-iotconnect:iotc-android-sdk:12.2.1-awsTest'
	}
```	


# Third Party Frameworks Used

- [Retrofit](https://github.com/square/retrofit) for HTTP networking
- [Retrofit Json Converter](https://github.com/square/retrofit) for json converter
- [OkHttp](https://github.com/square/okhttp) is an HTTP client 
- [OkHttp Interceptor](https://github.com/square/okhttp) for logging request and response
- [MQTTClient](https://github.com/eclipse/paho.mqtt.java) for MQTT
- [Bouncy Castle](https://github.com/bcgit/bc-java) for use of cryptographic algorithms


# Requirements

- Java 8 ( or above )
- Android software development kit
- Android Studio
- For support of SDK in Android 14 devices permission regarding Schedule exact alarms should be given when asked or manually by yourself



# SDK Usage:
	
## Prerequisite input data 
```java	
  	String uniqueId = <<uniqueId>>;

```

* "uniqueId" 	: Your device uniqueId

- SdkOptions is for the SDK configuration and needs to parse in SDK object initialize call. You need to manage the below configuration as per your device authentication type.
```json
	 String sdkOptions = {
		"devicePK": "",
	        "cpId": "",
   		"env": "",
		"pf": "",
		"skipValidation": false,
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
* "devicePK":  If authentication type is symmetric key then use it.
* "cpId" 		: It is the company code. It gets from the IoTConnect UI portal "Settings->Key Vault"
* "env" : Enter the given environment value
* "pf" : give the platform AZ or AWS
* "skipValidation": false = do not want to skip data validation for attributes, true= want to skip data validation for attributes
* "certificate": It is indicated to define the path of the certificate file. Mandatory for X.509/SSL device CA signed or self-signed authentication type only.
	- SSLKeyPath: your device key
	- SSLCertPath: your device certificate
	- SSLCaPath : Root CA certificate
> ****Note**:-** For **AWS platform** you have to convert files mentioned with below openssl command for SSLKeyPath and SSLCertPath:                          
		**SSLKeyPath** -> openssl rsa -in device.pem -out device.key                             
		**SSLCertPath** -> openssl x509 -in certificate.crt -out certificate.pem                               
		**SSLCaPath**  -> For Root CA you have to go to Settings->KeyVault of IOTConnect portal and save rootCA file from AmazonRootCA1 title and rename to .pem file


* "offlineStorage" : Define the configuration related to the offline data storage 
	- disabled : false = offline data storing, true = not storing offline data 
	- availSpaceInMb : Define the file size of off-line data which should be in (MB)
	- fileCount : Number of files need to create for off-line data

         > ****Note**:-** sdkOptions is optional but mandatory for SSL/x509 device authentication type only. Define proper setting or leave it NULL.
If you do not provide off-line storage, it will set the default settings as per defined above. It may harm your device by storing the large data. Once memory gets full may chance to stop the execution.

## Functions and callbacks:	
- To Initialize the SDK object and connect to the cloud.
```java		
	SDKClient sdkClient = SDKClient.getInstance(Context,uniqueId, DeviceCallback, sdkOptions);
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
                   case "116":
                        // Device Command
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
- To receive Device Command C2D(C2D)
```java	
	@Override
    	public void onDeviceCommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive OTA Command(C2D)
```java	
	@Override
    public void onOTACommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive Module Command(C2D)
```java	
	@Override
    	public void onModuleCommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive Module Command(C2D)
```java	
	@Override
    	public void onModuleCommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive Attributes Change Command(C2D)
```java	
	@Override
    	public void onAttrChangeCommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive Twin Change Command(C2D)
```java	
	@Override
    	public void onTwinChangeCommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive Rule Change Command(C2D)
```java	
	@Override
    	public void onRuleChangeCommand(String message) {
		Log.d(TAG, message);
	}
```
- To receive Device Change Command(C2D)
```java	
	@Override
    	public void onDeviceChangeCommand(String message) {
		Log.d(TAG, message);
	}
```
	
- To get the list of attributes with respective device.
```java
	String data = sdkClient.getAttributes();
    	Log.d("Attribute list device wise :", data);	
```
- To get the all twin property Desired and Reported
```java
	sdkClient.getTwins();
```
- To get the child devices
```java
	sdkClient.getChildDevices();
```
- To create child device
```java
	sdkClient.createChildDevice(deviceId,deviceTag,displayName);
```
- To delete child device
```java
	sdkClient.deleteChildDevice(deviceId);
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

* "ackId(*)" 	: Command Acknowledgment GUID which will receive from command payload (data.ackId)
* "st(*)"		: Acknowledgment status sent to cloud (4 = Fail, 6 = Device command[0x01], 7 = Firmware OTA command[0x02])
* "msg" 		: It is used to send your custom message
* "childId" 	: It is used for Gateway's child device OTA update only
				0x01 : null or "" for Device command
			  	0x02 : null or "" for Gateway device and mandatory for Gateway child device's OTA update.
		   		How to get the "childId" .?
		   		- You will get child uniqueId for child device OTA command from payload "data.urls[~].uniqueId"
* "msgType" 	: Message type (5 = "0x01" device command, 11 = "0x02" Firmware OTA command)
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

# Build Details
* Key Branches
	- develop: contains the latest dev code.
	- main: contains the code for the current library release.

# License
- [Softweb Proprietor](https://www.softwebsolutions.com/)

  

# Legacy Device Message 1.0 Format
Please refer this [link](https://github.com/avnet-iotconnect/iotc-android-sdk/blob/release/3.1.4/README.md)
