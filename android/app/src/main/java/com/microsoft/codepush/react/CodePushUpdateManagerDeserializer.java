package com.microsoft.codepush.react;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

/**
 * Created by sergey.akhalkov on 4/12/2017.
 */

public class CodePushUpdateManagerDeserializer {
    private CodePushUpdateManager mUpdateManager;
    private Gson mGson;

    public CodePushUpdateManagerDeserializer(CodePushUpdateManager updateManager) {
        mUpdateManager = updateManager;

        GsonBuilder builder = new GsonBuilder();
        mGson = builder.create();
    }

    public CodePushLocalPackage getCurrentPackage() {
        JSONObject currentPackage = mUpdateManager.getCurrentPackage();
        if (currentPackage != null) {
            return mGson.fromJson(mUpdateManager.getCurrentPackage().toString(), CodePushLocalPackage.class);
        }
        return null;
    }

    public CodePushLocalPackage getPackgage(String packageHash) {
        JSONObject localPackage = mUpdateManager.getPackage(packageHash);
        if (localPackage != null) {
            return mGson.fromJson(localPackage.toString(), CodePushLocalPackage.class);
        }
        return null;
    }

    public CodePushLocalPackage getPreviousPackage() {
        JSONObject previousPackage = mUpdateManager.getPreviousPackage();
        if (previousPackage != null) {
            return mGson.fromJson(previousPackage.toString(), CodePushLocalPackage.class);
        }
        return null;
    }
}
