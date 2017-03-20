package io.particle.android.sdk.cloud;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.Responses.ReadDoubleVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadIntVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadObjectVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadStringVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadVariableResponse;
import io.particle.android.sdk.cloud.models.DeviceStateChange;
import io.particle.android.sdk.utils.ParticleInternalStringUtils;
import io.particle.android.sdk.utils.Preconditions;
import io.particle.android.sdk.utils.TLog;
import okio.Okio;
import retrofit.RetrofitError;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedFile;

import static io.particle.android.sdk.utils.Py.list;


// don't warn about public APIs not being referenced inside this module, or about
// the _default locale_ in a bunch of backend code
@SuppressLint("DefaultLocale")
@SuppressWarnings({"UnusedDeclaration"})
@ParametersAreNonnullByDefault
public class ParticleDevice implements Parcelable {

    public enum ParticleDeviceType {
        CORE,
        PHOTON,
        ELECTRON;

        public static ParticleDeviceType fromInt(int intValue) {
            switch (intValue) {
                case 0:
                    return CORE;
                case 10:
                    return ELECTRON;
                case 5:
                case 6:
                default:
                    return PHOTON;
            }
        }
    }

    public enum ParticleDeviceState {
        CAME_ONLINE,
        FLASH_STARTED,
        FLASH_SUCCEEDED,
        FLASH_FAILED,
        APP_HASH_UPDATED,
        ENTERED_SAFE_MODE,
        SAFE_MODE_UPDATER,
        WENT_OFFLINE,
        UNKNOWN
    }

    public enum VariableType {
        INT,
        DOUBLE,
        STRING
    }


    public static class FunctionDoesNotExistException extends Exception {

        public FunctionDoesNotExistException(String functionName) {
            super("Function " + functionName + " does not exist on this device");
        }
    }


    public static class VariableDoesNotExistException extends Exception {

        public VariableDoesNotExistException(String variableName) {
            super("Variable " + variableName + " does not exist on this device");
        }
    }


    public enum KnownApp {
        TINKER("tinker");

        private final String appName;

        KnownApp(String appName) {
            this.appName = appName;
        }

        public String getAppName() {
            return appName;
        }
    }

    private static final int MAX_PARTICLE_FUNCTION_ARG_LENGTH = 63;

    private static final TLog log = TLog.get(ParticleDevice.class);

    private final CopyOnWriteArrayList<Long> subscriptions = new CopyOnWriteArrayList<>();
    private final ApiDefs.CloudApi mainApi;
    private final ParticleCloud cloud;

    volatile DeviceState deviceState;

    private volatile boolean isFlashing = false;

    ParticleDevice(ApiDefs.CloudApi mainApi, ParticleCloud cloud, DeviceState deviceState) {
        this.mainApi = mainApi;
        this.cloud = cloud;
        this.deviceState = deviceState;
    }

    /**
     * Device ID string
     */
    public String getID() {
        return deviceState.deviceId;
    }

    /**
     * Device name. Device can be renamed in the cloud via #setName(String)
     */
    public String getName() {
        return deviceState.name;
    }

    /**
     * Rename the device in the cloud. If renaming fails name will stay the same.
     */
    public void setName(String newName) throws ParticleCloudException {
        cloud.rename(this.deviceState.deviceId, newName);
    }

    /**
     * Is device connected to the cloud?
     */
    public boolean isConnected() {
        return deviceState.isConnected;
    }

    /**
     * Get an immutable set of all the function names exposed by device
     */
    public Set<String> getFunctions() {
        // no need for a defensive copy, this is an immutable set
        return deviceState.functions;
    }

    /**
     * Get an immutable map of exposed variables on device with their respective types.
     */
    public Map<String, VariableType> getVariables() {
        // no need for a defensive copy, this is an immutable set
        return deviceState.variables;
    }

    /**
     * Device firmware version string
     */
    public String getVersion() {
        return deviceState.version;
    }

    public boolean requiresUpdate() {
        return deviceState.requiresUpdate;
    }

    public ParticleDeviceType getDeviceType() {
        return deviceState.deviceType;
    }

