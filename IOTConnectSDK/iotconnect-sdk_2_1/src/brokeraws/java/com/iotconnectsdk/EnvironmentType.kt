package com.iotconnectsdk



enum class EnvironmentType(val value: String) {


    PROD("prod"),

    POC("POC"),

    PREQA("preqa")

}

/*
val environmentEnum = when (BuildConfig.BrokerType) {
    "az" -> setOf(
        EnvironmentType.DEV,
        EnvironmentType.QA,
        EnvironmentType.PROD,

    )
    "aws"-> setOf(
        EnvironmentType.PREQA,
        EnvironmentType.PROD,
        EnvironmentType.POC
    )
    else -> setOf(
        EnvironmentType.PREQA,
        EnvironmentType.PROD,
        EnvironmentType.POC
    )
}*/
