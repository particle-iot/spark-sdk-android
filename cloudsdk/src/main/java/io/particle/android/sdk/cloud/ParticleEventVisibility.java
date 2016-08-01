package io.particle.android.sdk.cloud;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@IntDef({ParticleEventVisibility.PRIVATE,
        ParticleEventVisibility.PUBLIC})
@Retention(RetentionPolicy.SOURCE)
public @interface ParticleEventVisibility {
    int PRIVATE = 1;
    int PUBLIC = 2;
}
