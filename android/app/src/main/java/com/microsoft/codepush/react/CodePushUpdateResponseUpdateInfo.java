package com.microsoft.codepush.react;

import com.google.gson.annotations.SerializedName;

public class CodePushUpdateResponseUpdateInfo {
    @SerializedName("downloadURL")
    public String DownloadUrl;

    @SerializedName("description")
    public String Description;

    @SerializedName("isAvailable")
    public boolean IsAvailable;

    @SerializedName("isMandatory")
    public boolean IsMandatory;

    @SerializedName("appVersion")
    public String AppVersion;

    @SerializedName("packageHash")
    public String PackageHash;

    @SerializedName("label")
    public String Label;

    @SerializedName("packageSize")
    public int PackageSize;

    @SerializedName("updateAppVersion")
    public boolean UpdateAppVersion;

    @SerializedName("shouldRunBinaryVersion")
    public boolean ShouldRunBinaryVersion;
}