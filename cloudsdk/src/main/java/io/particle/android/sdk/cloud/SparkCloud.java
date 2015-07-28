package io.particle.android.sdk.cloud;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.particle.android.sdk.cloud.Responses.Models;
import io.particle.android.sdk.persistance.AppDataStorage;
import io.particle.android.sdk.utils.TLog;
import retrofit.RetrofitError;

import static io.particle.android.sdk.utils.Py.all;
import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.map;
import static io.particle.android.sdk.utils.Py.set;
import static io.particle.android.sdk.utils.Py.truthy;


public class SparkCloud {

    private static final TLog log = TLog.get(SparkCloud.class);

    private static SparkCloud instance;

    /**
     * Singleton instance of SparkCloud class
     *
     * @return SparkCloud
     */
    public synchronized static SparkCloud get(Context context) {
        // TODO: try to eliminate singleton, consider replacing with dependency
        // injection where initializer gets:
        // CloudConnection, CloudEndpoint (URL) to allow private cloud
        if (instance == null) {
            log.d("Initializing SparkCloud instance");
            instance = buildInstance(context);
        }
        return instance;
    }

    private static SparkCloud buildInstance(Context context) {
        Context appContext = context.getApplicationContext();
        SDKGlobals.init(appContext);

        // FIXME: see if this TokenGetterDelegate setter issue can be resolved reasonably
        TokenGetter tokenGetter = new TokenGetter();
        ApiFactory factory = new ApiFactory(appContext, tokenGetter);
        SparkCloud cloud = new SparkCloud(
                factory.buildCloudApi(),
                factory.buildIdentityApi(),
                SDKGlobals.getAppDataStorage(),
                LocalBroadcastManager.getInstance(context));
        tokenGetter.cloud = cloud;

        return cloud;
    }


    private static class TokenGetter implements ApiFactory.TokenGetterDelegate {

        volatile SparkCloud cloud;

        @Override
        public String getTokenValue() {
            return cloud.getAccessToken();
        }
    }


    private final ApiDefs.CloudApi mainApi;
    private final ApiDefs.IdentityApi identityApi;
    private final AppDataStorage appDataStorage;
    private final TokenDelegate tokenDelegate = new TokenDelegate();
    private final LocalBroadcastManager broadcastManager;

    private volatile SparkAccessToken token;
    private volatile SparkUser user;

    private volatile Map<String, SparkDevice> deviceCache = map();

    private SparkCloud(ApiDefs.CloudApi mainApi, ApiDefs.IdentityApi identityApi,
                       AppDataStorage appDataStorage, LocalBroadcastManager broadcastManager) {
        this.mainApi = mainApi;
        this.identityApi = identityApi;
        this.appDataStorage = appDataStorage;
        this.broadcastManager = broadcastManager;
        this.user = SparkUser.fromSavedSession();
        this.token = SparkAccessToken.fromSavedSession();
        if (token != null) {
            token.setDelegate(new TokenDelegate());
        }
    }

    /**
     * Current session access token string.  Can be null.
     */
    public String getAccessToken() {
        return (this.token == null) ? null : this.token.getAccessToken();
    }

    /**
     * Currently logged in user name, or null if no session exists
     */
    public String getLoggedInUsername() {
        return all(this.token, this.user) ? this.user.getUser() : null;
    }

    /**
     * Login with existing account credentials to Spark cloud
     *
     * @param user     User name, must be a valid email address
     * @param password Password
     */
    public void logIn(String user, String password) throws SparkCloudException {
        try {
            Responses.LogInResponse response = identityApi.logIn("password", user, password);
            this.token = SparkAccessToken.fromNewSession(response);
            this.token.setDelegate(tokenDelegate);
            this.user = SparkUser.fromNewCredentials(user, password);

        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }

    }

    /**
     * Sign up with new account credentials to Spark cloud
     *
     * @param user     Required user name, must be a valid email address
     * @param password Required password
     */
    public void signUpWithUser(String user, String password) throws SparkCloudException {
        try {
            identityApi.signUp(user, password);
        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }
    }

    /**
     * Sign up with new account credentials to Spark cloud
     *
     * @param email      Required user name, must be a valid email address
     * @param password   Required password
     * @param inviteCode Optional invite code for opening an account
     * @param orgName    Organization name to include in cloud API endpoint URL
     */
    public void signUpWithOrganization(String email, String password, String inviteCode,
                                       String orgName) throws SparkCloudException {
        // TODO: review against spec
        if (!truthy(orgName)) {
            throw new IllegalArgumentException("Organization name not specified");
        }

        try {
            identityApi.signUpWithOrganizationalUser(email, password, inviteCode, orgName);
        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }
    }

    /**
     * Logout user, remove session data
     */
    public void logOut() {
        if (token != null) {
            token.cancelExpiration();
        }
        SparkUser.removeSession();
        SparkAccessToken.removeSession();
        token = null;
        user = null;
    }

