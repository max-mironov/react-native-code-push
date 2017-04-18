package com.microsoft.codepush.react;

import com.google.gson.annotations.SerializedName;

public class CodePushStatusReport {
    @SerializedName("appVersion")
    public String AppVersion;

    @SerializedName("deploymentKey")
    public String DeploymentKey;

    @SerializedName("label")
    public String Label;

    @SerializedName("package")
    public CodePushLocalPackage Package;

    @SerializedName("previousDeploymentKey")
    public String PreviousDeploymentKey;

    @SerializedName("previousLabelOrAppVersion")
    public String PreviousLabelOrAppVersion;

    @SerializedName("status")
    public String Status;
}
