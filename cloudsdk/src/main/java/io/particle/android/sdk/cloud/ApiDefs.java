package io.particle.android.sdk.cloud;

import java.util.List;

import io.particle.android.sdk.cloud.Responses.CallFunctionResponse;
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse;
import io.particle.android.sdk.cloud.Responses.Models;
import io.particle.android.sdk.cloud.Responses.ReadVariableResponse;
import io.particle.android.sdk.cloud.Responses.SimpleResponse;
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

        @GET("/v1/devices")
        List<Models.SimpleDevice> getDevices();

        @GET("/v1/devices/{deviceID}")
        ParticleDevice.Builder getDevice(@Path("deviceID") String deviceID);

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

        @GET("/v1/devices/{deviceID}/{variable}")
        ReadVariableResponse getVariable(@Path("deviceID") String deviceID,
                                         @Path("variable") String variable);

        @POST("/v1/devices/{deviceID}/{function}")
        CallFunctionResponse callFunction(@Path("deviceID") String deviceID,
                                          @Path("function") String function,
                                          @Body FunctionArgs args);

        /**
         * Newer versions of OkHttp <em>require</em> a body for POSTs, but just pass in
         * a blank string for the body and all is well.
         */
        @FormUrlEncoded
        @POST("/v1/device_claims")
        ClaimCodeResponse generateClaimCode(@Field("blank") String blankBody);

        @FormUrlEncoded
        @POST("/v1/orgs/{orgSlug}/products/{productSlug}/device_claims")
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
     * <p>
     * Also, the duplicated methods for orgs are unfortunate, but the best solution all around
     * in practice.  (This should be revisited in the unlikely case that endpoints for orgs and
     * non-orgs diverges further.)
     */
    public interface IdentityApi {

        @FormUrlEncoded
        @POST("/v1/users")
        Response signUp(@Field("username") String username,
                        @Field("password") String password);


        // NOTE: the `LogInResponse` used here is intentional.  It looks a little odd, but that's
        // how this endpoint works.
        @FormUrlEncoded
        @POST("/v1/orgs/{orgSlug}/customers")
        Responses.LogInResponse signUpAndLogInWithCustomer(@Field("grant_type") String grantType,
                                                           @Field("email") String email,
                                                           @Field("password") String password,
                                                           @Path("orgSlug") String orgSlug);

        @FormUrlEncoded
        @POST("/oauth/token")
        Responses.LogInResponse logIn(@Field("grant_type") String grantType,
                                      @Field("username") String username,
                                      @Field("password") String password);

        @FormUrlEncoded
        @POST("/v1/password/reset")
//        @POST("/v1/orgs/{orgName}/customers/reset_password")
        Response requestPasswordReset(@Field("email") String email);//,
//                                      @Path("orgName") String orgName);
    }

}
