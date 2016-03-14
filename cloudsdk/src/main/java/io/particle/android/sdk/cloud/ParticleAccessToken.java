package io.particle.android.sdk.cloud;


import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.persistance.SensitiveDataStorage;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.Py;
import io.particle.android.sdk.utils.TLog;


@ParametersAreNonnullByDefault
public class ParticleAccessToken {


    public interface ParticleAccessTokenDelegate {

        void accessTokenExpiredAt(ParticleAccessToken token, Date expirationDate);

    }


    public static synchronized ParticleAccessToken fromNewSession(Responses.LogInResponse logInResponse) {
        if (logInResponse == null
                || !Py.truthy(logInResponse.accessToken)
                || !"bearer".equalsIgnoreCase(logInResponse.tokenType)) {
            throw new IllegalArgumentException("Invalid LogInResponse: " + logInResponse);
        }

        long expirationMillis = logInResponse.expiresInSeconds * 1000;
        Date expirationDate = new Date(System.currentTimeMillis() + expirationMillis);
        return fromTokenData(expirationDate, logInResponse.accessToken);
    }


    @Nullable
    public static synchronized ParticleAccessToken fromSavedSession() {
        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        String accessToken = sensitiveDataStorage.getToken();
        Date expirationDate = sensitiveDataStorage.getTokenExpirationDate();

        // are either of the fields "falsey" or has the expr date passed?
        if (!Py.truthy(accessToken) || !Py.truthy(expirationDate) || expirationDate.before(new Date())) {
            return null;
        }

        ParticleAccessToken token = new ParticleAccessToken(accessToken, expirationDate,
                new Handler(Looper.getMainLooper()));
        token.scheduleExpiration();
        return token;
    }


    public static synchronized ParticleAccessToken fromTokenData(Date expirationDate, String accessToken) {
        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        sensitiveDataStorage.saveToken(accessToken);
        sensitiveDataStorage.saveTokenExpirationDate(expirationDate);

        ParticleAccessToken token = new ParticleAccessToken(accessToken, expirationDate,
                new Handler(Looper.getMainLooper()));
        token.scheduleExpiration();
        return token;
    }


    /**
     * Remove access token session data from keychain
     */
    public static synchronized void removeSession() {
        SensitiveDataStorage sensitiveDataStorage = SDKGlobals.getSensitiveDataStorage();
        sensitiveDataStorage.resetToken();
        sensitiveDataStorage.resetTokenExpirationDate();
    }

    private static final TLog log = TLog.get(ParticleAccessToken.class);

    // how many seconds before expiry date will a token be considered expired
    // (0 = expire on expiry date, 24*60*60 = expire a day before)
    // FIXME: should this be considered configurable?
    private static final int ACCESS_TOKEN_EXPIRY_MARGIN = 0;

    private final Handler handler;

    private String accessToken;
    private Date expiryDate;

    private volatile Runnable expirationRunnable;
    private volatile ParticleAccessTokenDelegate delegate;

    private ParticleAccessToken(String accessToken, Date expiryDate, Handler handler) {
        this.accessToken = accessToken;
        this.expiryDate = expiryDate;
        this.handler = handler;
    }

    /**
     * Access token string to be used when calling cloud API
     *
     * @return null if token is expired.
     */
    public String getAccessToken() {
        if (expiryDate.getTime() + ACCESS_TOKEN_EXPIRY_MARGIN < System.currentTimeMillis()) {
            return null;
        }
        return accessToken;
    }

    /**
     * Delegate to receive didExpireAt method call whenever a token is detected as expired
     */
    public ParticleAccessTokenDelegate getDelegate() {
        return delegate;
    }

    /**
     * Set the delegate described in #getDelegate()
     */
    public void setDelegate(ParticleAccessTokenDelegate delegate) {
        this.delegate = delegate;
    }

    private void onExpiration() {
        log.d("Entering onExpiration()");
        this.expirationRunnable = null;

        if (this.delegate == null) {
            log.w("Token expiration delegate is null");
            this.accessToken = null;
            return;
        }

        // ensure that we don't call accessTokenExpiredAt() on the main thread, since
        // the delegate (in the default impl) will make a call to try logging back
        // in, but making network calls on the main thread is doubleplus ungood.
        // (It'll throw an exception if you even try this, as well it should!)
        EZ.runAsync(new Runnable() {
            @Override
            public void run() {
                delegate.accessTokenExpiredAt(ParticleAccessToken.this, expiryDate);
            }
        });
    }

    private void scheduleExpiration() {
        long delay = expiryDate.getTime() - System.currentTimeMillis();
        log.d("Scheduling token expiration for " + expiryDate + " (" + delay + "ms.");
        handler.postDelayed(new ExpirationHandler(this), delay);
    }

    // visible because I don't want to completely trust the finalizer to call this...
    void cancelExpiration() {
        if (expirationRunnable != null) {
            handler.removeCallbacks(expirationRunnable);
            expirationRunnable = null;
        }
    }

    // FIXME: finalizers are a _last resort_.  Look for something better.
    @Override
    protected void finalize() throws Throwable {
        cancelExpiration();
        super.finalize();
    }


    private static class ExpirationHandler implements Runnable {

        final WeakReference<ParticleAccessToken> tokenRef;

        private ExpirationHandler(ParticleAccessToken token) {
            this.tokenRef = new WeakReference<>(token);
        }


        @Override
        public void run() {
            log.d("Running token expiration handler...");
            ParticleAccessToken token = tokenRef.get();
            if (token == null) {
                log.d("...but the token was null, doing nothing.");
                return;
            }
            token.onExpiration();
            tokenRef.clear();
        }
    }

}