    public int getPlatformID() {
        return deviceState.platformId;
    }

    public int getProductID() {
        return deviceState.productId;
    }

    public boolean isCellular() {
        return deviceState.cellular;
    }

    public String getImei() {
        return deviceState.imei;
    }

    public String getCurrentBuild() {
        return deviceState.currentBuild;
    }

    public String getDefaultBuild() {
        return deviceState.defaultBuild;
    }

    public String getIpAddress() {
        return deviceState.ipAddress;
    }

    public String getLastAppName() {
        return deviceState.lastAppName;
    }

    public String getStatus() {
        return deviceState.status;
    }

    public Date getLastHeard() {
        return deviceState.lastHeard;
    }

    /**
     * Return the value for <code>variableName</code> on this Particle device.
     * <p>
     * Unless you specifically require generic handling, it is recommended that you use the
     * <code>get(type)Variable</code> methods instead, e.g.:  <code>getIntVariable()</code>.
     * These type-specific methods don't require extra casting or type checking on your part, and
     * they more clearly and succinctly express your intent.
     */
    @WorkerThread
    public Object getVariable(String variableName)
            throws ParticleCloudException, IOException, VariableDoesNotExistException {

        VariableRequester<Object, ReadObjectVariableResponse> requester =
                new VariableRequester<Object, ReadObjectVariableResponse>(this) {
                    @Override
                    ReadObjectVariableResponse callApi(String variableName) {
                        return mainApi.getVariable(deviceState.deviceId, variableName);
                    }
                };

        return requester.getVariable(variableName);
    }

    /**
     * Return the value for <code>variableName</code> as an int.
     * <p>
     * Where practical, this method is recommended over the generic {@link #getVariable(String)}.
     * See the javadoc on that method for details.
     */
    @WorkerThread
    public int getIntVariable(String variableName) throws ParticleCloudException,
            IOException, VariableDoesNotExistException, ClassCastException {

        VariableRequester<Integer, ReadIntVariableResponse> requester =
                new VariableRequester<Integer, ReadIntVariableResponse>(this) {
                    @Override
                    ReadIntVariableResponse callApi(String variableName) {
                        return mainApi.getIntVariable(deviceState.deviceId, variableName);
                    }
                };

        return requester.getVariable(variableName);
    }

    /**
     * Return the value for <code>variableName</code> as a String.
     * <p>
     * Where practical, this method is recommended over the generic {@link #getVariable(String)}.
     * See the javadoc on that method for details.
     */
    @WorkerThread
    public String getStringVariable(String variableName) throws ParticleCloudException,
            IOException, VariableDoesNotExistException, ClassCastException {

        VariableRequester<String, ReadStringVariableResponse> requester =
                new VariableRequester<String, ReadStringVariableResponse>(this) {
                    @Override
                    ReadStringVariableResponse callApi(String variableName) {
                        return mainApi.getStringVariable(deviceState.deviceId, variableName);
                    }
                };

        return requester.getVariable(variableName);
    }

    /**
     * Return the value for <code>variableName</code> as a double.
     * <p>
     * Where practical, this method is recommended over the generic {@link #getVariable(String)}.
     * See the javadoc on that method for details.
     */
    @WorkerThread
    public double getDoubleVariable(String variableName) throws ParticleCloudException,
            IOException, VariableDoesNotExistException, ClassCastException {

        VariableRequester<Double, ReadDoubleVariableResponse> requester =
                new VariableRequester<Double, ReadDoubleVariableResponse>(this) {
                    @Override
                    ReadDoubleVariableResponse callApi(String variableName) {
                        return mainApi.getDoubleVariable(deviceState.deviceId, variableName);
                    }
                };

        return requester.getVariable(variableName);
    }


