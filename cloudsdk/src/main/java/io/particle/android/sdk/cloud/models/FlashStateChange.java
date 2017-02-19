package io.particle.android.sdk.cloud.models;

import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Parcelables;

@ParametersAreNonnullByDefault
public class FlashStateChange implements Parcelable {
    private final ParticleDevice device;
    private final boolean isFlashing;

    public FlashStateChange(ParticleDevice device, boolean isFlashing) {
        this.device = device;
        this.isFlashing = isFlashing;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.device, flags);
        Parcelables.writeBoolean(dest, this.isFlashing);
    }

    protected FlashStateChange(Parcel in) {
        this.device = in.readParcelable(ParticleDevice.class.getClassLoader());
        this.isFlashing = Parcelables.readBoolean(in);
    }

    public static final Parcelable.Creator<FlashStateChange> CREATOR = new Parcelable.Creator<FlashStateChange>() {
        @Override
        public FlashStateChange createFromParcel(Parcel source) {
            return new FlashStateChange(source);
        }

        @Override
        public FlashStateChange[] newArray(int size) {
            return new FlashStateChange[size];
        }
    };

    public ParticleDevice getDevice() {
        return device;
    }

    public boolean isFlashing() {
        return isFlashing;
    }
}
