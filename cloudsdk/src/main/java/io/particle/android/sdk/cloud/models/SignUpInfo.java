package io.particle.android.sdk.cloud.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Required and optional user information used in sign up process.
 */
@ParametersAreNonnullByDefault
public class SignUpInfo implements Parcelable {
    private String username, password;
    @SerializedName("grant_type") @Nullable
    private String grantType;
    @SerializedName("account_info") @Nullable
    private AccountInfo accountInfo;

    public SignUpInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public SignUpInfo(String username, String password, AccountInfo accountInfo) {
        this.username = username;
        this.password = password;
        this.accountInfo = accountInfo;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Nullable
    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(@Nullable String grantType) {
        this.grantType = grantType;
    }

    @Nullable
    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(@Nullable AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeString(this.grantType);
        dest.writeParcelable(this.accountInfo, flags);
    }

    protected SignUpInfo(Parcel in) {
        this.username = in.readString();
        this.password = in.readString();
        this.grantType = in.readString();
        this.accountInfo = in.readParcelable(AccountInfo.class.getClassLoader());
    }

    public static final Parcelable.Creator<SignUpInfo> CREATOR = new Parcelable.Creator<SignUpInfo>() {
        @Override
        public SignUpInfo createFromParcel(Parcel source) {
            return new SignUpInfo(source);
        }

        @Override
        public SignUpInfo[] newArray(int size) {
            return new SignUpInfo[size];
        }
    };
}
