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

public class CodePushNativeModule extends ReactContextBaseJavaModule {
    private String mBinaryContentsHash = null;
    private LifecycleEventListener mLifecycleEventListener = null;
    private int mMinimumBackgroundDuration = 0;

    private CodePushCore mCodePushCore;
    private SettingsManager mSettingsManager;
    private CodePushTelemetryManager mTelemetryManager;
    private CodePushUpdateManager mUpdateManager;

    public CodePushNativeModule(ReactApplicationContext reactContext, CodePushCore codePushCore, CodePushUpdateManager codePushUpdateManager, CodePushTelemetryManager codePushTelemetryManager, SettingsManager settingsManager) {
        super(reactContext);

        mCodePushCore = codePushCore;
        mSettingsManager = settingsManager;
        mTelemetryManager = codePushTelemetryManager;
        mUpdateManager = codePushUpdateManager;

        // Initialize module state while we have a reference to the current context.
        mBinaryContentsHash = CodePushUpdateUtils.getHashForBinaryContents(reactContext, mCodePushCore.isDebugMode());
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
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    JSONObject mutableUpdatePackage = CodePushUtils.convertReadableToJsonObject(updatePackage);
                    CodePushUtils.setJSONValueForKey(mutableUpdatePackage, CodePushConstants.BINARY_MODIFIED_TIME_KEY, "" + mCodePushCore.getBinaryResourcesModifiedTime());
                    mUpdateManager.downloadPackage(mutableUpdatePackage, mCodePushCore.getAssetsBundleFileName(), new DownloadProgressCallback() {
                        private boolean hasScheduledNextFrame = false;
                        private DownloadProgress latestDownloadProgress = null;

                        @Override
                        public void call(DownloadProgress downloadProgress) {
                            if (!notifyProgress) {
                                return;
                            }

                            latestDownloadProgress = downloadProgress;
                            // If the download is completed, synchronously send the last event.
                            if (latestDownloadProgress.isCompleted()) {
                                dispatchDownloadProgressEvent();
                                return;
                            }

                            if (hasScheduledNextFrame) {
                                return;
                            }

                            hasScheduledNextFrame = true;
                            getReactApplicationContext().runOnUiQueueThread(new Runnable() {
                                @Override
                                public void run() {
                                    ReactChoreographer.getInstance().postFrameCallback(ReactChoreographer.CallbackType.TIMERS_EVENTS, new ChoreographerCompat.FrameCallback() {
                                        @Override
                                        public void doFrame(long frameTimeNanos) {
                                            if (!latestDownloadProgress.isCompleted()) {
                                                dispatchDownloadProgressEvent();
                                            }

                                            hasScheduledNextFrame = false;
                                        }
                                    });
                                }
                            });
                        }

                        public void dispatchDownloadProgressEvent() {
                            getReactApplicationContext()
                                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit(CodePushConstants.DOWNLOAD_PROGRESS_EVENT_NAME, latestDownloadProgress.createWritableMap());
                        }
                    });

                    JSONObject newPackage = mUpdateManager.getPackage(CodePushUtils.tryGetString(updatePackage, CodePushConstants.PACKAGE_HASH_KEY));
                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(newPackage));
                } catch (IOException e) {
                    e.printStackTrace();
                    promise.reject(e);
                } catch (CodePushInvalidUpdateException e) {
                    e.printStackTrace();
                    mSettingsManager.saveFailedUpdate(CodePushUtils.convertReadableToJsonObject(updatePackage));
                    promise.reject(e);
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
                JSONObject currentPackage = mUpdateManager.getCurrentPackage();

                if (currentPackage == null) {
                    promise.resolve("");
                    return null;
                }

                Boolean currentUpdateIsPending = false;

                if (currentPackage.has(CodePushConstants.PACKAGE_HASH_KEY)) {
                    String currentHash = currentPackage.optString(CodePushConstants.PACKAGE_HASH_KEY, null);
                    currentUpdateIsPending = mSettingsManager.isPendingUpdate(currentHash);
                }

                if (updateState == CodePushUpdateState.PENDING.getValue() && !currentUpdateIsPending) {
                    // The caller wanted a pending update
                    // but there isn't currently one.
                    promise.resolve("");
                } else if (updateState == CodePushUpdateState.RUNNING.getValue() && currentUpdateIsPending) {
                    // The caller wants the running update, but the current
                    // one is pending, so we need to grab the previous.
                    JSONObject previousPackage = mUpdateManager.getPreviousPackage();

                    if (previousPackage == null) {
                        promise.resolve("");
                        return null;
                    }

                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(previousPackage));
                } else {
                    // The current package satisfies the request:
                    // 1) Caller wanted a pending, and there is a pending update
                    // 2) Caller wanted the running update, and there isn't a pending
                    // 3) Caller wants the latest update, regardless if it's pending or not
                    if (mCodePushCore.isRunningBinaryVersion()) {
                        // This only matters in Debug builds. Since we do not clear "outdated" updates,
                        // we need to indicate to the JS side that somehow we have a current update on
                        // disk that is not actually running.
                        CodePushUtils.setJSONValueForKey(currentPackage, "_isDebugOnly", true);
                    }

                    // Enable differentiating pending vs. non-pending updates
                    CodePushUtils.setJSONValueForKey(currentPackage, "isPending", currentUpdateIsPending);
                    promise.resolve(CodePushUtils.convertJsonObjectToWritable(currentPackage));
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
                if (mCodePushCore.needToReportRollback()) {
                    mCodePushCore.setNeedToReportRollback(false);
                    JSONArray failedUpdates = mSettingsManager.getFailedUpdates();
                    if (failedUpdates != null && failedUpdates.length() > 0) {
                        try {
                            JSONObject lastFailedPackageJSON = failedUpdates.getJSONObject(failedUpdates.length() - 1);
                            WritableMap lastFailedPackage = CodePushUtils.convertJsonObjectToWritable(lastFailedPackageJSON);
                            WritableMap failedStatusReport = mTelemetryManager.getRollbackReport(lastFailedPackage);
                            if (failedStatusReport != null) {
                                promise.resolve(failedStatusReport);
                                return null;
                            }
                        } catch (JSONException e) {
                            throw new CodePushUnknownException("Unable to read failed updates information stored in SharedPreferences.", e);
                        }
                    }
                } else if (mCodePushCore.didUpdate()) {
                    JSONObject currentPackage = mUpdateManager.getCurrentPackage();
                    if (currentPackage != null) {
                        WritableMap newPackageStatusReport = mTelemetryManager.getUpdateReport(CodePushUtils.convertJsonObjectToWritable(currentPackage));
                        if (newPackageStatusReport != null) {
                            promise.resolve(newPackageStatusReport);
                            return null;
                        }
                    }
                } else if (mCodePushCore.isRunningBinaryVersion()) {
                    WritableMap newAppVersionStatusReport = mTelemetryManager.getBinaryUpdateReport(mCodePushCore.getAppVersion());
                    if (newAppVersionStatusReport != null) {
                        promise.resolve(newAppVersionStatusReport);
                        return null;
                    }
                } else {
                    WritableMap retryStatusReport = mTelemetryManager.getRetryStatusReport();
                    if (retryStatusReport != null) {
                        promise.resolve(retryStatusReport);
                        return null;
                    }
                }

                promise.resolve("");
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
                mUpdateManager.installPackage(CodePushUtils.convertReadableToJsonObject(updatePackage), mSettingsManager.isPendingUpdate(null));

                String pendingHash = CodePushUtils.tryGetString(updatePackage, CodePushConstants.PACKAGE_HASH_KEY);
                if (pendingHash == null) {
                    throw new CodePushUnknownException("Update package to be installed has no hash.");
                } else {
                    mSettingsManager.savePendingUpdate(pendingHash, /* isLoading */false);
                }

                if (installMode == CodePushInstallMode.ON_NEXT_RESUME.getValue() ||
                    // We also add the resume listener if the installMode is IMMEDIATE, because
                    // if the current activity is backgrounded, we want to reload the bundle when
                    // it comes back into the foreground.
                    installMode == CodePushInstallMode.IMMEDIATE.getValue() ||
                    installMode == CodePushInstallMode.ON_NEXT_SUSPEND.getValue()) {

                    // Store the minimum duration on the native module as an instance
                    // variable instead of relying on a closure below, so that any
                    // subsequent resume-based installs could override it.
                    CodePushNativeModule.this.mMinimumBackgroundDuration = minimumBackgroundDuration;

                    if (mLifecycleEventListener == null) {
                        // Ensure we do not add the listener twice.
                        mLifecycleEventListener = new LifecycleEventListener() {
                            private Date lastPausedDate = null;
                            private Handler appSuspendHandler = new Handler(Looper.getMainLooper());
                            private Runnable loadBundleRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    CodePushUtils.log("Loading bundle on suspend");
                                    mCodePushCore.restartApp(false);
                                }
                            };

                            @Override
                            public void onHostResume() {
                                appSuspendHandler.removeCallbacks(loadBundleRunnable);
                                // As of RN 36, the resume handler fires immediately if the app is in
                                // the foreground, so explicitly wait for it to be backgrounded first
                                if (lastPausedDate != null) {
                                    long durationInBackground = (new Date().getTime() - lastPausedDate.getTime()) / 1000;
                                    if (installMode == CodePushInstallMode.IMMEDIATE.getValue()
                                            || durationInBackground >= CodePushNativeModule.this.mMinimumBackgroundDuration) {
                                        CodePushUtils.log("Loading bundle on resume");
                                        mCodePushCore.restartApp(false);
                                    }
                                }
                            }

                            @Override
                            public void onHostPause() {
                                // Save the current time so that when the app is later
                                // resumed, we can detect how long it was in the background.
                                lastPausedDate = new Date();

                                if (installMode == CodePushInstallMode.ON_NEXT_SUSPEND.getValue() && mSettingsManager.isPendingUpdate(null)) {
                                    appSuspendHandler.postDelayed(loadBundleRunnable, minimumBackgroundDuration * 1000);
                                }
                            }

                            @Override
                            public void onHostDestroy() {
                            }
                        };

                        getReactApplicationContext().addLifecycleEventListener(mLifecycleEventListener);
                    }
                }

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
        mSettingsManager.removePendingUpdate();
        promise.resolve("");
    }

    @ReactMethod
    public void recordStatusReported(ReadableMap statusReport) {
        mTelemetryManager.recordStatusReported(statusReport);
    }

    @ReactMethod
    public void restartApp(boolean onlyIfUpdateIsPending, Promise promise) {
        promise.resolve(mCodePushCore.getRestartManager().restartApp(onlyIfUpdateIsPending));
    }

    @ReactMethod
    public void disallowRestart(Promise promise) {
        mCodePushCore.getRestartManager().disallow();
        promise.resolve("");
    }

    @ReactMethod
    public void allowRestart(Promise promise) {
        mCodePushCore.getRestartManager().allow();
        promise.resolve("");
    }

    @ReactMethod
    public void saveStatusReportForRetry(ReadableMap statusReport) {
        mTelemetryManager.saveStatusReportForRetry(statusReport);
    }

    @ReactMethod
    // Replaces the current bundle with the one downloaded from removeBundleUrl.
    // It is only to be used during tests. No-ops if the test configuration flag is not set.
    public void downloadAndReplaceCurrentBundle(String remoteBundleUrl) {
        if (mCodePushCore.isUsingTestConfiguration()) {
            try {
                mUpdateManager.downloadAndReplaceCurrentBundle(remoteBundleUrl, mCodePushCore.getAssetsBundleFileName());
            } catch (IOException e) {
                throw new CodePushUnknownException("Unable to replace current bundle", e);
            }
        }
    }
}
