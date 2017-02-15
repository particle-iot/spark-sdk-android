package io.particle.android.sdk.cloud;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ParticleDevice.VariableType;
import io.particle.android.sdk.utils.Parcelables;


// FIXME: I'm about ready to give up on trying to make this actually immutable.  Bah.
// Instead, make an IDeviceState or something, which is an immutable interface, and then have a
// MutableDeviceState class which will also have setters, and only expose the mutable concrete
// class to whatever class ends up doing the device state management; *everything* else only ever
// gets to see IDeviceState objects.  (This might interfere with using Parcelable though.)
// FIXME: is device "state" really the right naming here?
@ParametersAreNonnullByDefault
class DeviceState implements Parcelable {

    final String deviceId;
    @Nullable final Integer platformId;
    @Nullable final Integer productId;
    @Nullable final String ipAddress;
    @Nullable final String lastAppName;
    @Nullable final String status;
    @Nullable final String name;
    @Nullable final Boolean isConnected;
    @Nullable final Boolean cellular;
    @Nullable final String imei;
    @Nullable final String currentBuild;
    @Nullable final String defaultBuild;
    final Set<String> functions;
    final Map<String, VariableType> variables;
    @Nullable final String version;
    @Nullable final ParticleDevice.ParticleDeviceType deviceType;
    @Nullable final Boolean requiresUpdate;
    @Nullable final Date lastHeard;

    DeviceState(DeviceStateBuilder deviceStateBuilder) {
        this.deviceId = deviceStateBuilder.deviceId;
        this.name = deviceStateBuilder.name;
        this.isConnected = deviceStateBuilder.isConnected;
        this.cellular = deviceStateBuilder.cellular;
        this.imei = deviceStateBuilder.imei;
        this.currentBuild = deviceStateBuilder.currentBuild;
        this.defaultBuild = deviceStateBuilder.defaultBuild;
        this.functions = deviceStateBuilder.functions;
        this.variables = deviceStateBuilder.variables;
        this.version = deviceStateBuilder.version == null ? "" : deviceStateBuilder.version;
        this.deviceType = deviceStateBuilder.deviceType;
        this.platformId = deviceStateBuilder.platformId;
        this.productId = deviceStateBuilder.productId;
        this.ipAddress = deviceStateBuilder.ipAddress;
        this.lastAppName = deviceStateBuilder.lastAppName;
        this.status = deviceStateBuilder.status;
        this.requiresUpdate = deviceStateBuilder.requiresUpdate;
        this.lastHeard = deviceStateBuilder.lastHeard;
    }

    //region ImmutabilityPhun
    // The following static builder methods are awkward and a little absurd, but they still seem
    // better than the alternatives.  If we have to add another couple mutable fields though, it
    // might be time to reconsider this...
    static DeviceState withNewName(DeviceState other, String newName) {
        return new DeviceStateBuilder(other.deviceId, other.functions, other.variables)
                .name(newName)
                .cellular(other.cellular)
                .connected(other.isConnected)
                .version(other.version)
                .deviceType(other.deviceType)
                .platformId(other.platformId)
                .productId(other.productId)
                .imei(other.imei)
                .currentBuild(other.currentBuild)
                .defaultBuild(other.defaultBuild)
                .ipAddress(other.ipAddress)
                .lastAppName(other.lastAppName)
                .status(other.status)
                .requiresUpdate(other.requiresUpdate)
                .lastHeard(other.lastHeard)
                .build();
    }


    static DeviceState withNewConnectedState(DeviceState other, boolean newConnectedState) {
        return new DeviceStateBuilder(other.deviceId, other.functions, other.variables)
                .name(other.name)
                .cellular(other.cellular)
                .connected(newConnectedState)
                .version(other.version)
                .deviceType(other.deviceType)
                .platformId(other.platformId)
                .productId(other.productId)
                .imei(other.imei)
                .currentBuild(other.currentBuild)
                .defaultBuild(other.defaultBuild)
                .ipAddress(other.ipAddress)
                .lastAppName(other.lastAppName)
                .status(other.status)
                .requiresUpdate(other.requiresUpdate)
                .lastHeard(other.lastHeard)
                .build();
    }
    //endregion

