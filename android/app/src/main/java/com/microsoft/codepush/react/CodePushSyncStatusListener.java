package com.microsoft.codepush.react;

/**
 * Created by sergey.akhalkov on 4/17/2017.
 */

public interface CodePushSyncStatusListener {
    void syncStatusChanged(CodePushSyncStatus syncStatus);
}
