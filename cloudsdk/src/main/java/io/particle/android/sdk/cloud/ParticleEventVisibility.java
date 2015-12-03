package io.particle.android.sdk.cloud;


import android.support.annotation.IntDef;

@IntDef({ParticleEventVisibility.PRIVATE,
        ParticleEventVisibility.PUBLIC})
public @interface ParticleEventVisibility {
    int PRIVATE = 1;
    int PUBLIC = 2;
}
