package com.iotconnectsdk.beans

internal class TumblingWindowBean {
    var attributeName: String? = null
    var min = 0.0
    var max = 0.0
    var sum // sum attribute value "temp= 10"
            = 0.0
    var avg // sum/count
            = 0.0
    var count // number of time client sends data
            = 0
    var lv //last value
            = 0.0
    var isMinSet = false
    var isMaxSet = false
    var uniqueId: String? = null
    var tag: String? = null
}