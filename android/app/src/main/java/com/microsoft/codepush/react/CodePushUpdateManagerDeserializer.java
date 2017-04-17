package com.microsoft.codepush.react;

import org.json.JSONObject;

/**
 * Created by sergey.akhalkov on 4/12/2017.
 */

public class CodePushUpdateManagerDeserializer {
    private CodePushUpdateManager mUpdateManager;

    public CodePushUpdateManagerDeserializer(CodePushUpdateManager updateManager) {
        mUpdateManager = updateManager;
    }

    public CodePushLocalPackage getCurrentPackage() {
        JSONObject currentPackage = mUpdateManager.getCurrentPackage();
        if (currentPackage != null) {
            return CodePushUtils.convertStringToObject(mUpdateManager.getCurrentPackage().toString(), CodePushLocalPackage.class);
        }
        return null;
    }

    public CodePushLocalPackage getPackage(String packageHash) {
        JSONObject localPackage = mUpdateManager.getPackage(packageHash);
        if (localPackage != null) {
            return CodePushUtils.convertStringToObject(localPackage.toString(), CodePushLocalPackage.class);
        }
        return null;
    }

    public CodePushLocalPackage getPreviousPackage() {
        JSONObject previousPackage = mUpdateManager.getPreviousPackage();
        if (previousPackage != null) {
            return CodePushUtils.convertStringToObject(previousPackage.toString(), CodePushLocalPackage.class);
        }
        return null;
    }
}
