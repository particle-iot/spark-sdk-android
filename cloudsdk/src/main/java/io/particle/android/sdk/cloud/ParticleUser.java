package io.particle.android.sdk.cloud;


import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.persistance.SensitiveDataStorage;
import io.particle.android.sdk.utils.Preconditions;

import static io.particle.android.sdk.utils.Py.truthy;


@ParametersAreNonnullByDefault
public class ParticleUser {

    /**
     * Initialize ParticleUser class with new credentials and store session in keychain
     */
    public static synchronized ParticleUser fromNewCredentials(String user, String password) {
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


    private final String user;
    private final String password;


    private ParticleUser(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUser() {
        return user;
    }
}
