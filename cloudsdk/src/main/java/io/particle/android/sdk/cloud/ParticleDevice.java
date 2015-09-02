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




    private static final int MAX_PARTICLE_FUNCTION_ARG_LENGTH = 63;

    @NonNull
    private final ApiDefs.CloudApi mainApi;
    @NonNull
    private final ParticleCloud cloud;

    @NonNull
    volatile DeviceState deviceState;
    private volatile boolean isFlashing = false;

    ParticleDevice(@NonNull ApiDefs.CloudApi mainApi,  @NonNull ParticleCloud cloud,
                   @NonNull DeviceState deviceState) {
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
     * <p>
     * Note: apologies for the (hopefully temporary) <em>stringly typed</em> interface.  We're
     * hoping to give this real types (e.g.: an enum) soon, but in the meantime, see the docs for
     * the possible values: https://docs.particle.io/reference/firmware/photon/#data-types
     */
    public Map<String, String> getVariables() {
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

    @WorkerThread
    public int getVariable(@NonNull String variableName)
            throws ParticleCloudException, IOException, VariableDoesNotExistException {
        if (!deviceState.variables.containsKey(variableName)) {
            throw new VariableDoesNotExistException(variableName);
        }

        Responses.ReadVariableResponse reply;
        try {
            reply = mainApi.getVariable(deviceState.deviceId, variableName);
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }

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
        if (!deviceState.functions.contains(functionName)) {
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
            response = mainApi.callFunction(deviceState.deviceId, functionName,
                    new FunctionArgs(argsString));
        } catch (RetrofitError e) {
            throw new ParticleCloudException(e);
        }

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
    public void flashKnownApp(@NonNull final KnownApp knownApp) throws ParticleCloudException {
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashKnownApp(deviceState.deviceId, knownApp.appName);
            }
        });
    }

    @WorkerThread
    public void flashBinaryFile(@NonNull final File file) throws ParticleCloudException {
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashFile(deviceState.deviceId,
                        new TypedFile("application/octet-stream", file));
            }
        });
    }

    @WorkerThread
    public void flashBinaryFile(final InputStream stream) throws ParticleCloudException, IOException {
        final byte[] bytes = Okio.buffer(Okio.source(stream)).readByteArray();
        performFlashingChange(new FlashingChange() {
            @Override
            public void executeFlashingChange() throws RetrofitError {
                mainApi.flashFile(deviceState.deviceId, new TypedFakeFile(bytes));
            }
        });
    }

    @NonNull
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


    static class TypedFakeFile extends TypedByteArray {

        /**
         * Constructs a new typed byte array.  Sets mimeType to {@code application/unknown} if absent.
         *
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
