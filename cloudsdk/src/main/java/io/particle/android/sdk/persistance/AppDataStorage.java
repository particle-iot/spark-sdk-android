package io.particle.android.sdk.persistance;

import android.content.Context;
import android.content.SharedPreferences;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Storage for misc settings to be persisted which <strong>aren't</strong> related to
 * identity, authorization, or any other sensitive data.
 */
@ParametersAreNonnullByDefault
public class AppDataStorage {

    private static final String KEY_USER_HAS_CLAIMED_DEVICES = "KEY_USER_HAS_CLAIMED_DEVICES";

    private final SharedPreferences sharedPrefs;


    public AppDataStorage(Context ctx) {
        ctx = ctx.getApplicationContext();
        this.sharedPrefs = ctx.getSharedPreferences("spark_sdk_prefs", Context.MODE_PRIVATE);
    }

    public void saveUserHasClaimedDevices(boolean value) {
        sharedPrefs.edit()
                .putBoolean(KEY_USER_HAS_CLAIMED_DEVICES, value)
                .apply();
    }

    public boolean getUserHasClaimedDevices() {
        return sharedPrefs.getBoolean(KEY_USER_HAS_CLAIMED_DEVICES, false);
    }

    public void resetUserHasClaimedDevices() {
        sharedPrefs.edit()
                .remove(KEY_USER_HAS_CLAIMED_DEVICES)
                .apply();
    }

}
