package com.microsoft.codepush.react;

public class CodePushSyncOptions {
    public String DeploymentKey;

    public CodePushInstallMode InstallMode;

    public CodePushInstallMode MandatoryInstallMode;

    public int MinimumBackgroundDuration;

    public boolean IgnoreFailedUpdates = true;
}
