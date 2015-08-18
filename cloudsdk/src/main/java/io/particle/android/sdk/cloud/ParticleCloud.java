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


public class ParticleCloud {

    private static final TLog log = TLog.get(ParticleCloud.class);

    private static ParticleCloud instance;

    /**
     * Singleton instance of ParticleCloud class
     *
     * @return ParticleCloud
     */
    public synchronized static ParticleCloud get(Context context) {
        // TODO: try to eliminate singleton, consider replacing with dependency
        // injection where initializer gets:
        // CloudConnection, CloudEndpoint (URL) to allow private cloud
        if (instance == null) {
            log.d("Initializing ParticleCloud instance");
            instance = buildInstance(context);
        }
        return instance;
    }

    private static ParticleCloud buildInstance(Context context) {
        Context appContext = context.getApplicationContext();
        SDKGlobals.init(appContext);

        // FIXME: see if this TokenGetterDelegate setter issue can be resolved reasonably
        TokenGetter tokenGetter = new TokenGetter();
        ApiFactory factory = new ApiFactory(appContext, tokenGetter);
        ParticleCloud cloud = new ParticleCloud(
                factory.buildCloudApi(),
                factory.buildIdentityApi(),
                SDKGlobals.getAppDataStorage(),
                LocalBroadcastManager.getInstance(context));
        tokenGetter.cloud = cloud;

        return cloud;
    }


    private static class TokenGetter implements ApiFactory.TokenGetterDelegate {

        volatile ParticleCloud cloud;

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

    private volatile ParticleAccessToken token;
    private volatile ParticleUser user;

    private volatile Map<String, ParticleDevice> deviceCache = map();

