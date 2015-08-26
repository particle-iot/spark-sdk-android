package io.particle.android.sdk.persistance;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Date;


// FIXME: crib the code from the Vault example to do crypto for all these values.
@SuppressLint("CommitPrefEdits")  // we need immediate commits of changes in this prefs file
public class SensitiveDataStorage {

    private static final String KEY_USERNAME = "KEY_USERNAME";
    private static final String KEY_PASSWORD = "KEY_PASSWORD";
    private static final String KEY_TOKEN = "KEY_TOKEN";
    private static final String KEY_TOKEN_EXPIRATION_DATE = "KEY_TOKEN_EXPIRATION_DATE";

    private final SharedPreferences sharedPrefs;


    public SensitiveDataStorage(@NonNull Context ctx) {
        ctx = ctx.getApplicationContext();
        this.sharedPrefs = ctx.getSharedPreferences("spark_sdk_sensitive_data", Context.MODE_PRIVATE);
    }

    public void saveUser(String user) {
        sharedPrefs.edit()
                .putString(KEY_USERNAME, user)
                .commit();
    }

    public String getUser() {
        return sharedPrefs.getString(KEY_USERNAME, null);
    }

    public void resetUser() {
        sharedPrefs.edit()
                .remove(KEY_USERNAME)
                .commit();
    }

    public void savePassword(String password) {
        sharedPrefs.edit()
                .putString(KEY_PASSWORD, password)
                .commit();
    }

    public String getPassword() {
        return sharedPrefs.getString(KEY_PASSWORD, null);
    }

    public void resetPassword() {
        sharedPrefs.edit()
                .remove(KEY_PASSWORD)
                .commit();
    }

    public void saveToken(String token) {
        sharedPrefs.edit()
                .putString(KEY_TOKEN, token)
                .commit();
    }

    public String getToken() {
        return sharedPrefs.getString(KEY_TOKEN, null);
    }

    public void resetToken() {
        sharedPrefs.edit()
                .remove(KEY_TOKEN)
                .commit();
    }

    public void saveTokenExpirationDate(Date expirationDate) {
        sharedPrefs.edit()
                .putLong(KEY_TOKEN_EXPIRATION_DATE, expirationDate.getTime())
                .commit();
    }

    public Date getTokenExpirationDate() {
        long expirationTs = sharedPrefs.getLong(KEY_TOKEN_EXPIRATION_DATE, -1);
        return (expirationTs == -1) ? null : new Date(expirationTs);
    }

    public void resetTokenExpirationDate() {
        sharedPrefs.edit()
                .remove(KEY_TOKEN_EXPIRATION_DATE)
                .commit();
    }

}