    //region Parcelable
    private DeviceState(Parcel in) {
        deviceId = in.readString();
        name = (String) in.readValue(String.class.getClassLoader());
        isConnected = (Boolean) in.readValue(Boolean.class.getClassLoader());
        functions = new HashSet<>(Parcelables.readStringList(in));
        variables = Parcelables.readSerializableMap(in);
        version = (String) in.readValue(String.class.getClassLoader());
        deviceType = ParticleDevice.ParticleDeviceType.valueOf((String) in.readValue(String.class.getClassLoader()));
        platformId = (Integer) in.readValue(Integer.class.getClassLoader());
        productId = (Integer) in.readValue(Integer.class.getClassLoader());
        cellular = (Boolean) in.readValue(Boolean.class.getClassLoader());
        imei = (String) in.readValue(String.class.getClassLoader());
        currentBuild = (String) in.readValue(String.class.getClassLoader());
        defaultBuild = (String) in.readValue(String.class.getClassLoader());
        ipAddress = (String) in.readValue(String.class.getClassLoader());
        lastAppName = (String) in.readValue(String.class.getClassLoader());
        status = (String) in.readValue(String.class.getClassLoader());
        requiresUpdate = (Boolean) in.readValue(Boolean.class.getClassLoader());
        lastHeard = new Date((Long) in.readValue(Long.class.getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceId);
        dest.writeValue(name);
        dest.writeValue(isConnected);
        dest.writeStringList(new ArrayList<>(functions));
        Parcelables.writeSerializableMap(dest, variables);
        dest.writeValue(version);
        dest.writeValue(deviceType != null ? deviceType.name() : null);
        dest.writeValue(platformId);
        dest.writeValue(productId);
        dest.writeValue(cellular);
        dest.writeValue(imei);
        dest.writeValue(currentBuild);
        dest.writeValue(defaultBuild);
        dest.writeValue(ipAddress);
        dest.writeValue(lastAppName);
        dest.writeValue(status);
        dest.writeValue(requiresUpdate);
        dest.writeValue(lastHeard != null ? lastHeard.getTime() : 0);
    }

    public static final Creator<DeviceState> CREATOR = new Creator<DeviceState>() {
        @Override
        public DeviceState createFromParcel(Parcel in) {
            return new DeviceState(in);
        }

        @Override
        public DeviceState[] newArray(int size) {
            return new DeviceState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
    //endregion

    public static class DeviceStateBuilder {
        private final String deviceId;
        @Nullable private Integer platformId;
        @Nullable private Integer productId;
        @Nullable private String ipAddress;
        @Nullable private String lastAppName;
        @Nullable private String status;
        @Nullable private String name;
        @Nullable private Boolean isConnected;
        @Nullable private Boolean cellular;
        @Nullable private String imei;
        @Nullable private String currentBuild;
        @Nullable private String defaultBuild;
        private final Set<String> functions;
        private final Map<String, ParticleDevice.VariableType> variables;
        @Nullable String version;
        @Nullable ParticleDevice.ParticleDeviceType deviceType;
        @Nullable Boolean requiresUpdate;
        @Nullable Date lastHeard;


        DeviceStateBuilder(String deviceId, Set<String> functions, Map<String, ParticleDevice.VariableType> variables) {
            this.deviceId = deviceId;
            this.functions = functions;
            this.variables = variables;
            this.version = version == null ? "" : version;
        }

        public DeviceStateBuilder platformId(@Nullable Integer platformId) {
            this.platformId = platformId;
            return this;
        }

        public DeviceStateBuilder productId(@Nullable Integer productId) {
            this.productId = productId;
            return this;
        }

        public DeviceStateBuilder ipAddress(@Nullable String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public DeviceStateBuilder lastAppName(@Nullable String lastAppName) {
            this.lastAppName = lastAppName;
            return this;
        }

        public DeviceStateBuilder status(@Nullable String status) {
            this.status = status;
            return this;
        }

        public DeviceStateBuilder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public DeviceStateBuilder connected(@Nullable Boolean connected) {
            isConnected = connected;
            return this;
        }

        public DeviceStateBuilder cellular(@Nullable Boolean cellular) {
            this.cellular = cellular;
            return this;
        }

        public DeviceStateBuilder imei(@Nullable String imei) {
            this.imei = imei;
            return this;
        }

        public DeviceStateBuilder currentBuild(@Nullable String currentBuild) {
            this.currentBuild = currentBuild;
            return this;
        }

        public DeviceStateBuilder defaultBuild(@Nullable String defaultBuild) {
            this.defaultBuild = defaultBuild;
            return this;
        }

        public DeviceStateBuilder version(@Nullable String version) {
            this.version = version;
            return this;
        }

        public DeviceStateBuilder deviceType(@Nullable ParticleDevice.ParticleDeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public DeviceStateBuilder requiresUpdate(@Nullable Boolean requiresUpdate) {
            this.requiresUpdate = requiresUpdate;
            return this;
        }

        public DeviceStateBuilder lastHeard(@Nullable Date lastHeard) {
            this.lastHeard = lastHeard;
            return this;
        }

        public DeviceState build() {
            return new DeviceState(this);
        }
    }

}