    /**
     * Call a function on the device
     *
     * @param functionName Function name
     * @param args         Array of arguments to pass to the function on the device.
     *                     Arguments must not be more than MAX_PARTICLE_FUNCTION_ARG_LENGTH chars
     *                     in length. If any arguments are longer, a runtime exception will be thrown.
     * @return result code: a value of 1 indicates success
     */
    @WorkerThread
    public int callFunction(String functionName, @Nullable List<String> args)
            throws ParticleCloudException, IOException, FunctionDoesNotExistException {
        // TODO: check response of calling a non-existent function
        if (!deviceState.functions.contains(functionName)) {
            throw new FunctionDoesNotExistException(functionName);
        }

        // null is accepted here, but it won't be in the Retrofit API call later
        if (args == null) {
            args = list();
        }

        String argsString = ParticleInternalStringUtils.join(args, ',');
        Preconditions.checkArgument(argsString.length() < MAX_PARTICLE_FUNCTION_ARG_LENGTH,
                String.format("Arguments '%s' exceed max args length of %d",
                        argsString, MAX_PARTICLE_FUNCTION_ARG_LENGTH));

        Responses.CallFunctionResponse response;
        try {
            response = mainApi.callFunction(deviceState.deviceId, functionName,
                    new FunctionArgs(argsString));
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }

        if (!response.connected) {
            cloud.onDeviceNotConnected(deviceState);
            throw new IOException("Device is not connected.");
        } else {
            return response.returnValue;
        }
    }

    /**
     * Call a function on the device
     *
     * @param functionName Function name
     * @return value of the function
     */
    @WorkerThread
    public int callFunction(String functionName) throws ParticleCloudException, IOException,
            FunctionDoesNotExistException {
        return callFunction(functionName, null);
    }

    /**
     * Subscribe to events from this device
     *
     * @param eventNamePrefix (optional, may be null) a filter to match against for events.  If
     *                        null or an empty string, all device events will be received by the handler
     *                        trigger eventHandler
     * @param handler         The handler for the events received for this subscription.
     * @return the subscription ID
     * (see {@link ParticleCloud#subscribeToAllEvents(String, ParticleEventHandler)} for more info
     */
    public long subscribeToEvents(@Nullable String eventNamePrefix,
                                  ParticleEventHandler handler)
            throws IOException {
        return cloud.subscribeToDeviceEvents(eventNamePrefix, deviceState.deviceId, handler);
    }

    /**
     * Unsubscribe from events.
     *
     * @param eventListenerID The ID of the subscription to be cancelled. (returned from
     *                        {@link #subscribeToEvents(String, ParticleEventHandler)}
     */
    public void unsubscribeFromEvents(long eventListenerID) throws ParticleCloudException {
        cloud.unsubscribeFromEventWithID(eventListenerID);
    }

    /**
     * Remove device from current logged in user account
     */
    @WorkerThread
    public void unclaim() throws ParticleCloudException {
        try {
            cloud.unclaimDevice(deviceState.deviceId);
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }
    }

    public boolean isRunningTinker() {
        List<String> lowercaseFunctions = list();
        for (String func : deviceState.functions) {
            lowercaseFunctions.add(func.toLowerCase());
        }
        List<String> tinkerFunctions = list("analogread", "analogwrite", "digitalread", "digitalwrite");
        return (isConnected() && lowercaseFunctions.containsAll(tinkerFunctions));
    }

    public boolean isFlashing() {
        return isFlashing;
    }

    @WorkerThread
    public void flashKnownApp(final KnownApp knownApp) throws ParticleCloudException {
        performFlashingChange(() -> mainApi.flashKnownApp(deviceState.deviceId, knownApp.appName));
    }

    @WorkerThread
    public void flashBinaryFile(final File file) throws ParticleCloudException {
        performFlashingChange(() -> mainApi.flashFile(deviceState.deviceId,
                new TypedFile("application/octet-stream", file)));
    }

    @WorkerThread
    public void flashBinaryFile(InputStream stream) throws ParticleCloudException, IOException {
        final byte[] bytes = Okio.buffer(Okio.source(stream)).readByteArray();
        performFlashingChange(() -> mainApi.flashFile(deviceState.deviceId, new TypedFakeFile(bytes)));
    }

    @WorkerThread
    public void flashCodeFile(final File file) throws ParticleCloudException {
        performFlashingChange(() -> mainApi.flashFile(deviceState.deviceId,
                new TypedFile("multipart/form-data", file)));
    }


