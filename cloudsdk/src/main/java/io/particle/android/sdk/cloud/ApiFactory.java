package io.particle.android.sdk.cloud;

import android.content.Context;
import android.net.Uri;
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

    private final Context ctx;
    private final TokenGetterDelegate tokenDelegate;
    private final OkHttpClient okHttpClient;

    public ApiFactory(Context ctx, TokenGetterDelegate tokenGetterDelegate) {
        this.ctx = ctx.getApplicationContext();
        this.tokenDelegate = tokenGetterDelegate;
        this.okHttpClient = new OkHttpClient();
        // FIXME: remove later
//        this.okHttpClient.setReadTimeout(35 * 1000, TimeUnit.MILLISECONDS);
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
        final String clientId = ctx.getString(R.string.oauth_client_id);
        final String clientSecret = ctx.getString(R.string.oauth_client_secret);
        return "Basic " + Base64.encodeToString(
                String.format("%s:%s", clientId, clientSecret).getBytes(),
                Base64.NO_WRAP);
    }

    private RestAdapter.Builder buildCommonRestAdapterBuilder() {
        return new RestAdapter.Builder()
                .setClient(new OkClient(okHttpClient))
                .setEndpoint(getApiUri().toString())
                .setLogLevel(LogLevel.valueOf(ctx.getString(R.string.http_log_level)));
    }

}
