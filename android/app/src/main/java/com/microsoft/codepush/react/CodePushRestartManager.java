package com.microsoft.codepush.react;

import java.util.ArrayList;
import java.util.List;

public class CodePushRestartManager {
    private boolean mAllowed = true;

    private boolean mRestartInProgress = false;

    private List<Boolean> mRestartQueue = new ArrayList<>();

    private CodePushCore mCodePushCore;

    public CodePushRestartManager(CodePushCore codePushCore) {
        mCodePushCore = codePushCore;
    }

    public void allow() {
        CodePushUtils.log("Re-allowing restarts");
        mAllowed = true;

        if (mRestartQueue.size() > 0) {
            CodePushUtils.log("Executing pending restart");
            //restartApp
        }
    }

    public void disallow() {
        CodePushUtils.log("Disallowing restarts");
        mAllowed = false;
    }

    public void clearPendingRestart() {
        mRestartQueue.clear();
    }

    public void restartApp(boolean onlyIfUpdateIsPending) {
        if (mRestartInProgress) {
            CodePushUtils.log("Restart request queued until the current restart is completed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else if (!mAllowed) {
            CodePushUtils.log("Restart request queued until restarts are re-allowed");
            mRestartQueue.add(onlyIfUpdateIsPending);
        } else {
            mRestartInProgress = true;
            if (mCodePushCore.restartApp(onlyIfUpdateIsPending)) {
                CodePushUtils.log("Restarting app");
                return;
            }

            mRestartInProgress = false;
            if (mRestartQueue.size() > 0) {
                restartApp(mRestartQueue.remove(0));
            }
        }
    }
 }
