package com.microsoft.codepush.react;

/**
 * Created by sergey.akhalkov on 4/11/2017.
 */

public final class CodePushConfiguration {
    public final String AppVersion;
    public final String ClientUniqueId;
    public final String DeploymentKey;
    public final String ServerUrl;
    public final String PackageHash;
    public CodePushConfiguration(
            final String appVersion,
            final String clientUniqueId,
            final String deploymentKey,
            final String serverUrl,
            final String packageHash
    ) {
        AppVersion = appVersion;
        ClientUniqueId = clientUniqueId;
        DeploymentKey = deploymentKey;
        ServerUrl = serverUrl;
        PackageHash = packageHash;
    }
}
