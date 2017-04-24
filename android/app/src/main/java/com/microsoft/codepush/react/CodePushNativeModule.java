package com.microsoft.codepush.react;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.ChoreographerCompat;
import com.facebook.react.modules.core.ReactChoreographer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CodePushNativeModule extends ReactContextBaseJavaModule implements CodePushDownloadProgressListener {
    private String mBinaryContentsHash = null;
    private static boolean mNotifyDownloadProgress = false;

    private CodePushCore mCodePushCore;

    public CodePushNativeModule(ReactApplicationContext reactContext, CodePushCore codePushCore) {
        super(reactContext);

        mCodePushCore = codePushCore;

        // Initialize module state while we have a reference to the current context.
        mBinaryContentsHash = CodePushUpdateUtils.getHashForBinaryContents(reactContext, mCodePushCore.isDebugMode());

        mCodePushCore.addDownloadProgressListener(this);
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        constants.put("codePushInstallModeImmediate", CodePushInstallMode.IMMEDIATE.getValue());
        constants.put("codePushInstallModeOnNextRestart", CodePushInstallMode.ON_NEXT_RESTART.getValue());
        constants.put("codePushInstallModeOnNextResume", CodePushInstallMode.ON_NEXT_RESUME.getValue());
        constants.put("codePushInstallModeOnNextSuspend", CodePushInstallMode.ON_NEXT_SUSPEND.getValue());

        constants.put("codePushUpdateStateRunning", CodePushUpdateState.RUNNING.getValue());
        constants.put("codePushUpdateStatePending", CodePushUpdateState.PENDING.getValue());
        constants.put("codePushUpdateStateLatest", CodePushUpdateState.LATEST.getValue());

        constants.put("codePushSyncStatusUpToDate", CodePushSyncStatus.UP_TO_DATE.getValue());
        constants.put("codePushSyncStatusUpdateInstalled", CodePushSyncStatus.UPDATE_INSTALLED.getValue());
        constants.put("codePushSyncStatusUpdateIgnored", CodePushSyncStatus.UPDATE_IGNORED.getValue());
        constants.put("codePushSyncStatusUnknownError", CodePushSyncStatus.UNKNOWN_ERROR.getValue());
        constants.put("codePushSyncStatusSyncInProgress", CodePushSyncStatus.SYNC_IN_PROGRESS.getValue());
        constants.put("codePushSyncStatusCheckingForUpdate", CodePushSyncStatus.CHECKING_FOR_UPDATE.getValue());
        constants.put("codePushSyncStatusAwaitingUserAction", CodePushSyncStatus.AWAITING_USER_ACTION.getValue());
        constants.put("codePushSyncStatusDownloadingPackage", CodePushSyncStatus.DOWNLOADING_PACKAGE.getValue());
        constants.put("codePushSyncStatusInstallingUpdate", CodePushSyncStatus.INSTALLING_UPDATE.getValue());

        return constants;
    }

    @Override
    public String getName() {
        return "CodePush";
    }

    @ReactMethod
    public void checkForUpdate(final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                CodePushRemotePackage remotePackage = mCodePushCore.checkForUpdate();
                if (remotePackage != null) {
                    JSONObject jsonObject = CodePushUtils.convertObjectToJsonObject(remotePackage);
                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(jsonObject));
                } else {
                    promise.resolve("");
                }
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void sync(final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mCodePushCore.sync();
                promise.resolve("");
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void downloadUpdate(final ReadableMap updatePackage, final boolean notifyProgress, final Promise promise) {
        mNotifyDownloadProgress = notifyProgress;
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                CodePushLocalPackage newPackage = mCodePushCore.downloadUpdate(
                        CodePushUtils.convertReadableToObject(updatePackage, CodePushRemotePackage.class)
                );

                if (newPackage.DownloadException == null) {
                    promise.resolve(CodePushUtils.convertObjectToWritableMap(newPackage));
                } else {
                    promise.reject(newPackage.DownloadException);
                }

                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void getConfiguration(Promise promise) {
        WritableMap configMap =  Arguments.createMap();
        CodePushConfiguration nativeConfiguration = mCodePushCore.getConfiguration();
        configMap.putString("appVersion", nativeConfiguration.AppVersion);
        configMap.putString("clientUniqueId", nativeConfiguration.ClientUniqueId);
        configMap.putString("deploymentKey", nativeConfiguration.DeploymentKey);
        configMap.putString("serverUrl", nativeConfiguration.ServerUrl);

        // The binary hash may be null in debug builds
        if (mBinaryContentsHash != null) {
            configMap.putString(CodePushConstants.PACKAGE_HASH_KEY, mBinaryContentsHash);
        }

        promise.resolve(configMap);
    }

    @ReactMethod
    public void getUpdateMetadata(final int updateState, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                CodePushLocalPackage currentPackage = mCodePushCore.getUpdateMetadata(CodePushUpdateState.values()[updateState]);
                if (currentPackage != null) {
                    promise.resolve(CodePushUtils.convertObjectToWritableMap(currentPackage));
                } else {
                    promise.resolve("");
                }
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void getNewStatusReport(final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                CodePushStatusReport statusReport = mCodePushCore.getNewStatusReport();
                if (statusReport != null) {
                    promise.resolve(CodePushUtils.convertObjectToWritableMap(statusReport));
                } else {
                    promise.resolve("");
                }
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void installUpdate(final ReadableMap updatePackage, final int installMode, final int minimumBackgroundDuration, final Promise promise) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mCodePushCore.installUpdate(
                        CodePushUtils.convertReadableToObject(updatePackage, CodePushLocalPackage.class),
                        CodePushInstallMode.values()[installMode],
                        minimumBackgroundDuration);
                promise.resolve("");
                return null;
            }
        };

        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void isFailedUpdate(String packageHash, Promise promise) {
        promise.resolve(mCodePushCore.isFailedUpdate(packageHash));
    }

    @ReactMethod
    public void isFirstRun(String packageHash, Promise promise) {
        promise.resolve(mCodePushCore.isFirstRun(packageHash));
    }

    @ReactMethod
    public void notifyApplicationReady(Promise promise) {
        mCodePushCore.removePendingUpdate();
        promise.resolve("");
    }

    @ReactMethod
    public void recordStatusReported(ReadableMap statusReport) {
        mCodePushCore.recordStatusReported(CodePushUtils.convertReadableToObject(statusReport, CodePushStatusReport.class));
    }

    @ReactMethod
    public void restartApp(boolean onlyIfUpdateIsPending, Promise promise) {
        promise.resolve(mCodePushCore.getRestartManager().restartApp(onlyIfUpdateIsPending));
    }

    @ReactMethod
    public void clearPendingRestart() {
        mCodePushCore.getRestartManager().clearPendingRestart();
    }

    @ReactMethod
    public void disallowRestart() {
        mCodePushCore.getRestartManager().disallow();
    }

    @ReactMethod
    public void allowRestart() {
        mCodePushCore.getRestartManager().allow();
    }

    @ReactMethod
    public void saveStatusReportForRetry(ReadableMap statusReport) {
        mCodePushCore.saveStatusReportForRetry(CodePushUtils.convertReadableToObject(statusReport, CodePushStatusReport.class));
    }

    @ReactMethod
    // Replaces the current bundle with the one downloaded from removeBundleUrl.
    // It is only to be used during tests. No-ops if the test configuration flag is not set.
    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        mCodePushCore.downloadAndReplaceCurrentBundle(remoteBundleUrl);
    }

    public void downloadProgressChanged(long receivedBytes, long totalBytes) {
        if (mNotifyDownloadProgress) {
            DownloadProgress downloadProgress = new DownloadProgress(totalBytes, receivedBytes);
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(CodePushConstants.DOWNLOAD_PROGRESS_EVENT_NAME, downloadProgress.createWritableMap());
        }
    }
}
