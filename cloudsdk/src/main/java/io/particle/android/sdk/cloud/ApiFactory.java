package io.particle.android.sdk.cloud;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.util.Base64;

import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Constructs ParticleCloud instances
 */
@ParametersAreNonnullByDefault
public class ApiFactory {

    // both values are in seconds
    private static final int REGULAR_TIMEOUT = 35;
    // FIXME: find a less cheesy solution to the "rapid timeout" problem
    private static final int PER_DEVICE_FAST_TIMEOUT = 5;


    // FIXME: this feels kind of lame... but maybe it's OK in practice. Need to think more about it.
    public interface TokenGetterDelegate {

        String getTokenValue();

    }


    public interface OauthBasicAuthCredentialsProvider {

        String getClientId();

        String getClientSecret();
    }


    private final Context ctx;
    private final TokenGetterDelegate tokenDelegate;
    private final OkHttpClient normalTimeoutClient;
    private final OkHttpClient fastTimeoutClient;
    private final OauthBasicAuthCredentialsProvider basicAuthCredentialsProvider;
    private final Gson gson;

    ApiFactory(Context ctx, TokenGetterDelegate tokenGetterDelegate,
               OauthBasicAuthCredentialsProvider basicAuthProvider) {
        this.ctx = ctx.getApplicationContext();
        this.tokenDelegate = tokenGetterDelegate;
        this.basicAuthCredentialsProvider = basicAuthProvider;
        this.gson = new Gson();

        normalTimeoutClient = buildClientWithTimeout(REGULAR_TIMEOUT);
        fastTimeoutClient = buildClientWithTimeout(PER_DEVICE_FAST_TIMEOUT);
    }

    private static OkHttpClient buildClientWithTimeout(int timeoutInSeconds) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(timeoutInSeconds, TimeUnit.SECONDS);
        client.setReadTimeout(timeoutInSeconds, TimeUnit.SECONDS);
        client.setWriteTimeout(timeoutInSeconds, TimeUnit.SECONDS);
        return client;
    }

    ApiDefs.CloudApi buildNewCloudApi() {
        RestAdapter restAdapter = buildCommonRestAdapterBuilder(gson, normalTimeoutClient)
                .setRequestInterceptor(request -> request.addHeader("Authorization", "Bearer " +
                        tokenDelegate.getTokenValue()))
                .build();
        return restAdapter.create(ApiDefs.CloudApi.class);
    }

    // FIXME: fix this ugliness
    ApiDefs.CloudApi buildNewFastTimeoutCloudApi() {
        RestAdapter restAdapter = buildCommonRestAdapterBuilder(gson, fastTimeoutClient)
                .setRequestInterceptor(request -> request.addHeader("Authorization", "Bearer " +
                        tokenDelegate.getTokenValue()))
                .build();
        return restAdapter.create(ApiDefs.CloudApi.class);
    }

    ApiDefs.IdentityApi buildNewIdentityApi() {
        final String basicAuthValue = getBasicAuthValue();

        RestAdapter restAdapter = buildCommonRestAdapterBuilder(gson, normalTimeoutClient)
                .setRequestInterceptor(request -> request.addHeader("Authorization", basicAuthValue))
                .build();
        return restAdapter.create(ApiDefs.IdentityApi.class);
    }

    Uri getApiUri() {
        return Uri.parse(ctx.getString(R.string.api_base_uri));
    }

    Gson getGsonInstance() {
        return gson;
    }

    private String getBasicAuthValue() {
        String authString = String.format("%s:%s",
                basicAuthCredentialsProvider.getClientId(),
                basicAuthCredentialsProvider.getClientSecret());
        return "Basic " + Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
    }

    private RestAdapter.Builder buildCommonRestAdapterBuilder(Gson gson, OkHttpClient client) {
        return new RestAdapter.Builder()
                .setClient(new OkClient(client))
                .setConverter(new GsonConverter(gson))
                .setEndpoint(getApiUri().toString())
                .setLogLevel(LogLevel.valueOf(ctx.getString(R.string.http_log_level)));
    }


    public static class ResourceValueBasicAuthCredentialsProvider
            implements OauthBasicAuthCredentialsProvider {

        private final String clientId;
        private final String clientSecret;

        public ResourceValueBasicAuthCredentialsProvider(
                Context ctx, @StringRes int clientIdResId, @StringRes int clientSecretResId) {
            this.clientId = ctx.getString(clientIdResId);
            this.clientSecret = ctx.getString(clientSecretResId);
        }


        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public String getClientSecret() {
            return clientSecret;
        }
    }

}
