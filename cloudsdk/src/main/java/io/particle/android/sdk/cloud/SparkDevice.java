package io.particle.android.sdk.cloud;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
// this should be just a proxy to SparkCloud, and should hold NO STATE of its own!
// create a separate DeviceState class or something to wrap up all this stuff, and use some
// getters to expose the fields
public class SparkDevice {


    public enum SparkDeviceType {
        CORE,
        PHOTON;

        public static SparkDeviceType fromInt(int intValue) {
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

        public FunctionDoesNotExistException(String functionName)
        {
            super("Function "+functionName+" does not exist on this device");
        }
    }

    public class VariableDoesNotExistException extends Exception {

        public VariableDoesNotExistException(String variableName)
        {
            super("Variable "+variableName+" does not exist on this device");
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


    private static final int MAX_SPARK_FUNCTION_ARG_LENGTH = 63;

    private final ApiDefs.CloudApi mainApi;
    private final LocalBroadcastManager broadcastManager;
    private final SparkCloud cloud;

    private volatile String deviceId;
    private volatile String name;
    private volatile boolean isConnected;
    private volatile boolean isFlashing;
    private volatile List<String> functions = list();
    private volatile Map<String, Object> variables = map();
    private volatile String version;
    private volatile SparkDeviceType deviceType;
    private volatile boolean requiresUpdate;


    // No public constructor -- use the supplied Builder class via SparkDevice.newBuilder()
    private SparkDevice(ApiDefs.CloudApi mainApi, LocalBroadcastManager broadcastManager,
                        SparkCloud cloud, String deviceId, String name, boolean isConnected,
                        List<String> functions, Map<String, Object> variables, String version,
                        SparkDeviceType deviceType, boolean requiresUpdate, boolean isFlashing) {
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
     *
     * @param newName the new name to use
     */
    public void setName(String newName) throws SparkCloudException {
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
            throw new SparkCloudException(e);
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

    public SparkDeviceType getDeviceType() {
        return deviceType;
    }

    /**
     * Retrieve a variable value from the device
     *
     * @param variableName Variable name
     * @return result value
     */
    public int getVariable(String variableName) throws SparkCloudException, IOException, VariableDoesNotExistException {
        if (!variables.containsKey(variableName))
            throw new VariableDoesNotExistException(variableName);

        Responses.ReadVariableResponse reply;
        try {
            reply = mainApi.getVariable(deviceId, variableName);
        } catch (RetrofitError e) {
            throw new SparkCloudException(e);
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
     *                     Arguments must not be more than MAX_SPARK_FUNCTION_ARG_LENGTH chars
     *                     in length. If any arguments are longer, a runtime exception will be thrown.
     * @return value of 1 represents success
     */
    public int callFunction(String functionName, List<String> args) throws SparkCloudException, IOException, FunctionDoesNotExistException {
        // TODO: check response of calling a non-existent function
        if (!functions.contains(functionName))
            throw new FunctionDoesNotExistException(functionName);

        // null is accepted here, but it won't be in the Retrofit API call later
        if (args == null) {
            args = list();
        }

        String argsString = StringUtils.join(args, ",");
        Preconditions.checkArgument(argsString.length() < MAX_SPARK_FUNCTION_ARG_LENGTH,
                String.format("Arguments '%s' exceed max args length of %d",
                        argsString, MAX_SPARK_FUNCTION_ARG_LENGTH));

        Responses.CallFunctionResponse response;
        try {
            response = mainApi.callFunction(deviceId, functionName, new FunctionArgs(argsString));
        } catch (RetrofitError e) {
            throw new SparkCloudException(e);
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
    public int callFunction(String functionName) throws SparkCloudException, IOException, FunctionDoesNotExistException {
        return callFunction(functionName, new ArrayList<String>());
    }

    // FIXME: support event handling
    // FIXME: Also, stop taking "Object" here.  Bah.
    public void addEventHandler(String eventName, Object eventHandler) {

    }

    public void removeEventHandler(String eventName) {

    }

    /**
     * Remove device from current logged in user account
     */
    public void unclaim() throws SparkCloudException {
        try {
            mainApi.unclaimDevice(deviceId);
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
        } catch (RetrofitError e) {
            throw new SparkCloudException(e);
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

    public void flashKnownApp(final KnownApp knownApp) throws SparkCloudException {
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashKnownApp(deviceId, knownApp.appName);
            }
        });
    }

    public void flashBinaryFile(final File file) throws SparkCloudException {
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashFile(deviceId, new TypedFile("application/octet-stream", file));
            }
        });
    }

    public void flashBinaryFile(final InputStream stream) throws SparkCloudException, IOException {
        final byte[] bytes = Okio.buffer(Okio.source(stream)).readByteArray();
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                // TypedByteArray
                mainApi.flashFile(deviceId, new TypedFakeFile(bytes));
            }
        });
    }

    private void resetFlashingState() {
        isFlashing = false;
        // FIXME: ugh, this is another side effect of swapping out SparkDevices all the time.
        // impl the suggesting at the top of this class to resolve.
        try {
            SparkDevice currentDevice = cloud.getDevice(deviceId);  // gets the current device
            currentDevice.isFlashing = false;
        } catch (SparkCloudException e) {
            e.printStackTrace();
        }
        broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
    }

    public SparkCloud getCloud() {
        return cloud;
    }

    private interface FlashingChange {
        void executeFlashingChange() throws RetrofitError;
    }

    private void performFlashingChange(FlashingChange flashingChange) throws SparkCloudException {
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
            throw new SparkCloudException(e);
        } finally {
            broadcastManager.sendBroadcast(new Intent(BroadcastContract.BROADCAST_DEVICES_UPDATED));
        }
    }


    // FIXME: these methods are called out in the SparkDevice.h file
    // but aren't actually implemented in SparkDevice.m?!  (How does
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
        private transient SparkCloud cloud;

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

        private transient SparkDevice.SparkDeviceType deviceType;
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

        SparkDevice build() {
            if (deviceType == null) {
                // FIXME: expand this later.
                deviceType = productId == 0 ? SparkDeviceType.CORE : SparkDeviceType.PHOTON;
            }

            validate();  // throws if invalid args found

            if (variables == null) {
                variables = map();
            }
            if (functions == null) {
                functions = list();
            }

            return new SparkDevice(mainApi, broadcastManager, cloud, deviceId, name, isConnected, functions,
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

        Builder setSparkCloud(SparkCloud cloud) {
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

        Builder setDeviceType(SparkDevice.SparkDeviceType deviceType) {
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

        SparkCloud getCloud() {
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

        SparkDevice.SparkDeviceType getDeviceType() {
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
