package com.iotconnectsdk.beans

internal class TumblingWindowBean {
    var attributeName: String? = null
    var min = 0.0
    var max = 0.0
    var sum = 0.0
    var avg = 0.0
    var count = 0
    var lv = 0.0
    var isMinSet = false
    var isMaxSet = false
    var uniqueId: String? = null
    var tag: String? = null
}