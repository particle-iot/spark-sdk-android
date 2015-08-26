package io.particle.android.sdk.cloud;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import io.particle.android.sdk.persistance.SensitiveDataStorage;

import static io.particle.android.sdk.utils.Py.truthy;


public class ParticleUser {

    /**
     * Initialize ParticleUser class with new credentials and store session in keychain
     */
    public static synchronized ParticleUser fromNewCredentials(
            @NonNull String user, @NonNull String password) {
        Preconditions.checkArgument(truthy(user), "Username cannot be empty or null");
        Preconditions.checkArgument(truthy(password), "Password cannot be empty or null");

        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        sensitiveDataStorage.saveUser(user);
        sensitiveDataStorage.savePassword(password);

        return new ParticleUser(user, password);
    }

    /**
     * Try to initialize a ParticleUser class with stored credentials
     *
     * @return ParticleUser instance if successfully retrieved session, else null
     */
    public static synchronized ParticleUser fromSavedSession() {
        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        String user = sensitiveDataStorage.getUser();
        String password = sensitiveDataStorage.getPassword();

        if (truthy(user) && truthy(password)) {
            return new ParticleUser(user, password);
        } else {
            return null;
        }
    }

    public static void removeSession() {
        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        sensitiveDataStorage.resetPassword();
        sensitiveDataStorage.resetUser();
    }


    @NonNull
    private final String user;
    @NonNull
    private final String password;


    private ParticleUser(@NonNull String user, @NonNull String password) {
        this.user = user;
        this.password = password;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    @NonNull
    public String getUser() {
        return user;
    }
}
