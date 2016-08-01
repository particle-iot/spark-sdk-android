package io.particle.android.sdk.cloud;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ApiDefs.CloudApi;
import io.particle.android.sdk.cloud.ParallelDeviceFetcher.DeviceFetchResult;
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType;
import io.particle.android.sdk.cloud.ParticleDevice.VariableType;
import io.particle.android.sdk.cloud.Responses.Models;
import io.particle.android.sdk.cloud.Responses.Models.CompleteDevice;
import io.particle.android.sdk.cloud.Responses.Models.SimpleDevice;
import io.particle.android.sdk.persistance.AppDataStorage;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.Funcy.Func;
import io.particle.android.sdk.utils.Py.PySet;
import io.particle.android.sdk.utils.TLog;
import retrofit.RetrofitError;

import static io.particle.android.sdk.utils.Py.all;
import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.set;
import static io.particle.android.sdk.utils.Py.truthy;


// FIXME: move device state management out to another class
// FIXME: move some of the type conversion junk out of this into another class, too

// this is an SDK; it's expected it won't reference all its own methods
@SuppressWarnings({"UnusedDeclaration"})
@ParametersAreNonnullByDefault
public class ParticleCloud {

    private static final TLog log = TLog.get(ParticleCloud.class);

    /**
     * Singleton instance of ParticleCloud class
     *
     * @return ParticleCloud
     * @deprecated use {@link ParticleCloudSDK#getCloud()} instead.  This interface will be removed
     * some time before the 1.0 release.
     */
    @Deprecated
    public synchronized static ParticleCloud get(Context context) {
        log.w("ParticleCloud.get() is deprecated and will be removed before the 1.0 release. " +
                "Use ParticleCloudSDK.getCloud() instead!");
        if (!ParticleCloudSDK.isInitialized()) {
            ParticleCloudSDK.init(context);
        }
        return ParticleCloudSDK.getCloud();
    }

    private final ApiDefs.CloudApi mainApi;
    private final ApiDefs.IdentityApi identityApi;
    // FIXME: document why this exists (and try to make it not exist...)
    private final ApiDefs.CloudApi deviceFastTimeoutApi;
    private final AppDataStorage appDataStorage;
    private final TokenDelegate tokenDelegate = new TokenDelegate();
    private final LocalBroadcastManager broadcastManager;
    private final EventsDelegate eventsDelegate;
    private final ParallelDeviceFetcher parallelDeviceFetcher;

    private final Map<String, ParticleDevice> devices = new ArrayMap<>();

    // We should be able to mark these both @Nullable, but Android Studio has been incorrectly
    // inferring that these could be null in code blocks which _directly follow a null check_.
    // Try again later after a few more releases, I guess...
//    @Nullable
    private volatile ParticleAccessToken token;
    //    @Nullable
    private volatile ParticleUser user;

    ParticleCloud(Uri schemeAndHostname,
                  ApiDefs.CloudApi mainApi,
                  ApiDefs.IdentityApi identityApi,
                  ApiDefs.CloudApi perDeviceFastTimeoutApi,
                  AppDataStorage appDataStorage, LocalBroadcastManager broadcastManager,
                  Gson gson, ExecutorService executor) {
        this.mainApi = mainApi;
        this.identityApi = identityApi;
        this.deviceFastTimeoutApi = perDeviceFastTimeoutApi;
        this.appDataStorage = appDataStorage;
        this.broadcastManager = broadcastManager;
        this.user = ParticleUser.fromSavedSession();
        this.token = ParticleAccessToken.fromSavedSession();
        if (this.token != null) {
            this.token.setDelegate(new TokenDelegate());
        }
        this.eventsDelegate = new EventsDelegate(mainApi, schemeAndHostname, gson, executor, this);
        this.parallelDeviceFetcher = ParallelDeviceFetcher.newFetcherUsingExecutor(executor);
    }

    //region general public API

    /**
     * Current session access token string.  Can be null.
     */
    @Nullable
    public String getAccessToken() {
        return (this.token == null) ? null : this.token.getAccessToken();
    }

    public void setAccessToken(String tokenString, Date expirationDate) {
        ParticleAccessToken.removeSession();
        this.token = ParticleAccessToken.fromTokenData(expirationDate, tokenString);
        this.token.setDelegate(tokenDelegate);
    }

