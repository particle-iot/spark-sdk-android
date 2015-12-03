package io.particle.android.sdk.cloud;


public interface ParticleEventHandler {

    void onEvent(String eventName, ParticleEvent particleEvent);
    // FIXME: ugh, use a more specific exception here
    void onEventError(Exception e);

}
