package com.microsoft.codepush.react;

/**
 * Created by sergey.akhalkov on 4/13/2017.
 */

public class CodePushSyncOptions {
    public String DeploymentKey;

    public CodePushInstallMode InstallMode;

    public CodePushInstallMode MandatoryInstallMode;

    public int MinimumBackgroundDuration;

    public boolean IgnoreFailedUpdates = true;
}
