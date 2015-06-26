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
 * Spark cloud APIs, modelled for the Retrofit REST library
 */
public class ApiDefs {

    /**
     * The main Spark cloud API
     */
    public interface CloudApi {

        @GET("/v1/devices")
        List<Models.SimpleDevice> getDevices();

        @GET("/v1/devices/{deviceID}")
        SparkDevice.Builder getDevice(@Path("deviceID") String deviceID);

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

        // FIXME: remove
//        @Multipart
//        @POST("/path/to/api")
//        uploadFile(
//                @Query("paramteer") String value,
//                @Part("file") TypedFileString fileData,
//                @Header("[...]")[...]
//        );
//        [...]
//
//        public class TypedFileString extends TypedString {
//            private final String filename;
//
//            public TypedFileString(String string, String filename) {
//                super(string);
//                this.filename = filename;
//            }
//
//            @Override public String fileName() {
//                return filename;
//            }
//        }
//
//        TypedFileString typedFileString = new TypedFileString("text", "test.txt");
//        uploadFile(123456, typedFileString, [...]);


        @GET("/v1/devices/{deviceID}/{variable}")
        ReadVariableResponse getVariable(@Path("deviceID") String deviceID,
                                         @Path("variable") String variable);

        @POST("/v1/devices/{deviceID}/{function}")
        CallFunctionResponse callFunction(@Path("deviceID") String deviceID,
                                          @Path("function") String function,
                                          @Body FunctionArgs args);

        @POST("/v1/device_claims")
        ClaimCodeResponse generateClaimCode();

        @FormUrlEncoded
        @POST("/v1/devices")
        SimpleResponse claimDevice(@Field("id") String deviceID);

        @DELETE("/v1/devices/{deviceID}")
        SimpleResponse unclaimDevice(@Path("deviceID") String deviceID);
    }

    /**
     * APIs dealing with identity and authorization
     * <p/>
     * These are separated out from the main API, since they aren't
     * authenticated like the main API, and as such need different
     * headers.
     * <p/>
     * Also, the duplicated methods for orgs are unfortunate, but the best solution all around
     * in practice.  (This should be revisited in the unlikely case that endpoints for orgs and
     * non-orgs diverges further.)
     */
    public interface IdentityApi {

        @FormUrlEncoded
        @POST("/v1/users")
        Response signUp(@Field("username") String username,
                        @Field("password") String password);

        @FormUrlEncoded
        @POST("/v1/orgs/{orgName}/customers")
        Response signUpWithOrganizationalUser(@Field("email") String email,
                                              @Field("password") String password,
                                              @Field("activation_code") String inviteCode,
                                              @Path("orgName") String orgName);

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
