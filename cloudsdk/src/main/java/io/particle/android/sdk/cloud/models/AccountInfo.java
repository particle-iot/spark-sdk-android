package io.particle.android.sdk.cloud.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/**
 * Keeps secondary user account information.
 */
public class AccountInfo implements Parcelable {
    @SerializedName("first_name")
    private String firstName;
    @SerializedName("last_name")
    private String lastName;
    @SerializedName("company_name")
    private String companyName;
    @SerializedName("business_account")
    private boolean businessAccount;

    public AccountInfo() {
    }

    public AccountInfo(String firstName, String lastName, String companyName, boolean businessAccount) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.companyName = companyName;
        this.businessAccount = businessAccount;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean isBusinessAccount() {
        return businessAccount;
    }

    public void setBusinessAccount(boolean isBusinessAccount) {
        this.businessAccount = isBusinessAccount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.firstName);
        dest.writeString(this.lastName);
        dest.writeString(this.companyName);
        dest.writeByte(this.businessAccount ? (byte) 1 : (byte) 0);
    }

    protected AccountInfo(Parcel in) {
        this.firstName = in.readString();
        this.lastName = in.readString();
        this.companyName = in.readString();
        this.businessAccount = in.readByte() != 0;
    }

    public static final Creator<AccountInfo> CREATOR = new Creator<AccountInfo>() {
        @Override
        public AccountInfo createFromParcel(Parcel source) {
            return new AccountInfo(source);
        }

        @Override
        public AccountInfo[] newArray(int size) {
            return new AccountInfo[size];
        }
    };
}
