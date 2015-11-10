package io.particle.android.sdk.cloud;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.collect.Sets;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType;
import io.particle.android.sdk.cloud.ParticleDevice.VariableType;
import io.particle.android.sdk.cloud.Responses.Models;
import io.particle.android.sdk.cloud.Responses.Models.CompleteDevice;
import io.particle.android.sdk.persistance.AppDataStorage;
import io.particle.android.sdk.utils.TLog;
import retrofit.RetrofitError;

import static io.particle.android.sdk.utils.Py.all;
import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


// FIXME: move device state management out to another class
// FIXME: move some of the type conversion junk out of this into another class, too
public class ParticleCloud {

    private static final TLog log = TLog.get(ParticleCloud.class);

    /**
     * Singleton instance of ParticleCloud class
     *
     * @return ParticleCloud
     *
     * @deprecated use {@link ParticleCloudSDK#getCloud()} instead.  This interface will be removed
     * some time before the 1.0 release.
     */
    @Deprecated
    public synchronized static ParticleCloud get(@NonNull Context context) {
        log.w("ParticleCloud.get() is deprecated and will be removed before the 1.0 release. " +
                "Use ParticleCloudSDK.getCloud() instead!");
        if (!ParticleCloudSDK.isInitialized()) {
            ParticleCloudSDK.init(context);
        }
        return ParticleCloudSDK.getCloud();
    }

    @NonNull
    private final ApiDefs.CloudApi mainApi;
    @NonNull
    private final ApiDefs.IdentityApi identityApi;
    @NonNull
    private final AppDataStorage appDataStorage;
    @NonNull
    private final TokenDelegate tokenDelegate = new TokenDelegate();
    @NonNull
    private final LocalBroadcastManager broadcastManager;

    private final Map<String, ParticleDevice> devices = new HashMap<>();

    // We should be able to mark these both @Nullable, but Android Studio is incorrectly
    // inferring that these could be null, in spite of _directly following a null check_.
    // Try again later after a few more releases, I guess...
    // @Nullable
    private volatile ParticleAccessToken token;
    // @Nullable
    private volatile ParticleUser user;

    ParticleCloud(@NonNull ApiDefs.CloudApi mainApi,
                  @NonNull ApiDefs.IdentityApi identityApi,
                  @NonNull AppDataStorage appDataStorage,
                  @NonNull LocalBroadcastManager broadcastManager) {
        this.mainApi = mainApi;
        this.identityApi = identityApi;
        this.appDataStorage = appDataStorage;
        this.broadcastManager = broadcastManager;
        this.user = ParticleUser.fromSavedSession();
        this.token = ParticleAccessToken.fromSavedSession();
        if (this.token != null) {
            this.token.setDelegate(new TokenDelegate());
        }
    }

    //region public API
    /**
     * Current session access token string.  Can be null.
     */
    public String getAccessToken() {
        return (this.token == null) ? null : this.token.getAccessToken();
    }

    public void setAccessToken(@NonNull String tokenString, @NonNull Date expirationDate) {
        ParticleAccessToken.removeSession();
        this.token = ParticleAccessToken.fromTokenData(expirationDate, tokenString);
        this.token.setDelegate(tokenDelegate);
    }

    /**
     * Currently logged in user name, or null if no session exists
     */
    public String getLoggedInUsername() {
        return all(this.token, this.user) ? this.user.getUser() : null;
    }

    public boolean isLoggedIn() {
        return getLoggedInUsername() != null;
    }

