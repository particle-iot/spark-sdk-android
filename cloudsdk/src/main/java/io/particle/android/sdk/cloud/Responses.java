package io.particle.android.sdk.cloud;

import com.google.gson.annotations.SerializedName;

/**
 * All API responses, collected together in one outer class for simplicity's sake.
 */
public class Responses {

    /**
     * ...and to go with the responses, a series of model objects only
     * used internally when dealing with the REST API, never returned
     * outside of the cloudapi package.
     */
    static class Models {


        public static class CoreInfo {

            @SerializedName("last_app")
            public final String lastApp;

            @SerializedName("last_heard")
            public final String lastHeard;

            public final boolean connected;

            public final String deviceId;

            public CoreInfo(String lastApp, String lastHeard, boolean connected, String deviceId) {
                this.lastApp = lastApp;
                this.lastHeard = lastHeard;
                this.connected = connected;
                this.deviceId = deviceId;
            }
        }


        /**
         * Represents a single Particle device in the list returned
         * by a call to "GET /v1/devices"
         */
        public static class SimpleDevice {

            public final String id;
            public final String name;
            @SerializedName("connected")
            public final boolean isConnected;
            @SerializedName("product_id")
            public final int productId;

            public SimpleDevice(String id, String name, boolean isConnected, int productId) {
                this.id = id;
                this.name = name;
                this.isConnected = isConnected;
                this.productId = productId;
            }
        }
    }


    public static class TokenResponse {

        public final String token;

        public TokenResponse(String token) {
            this.token = token;
        }
    }


    public static class CallFunctionResponse {

        @SerializedName("id")
        public final String deviceId;

        @SerializedName("name")
        public final String deviceName;

        public final boolean connected;

        @SerializedName("return_value")
        public final int returnValue;

        public CallFunctionResponse(String deviceId, String deviceName, boolean connected, int returnValue) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
            this.connected = connected;
            this.returnValue = returnValue;
        }
    }


    public static class LogInResponse {

        @SerializedName("expires_in")
        public final long expiresInSeconds;

        @SerializedName("access_token")
        public final String accessToken;

        @SerializedName("token_type")
        public final String tokenType;

        public LogInResponse(long expiresInSeconds, String accessToken, String tokenType) {
            this.expiresInSeconds = expiresInSeconds;
            this.accessToken = accessToken;
            this.tokenType = tokenType;
        }
    }


    public static class SimpleResponse {

        public final boolean ok;
        public final String error;

        public SimpleResponse(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        @Override
        public String toString() {
            return "SimpleResponse [ok=" + ok + ", error=" + error + "]";
        }
    }


    public static class ClaimCodeResponse {

        @SerializedName("claim_code")
        public final String claimCode;

        @SerializedName("device_ids")
        public final String[] deviceIds;

        public ClaimCodeResponse(String claimCode, String[] deviceIds) {
            this.claimCode = claimCode;
            this.deviceIds = deviceIds;
        }
    }


    public static class ReadVariableResponse {

        @SerializedName("cmd")
        public final String commandName;

        @SerializedName("name")
        public final String variableName;

        public final int result;

        public final Models.CoreInfo coreInfo;

        public ReadVariableResponse(String commandName, String variableName, int result, Models.CoreInfo coreInfo) {
            this.commandName = commandName;
            this.variableName = variableName;
            this.result = result;
            this.coreInfo = coreInfo;
        }
    }
}
