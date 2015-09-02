package io.particle.android.sdk.cloud;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import io.particle.android.sdk.cloud.ApiDefs.CloudApi;
import io.particle.android.sdk.cloud.ApiDefs.IdentityApi;
import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider;
import io.particle.android.sdk.cloud.ApiFactory.ResourceValueBasicAuthCredentialsProvider;
import io.particle.android.sdk.cloud.ApiFactory.TokenGetterDelegate;


// FIXME: there are a lot of details lacking in this class, but it's not public API, and the
// structure makes it easy enough to do something better later on.
class SDKProvider {

    private final Context ctx;
    private final CloudApi cloudApi;
    private final IdentityApi identityApi;
    private final ParticleCloud particleCloud;
    private final TokenGetterDelegateImpl tokenGetter;

    SDKProvider(Context context,
                @Nullable OauthBasicAuthCredentialsProvider oAuthCredentialsProvider) {

        this.ctx = context.getApplicationContext();

        if (oAuthCredentialsProvider == null) {
            oAuthCredentialsProvider = new ResourceValueBasicAuthCredentialsProvider(
                    ctx, R.string.oauth_client_id, R.string.oauth_client_secret);
        }

        tokenGetter = new TokenGetterDelegateImpl();

        ApiFactory apiFactory = new ApiFactory(ctx, tokenGetter, oAuthCredentialsProvider);
        cloudApi = apiFactory.buildNewCloudApi();
        identityApi = apiFactory.buildNewIdentityApi();
        particleCloud = buildCloud();
    }


    CloudApi getCloudApi() {
        return cloudApi;
    }

    IdentityApi getIdentityApi() {
        return identityApi;
    }

    ParticleCloud getParticleCloud() {
        return particleCloud;
    }


    private ParticleCloud buildCloud() {
        SDKGlobals.init(ctx);

        // FIXME: see if this TokenGetterDelegate setter issue can be resolved reasonably
        ParticleCloud cloud = new ParticleCloud(cloudApi, identityApi,
                SDKGlobals.getAppDataStorage(), LocalBroadcastManager.getInstance(ctx));
        // FIXME: gross circular dependency
        tokenGetter.cloud = cloud;

        return cloud;
    }


    private static class TokenGetterDelegateImpl implements TokenGetterDelegate {

        private volatile ParticleCloud cloud;

        @Override
        public String getTokenValue() {
            return cloud.getAccessToken();
        }
    }
}
