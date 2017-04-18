package com.microsoft.codepush.react;

import com.google.gson.annotations.SerializedName;

public class CodePushRemotePackage extends CodePushPackage {
    @SerializedName("downloadUrl")
    public final String DownloadUrl;

    @SerializedName("packageSize")
    public final int PackageSize;

    public CodePushRemotePackage(
            final String appVersion,
            final String deploymentKey,
            final String description,
            final boolean failedInstall,
            final boolean isMandatory,
            final String label,
            final String packageHash,
            final int packageSize,
            final String downloadUrl
    ) {
        super(appVersion, deploymentKey, description, failedInstall, isMandatory, label, packageHash);
        DownloadUrl = downloadUrl;
        PackageSize = packageSize;
    }
}
