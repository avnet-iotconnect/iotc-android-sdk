package com.iotconnectsdk.webservices.api

import com.google.gson.GsonBuilder
import java.lang.reflect.Modifier

/**
 * Gson Builder class for retrofit object
 */
internal object AppGSonBuilder {
    val internal = GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
        .serializeNulls()
        .create()
    val external = GsonBuilder()
        .excludeFieldsWithModifiers(
            Modifier.FINAL,
            Modifier.TRANSIENT,
            Modifier.STATIC,
            Modifier.VOLATILE
        )
        .serializeNulls()
        .create()
}