    @WorkerThread
    public void flashCodeFile(InputStream stream) throws ParticleCloudException, IOException {
        final byte[] bytes = Okio.buffer(Okio.source(stream)).readByteArray();
        performFlashingChange(() -> mainApi.flashFile(deviceState.deviceId, new TypedFakeFile(bytes, "multipart/form-data", "code.ino")));
    }

    public ParticleCloud getCloud() {
        return cloud;
    }

    @WorkerThread
    public void refresh() throws ParticleCloudException {
        // just calling this get method will update everything as expected.
        cloud.getDevice(deviceState.deviceId);
    }

    private interface FlashingChange {
        void executeFlashingChange() throws RetrofitError;
    }

    // FIXME: ugh.  these "cloud.notifyDeviceChanged();" calls are a hint that flashing maybe
    // should just live in a class of its own, or that it should just be a delegate on
    // ParticleCloud.  Review this later.
    private void performFlashingChange(FlashingChange flashingChange) throws ParticleCloudException {
        try {
            flashingChange.executeFlashingChange();
            //listens for flashing event, on success unsubscribe from listening.
            subscribeToSystemEvent("spark/flash/status", new SimpleParticleEventHandler() {
                @Override
                public void onEvent(String eventName, ParticleEvent particleEvent) {
                    if ("success".equals(particleEvent.dataPayload)) {
                        isFlashing = false;
                        try {
                            ParticleDevice.this.refresh();
                            cloud.unsubscribeFromEventWithHandler(this);
                        } catch (ParticleCloudException e) {
                            // not much else we can really do here...
                            log.w("Unable to reset flashing state for %s" + deviceState.deviceId, e);
                        }
                    } else {
                        isFlashing = true;
                    }
                    cloud.notifyDeviceChanged();
                }
            });
        } catch (RetrofitError | IOException e) {
            throw new ParticleCloudException(e);
        }
    }

    /**
     * Subscribes to system events of current device. Events emitted to EventBus listener.
     *
     * @throws ParticleCloudException Failure to subscribe to system events.
     * @see <a href="https://github.com/greenrobot/EventBus">EventBus</a>
     */
    @MainThread
    public void subscribeToSystemEvents() throws ParticleCloudException {
        try {
            EventBus eventBus = EventBus.getDefault();
            subscriptions.add(subscribeToSystemEvent("spark/status", (eventName, particleEvent) ->
                    sendUpdateStatusChange(eventBus, particleEvent.dataPayload)));
            subscriptions.add(subscribeToSystemEvent("spark/flash/status", (eventName, particleEvent) ->
                    sendUpdateFlashChange(eventBus, particleEvent.dataPayload)));
            subscriptions.add(subscribeToSystemEvent("spark/device/app-hash", (eventName, particleEvent) ->
                    sendSystemEventBroadcast(new DeviceStateChange(ParticleDevice.this,
                            ParticleDeviceState.APP_HASH_UPDATED), eventBus)));
            subscriptions.add(subscribeToSystemEvent("spark/status/safe-mode", (eventName, particleEvent) ->
                    sendSystemEventBroadcast(new DeviceStateChange(ParticleDevice.this,
                            ParticleDeviceState.SAFE_MODE_UPDATER), eventBus)));
            subscriptions.add(subscribeToSystemEvent("spark/safe-mode-updater/updating", (eventName, particleEvent) ->
                    sendSystemEventBroadcast(new DeviceStateChange(ParticleDevice.this,
                            ParticleDeviceState.ENTERED_SAFE_MODE), eventBus)));
        } catch (IOException e) {
            log.d("Failed to auto-subscribe to system events");
            throw new ParticleCloudException(e);
        }
    }

    private void sendSystemEventBroadcast(DeviceStateChange deviceStateChange, EventBus eventBus) {
        cloud.sendSystemEventBroadcast(deviceStateChange);
        eventBus.post(deviceStateChange);
    }

    /**
     * Unsubscribes from system events of current device.
     *
     * @throws ParticleCloudException Failure to unsubscribe from system events.
     */
    public void unsubscribeFromSystemEvents() throws ParticleCloudException {
        for (Long subscriptionId : subscriptions) {
            unsubscribeFromEvents(subscriptionId);
        }
    }

