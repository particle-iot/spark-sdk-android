package io.particle.android.sdk.cloud;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.ParticleInternalStringUtils;
import io.particle.android.sdk.utils.TLog;
import okio.BufferedSource;
import okio.Okio;
import retrofit.RetrofitError;

import static io.particle.android.sdk.utils.Py.list;


// Heavily inspired by RetrofitError, which we are mostly wrapping here, but
// we're making our own exception to make it a checked exception, and to avoid
// tying the API to a particular library used by the API's implementation
@ParametersAreNonnullByDefault
public class ParticleCloudException extends Exception {

    private static final TLog log = TLog.get(ParticleCloudException.class);

    /** Identifies the event kind which triggered a {@link ParticleCloudException}. */
    public enum Kind {

        /** An {@link java.io.IOException} occurred while communicating to the server. */
        NETWORK,

        /** An exception was thrown while (de)serializing a body. */
        CONVERSION,

        /** A non-200 HTTP status code was received from the server. */
        HTTP,

        /**
         * An internal error occurred while attempting to execute a request.  It is best practice to
         * re-throw this exception so your application intentionally crashes.
         */
        UNEXPECTED
    }


    public static class ResponseErrorData {

        private final int httpStatusCode;
        private final InputStream httpBodyInputStream;

        private String lazyLoadedBody;
        private boolean isBodyLoaded;

        ResponseErrorData(int httpStatusCode, InputStream httpBodyInputStream) {
            this.httpStatusCode = httpStatusCode;
            this.httpBodyInputStream = httpBodyInputStream;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        /**
         * @return response body as a String, or null if no body was returned.
         */
        public String getBody() {
            if (!isBodyLoaded) {
                isBodyLoaded = true;
                lazyLoadedBody = loadBody();
            }
            return lazyLoadedBody;
        }

        private String loadBody() {
            if (httpBodyInputStream == null) {
                return null;
            }
            BufferedSource buffer = null;
            try {
                buffer = Okio.buffer(Okio.source(httpBodyInputStream));
                return buffer.readUtf8();

            } catch (IOException e) {
                log.i("Error reading HTTP response body: ", e);
                return null;

            } finally {
                EZ.closeThisThingOrMaybeDont(buffer);
            }
        }

    }


    private final RetrofitError innerError;
    private final ResponseErrorData responseData;
    private boolean checkedForServerErrorMsg = false;
    private String serverErrorMessage;

    public ParticleCloudException(Exception exception) {
        super(exception);
        // FIXME: ugly hack to get around even uglier bug.
        this.innerError = RetrofitError.unexpectedError("(URL UNKNOWN)", exception);
        this.responseData = null;
    }

    ParticleCloudException(RetrofitError innerError) {
        this.innerError = innerError;
        this.responseData = buildResponseData(innerError);
    }

    /**
     * Response containing HTTP status code & body.
     *
     * May be null depending on the nature of the error.
     */
    public ResponseErrorData getResponseData() {
        return responseData;
    }

    /** The event kind which triggered this error. */
    public Kind getKind() {
        return Kind.valueOf(innerError.getKind().toString());
    }

    /**
     * Any server-provided error message.  May be null.
     *
     * If the server sent multiple errors, they will be concatenated together with newline characters.
     *
     * @return server-provided error or null
     */
    public String getServerErrorMsg() {
        if (!checkedForServerErrorMsg) {
            checkedForServerErrorMsg = true;
            serverErrorMessage = loadServerErrorMsg();
        }
        return serverErrorMessage;
    }

    /**
     * Returns a server provided message, if found, else just returns the result of the inner
     * exception's .getMessage()
     */
    public String getBestMessage() {
        // FIXME: this isn't the right place for user-facing data
        if (getKind() == Kind.NETWORK ) {
            return "Unable to connect to the server.";

        } else if (getKind() == Kind.UNEXPECTED) {
            return "Unknown error communicating with server.";
        }
        String serverMsg = getServerErrorMsg();
        return (serverMsg == null) ? getMessage() : serverMsg;
    }

    private String loadServerErrorMsg() {
        if (responseData == null || responseData.getBody() == null) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(responseData.getBody());

            if (jsonObject.has("error_description")) {
                return jsonObject.getString("error_description");

            } else if (jsonObject.has("errors")) {
                List<String> errors = getErrors(jsonObject);
                return errors.isEmpty() ? null : ParticleInternalStringUtils.join(errors, '\n');

            } else if (jsonObject.has("error")) {
                return jsonObject.getString("error");
            }

        } catch (JSONException e) {
        }
        return null;
    }

    private List<String> getErrors(JSONObject jsonObject) throws JSONException {
        List<String> errors = list();
        JSONArray jsonArray = jsonObject.getJSONArray("errors");
        if (jsonArray == null || jsonArray.length() == 0) {
            return errors;
        }
        for (int i=0; i < jsonArray.length(); i++){
            String msg = null;

            JSONObject msgObj = jsonArray.optJSONObject(i);
            if (msgObj != null) {
                msg = msgObj.getString("message");
            } else {
                msg = jsonArray.get(i).toString();
            }

            errors.add(msg);
        }

        return errors;
    }


    private ResponseErrorData buildResponseData(RetrofitError error) {
        if (error.getResponse() == null) {
            return null;
        }

        InputStream in = null;
        if (error.getResponse().getBody() != null) {
            try {
                in = error.getResponse().getBody().in();
            } catch (IOException e) {
                // Yo, dawg, I heard you like error handling in your error handling...
            }
        }
        return new ResponseErrorData(error.getResponse().getStatus(), in);
    }

}
