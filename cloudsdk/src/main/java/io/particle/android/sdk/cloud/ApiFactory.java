package io.particle.android.sdk.cloud;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.util.Base64;

import com.squareup.okhttp.OkHttpClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.client.OkClient;

/**
 * Constructs ParticleCloud instances
 */
public class ApiFactory {

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
    private final OkHttpClient okHttpClient;
    private final OauthBasicAuthCredentialsProvider basicAuthCredentialsProvider;

    public ApiFactory(Context ctx, TokenGetterDelegate tokenGetterDelegate) {
        this(ctx, tokenGetterDelegate,
                new ResourceValueBasicAuthCredentialsProvider(
                        ctx.getApplicationContext(),
                        R.string.oauth_client_id,
                        R.string.oauth_client_secret));
    }

    public ApiFactory(Context ctx, TokenGetterDelegate tokenGetterDelegate,
                      OauthBasicAuthCredentialsProvider basicAuthProvider) {
        this.ctx = ctx.getApplicationContext();
        this.tokenDelegate = tokenGetterDelegate;
        this.okHttpClient = new OkHttpClient();
        this.basicAuthCredentialsProvider = basicAuthProvider;
    }


    public ApiDefs.CloudApi buildCloudApi() {
        RestAdapter restAdapter = buildCommonRestAdapterBuilder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", "Bearer " + tokenDelegate.getTokenValue());
                    }
                })
                .build();
        return restAdapter.create(ApiDefs.CloudApi.class);
    }

    public ApiDefs.IdentityApi buildIdentityApi() {
        final String basicAuthValue = getBasicAuthValue();

        RestAdapter restAdapter = buildCommonRestAdapterBuilder()
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", basicAuthValue);
                    }
                })
                .build();
        return restAdapter.create(ApiDefs.IdentityApi.class);
    }

    public Uri getApiUri() {
        return Uri.parse(ctx.getString(R.string.api_base_uri));
    }

    private String getBasicAuthValue() {
        String authString = String.format("%s:%s",
                basicAuthCredentialsProvider.getClientId(),
                basicAuthCredentialsProvider.getClientSecret());
        return "Basic " + Base64.encodeToString(authString.getBytes(), Base64.NO_WRAP);
    }

    private RestAdapter.Builder buildCommonRestAdapterBuilder() {
        return new RestAdapter.Builder()
                .setClient(new OkClient(okHttpClient))
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
