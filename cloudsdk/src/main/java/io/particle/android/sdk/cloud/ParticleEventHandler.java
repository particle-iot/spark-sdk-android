package io.particle.android.sdk.cloud;


public interface ParticleEventHandler extends SimpleParticleEventHandler {

    // FIXME: ugh, use a more specific exception here
    void onEventError(Exception e);
}
