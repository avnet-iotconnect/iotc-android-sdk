package com.iotconnectsdk.enums

enum class C2DMessageEnums(val value: Int) {

    //Device Commands
    DEVICE_COMMAND(0),
    OTA_COMMAND(1),
    MODULE_COMMAND(2),
    REFRESH_ATTRIBUTE(101),
    REFRESH_SETTING_TWIN(102),
    REFRESH_EDGE_RULE(103),
    REFRESH_CHILD_DEVICE(104),
    DATA_FREQUENCY_CHANGE(105),

    //Device lifecycle Commands
    DEVICE_DELETED(106),
    DEVICE_DISABLED(107),
    DEVICE_RELEASED(108),
    STOP_OPERATION(109),
    DEVICE_CONNECTION_STATUS(116),

    //Heartbeat commands
    START_HEARTBEAT(110),
    STOP_HEARTBEAT(111),
}