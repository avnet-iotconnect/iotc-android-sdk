package com.iotconnectsdk.webservices.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

/**
 * Gson Builder class for retrofit object
 */

public class AppGSonBuilder {

    public static Gson getInternal() {

        return new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
    }

    public static Gson getExternal() {

        return new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC, Modifier.VOLATILE)
                .serializeNulls()
                .create();
    }
}
