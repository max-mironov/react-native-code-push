package com.microsoft.codepush.react;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sergey.akhalkov on 4/11/2017.
 */

public class CodePushLocalPackage extends CodePushPackage {
    @SerializedName("isPending")
    public final boolean IsPending;

    @SerializedName("isFirstRun")
    public final boolean IsFirstRun;

    public CodePushLocalPackage(
            final String appVersion,
            final String deploymentKey,
            final String description,
            final boolean failedInstall,
            final boolean isFirstRun,
            final boolean isMandatory,
            final boolean isPending,
            final String label,
            final String packageHash
    ) {
        super(appVersion, deploymentKey, description, failedInstall, isMandatory, label, packageHash);
        IsPending = isPending;
        IsFirstRun = isFirstRun;
    }
}
