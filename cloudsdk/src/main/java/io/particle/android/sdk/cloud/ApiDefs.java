package io.particle.android.sdk.cloud;

import java.util.List;

import io.particle.android.sdk.cloud.Responses.CallFunctionResponse;
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse;
import io.particle.android.sdk.cloud.Responses.Models;
import io.particle.android.sdk.cloud.Responses.ReadDoubleVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadIntVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadObjectVariableResponse;
import io.particle.android.sdk.cloud.Responses.ReadStringVariableResponse;
import io.particle.android.sdk.cloud.Responses.SimpleResponse;
import io.particle.android.sdk.cloud.models.SignUpInfo;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedOutput;


/**
 * Particle cloud REST APIs, modelled for the Retrofit library
 */
public class ApiDefs {

    /**
     * The main Particle cloud API
     */
    public interface CloudApi {

        @GET("/v1/sims/{lastIccid}/data_usage")
        Response getCurrentDataUsage(@Path("lastIccid") String lastIccid);

        @GET("/v1/devices")
        List<Models.SimpleDevice> getDevices();

        @GET("/v1/devices/{deviceID}")
        Models.CompleteDevice getDevice(@Path("deviceID") String deviceID);

        // FIXME: put a real response type on this?
        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}")
        Response nameDevice(@Path("deviceID") String deviceID,
                            @Field("name") String name);

        @FormUrlEncoded
        @PUT("/v1/devices/{deviceID}")
        Response flashKnownApp(@Path("deviceID") String deviceID,
                               @Field("app") String appName);

        @Multipart
        @PUT("/v1/devices/{deviceID}")
        Response flashFile(@Path("deviceID") String deviceID,
                           @Part("file") TypedOutput file);

        @POST("/v1/devices/{deviceID}/{function}")
        CallFunctionResponse callFunction(@Path("deviceID") String deviceID,
                                          @Path("function") String function,
                                          @Body FunctionArgs args);

        @GET("/v1/devices/{deviceID}/{variable}")
        ReadObjectVariableResponse getVariable(@Path("deviceID") String deviceID,
                                               @Path("variable") String variable);

        @GET("/v1/devices/{deviceID}/{variable}")
        ReadIntVariableResponse getIntVariable(@Path("deviceID") String deviceID,
                                               @Path("variable") String variable);

        @GET("/v1/devices/{deviceID}/{variable}")
        ReadStringVariableResponse getStringVariable(@Path("deviceID") String deviceID,
                                                     @Path("variable") String variable);

        @GET("/v1/devices/{deviceID}/{variable}")
        ReadDoubleVariableResponse getDoubleVariable(@Path("deviceID") String deviceID,
                                                     @Path("variable") String variable);

        @FormUrlEncoded
        @POST("/v1/devices/events")
        SimpleResponse publishEvent(@Field("name") String eventName,
                                    @Field("data") String eventData,
                                    @Field("private") boolean isPrivate,
                                    @Field("ttl") int timeToLive);

        /**
         * Newer versions of OkHttp <em>require</em> a body for POSTs, but just pass in
         * a blank string for the body and all is well.
         */
        @FormUrlEncoded
        @POST("/v1/device_claims")
        ClaimCodeResponse generateClaimCode(@Field("blank") String blankBody);

        @FormUrlEncoded
        @POST("/v1/products/{productId}/device_claims")
        ClaimCodeResponse generateClaimCodeForOrg(@Field("blank") String blankBody,
                                                  @Path("productId") Integer productId);

        @FormUrlEncoded
        @POST("/v1/orgs/{orgSlug}/products/{productSlug}/device_claims")
        @Deprecated
        ClaimCodeResponse generateClaimCodeForOrg(@Field("blank") String blankBody,
                                                  @Path("orgSlug") String orgSlug,
                                                  @Path("productSlug") String productSlug);

        @FormUrlEncoded
        @POST("/v1/devices")
        SimpleResponse claimDevice(@Field("id") String deviceID);

        @DELETE("/v1/devices/{deviceID}")
        SimpleResponse unclaimDevice(@Path("deviceID") String deviceID);

    }

    /**
     * APIs dealing with identity and authorization
     * <p>
     * These are separated out from the main API, since they aren't
     * authenticated like the main API, and as such need different
     * headers.
     */
    public interface IdentityApi {

        @POST("/v1/users")
        Response signUp(@Body SignUpInfo signUpInfo);

        // NOTE: the `LogInResponse` used here as a return type is intentional.  It looks
        // a little odd, but that's how this endpoint works.
        @POST("/v1/products/{productId}/customers")
        Responses.LogInResponse signUpAndLogInWithCustomer(@Body SignUpInfo signUpInfo,
                                                           @Path("productId") Integer productId);

        // NOTE: the `LogInResponse` used here as a return type is intentional.  It looks
        // a little odd, but that's how this endpoint works.
        @POST("/v1/orgs/{orgSlug}/customers")
        @Deprecated
        Responses.LogInResponse signUpAndLogInWithCustomer(@Body SignUpInfo signUpInfo,
                                                           @Path("orgSlug") String orgSlug);

        @FormUrlEncoded
        @POST("/oauth/token")
        Responses.LogInResponse logIn(@Field("grant_type") String grantType,
                                      @Field("username") String username,
                                      @Field("password") String password);

        @FormUrlEncoded
        @POST("/oauth/token")
        Responses.LogInResponse authenticate(@Field("grant_type") String grantType,
                                      @Field("mfa_token") String mfaToken,
                                      @Field("otp") String otp);

        @FormUrlEncoded
        @POST("/oauth/token")
        Responses.LogInResponse logIn(@Field("grant_type") String grantType,
                                      @Field("refresh_token") String refreshToken);

        @FormUrlEncoded
        @POST("/v1/user/password-reset")
        Response requestPasswordReset(@Field("username") String email);

        @FormUrlEncoded
        @POST("/v1/products/{productId}/customers/reset_password")
        Response requestPasswordResetForCustomer(@Field("email") String email,
                                                 @Path("productId") Integer productId);

        @FormUrlEncoded
        @POST("/v1/orgs/{orgName}/customers/reset_password")
        @Deprecated
        Response requestPasswordResetForCustomer(@Field("email") String email,
                                                 @Path("orgName") String orgName);

    }
}
