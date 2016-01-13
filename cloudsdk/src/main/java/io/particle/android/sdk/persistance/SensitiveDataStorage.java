package io.particle.android.sdk.persistance;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;


// FIXME: crib the code from the Vault example to do crypto for all these values.
@ParametersAreNonnullByDefault
public class SensitiveDataStorage {

    private static final String KEY_USERNAME = "KEY_USERNAME";
    private static final String KEY_PASSWORD = "KEY_PASSWORD";
    private static final String KEY_TOKEN = "KEY_TOKEN";
    private static final String KEY_TOKEN_EXPIRATION_DATE = "KEY_TOKEN_EXPIRATION_DATE";

    private final SharedPreferences sharedPrefs;


    public SensitiveDataStorage(Context ctx) {
        ctx = ctx.getApplicationContext();
        this.sharedPrefs = ctx.getSharedPreferences("spark_sdk_sensitive_data", Context.MODE_PRIVATE);
    }

    public void saveUser(String user) {
        sharedPrefs.edit()
                .putString(KEY_USERNAME, user)
                .apply();
    }

    public String getUser() {
        return sharedPrefs.getString(KEY_USERNAME, null);
    }

    public void resetUser() {
        sharedPrefs.edit()
                .remove(KEY_USERNAME)
                .apply();
    }

    public void savePassword(String password) {
        sharedPrefs.edit()
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public String getPassword() {
        return sharedPrefs.getString(KEY_PASSWORD, null);
    }

    public void resetPassword() {
        sharedPrefs.edit()
                .remove(KEY_PASSWORD)
                .apply();
    }

    public void saveToken(String token) {
        sharedPrefs.edit()
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public String getToken() {
        return sharedPrefs.getString(KEY_TOKEN, null);
    }

    public void resetToken() {
        sharedPrefs.edit()
                .remove(KEY_TOKEN)
                .apply();
    }

    public void saveTokenExpirationDate(Date expirationDate) {
        sharedPrefs.edit()
                .putLong(KEY_TOKEN_EXPIRATION_DATE, expirationDate.getTime())
                .apply();
    }

    public Date getTokenExpirationDate() {
        long expirationTs = sharedPrefs.getLong(KEY_TOKEN_EXPIRATION_DATE, -1);
        return (expirationTs == -1) ? null : new Date(expirationTs);
    }

    public void resetTokenExpirationDate() {
        sharedPrefs.edit()
                .remove(KEY_TOKEN_EXPIRATION_DATE)
                .apply();
    }

}
