package io.particle.android.sdk.cloud;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.particle.android.sdk.utils.EZ;
import okio.Okio;
import retrofit.RetrofitError;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedFile;

import static io.particle.android.sdk.utils.Py.all;
import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.map;
import static io.particle.android.sdk.utils.Py.set;


// FIXME:
// this should be just a proxy to ParticleCloud, and should hold no state of its own,
// create a separate DeviceState class or something to wrap up all this stuff, and use some
// getters to expose the fields
public class ParticleDevice {


    public enum ParticleDeviceType {
        CORE,
        PHOTON;

        public static ParticleDeviceType fromInt(int intValue) {
            switch (intValue) {
                case 0:
                    return CORE;
                case 5:
                case 6:
                default:
                    return PHOTON;
            }
        }
    }


    public class FunctionDoesNotExistException extends Exception {

        public FunctionDoesNotExistException(String functionName) {
            super("Function " + functionName + " does not exist on this device");
        }
    }


    public class VariableDoesNotExistException extends Exception {

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


    public static Builder newBuilder() {
        return new Builder();
    }


    private static final int MAX_PARTICLE_FUNCTION_ARG_LENGTH = 63;

    private final ApiDefs.CloudApi mainApi;
    private final LocalBroadcastManager broadcastManager;
    private final ParticleCloud cloud;

    private volatile String deviceId;
    private volatile String name;
    private volatile boolean isConnected;
    private volatile boolean isFlashing;
    private volatile List<String> functions = list();
    private volatile Map<String, Object> variables = map();
    private volatile String version;
    private volatile ParticleDeviceType deviceType;
    private volatile boolean requiresUpdate;


    // No public constructor -- use the supplied Builder class via ParticleDevice.newBuilder()
    private ParticleDevice(ApiDefs.CloudApi mainApi, LocalBroadcastManager broadcastManager,
                           ParticleCloud cloud, String deviceId, String name, boolean isConnected,
                           List<String> functions, Map<String, Object> variables, String version,
                           ParticleDeviceType deviceType, boolean requiresUpdate, boolean isFlashing) {
        this.mainApi = mainApi;
        this.broadcastManager = broadcastManager;
        this.cloud = cloud;
        this.deviceId = deviceId;
        this.name = name;
        this.isConnected = isConnected;
        this.functions = functions;
        this.variables = variables;
        this.version = version;
        this.deviceType = deviceType;
        this.requiresUpdate = requiresUpdate;
        this.isFlashing = isFlashing;
    }

    /**
     * Device ID string
     */
    public String getID() {
        return deviceId;
    }

    /**
     * Device name. Device can be renamed in the cloud via #setName(String)
     */
    public String getName() {
        return name;
    }

    /**
     * Rename the device in the cloud. If renaming fails name will stay the same.
     */
    public void setName(@NonNull String newName) throws ParticleCloudException {
        // FIXME: later on look into what iOS ends up doing here.
        String oldName = name;
        name = newName;
        // update now, to immediately show the change in the UI
        broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
        try {
            mainApi.nameDevice(deviceId, newName);
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
        } catch (RetrofitError e) {
            // oops, change the name back.
            name = oldName;
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
            throw new ParticleCloudException(e);
        }
    }

    /**
     * Is device connected to the cloud?
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * List of function names exposed by device
     */
    public Set<String> getFunctions() {
        // defensive copy
        return set(functions);
    }

    /**
     * Dictionary of exposed variables on device with their respective types.
     */
    public Map<String, Object> getVariables() {
        // defensive copy
        Map<String, Object> copy = new ArrayMap<>();
        copy.putAll(variables);
        return copy;
    }

    /**
     * Device firmware version string
     */
    public String getVersion() {
        return version;
    }

    public boolean requiresUpdate() {
        return requiresUpdate;
    }

    public ParticleDeviceType getDeviceType() {
        return deviceType;
    }

    /**
     * Retrieve a variable value from the device
     *
     * @param variableName Variable name
     * @return result value
     */
    @WorkerThread
    public int getVariable(@NonNull String variableName)
            throws ParticleCloudException, IOException, VariableDoesNotExistException {
        if (!variables.containsKey(variableName))
            throw new VariableDoesNotExistException(variableName);

        Responses.ReadVariableResponse reply;
        try {
            reply = mainApi.getVariable(deviceId, variableName);
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }

        this.isConnected = reply.coreInfo.connected;
        if (!reply.coreInfo.connected) {
            throw new IOException("Device is not connected.");
        } else {
            return reply.result;
        }
    }