    /**
     * Currently logged in user name, or null if no session exists
     */
    @Nullable
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
    public void logIn(String user, String password) throws ParticleCloudException {
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
    public void signUpWithUser(String user, String password) throws ParticleCloudException {
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
    public void signUpAndLogInWithCustomer(String email, String password, String orgSlug)
            throws ParticleCloudException {
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

            pruneDeviceMap(simpleDevices);

            return result;

        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }

    // FIXME: devise a less temporary way to expose this method
    // FIXME: stop the duplication that's happening here
    // FIXME: ...think harder about this whole thing.  This is unique in that it's the only
    // operation that could _partially_ succeed.
    @WorkerThread
    List<ParticleDevice> getDevicesParallel(boolean useShortTimeout)
            throws PartialDeviceListResultException, ParticleCloudException {
        List<Models.SimpleDevice> simpleDevices;
        try {
            simpleDevices = mainApi.getDevices();
            appDataStorage.saveUserHasClaimedDevices(truthy(simpleDevices));


            // divide up into online and offline
            List<Models.SimpleDevice> offlineDevices = list();
            List<Models.SimpleDevice> onlineDevices = list();

            for (Models.SimpleDevice simpleDevice : simpleDevices) {
                List<Models.SimpleDevice> targetList = (simpleDevice.isConnected)
                        ? onlineDevices
                        : offlineDevices;
                targetList.add(simpleDevice);
            }


            List<ParticleDevice> result = list();

            // handle the offline devices
            for (SimpleDevice offlineDevice : offlineDevices) {
                result.add(getOfflineDevice(offlineDevice));
            }


            // handle the online devices
            CloudApi apiToUse = (useShortTimeout)
                    ? deviceFastTimeoutApi
                    : mainApi;
            // FIXME: don't hardcode this here
            int timeoutInSecs = useShortTimeout ? 5 : 35;
            Collection<DeviceFetchResult> results = parallelDeviceFetcher.fetchDevicesInParallel(
                    onlineDevices, apiToUse, timeoutInSecs);

            // FIXME: make this logic more elegant
            boolean shouldThrowIncompleteException = false;
            for (DeviceFetchResult fetchResult : results) {
                // fetchResult shouldn't be null, but...
                // FIXME: eliminate this ambiguity ^^^, it's either possible that it's null, or it isn't.
                if (fetchResult == null || fetchResult.fetchedDevice == null) {
                    shouldThrowIncompleteException = true;
                } else {
                    result.add(getDevice(fetchResult.fetchedDevice, false));
                }
            }

            pruneDeviceMap(simpleDevices);

            if (shouldThrowIncompleteException) {
                throw new PartialDeviceListResultException(result);
            }

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
    public ParticleDevice getDevice(String deviceID) throws ParticleCloudException {
        return getDevice(deviceID, true);
    }

    /**
     * Claim the specified device to the currently logged in user (without claim code mechanism)
     *
     * @param deviceID the deviceID
     */
    @WorkerThread
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
    public Responses.ClaimCodeResponse generateClaimCodeForOrg(String orgSlug, String productSlug)
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
    public void requestPasswordReset(String email) throws ParticleCloudException {
        try {
            identityApi.requestPasswordReset(email);
        } catch (RetrofitError error) {
            throw new ParticleCloudException(error);
        }
    }
    //endregion


    //region Events pub/sub methods

    /**
     * Subscribe to events from one specific device. If the API user has the device claimed, then
     * she will receive all events, public and private, published by that device.  If the API user
     * does not own the device she will only receive public events.
     *
     * @param eventName       The name for the event
     * @param event           A JSON-formatted string to use as the event payload
     * @param eventVisibility An IntDef "enum" determining the visibility of the event
     * @param timeToLive      TTL, or Time To Live: a piece of event metadata representing the
     *                        number of seconds that the event data is still considered relevant.
     *                        After the TTL has passed, event listeners should consider the
     *                        information stale or out of date.
     *                        e.g.: an outdoor temperature reading might have a TTL of somewhere
     *                        between 600 (10 minutes) and 1800 (30 minutes).  The geolocation of a
     *                        large piece of farm equipment which remains stationary most of the
     *                        time but may be moved to a different field once in a while might
     *                        have a TTL of 86400 (24 hours).
     */
    @WorkerThread
    public void publishEvent(String eventName, String event,
                             @ParticleEventVisibility int eventVisibility, int timeToLive)
            throws ParticleCloudException {
        eventsDelegate.publishEvent(eventName, event, eventVisibility, timeToLive);
    }

    /**
     * Subscribe to the <em>firehose</em> of public events, plus all private events published by
     * the devices the API user owns.
     *
     * @param eventNamePrefix A string to filter on for events.  If null, all events will be matched.
     * @param handler         The ParticleEventHandler to receive the events
     * @return a unique subscription ID for the eventListener that's been registered.  This ID is
     * used to unsubscribe this event listener later.
     */
    @WorkerThread
    public long subscribeToAllEvents(@Nullable String eventNamePrefix, ParticleEventHandler handler)
            throws IOException {
        return eventsDelegate.subscribeToAllEvents(eventNamePrefix, handler);
    }

    /**
     * Subscribe to all events, public and private, published by devices owned by the logged-in account.
     * <p>
     * see {@link #subscribeToAllEvents(String, ParticleEventHandler)} for info on the
     * arguments and return value.
     */
    @WorkerThread
    public long subscribeToMyDevicesEvents(@Nullable String eventNamePrefix,
                                           ParticleEventHandler handler)
            throws IOException {
        return eventsDelegate.subscribeToMyDevicesEvents(eventNamePrefix, handler);
    }

    /**
     * Subscribe to events from a specific device.
     * <p>
     * If the API user has claimed the device, then she will receive all events, public and private,
     * published by this device.  If the API user does <em>not</em> own the device, she will only
     * receive public events.
     *
     * @param deviceID the device to listen to events from
     *                 <p>
     *                 see {@link #subscribeToAllEvents(String, ParticleEventHandler)} for info on the
     *                 arguments and return value.
     */
    @WorkerThread
    public long subscribeToDeviceEvents(@Nullable String eventNamePrefix, String deviceID,
                                        ParticleEventHandler eventHandler)
            throws IOException {
        return eventsDelegate.subscribeToDeviceEvents(eventNamePrefix, deviceID, eventHandler);
    }

    /**
     * Unsubscribe event listener from events.
     *
     * @param eventListenerID The ID of the event listener you want to unsubscribe from events
     */
    @WorkerThread
    public void unsubscribeFromEventWithID(long eventListenerID) throws ParticleCloudException {
        eventsDelegate.unsubscribeFromEventWithID(eventListenerID);
    }
    //endregion


    //region package-only API
    @WorkerThread
    void unclaimDevice(String deviceId) {
        mainApi.unclaimDevice(deviceId);
        synchronized (devices) {
            devices.remove(deviceId);
        }
        sendUpdateBroadcast();
    }

    @WorkerThread
    void changeDeviceName(String deviceId, String newName) throws ParticleCloudException {
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
    void onDeviceNotConnected(DeviceState deviceState) {
        DeviceState newState = DeviceState.withNewConnectedState(deviceState, false);
        updateDeviceState(newState, true);
    }

    // FIXME: exposing this is weak, figure out something better
    void notifyDeviceChanged() {
        sendUpdateBroadcast();
    }

    // this is accessible at the package level for access from ParticleDevice's Parcelable impl
    ParticleDevice getDeviceFromState(DeviceState deviceState) {
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

        return getDevice(deviceCloudModel, sendUpdate);
    }

    private ParticleDevice getDevice(CompleteDevice deviceModel, boolean sendUpdate) {
        DeviceState newDeviceState = fromCompleteDevice(deviceModel);
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
        // FIXME: we're sometimes getting back nulls in the list of functions...  WUT?
        // Once analytics are in place, look into adding something here so we know where
        // this is coming from.  In the meantime, filter out nulls from this list, since that's
        // obviously doubleplusungood.
        Set<String> functions = set(Funcy.filter(completeDevice.functions, Funcy.<String>notNull()));
        Map<String, VariableType> variables = transformVariables(completeDevice);

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
        Set<String> functions = new HashSet<>();
        Map<String, VariableType> variables = new ArrayMap<>();
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


    private static Map<String, VariableType> transformVariables(CompleteDevice completeDevice) {
        if (completeDevice.variables == null) {
            return Collections.emptyMap();
        }

        Map<String, VariableType> variables = new ArrayMap<>();

        for (Entry<String, String> entry : completeDevice.variables.entrySet()) {
            if (!all(entry.getKey(), entry.getValue())) {
                log.w(String.format(
                        "Found null key and/or value for variable in device $1%s.  key=$2%s, value=$3%s",
                        completeDevice.name, entry.getKey(), entry.getValue()));
                continue;
            }

            VariableType variableType = toVariableType.apply(entry.getValue());
            if (variableType == null) {
                log.w(String.format("Unknown variable type for device $1%s: '$2%s'",
                        completeDevice.name, entry.getKey()));
                continue;
            }

            variables.put(entry.getKey(), variableType);
        }

        return variables;
    }


    private void pruneDeviceMap(List<SimpleDevice> latestCloudDeviceList) {
        synchronized (devices) {
            // make a copy of the current keyset since we mutate `devices` below
            PySet<String> currentDeviceIds = set(devices.keySet());
            PySet<String> newDeviceIds = set(Funcy.transformList(latestCloudDeviceList, toDeviceId));
            // quoting the Sets docs for this next operation:
            // "The returned set contains all elements that are contained by set1 and
            //  not contained by set2"
            // In short, this set is all the device IDs which we have in our devices map,
            // but which we did not hear about in this latest update from the cloud
            Set<String> toRemove = currentDeviceIds.getDifference(newDeviceIds);
            for (String deviceId : toRemove) {
                devices.remove(deviceId);
            }
        }
    }


    private static final Func<SimpleDevice, String> toDeviceId = new Func<SimpleDevice, String>() {
        @Override
        public String apply(SimpleDevice input) {
            return input.id;
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


    private static Func<String, VariableType> toVariableType = new Func<String, VariableType>() {
                @Override
                public VariableType apply(@Nullable String value) {
                    if (value == null) {
                        return null;
                    }

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


    // FIXME: review and polish this.  The more I think about it, the more I like it, but
    // make sure it's what we _really_ want.   Maybe apply it to the regular getDevices() too?
    public static class PartialDeviceListResultException extends Exception {

        final List<ParticleDevice> devices;

        public PartialDeviceListResultException(List<ParticleDevice> devices, Exception cause) {
            super(cause);
            this.devices = devices;
        }

        public PartialDeviceListResultException(List<ParticleDevice> devices, RetrofitError error) {
            super(error);
            this.devices = devices;
        }

        public PartialDeviceListResultException(List<ParticleDevice> devices) {
            super("Undefined error while fetching devices");
            this.devices = devices;
        }
    }

}
