package io.particle.android.sdk.cloud;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ApiDefs.CloudApi;
import io.particle.android.sdk.cloud.ApiDefs.IdentityApi;
import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider;
import io.particle.android.sdk.cloud.ApiFactory.ResourceValueBasicAuthCredentialsProvider;
import io.particle.android.sdk.cloud.ApiFactory.TokenGetterDelegate;


// FIXME: there are a lot of details lacking in this class, but it's not public API, and the
// structure makes it easy enough to do something better later on.
@ParametersAreNonnullByDefault
class SDKProvider {

    private final Context ctx;
    private final CloudApi cloudApi;
    private final CloudApi fastTimeoutCloudApi;
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
        fastTimeoutCloudApi = apiFactory.buildNewFastTimeoutCloudApi();
        particleCloud = buildCloud(apiFactory);
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


    private ParticleCloud buildCloud(ApiFactory apiFactory) {
        SDKGlobals.init(ctx);

        // FIXME: see if this TokenGetterDelegate setter issue can be resolved reasonably
        ParticleCloud cloud = new ParticleCloud(
                apiFactory.getApiUri(), cloudApi, identityApi, fastTimeoutCloudApi,
                SDKGlobals.getAppDataStorage(), LocalBroadcastManager.getInstance(ctx),
                apiFactory.getGsonInstance(), buildExecutor());
        // FIXME: gross circular dependency
        tokenGetter.cloud = cloud;

        return cloud;
    }


    private static ExecutorService buildExecutor() {
        // lifted from AsyncTask's executor config
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        int CORE_POOL_SIZE = CPU_COUNT + 1;
        int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        int KEEP_ALIVE = 1;
        // FIXME: how big should this queue be?
        BlockingQueue<Runnable> poolWorkQueue = new LinkedBlockingQueue<>(1024);
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);
            public Thread newThread(Runnable r) {
                return new Thread(r, "Particle Exec #" + mCount.getAndIncrement());
            }
        };

        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                TimeUnit.SECONDS, poolWorkQueue, threadFactory);
    }


    private static class TokenGetterDelegateImpl implements TokenGetterDelegate {

        private volatile ParticleCloud cloud;

        @Override
        public String getTokenValue() {
            return cloud.getAccessToken();
        }
    }
}