    private ParticleCloud(ApiDefs.CloudApi mainApi, ApiDefs.IdentityApi identityApi,
                          AppDataStorage appDataStorage, LocalBroadcastManager broadcastManager) {
        this.mainApi = mainApi;
        this.identityApi = identityApi;
        this.appDataStorage = appDataStorage;
        this.broadcastManager = broadcastManager;
        this.user = ParticleUser.fromSavedSession();
        this.token = ParticleAccessToken.fromSavedSession();
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
     * Login with existing account credentials to Particle cloud
     *
     * @param user     User name, must be a valid email address
     * @param password Password
     */
    public void logIn(String user, String password) throws ParticleCloudException {
        try {
            Responses.LogInResponse response = identityApi.logIn("password", user, password);
            this.token = ParticleAccessToken.fromNewSession(response);
            this.token.setDelegate(tokenDelegate);
            this.user = ParticleUser.fromNewCredentials(user, password);

        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }

    }

    /**
     * Sign up with new account credentials to Particle cloud
     *
     * @param user     Required user name, must be a valid email address
     * @param password Required password
     */
    public void signUpWithUser(String user, String password) throws ParticleCloudException {
        try {
            identityApi.signUp(user, password);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    /**
     * Sign up with new account credentials to Particle cloud
     *
     * @param email      Required user name, must be a valid email address
     * @param password   Required password
     * @param inviteCode Optional invite code for opening an account
     * @param orgName    Organization name to include in cloud API endpoint URL
     */
    public void signUpWithOrganization(String email, String password, String inviteCode,
                                       String orgName) throws ParticleCloudException {
        // TODO: review against spec
        if (!truthy(orgName)) {
            throw new IllegalArgumentException("Organization name not specified");
        }

        try {
            identityApi.signUpWithOrganizationalUser(email, password, inviteCode, orgName);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    /**
     * Logout user, remove session data
     */
    public void logOut() {
        if (token != null) {
            token.cancelExpiration();
        }
        ParticleUser.removeSession();
        ParticleAccessToken.removeSession();
        token = null;
        user = null;
    }

    /**
     * Get an array of instances of all user's claimed devices
     */
    public List<ParticleDevice> getDevices() throws ParticleCloudException {
        List<Models.SimpleDevice> simpleDevices;
        try {
            simpleDevices = mainApi.getDevices();

            appDataStorage.saveUserHasClaimedDevices(truthy(simpleDevices));
//            appDataStorage.saveUserHasClaimedDevices(true);
//
            List<ParticleDevice> devices = list();

            // FIXME: TEST DATA, REMOVE
//            devices.add(ParticleDevice.newBuilder()
//                            .setName("PhotonsSoldCounter")
//                            .setDeviceType(ParticleDevice.ParticleDeviceType.PHOTON)
//                            .setDeviceId("ECB6F4DD1DE54700849DACA4")
//                            .setIsConnected(false)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(ParticleDevice.newBuilder()
//                            .setName("test_core2")
//                            .setDeviceType(ParticleDevice.ParticleDeviceType.PHOTON)
//                            .setDeviceId("408B061A70B746D18110BD6A")
//                            .setIsConnected(false)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(ParticleDevice.newBuilder()
//                            .setName("CoreOnTheTable")
//                            .setDeviceType(ParticleDevice.ParticleDeviceType.PHOTON)
//                            .setDeviceId("8DFDA5C4C8A1408DB1CA4677")
//                            .setIsConnected(false)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(ParticleDevice.newBuilder()
//                            .setName("Custom_FW")
//                            .setDeviceType(ParticleDevice.ParticleDeviceType.PHOTON)
//                            .setDeviceId("74AF91816D364B338D38ABD3")
//                            .setIsConnected(true)
//                            .setMainApi(mainApi)
//                            .build()
//            );
//            devices.add(ParticleDevice.newBuilder()
//                            .setName("zombie_pirate")
//                            .setDeviceType(ParticleDevice.ParticleDeviceType.PHOTON)
//                            .setDeviceId("824E7FBD7B194D9D982552A6")
//                            .setIsConnected(true)
//                            .setMainApi(mainApi)
//                            .build()
//            );

            for (Models.SimpleDevice simpleDevice : simpleDevices) {
                ParticleDevice.Builder builder;
                if (simpleDevice.isConnected) {
                    builder = mainApi.getDevice(simpleDevice.id);
                } else {
                    builder = ParticleDevice.newBuilder()
                            .setDeviceId(simpleDevice.id)
                            .setIsConnected(simpleDevice.isConnected)
                            .setName(simpleDevice.name);
                }

                // FIXME: this is nasty.  go with the suggestion at the top of ParticleDevice
                // to resolve this crud.
                ParticleDevice oldDevice = deviceCache.get(simpleDevice.id);
                if (oldDevice != null) {
                    builder.setIsFlashing(oldDevice.isFlashing());
                }
                devices.add(builder
                        .setMainApi(mainApi)
                        .setDeviceType(ParticleDevice.ParticleDeviceType.fromInt(simpleDevice.productId))
                        .setBroadcastManager(broadcastManager)
                        .setParticleCloud(this)
                        .build());
            }

            // FIXME: remove test data
//            devices.add(ParticleDevice.newBuilder()
//                    .setName("Custom_FW")
//                    .setDeviceType(ParticleDevice.ParticleDeviceType.CORE)
//                    .setDeviceId("74AF91816A364B338D38AFD3")
//                    .setIsConnected(false)
//                    .setBroadcastManager(broadcastManager)
//                    .setParticleCloud(this)
//                    .setMainApi(mainApi)
//                    .build());

            // TODO: review this approach, is this the right way to ensure access to devices?
            Map<String, ParticleDevice> deviceMap = map();
            for (ParticleDevice d : devices) {
                deviceMap.put(d.getID(), d);
            }
            deviceCache = deviceMap;

            return devices;

        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    /**
     * Get a specific device instance by its deviceID
     *
     * @param deviceID required deviceID
     * @return the device instance on success
     */
    public ParticleDevice getDevice(String deviceID) throws ParticleCloudException {
        // FIXME: not a long term solution!  We shouldn't have a method call that
        // usually returns instantly and other times hits the network!
        if (deviceCache.containsKey(deviceID)) {
            return deviceCache.get(deviceID);
        }

        ParticleDevice.Builder deviceBuilder;
        try {
            deviceBuilder = mainApi.getDevice(deviceID);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }

        return deviceBuilder
                .setMainApi(mainApi)
                .setBroadcastManager(broadcastManager)
                .setParticleCloud(this)
                .build();
    }

    // Not available yet
    private void publishEvent(String eventName, byte[] eventData) throws ParticleCloudException {

    }

    /**
     * Claim the specified device to the currently logged in user (without claim code mechanism)
     *
     * @param deviceID the deviceID
     */
    public void claimDevice(String deviceID) throws ParticleCloudException {
        try {
            mainApi.claimDevice(deviceID);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    /**
     * Get a short-lived claiming token for transmitting to soon-to-be-claimed device in
     * soft AP setup process
     *
     * @return a claim code string set on success (48 random bytes, base64 encoded
     * to 64 ASCII characters)
     */
    public Responses.ClaimCodeResponse generateClaimCode() throws ParticleCloudException {
        try {
            // appease newer OkHttp versions with a blank POST body
            return mainApi.generateClaimCode("");
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    // TODO: check if any javadoc has been added for this method in the iOS SDK
    public void requestPasswordReset(String email) throws ParticleCloudException {
        try {
            identityApi.requestPasswordReset(email);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    // FIXME: reconsider?
    // lame workaround for the fact that we need main-thread access to current
    // device names for the rename feature.  Maybe we should just expose the device
    // cache after all?
    public Set<String> getDeviceNames() {
        Set<String> deviceNames = set();
        for (ParticleDevice device : deviceCache.values()) {
            deviceNames.add(device.getName());
        }
        return deviceNames;
    }

    private class TokenDelegate implements ParticleAccessToken.ParticleAccessTokenDelegate {

        @Override
        public void accessTokenExpiredAt(final ParticleAccessToken accessToken, Date expirationDate) {
            // handle auto-renewal of expired access tokens by internal timer event
            // If user is null, don't bother because we have no credentials.
            if (user != null) {
                try {
                    logIn(user.getUser(), user.getPassword());
                } catch (ParticleCloudException e) {
                    log.e("Error while trying to log in: ", e);
                    token = null;
                }
            } else {
                token = null;
            }
        }
    }

}
