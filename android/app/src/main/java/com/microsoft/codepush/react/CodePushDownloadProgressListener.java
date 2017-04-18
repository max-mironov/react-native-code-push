package com.microsoft.codepush.react;

public interface CodePushDownloadProgressListener {
    void downloadProgressChanged(long receivedBytes, long totalBytes);
}