    /**
     * Call a function on the device
     *
     * @param functionName Function name
     * @param args         Array of arguments to pass to the function on the device.
     *                     Arguments must not be more than MAX_PARTICLE_FUNCTION_ARG_LENGTH chars
     *                     in length. If any arguments are longer, a runtime exception will be thrown.
     * @return value of 1 represents success
     */
    @WorkerThread
    public int callFunction(@NonNull String functionName, @Nullable List<String> args)
            throws ParticleCloudException, IOException, FunctionDoesNotExistException {
        // TODO: check response of calling a non-existent function
        if (!functions.contains(functionName)) {
            throw new FunctionDoesNotExistException(functionName);
        }

        // null is accepted here, but it won't be in the Retrofit API call later
        if (args == null) {
            args = list();
        }

        String argsString = StringUtils.join(args, ",");
        Preconditions.checkArgument(argsString.length() < MAX_PARTICLE_FUNCTION_ARG_LENGTH,
                String.format("Arguments '%s' exceed max args length of %d",
                        argsString, MAX_PARTICLE_FUNCTION_ARG_LENGTH));

        Responses.CallFunctionResponse response;
        try {
            response = mainApi.callFunction(deviceId, functionName, new FunctionArgs(argsString));
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }

        this.isConnected = response.connected;
        if (!response.connected) {
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
    public int callFunction(@NonNull String functionName) throws ParticleCloudException, IOException,
            FunctionDoesNotExistException {
        return callFunction(functionName, null);
    }

    // FIXME: support event handling
    // FIXME: Also, stop taking "Object" here.  Bah.
    private void addEventHandler(String eventName, Object eventHandler) {

    }

    private void removeEventHandler(String eventName) {

    }

    /**
     * Remove device from current logged in user account
     */
    @WorkerThread
    public void unclaim() throws ParticleCloudException {
        try {
            mainApi.unclaimDevice(deviceId);
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }
    }

    public boolean isRunningTinker() {
        List<String> lowercaseFunctions = list();
        for (String func : functions) {
            lowercaseFunctions.add(func.toLowerCase());
        }
        List<String> tinkerFunctions = list("analogread", "analogwrite", "digitalread", "digitalwrite");
        return (isConnected() && lowercaseFunctions.containsAll(tinkerFunctions));
    }

    public boolean isFlashing() {
        return isFlashing;
    }

    @WorkerThread
    public void flashKnownApp(@NonNull final KnownApp knownApp) throws ParticleCloudException {
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashKnownApp(deviceId, knownApp.appName);
            }
        });
    }

    @WorkerThread
    public void flashBinaryFile(@NonNull final File file) throws ParticleCloudException {
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashFile(deviceId, new TypedFile("application/octet-stream", file));
            }
        });
    }

    @WorkerThread
    public void flashBinaryFile(final InputStream stream) throws ParticleCloudException, IOException {
        final byte[] bytes = Okio.buffer(Okio.source(stream)).readByteArray();
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                // TypedByteArray
                mainApi.flashFile(deviceId, new TypedFakeFile(bytes));
            }
        });
    }

    public ParticleCloud getCloud() {
        return cloud;
    }

    private void resetFlashingState() {
        isFlashing = false;
        // FIXME: ugh, this is another side effect of swapping out SparkDevices all the time.
        // impl the suggesting at the top of this class to resolve.
        try {
            ParticleDevice currentDevice = cloud.getDevice(deviceId);  // gets the current device
            currentDevice.isFlashing = false;
        } catch (ParticleCloudException e) {
            e.printStackTrace();
        }
        broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
    }

    private interface FlashingChange {
        void executeFlashingChange() throws RetrofitError;
    }

    private void performFlashingChange(FlashingChange flashingChange) throws ParticleCloudException {
        try {
            flashingChange.executeFlashingChange();
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
            isFlashing = true;
            EZ.runOnMainThreadDelayed(30000, new Runnable() {
                @Override
                public void run() {
                    resetFlashingState();
                }
            });
        } catch (RetrofitError e) {
            resetFlashingState();
            throw new ParticleCloudException(e);
        } finally {
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
        }
    }


    // FIXME: these methods are called out in the ParticleDevice.h file
    // but aren't actually implemented in ParticleDevice.m?!  (How does
    // that even compile?)  Anyway, impl this.
