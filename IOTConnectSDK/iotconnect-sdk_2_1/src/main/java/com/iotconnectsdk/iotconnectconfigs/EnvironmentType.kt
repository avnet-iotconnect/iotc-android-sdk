package com.iotconnectsdk.iotconnectconfigs

enum class EnvironmentType constructor(val value:String) {
    DEV("DEV"),
    STAGE("STAGE"),
    POC("POC"),
    QA("QA"),
    AVNETPOC("AVNETPOC"),
    PROD("PROD")
}