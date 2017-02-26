package io.particle.android.sdk.cloud.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ParticleDevice;

@ParametersAreNonnullByDefault
public class DeviceStateChange implements Parcelable {
    private final ParticleDevice device;
    @NonNull private final ParticleDevice.ParticleDeviceState state;

    public DeviceStateChange(ParticleDevice device, @NonNull ParticleDevice.ParticleDeviceState state) {
        this.device = device;
        this.state = state;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.device, flags);
        dest.writeInt(this.state == ParticleDevice.ParticleDeviceState.UNKNOWN ? -1 : this.state.ordinal());
    }

    protected DeviceStateChange(Parcel in) {
        this.device = in.readParcelable(ParticleDevice.class.getClassLoader());
        int tmpState = in.readInt();
        this.state = tmpState == -1 ? ParticleDevice.ParticleDeviceState.UNKNOWN :
                ParticleDevice.ParticleDeviceState.values()[tmpState];
    }

    public static final Parcelable.Creator<DeviceStateChange> CREATOR = new Parcelable.Creator<DeviceStateChange>() {
        @Override
        public DeviceStateChange createFromParcel(Parcel source) {
            return new DeviceStateChange(source);
        }

        @Override
        public DeviceStateChange[] newArray(int size) {
            return new DeviceStateChange[size];
        }
    };

    public ParticleDevice getDevice() {
        return device;
    }

    @NonNull
    public ParticleDevice.ParticleDeviceState getState() {
        return state;
    }
}
