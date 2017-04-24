package com.microsoft.codepush.react;

public class CodePushSyncOptions {
    public String DeploymentKey;

    public CodePushInstallMode InstallMode;

    public CodePushInstallMode MandatoryInstallMode;

    public Integer MinimumBackgroundDuration;

    public Boolean IgnoreFailedUpdates = true;
}
