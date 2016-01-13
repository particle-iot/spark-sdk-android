package io.particle.android.sdk.cloud;


import android.content.Context;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.persistance.AppDataStorage;
import io.particle.android.sdk.persistance.SensitiveDataStorage;


@ParametersAreNonnullByDefault
public class SDKGlobals {

    private static volatile SensitiveDataStorage sensitiveDataStorage;
    private static volatile AppDataStorage appDataStorage;

    private static boolean isInitialized = false;


    public static synchronized void init(Context ctx) {
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
