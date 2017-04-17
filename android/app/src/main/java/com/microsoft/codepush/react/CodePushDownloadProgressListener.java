package com.microsoft.codepush.react;

/**
 * Created by sergey.akhalkov on 4/17/2017.
 */

public interface CodePushDownloadProgressListener {
    void downloadProgressChanged(long receivedBytes, long totalBytes);
}
