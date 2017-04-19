package com.microsoft.codepush.react;

import android.content.Context;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.List;

public class CodePush implements ReactPackage {
    private CodePushCore mCodePushCore;
    private CodePushRestartManager mRestartManager;

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactApplicationContext) {
        return mCodePushCore.createNativeModules(reactApplicationContext);
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return new ArrayList<>();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactApplicationContext) {
        return new ArrayList<>();
    }

    public CodePush(String deploymentKey, Context context) {
        this(deploymentKey, context, false);
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode) {
        mCodePushCore = new CodePushCore(deploymentKey, context, isDebugMode);
        mRestartManager = mCodePushCore.getRestartManager();
    }

    public CodePush(String deploymentKey, Context context, boolean isDebugMode, String serverUrl) {
        mCodePushCore = new CodePushCore(deploymentKey, context, isDebugMode, serverUrl);
        mRestartManager = mCodePushCore.getRestartManager();
    }

    public static String getJSBundleFile() {
        return CodePushCore.getJSBundleFile();
    }

    public static String getJSBundleFile(String assetsBundleFileName) {
        return CodePushCore.getJSBundleFile(assetsBundleFileName);
    }

    public CodePushConfiguration getConfiguration() {
        return mCodePushCore.getConfiguration();
    }

    public CodePushAcquisitionManager getAcquisitionSdk() {
        return new CodePushAcquisitionManager(getConfiguration());
    }

    public CodePushAcquisitionManager getAcquisitionSdk(CodePushConfiguration configuration) {
        return new CodePushAcquisitionManager(configuration);
    }

    public CodePushRemotePackage checkForUpdate() {
        return mCodePushCore.checkForUpdate();
    }

    public CodePushRemotePackage checkForUpdate(String deploymentKey) {
        return mCodePushCore.checkForUpdate(deploymentKey);
    }

    public CodePushLocalPackage getCurrentPackage() {
        return mCodePushCore.getCurrentPackage();
    }

    public CodePushLocalPackage getUpdateMetadata(CodePushUpdateState updateState) {
        return mCodePushCore.getUpdateMetadata(updateState);
    }

    public CodePushLocalPackage getUpdateMetadata() {
        return mCodePushCore.getUpdateMetadata(CodePushUpdateState.RUNNING);
    }

    public static void log(String message) {
        CodePushUtils.log(message);
    }

    public void notifyApplicationReady() {
        mCodePushCore.notifyApplicationReady();
    }

    public void restartApp() {
        mRestartManager.restartApp(false);
    }

    public void restartApp(boolean onlyIfUpdateIsPending) {
        mRestartManager.restartApp(onlyIfUpdateIsPending);
    }

    public void disallowRestart() {
        mRestartManager.disallow();
    }

    public void allowRestart() {
        mRestartManager.allow();
    }

    public void sync() {
        mCodePushCore.sync();
    }

    public void sync(CodePushSyncOptions syncOptions) {
        mCodePushCore.sync(syncOptions);
    }

    public void addSyncStatusListener(CodePushSyncStatusListener syncStatusListener) {
        mCodePushCore.addSyncStatusListener(syncStatusListener);
    }

    public void addDownloadProgressListener(CodePushDownloadProgressListener downloadProgressListener) {
        mCodePushCore.addDownloadProgressListener(downloadProgressListener);
    }
}