//    public void compileAndFlash(String sourceCode) {
//
//    }
//
//    public void flash(byte[] binary) {
//
//    }


    // ############## BUILDER ################
    // this is long enough to warrant its own class, but I hate separating
    // classes from their builders unless it's truly unwieldy
    public static class Builder {

        private transient ApiDefs.CloudApi mainApi;
        private transient LocalBroadcastManager broadcastManager;
        private transient ParticleCloud cloud;

        @SerializedName("id")
        private String deviceId;

        @SerializedName("name")
        private String name;

        @SerializedName("connected")
        private boolean isConnected;

        @SerializedName("variables")
        private Map<String, Object> variables;

        @SerializedName("functions")
        private List<String> functions;

        @SerializedName("cc3000_patch_version")
        private String version;

        @SerializedName("product_id")
        private int productId;

        @SerializedName("device_needs_update")
        private boolean requiresUpdate;

        private transient ParticleDeviceType deviceType;
        private transient boolean isFlashing;

        private void validate() {
            // FIXME: check for any other required fields here
            if (!all(mainApi, broadcastManager, cloud, deviceId, deviceType)) {
                throw new IllegalStateException(String.format(
                        "One or more required arguments were not set on builder. " +
                                "mainApi=%s, broadcastManager=%s, cloud=%s, deviceId=%s, deviceType=%s",
                        mainApi, broadcastManager, cloud, deviceId, deviceType));
            }
        }

        ParticleDevice build() {
            if (deviceType == null) {
                // FIXME: expand this later.
                deviceType = productId == 0 ? ParticleDeviceType.CORE : ParticleDeviceType.PHOTON;
            }

            validate();  // throws if invalid args found

            if (variables == null) {
                variables = map();
            }
            if (functions == null) {
                functions = list();
            }

            return new ParticleDevice(mainApi, broadcastManager, cloud, deviceId, name, isConnected, functions,
                    variables, version, deviceType, requiresUpdate, isFlashing);
        }

        Builder setBroadcastManager(LocalBroadcastManager broadcastManager) {
            this.broadcastManager = broadcastManager;
            return this;
        }

        Builder setMainApi(ApiDefs.CloudApi mainApi) {
            this.mainApi = mainApi;
            return this;
        }

        Builder setParticleCloud(ParticleCloud cloud) {
            this.cloud = cloud;
            return this;
        }

        Builder setProductId(int productId) {
            this.productId = productId;
            return this;
        }

        Builder setDeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        Builder setName(String name) {
            this.name = name;
            return this;
        }

        Builder setIsConnected(boolean isConnected) {
            this.isConnected = isConnected;
            return this;
        }

        Builder setFunctions(List<String> functions) {
            this.functions = functions;
            return this;
        }

        Builder setVariables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        Builder setDeviceType(ParticleDeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        Builder setRequiresUpdate(boolean requiresUpdate) {
            this.requiresUpdate = requiresUpdate;
            return this;
        }

        Builder setIsFlashing(boolean flashing) {
            this.isFlashing = flashing;
            return this;
        }

        ApiDefs.CloudApi getMainApi() {
            return mainApi;
        }

        ParticleCloud getCloud() {
            return cloud;
        }

        LocalBroadcastManager getBroadcastManager() {
            return broadcastManager;
        }

        String getDeviceId() {
            return deviceId;
        }

        String getName() {
            return name;
        }

        boolean isConnected() {
            return isConnected;
        }

        List<String> getFunctions() {
            return functions;
        }

        Map<String, Object> getVariables() {
            return variables;
        }

        String getVersion() {
            return version;
        }

        ParticleDeviceType getDeviceType() {
            return deviceType;
        }

        boolean requiresUpdate() {
            return requiresUpdate;
        }

    }


    static class TypedFakeFile extends TypedByteArray {

        /**
         * Constructs a new typed byte array.  Sets mimeType to {@code application/unknown} if absent.
         *
         * @param bytes
         * @throws NullPointerException if bytes are null
         */
        public TypedFakeFile(byte[] bytes) {
            super("application/octet-stream", bytes);
        }

        @Override
        public String fileName() {
            return "tinker_firmware.bin";
        }
    }

}
