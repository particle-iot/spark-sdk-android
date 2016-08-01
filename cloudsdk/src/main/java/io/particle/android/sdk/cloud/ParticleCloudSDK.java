package io.particle.android.sdk.cloud;

import android.content.Context;
import android.support.annotation.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider;
import io.particle.android.sdk.utils.TLog;

/**
 * Entry point for the Particle Cloud SDK.
 */
@ParametersAreNonnullByDefault
public class ParticleCloudSDK {
    // NOTE: pay attention to the interface, try to ignore the implementation, it's going to change.

    /**
     * Initialize the cloud SDK.  Must be called somewhere in your Application.onCreate()
     *
     * (or anywhere else before your first Activity.onCreate() is called)
     */
    public static void init(Context ctx) {
        initWithParams(ctx, null);
    }

    public static void initWithOauthCredentialsProvider(
            Context ctx, @Nullable OauthBasicAuthCredentialsProvider oauthProvider) {
        initWithParams(ctx, oauthProvider);
    }

    public static ParticleCloud getCloud() {
        verifyInitCalled();
        return instance.sdkProvider.getParticleCloud();
    }


    // NOTE: This is closer to the interface I'd like to provide eventually
    static void initWithParams(Context ctx,
                               @Nullable OauthBasicAuthCredentialsProvider oauthProvider) {
        if (instance != null) {
            log.w("Calling ParticleCloudSDK.init() more than once does not re-initialize the SDK.");
            return;
        }

        Context appContext = ctx.getApplicationContext();
        SDKProvider sdkProvider = new SDKProvider(appContext, oauthProvider);
        instance = new ParticleCloudSDK(sdkProvider);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isInitialized() {
        return instance != null;
    }

    static SDKProvider getSdkProvider() {
        verifyInitCalled();
        return instance.sdkProvider;
    }

    static void verifyInitCalled() {
        if (!isInitialized()) {
            throw new IllegalStateException("init not called before using the Particle SDK. "
            + "Are you calling ParticleCloudSDK.init() in your Application.onCreate()?");
        }
    }


    private static final TLog log = TLog.get(ParticleCloudSDK.class);

    private static ParticleCloudSDK instance;


    private final SDKProvider sdkProvider;

    private ParticleCloudSDK(SDKProvider sdkProvider) {
        this.sdkProvider = sdkProvider;
    }

}