    /**
     * Get an array of instances of all user's claimed devices
     */
    public List<SparkDevice> getDevices() throws SparkCloudException {
        List<Models.SimpleDevice> simpleDevices;
        try {
            simpleDevices = mainApi.getDevices();

            appDataStorage.saveUserHasClaimedDevices(truthy(simpleDevices));
//            appDataStorage.saveUserHasClaimedDevices(true);
//
            List<SparkDevice> devices = list();

            // FIXME: TEST DATA, REMOVE
//            devices.add(SparkDevice.newBuilder()
//                            .setName("PhotonsSoldCounter")
//                            .setDeviceType(SparkDevice.SparkDeviceType.PHOTON)
//                            .setDeviceId("ECB6F4DD1DE54700849DACA4")
//                            .setIsConnected(false)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(SparkDevice.newBuilder()
//                            .setName("test_core2")
//                            .setDeviceType(SparkDevice.SparkDeviceType.PHOTON)
//                            .setDeviceId("408B061A70B746D18110BD6A")
//                            .setIsConnected(false)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(SparkDevice.newBuilder()
//                            .setName("CoreOnTheTable")
//                            .setDeviceType(SparkDevice.SparkDeviceType.PHOTON)
//                            .setDeviceId("8DFDA5C4C8A1408DB1CA4677")
//                            .setIsConnected(false)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(SparkDevice.newBuilder()
//                            .setName("Custom_FW")
//                            .setDeviceType(SparkDevice.SparkDeviceType.PHOTON)
//                            .setDeviceId("74AF91816D364B338D38ABD3")
//                            .setIsConnected(true)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(SparkDevice.newBuilder()
//                            .setName("zombie_pirate")
//                            .setDeviceType(SparkDevice.SparkDeviceType.PHOTON)
//                            .setDeviceId("824E7FBD7B194D9D982552A6")
//                            .setIsConnected(true)
//                            .setMainApi(mainApi)
//                            .build()
//            );

            for (Models.SimpleDevice simpleDevice : simpleDevices) {
                SparkDevice.Builder builder;
                if (simpleDevice.isConnected) {
                    builder = mainApi.getDevice(simpleDevice.id);
                } else {
                    builder = SparkDevice.newBuilder()
                            .setDeviceId(simpleDevice.id)
                            .setIsConnected(simpleDevice.isConnected)
                            .setName(simpleDevice.name);
                }

                // FIXME: this is nasty.  go with the suggestion at the top of SparkDevice
                // to resolve this crud.
                SparkDevice oldDevice = deviceCache.get(simpleDevice.id);
                if (oldDevice != null) {
                    builder.setIsFlashing(oldDevice.isFlashing());
                }
                devices.add(builder
                        .setMainApi(mainApi)
                        .setDeviceType(SparkDevice.SparkDeviceType.fromInt(simpleDevice.productId))
                        .setBroadcastManager(broadcastManager)
                        .setSparkCloud(this)
                        .build());
            }

            // FIXME: remove test data
//            devices.add(SparkDevice.newBuilder()
//                    .setName("Custom_FW")
//                    .setDeviceType(SparkDevice.SparkDeviceType.CORE)
//                    .setDeviceId("74AF91816A364B338D38AFD3")
//                    .setIsConnected(false)
//                    .setBroadcastManager(broadcastManager)
//                    .setSparkCloud(this)
//                    .setMainApi(mainApi)
//                    .build());

            // TODO: review this approach, is this the right way to ensure access to devices?
            Map<String, SparkDevice> deviceMap = map();
            for (SparkDevice d : devices) {
                deviceMap.put(d.getID(), d);
            }
            deviceCache = deviceMap;

            return devices;

        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }
    }

    /**
     * Get a specific device instance by its deviceID
     *
     * @param deviceID required deviceID
     * @return the device instance on success
     */
    public SparkDevice getDevice(String deviceID) throws SparkCloudException {
        // FIXME: not a long term solution!  We shouldn't have a method call that
        // usually returns instantly and other times hits the network!
        if (deviceCache.containsKey(deviceID)) {
            return deviceCache.get(deviceID);
        }

        SparkDevice.Builder deviceBuilder;
        try {
            deviceBuilder = mainApi.getDevice(deviceID);
        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }

        return deviceBuilder
                .setMainApi(mainApi)
                .setBroadcastManager(broadcastManager)
                .setSparkCloud(this)
                .build();
    }

    // Not available yet
    private void publishEvent(String eventName, byte[] eventData) throws SparkCloudException {

    }

    /**
     * Claim the specified device to the currently logged in user (without claim code mechanism)
     *
     * @param deviceID the deviceID
     */
    public void claimDevice(String deviceID) throws SparkCloudException {
        try {
            mainApi.claimDevice(deviceID);
        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }
    }

    /**
     * Get a short-lived claiming token for transmitting to soon-to-be-claimed device in
     * soft AP setup process
     *
     * @return a claim code string set on success (48 random bytes, base64 encoded
     * to 64 ASCII characters)
     */
    public Responses.ClaimCodeResponse generateClaimCode() throws SparkCloudException {
        try {
            // appease newer OkHttp versions with a blank POST body
            return mainApi.generateClaimCode("");
        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }
    }

    // TODO: check if any javadoc has been added for this method in the iOS SDK
    public void requestPasswordReset(String email) throws SparkCloudException {
        try {
            identityApi.requestPasswordReset(email);
        } catch (RetrofitError error) {
            throw new SparkCloudException(error);
        }
    }

    // FIXME: reconsider?
    // lame workaround for the fact that we need main-thread access to current
    // device names for the rename feature.  Maybe we should just expose the device
    // cache after all?
    public Set<String> getDeviceNames() {
        Set<String> deviceNames = set();
        for (SparkDevice device : deviceCache.values()) {
            deviceNames.add(device.getName());
        }
        return deviceNames;
    }

    private class TokenDelegate implements SparkAccessToken.SparkAccessTokenDelegate {

        @Override
        public void accessTokenExpiredAt(final SparkAccessToken accessToken, Date expirationDate) {
            // handle auto-renewal of expired access tokens by internal timer event
            // If user is null, don't bother because we have no credentials.
            if (user != null) {
                try {
                    logIn(user.getUser(), user.getPassword());
                } catch (SparkCloudException e) {
                    log.e("Error while trying to log in: ", e);
                    token = null;
                }
            } else {
                token = null;
            }
        }
    }

}
