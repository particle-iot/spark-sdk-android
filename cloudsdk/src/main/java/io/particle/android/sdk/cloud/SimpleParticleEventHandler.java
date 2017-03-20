package io.particle.android.sdk.cloud;

/**
 * Created by Julius.
 */

public interface SimpleParticleEventHandler {
    void onEvent(String eventName, ParticleEvent particleEvent);
}
