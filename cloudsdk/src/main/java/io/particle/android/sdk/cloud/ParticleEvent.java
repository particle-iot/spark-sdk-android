package io.particle.android.sdk.cloud;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Date;


// Normally it's bad form to use network data models as API data models, but considering that
// for the moment, they'd be a 1:1 mapping, we'll just reuse this data model class.  If the
// network API changes, then we can write new classes for the network API models, without
// impacting the public API of the SDK.
public class ParticleEvent {

    @SerializedName("coreid")
    public final String deviceId;

    @SerializedName("data")
    public final String dataPayload;

    @SerializedName("published_at")
    public final Date publishedAt;

    @SerializedName("ttl")
    public final int timeToLive;

    public ParticleEvent(@NonNull String deviceId, @NonNull String dataPayload,
                         @NonNull Date publishedAt, int timeToLive) {
        this.deviceId = deviceId;
        this.dataPayload = dataPayload;
        this.publishedAt = publishedAt;
        this.timeToLive = timeToLive;
    }
}
