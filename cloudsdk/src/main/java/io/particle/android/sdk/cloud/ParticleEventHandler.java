package io.particle.android.sdk.cloud;


public interface ParticleEventHandler extends SimpleParticleEventHandler {

    void onEvent(String eventName, ParticleEvent particleEvent);

    // FIXME: ugh, use a more specific exception here
    void onEventError(Exception e);
}
