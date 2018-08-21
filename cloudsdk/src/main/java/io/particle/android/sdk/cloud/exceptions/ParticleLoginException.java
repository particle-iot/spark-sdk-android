package io.particle.android.sdk.cloud.exceptions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Julius.
 */
public class ParticleLoginException extends ParticleCloudException {

    public ParticleLoginException(Exception exception) {
        super(exception);
    }

    /**
     * Server-provided multi-factor auth token.  May be null.
     *
     * @return server-provided multi-factor auth token or null
     */
    public String getMfaToken() {
        if (responseData == null || responseData.getBody() == null) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(responseData.getBody());
            if (jsonObject.has("mfa_token")) {
                return jsonObject.getString("mfa_token");
            }

        } catch (JSONException ignore) {
        }
        return null;
    }
}