    /**
     * Login with existing account credentials to Particle cloud
     *
     * @param user     User name, must be a valid email address
     * @param password Password
     */
    @WorkerThread
    public void logIn(@NonNull String user, @NonNull String password) throws ParticleCloudException {
        try {
            Responses.LogInResponse response = identityApi.logIn("password", user, password);
            onLogIn(response, user, password);
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
    @WorkerThread
    public void signUpWithUser(@NonNull String user, @NonNull String password)
            throws ParticleCloudException {
        try {
            identityApi.signUp(user, password);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    /**
     * Create new customer account on the Particle cloud and log in
     *
     * @param email    Required user name, must be a valid email address
     * @param password Required password
     * @param orgSlug  Organization slug to use
     */
    @WorkerThread
    public void signUpAndLogInWithCustomer(@NonNull String email,
                                           @NonNull String password,
                                           @NonNull String orgSlug) throws ParticleCloudException {
        if (!all(email, password, orgSlug)) {
            throw new IllegalArgumentException(
                    "Email, password, and organization must all be specified");
        }

        try {
            Responses.LogInResponse response = identityApi.signUpAndLogInWithCustomer(
                    "client_credentials", email, password, orgSlug);
            onLogIn(response, email, password);
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
    @WorkerThread
    public List<ParticleDevice> getDevices() throws ParticleCloudException {
        List<Models.SimpleDevice> simpleDevices;
        try {
            simpleDevices = mainApi.getDevices();

            appDataStorage.saveUserHasClaimedDevices(truthy(simpleDevices));

            List<ParticleDevice> result = list();

            for (Models.SimpleDevice simpleDevice : simpleDevices) {
                ParticleDevice device;
                if (simpleDevice.isConnected) {
                    device = getDevice(simpleDevice.id, false);
                } else {
                    device = getOfflineDevice(simpleDevice);
                }
                result.add(device);
            }

            pruneDeviceMap(result);

            return result;

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
    @WorkerThread
    public ParticleDevice getDevice(@NonNull String deviceID) throws ParticleCloudException {
        return getDevice(deviceID, true);
    }

    // Not available yet
    private void publishEvent(String eventName, byte[] eventData) throws ParticleCloudException {

    }

    /**
     * Claim the specified device to the currently logged in user (without claim code mechanism)
     *
     * @param deviceID the deviceID
     */
    @WorkerThread
    public void claimDevice(@NonNull String deviceID) throws ParticleCloudException {
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
    @WorkerThread
    public Responses.ClaimCodeResponse generateClaimCode() throws ParticleCloudException {
        try {
            // Offer empty string to appease newer OkHttp versions which require a POST body,
            // even if it's empty or (as far as the endpoint cares) nonsense
            return mainApi.generateClaimCode("okhttp_appeasement");
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    @WorkerThread
    public Responses.ClaimCodeResponse generateClaimCodeForOrg(@NonNull String orgSlug,
                                                               @NonNull String productSlug)
            throws ParticleCloudException {
        try {
            // Offer empty string to appease newer OkHttp versions which require a POST body,
            // even if it's empty or (as far as the endpoint cares) nonsense
            return mainApi.generateClaimCodeForOrg("okhttp_appeasement", orgSlug, productSlug);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    // TODO: check if any javadoc has been added for this method in the iOS SDK
    @WorkerThread
    public void requestPasswordReset(@NonNull String email) throws ParticleCloudException {
        try {
            identityApi.requestPasswordReset(email);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }
    //endregion

    //region package-only API
    @WorkerThread
    void unclaimDevice(@NonNull String deviceId) {
        mainApi.unclaimDevice(deviceId);
        synchronized (devices) {
            devices.remove(deviceId);
        }
        sendUpdateBroadcast();
    }

    @WorkerThread
    void changeDeviceName(@NonNull String deviceId, @NonNull String newName)
            throws ParticleCloudException {
        ParticleDevice particleDevice;
        synchronized (devices) {
            particleDevice = devices.get(deviceId);
        }
        DeviceState originalDeviceState = particleDevice.deviceState;

        DeviceState stateWithNewName = DeviceState.withNewName(originalDeviceState, newName);
        updateDeviceState(stateWithNewName, true);
        try {
            mainApi.nameDevice(originalDeviceState.deviceId, newName);
        } catch (RetrofitError e) {
            // oops, change the name back.
            updateDeviceState(originalDeviceState, true);
            throw new ParticleCloudException(e);
        }
    }

    @WorkerThread
    // Called when a cloud API call receives a result in which the "coreInfo.connected" is false
    void onDeviceNotConnected(@NonNull DeviceState deviceState) {
        DeviceState newState = DeviceState.withNewConnectedState(deviceState, false);
        updateDeviceState(newState, true);
    }

    // FIXME: exposing this is weak, figure out something better
    void notifyDeviceChanged() {
        sendUpdateBroadcast();
    }

    // this is accessible at the package level for access from ParticleDevice's Parcelable impl
    ParticleDevice getDeviceFromState(@NonNull DeviceState deviceState) {
        synchronized (devices) {
            if (devices.containsKey(deviceState.deviceId)) {
                return devices.get(deviceState.deviceId);
            } else {
                ParticleDevice device = new ParticleDevice(mainApi, this, deviceState);
                devices.put(deviceState.deviceId, device);
                return device;
            }
        }
    }
    //endregion

    //region private API
    @WorkerThread
    private ParticleDevice getDevice(String deviceID, boolean sendUpdate)
            throws ParticleCloudException {
        CompleteDevice deviceCloudModel;
        try {
            deviceCloudModel = mainApi.getDevice(deviceID);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }

        DeviceState newDeviceState = fromCompleteDevice(deviceCloudModel);
        ParticleDevice device = getDeviceFromState(newDeviceState);
        updateDeviceState(newDeviceState, sendUpdate);

        return device;
    }

    private ParticleDevice getOfflineDevice(Models.SimpleDevice offlineDevice) {
        DeviceState newDeviceState = fromSimpleDeviceModel(offlineDevice);
        ParticleDevice device = getDeviceFromState(newDeviceState);
        updateDeviceState(newDeviceState, false);
        return device;
    }

    private void updateDeviceState(DeviceState newState, boolean sendUpdateBroadcast) {
        ParticleDevice device = getDeviceFromState(newState);
        device.deviceState = newState;
        if (sendUpdateBroadcast) {
            sendUpdateBroadcast();
        }
    }

    private void sendUpdateBroadcast() {
        broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
    }

    private void onLogIn(Responses.LogInResponse response, String user, String password) {
        ParticleAccessToken.removeSession();
        this.token = ParticleAccessToken.fromNewSession(response);
        this.token.setDelegate(tokenDelegate);
        this.user = ParticleUser.fromNewCredentials(user, password);
    }

    private DeviceState fromCompleteDevice(CompleteDevice completeDevice) {
        ImmutableSet<String> functions = completeDevice.functions == null
                ? ImmutableSet.<String>of()
                : ImmutableSet.copyOf(completeDevice.functions);
        ImmutableMap<String, VariableType> variables = ImmutableMap.of();
        if (completeDevice.variables != null ) {
            variables = ImmutableMap.copyOf(Maps.transformEntries(
                    completeDevice.variables, sMapTransformer));
        }
        return new DeviceState(
                completeDevice.deviceId,
                completeDevice.name,
                completeDevice.isConnected,
                functions,
                variables,
                completeDevice.version,
                ParticleDeviceType.fromInt(completeDevice.productId),
                completeDevice.requiresUpdate,
                completeDevice.lastHeard
        );
    }

    // for offline devices
    private DeviceState fromSimpleDeviceModel(Models.SimpleDevice offlineDevice) {
        ImmutableSet<String> functions = ImmutableSet.of();
        ImmutableMap<String, VariableType> variables = ImmutableMap.of();
        return new DeviceState(
                offlineDevice.id,
                offlineDevice.name,
                offlineDevice.isConnected,
                functions,
                variables,
                "",  // gross, but what else are we going to do?
                ParticleDeviceType.fromInt(offlineDevice.productId),
                false,
                offlineDevice.lastHeard
        );
    }


    private void pruneDeviceMap(List<ParticleDevice> latestCloudDeviceList) {
        synchronized (devices) {
            // make a copy of the current keyset since we mutate `devices` below
            Set<String> currentDeviceIds = Sets.newHashSet(devices.keySet());
            Set<String> newDeviceIds = FluentIterable.from(latestCloudDeviceList)
                    .transform(toDeviceId)
                    .toSet();
            // quoting the Sets docs for this next operation:
            // "The returned set contains all elements that are contained by set1 and
            //  not contained by set2"
            // In short, this set is all the device IDs which we have in our devices map,
            // but which we did not hear about in this latest update from the cloud
            Set<String> toRemove = Sets.difference(currentDeviceIds, newDeviceIds);
            for (String deviceId : toRemove) {
                devices.remove(deviceId);
            }
        }
    }


    private static final Function<ParticleDevice, String> toDeviceId =
            new Function<ParticleDevice, String>() {
                @Override
                public String apply(ParticleDevice input) {
                    return input.deviceState.deviceId;
                }
            };


    private class TokenDelegate implements ParticleAccessToken.ParticleAccessTokenDelegate {

        @Override
        public void accessTokenExpiredAt(final ParticleAccessToken accessToken, Date expirationDate) {
            // handle auto-renewal of expired access tokens by internal timer event
            // If user is null, don't bother because we have no credentials.
            if (user != null) {
                try {
                    logIn(user.getUser(), user.getPassword());
                    return;

                } catch (ParticleCloudException e) {
                    log.e("Error while trying to log in: ", e);
                }
            }

            ParticleAccessToken.removeSession();
            token = null;
        }
    }
    //endregion


    private static Maps.EntryTransformer<String, String, VariableType> sMapTransformer =
            new EntryTransformer<String, String, VariableType>() {
        @Override
        public VariableType transformEntry(String key, String value) {
            switch (value) {
                case "int32":
                    return VariableType.INT;
                case "double":
                    return VariableType.DOUBLE;
                case "string":
                    return VariableType.STRING;
                default:
                    return null;
            }
        }
    };

}