    private long subscribeToSystemEvent(String eventNamePrefix,
                                        SimpleParticleEventHandler
                                                particleEventHandler) throws IOException {
        //Error would be handled in same way for every event name prefix, thus only simple onEvent listener is needed
        return subscribeToEvents(eventNamePrefix, new ParticleEventHandler() {
            @Override
            public void onEvent(String eventName, ParticleEvent particleEvent) {
                particleEventHandler.onEvent(eventName, particleEvent);
            }

            @Override
            public void onEventError(Exception e) {
                log.d("Event error in system event handler");
            }
        });
    }

    private void sendUpdateStatusChange(EventBus eventBus, String data) {
        DeviceStateChange deviceStateChange = null;
        switch (data) {
            case "online":
                sendSystemEventBroadcast(new DeviceStateChange(this, ParticleDeviceState.CAME_ONLINE),
                        eventBus);
                break;
            case "offline":
                sendSystemEventBroadcast(new DeviceStateChange(this, ParticleDeviceState.WENT_OFFLINE),
                        eventBus);
                break;
        }
    }

    private void sendUpdateFlashChange(EventBus eventBus, String data) {
        DeviceStateChange deviceStateChange = null;
        switch (data) {
            case "started":
                sendSystemEventBroadcast(new DeviceStateChange(this, ParticleDeviceState.FLASH_STARTED),
                        eventBus);
                break;
            case "success":
                sendSystemEventBroadcast(new DeviceStateChange(this, ParticleDeviceState.FLASH_SUCCEEDED),
                        eventBus);
                break;
        }
    }

    @Override
    public String toString() {
        return "ParticleDevice{" +
                "deviceId=" + deviceState.deviceId +
                ", isConnected=" + deviceState.isConnected +
                '}';
    }

    //region Parcelable
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(deviceState, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static final Creator<ParticleDevice> CREATOR = new Creator<ParticleDevice>() {
        @Override
        public ParticleDevice createFromParcel(Parcel in) {
            SDKProvider sdkProvider = ParticleCloudSDK.getSdkProvider();
            DeviceState deviceState = in.readParcelable(DeviceState.class.getClassLoader());
            return sdkProvider.getParticleCloud().getDeviceFromState(deviceState);
        }

        @Override
        public ParticleDevice[] newArray(int size) {
            return new ParticleDevice[size];
        }
    };
    //endregion


    private static class TypedFakeFile extends TypedByteArray {

        private final String fileName;

        /**
         * Constructs a new typed byte array.  Sets mimeType to {@code application/unknown} if absent.
         *
         * @throws NullPointerException if bytes are null
         */
        public TypedFakeFile(byte[] bytes) {
            this(bytes, "application/octet-stream", "tinker_firmware.bin");
        }

        public TypedFakeFile(byte[] bytes, String mimeType, String fileName) {
            super(mimeType, bytes);
            this.fileName = fileName;
        }

        @Override
        public String fileName() {
            return fileName;
        }
    }


    private static abstract class VariableRequester<T, R extends ReadVariableResponse<T>> {

        @WorkerThread
        abstract R callApi(String variableName);


        private final ParticleDevice device;

        VariableRequester(ParticleDevice device) {
            this.device = device;
        }


        @WorkerThread
        T getVariable(String variableName)
                throws ParticleCloudException, IOException, VariableDoesNotExistException {

            if (!device.deviceState.variables.containsKey(variableName)) {
                throw new VariableDoesNotExistException(variableName);
            }

            R reply;
            try {
                reply = callApi(variableName);
            } catch (RetrofitError e) {
                throw new ParticleCloudException(e);
            }

            if (!reply.coreInfo.connected) {
                // FIXME: we should be doing this "connected" check on _any_ reply that comes back
                // with a "coreInfo" block.
                device.cloud.onDeviceNotConnected(device.deviceState);
                throw new IOException("Device is not connected.");
            } else {
                return reply.result;
            }
        }

    }

}
