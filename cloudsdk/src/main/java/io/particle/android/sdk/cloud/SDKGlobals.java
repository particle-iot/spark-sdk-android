package io.particle.android.sdk.cloud;


import android.content.Context;
import android.support.annotation.NonNull;

import io.particle.android.sdk.persistance.AppDataStorage;
import io.particle.android.sdk.persistance.SensitiveDataStorage;


public class SDKGlobals {

    private static volatile SensitiveDataStorage sensitiveDataStorage;
    private static volatile AppDataStorage appDataStorage;

    private static boolean isInitialized = false;


    public static synchronized void init(@NonNull Context ctx) {
        ctx = ctx.getApplicationContext();
        if (isInitialized) {
            return;
        }

        sensitiveDataStorage = new SensitiveDataStorage(ctx);
        appDataStorage = new AppDataStorage(ctx);

        isInitialized = true;
    }


    public static SensitiveDataStorage getSensitiveDataStorage() {
        return sensitiveDataStorage;
    }

    public static AppDataStorage getAppDataStorage() {
        return appDataStorage;
    }

}
