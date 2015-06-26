package io.particle.android.sdk.cloud;

import com.google.common.base.Preconditions;

import io.particle.android.sdk.persistance.SensitiveDataStorage;

import static io.particle.android.sdk.utils.Py.truthy;


public class SparkUser {

    /**
     * Initialize SparkUser class with new credentials and store session in keychain
     *
     * @param user     New username credential
     * @param password New password credential
     * @return SparkUser instance
     */
    public static synchronized SparkUser fromNewCredentials(String user, String password) {
        Preconditions.checkArgument(truthy(user), "Username cannot be empty or null");
        Preconditions.checkArgument(truthy(password), "Password cannot be empty or null");

        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        sensitiveDataStorage.saveUser(user);
        sensitiveDataStorage.savePassword(password);

        return new SparkUser(user, password);
    }

    /**
     * Try to initialize a SparkUser class with stored credentials
     *
     * @return SparkUser instance if successfully retrieved session, else null
     */
    public static synchronized SparkUser fromSavedSession() {
        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        String user = sensitiveDataStorage.getUser();
        String password = sensitiveDataStorage.getPassword();

        if (truthy(user) && truthy(password)) {
            return new SparkUser(user, password);
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


    private SparkUser(String user, String password) {
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
