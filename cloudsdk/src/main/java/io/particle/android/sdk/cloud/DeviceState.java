package io.particle.android.sdk.cloud;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Date;

import io.particle.android.sdk.cloud.ParticleDevice.VariableType;
import io.particle.android.sdk.utils.Parcelables;


// FIXME: I'm about ready to give up on trying to make this actually immutable.  Bah.
// Instead, make an IDeviceState or something, which is an immutable interface, and then have a
// MutableDeviceState class which will also have setters, and only expose the mutable concrete
// class to whatever class ends up doing the device state management; *everything* else only ever
// gets to see IDeviceState objects.  (This might interfere with using Parcelable though.)
// FIXME: is device "state" really the right naming here?
class DeviceState implements Parcelable {

    final String deviceId;
    final String name;
    final boolean isConnected;
    final ImmutableSet<String> functions;
    final ImmutableMap<String, VariableType> variables;
    final String version;
    final ParticleDevice.ParticleDeviceType deviceType;
    final boolean requiresUpdate;
    final Date lastHeard;

    DeviceState(String deviceId, String name, boolean isConnected, ImmutableSet<String> functions,
                ImmutableMap<String, VariableType> variables, @Nullable String version,
                ParticleDevice.ParticleDeviceType deviceType, boolean requiresUpdate, Date lastHeard) {
        this.deviceId = deviceId;
        this.name = name;
        this.isConnected = isConnected;
        this.functions = functions;
        this.variables = variables;
        this.version = version == null ? "" : version;
        this.deviceType = deviceType;
        this.requiresUpdate = requiresUpdate;
        this.lastHeard = lastHeard;
    }

    //region ImmutabilityPhun
    // The following static builder methods are awkward and a little absurd, but they still seem
    // better than the alternatives.  If we have to add another couple mutable fields though, it
    // might be time to reconsider this...
    static DeviceState withNewName(@NonNull DeviceState other, String newName) {
        return new DeviceState(
                other.deviceId,
                // NEW STATE:
                newName,
                other.isConnected,
                other.functions,
                other.variables,
                other.version,
                other.deviceType,
                other.requiresUpdate,
                other.lastHeard
        );
    }


    static DeviceState withNewConnectedState(@NonNull DeviceState other, boolean newConnectedState) {
        return new DeviceState(
                other.deviceId,
                other.name,
                // NEW STATE:
                newConnectedState,
                other.functions,
                other.variables,
                other.version,
                other.deviceType,
                other.requiresUpdate,
                other.lastHeard
        );
    }
    //endregion

    //region Parcelable
    private DeviceState(Parcel in) {
        deviceId = in.readString();
        name = in.readString();
        isConnected = Parcelables.readBoolean(in);
        functions = ImmutableSet.copyOf(Parcelables.readStringList(in));
        variables = ImmutableMap.copyOf(Parcelables.<VariableType>readSerializableMap(in));
        version = in.readString();
        deviceType = ParticleDevice.ParticleDeviceType.valueOf(in.readString());
        requiresUpdate = Parcelables.readBoolean(in);
        lastHeard = new Date(in.readLong());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceId);
        dest.writeString(name);
        Parcelables.writeBoolean(dest, isConnected);
        dest.writeStringList(functions.asList());
        Parcelables.writeSerializableMap(dest, variables);
        dest.writeString(version);
        dest.writeString(deviceType.name());
        Parcelables.writeBoolean(dest, requiresUpdate);
        dest.writeLong(lastHeard.getTime());
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
}
