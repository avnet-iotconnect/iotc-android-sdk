package com.iotconnectsdk.enums

internal enum class DeviceIdentityMessages(val value: Int) {
    GET_DEVICE_TEMPLATE_ATTRIBUTES(201),
    GET_DEVICE_TEMPLATE_SETTINGS_TWIN(202),
    GET_EDGE_RULE(203),
    GET_CHILD_DEVICES(204),
    GET_PENDING_OTA(205),
    CREATE_CHILD_DEVICE(221),
    DELETE_CHILD_DEVICE(222)
}