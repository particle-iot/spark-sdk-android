package io.particle.android.sdk.cloud;

import com.google.gson.annotations.SerializedName;

public class FunctionArgs {

    @SerializedName("params")
    public final String params;

    public FunctionArgs(String params) {
        this.params = params;
    }
}
