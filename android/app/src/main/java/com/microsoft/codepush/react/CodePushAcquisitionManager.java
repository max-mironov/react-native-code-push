package com.microsoft.codepush.react;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sergey.akhalkov on 4/12/2017.
 */

public class CodePushAcquisitionManager {
    private String mServerUrl;
    private String mAppVersion;
    private String mClientUniqueId;
    private String mDeploymentKey;

    public CodePushAcquisitionManager(CodePushConfiguration configuration) {
        mServerUrl = configuration.ServerUrl;
        if (!mServerUrl.endsWith("/")) {
            mServerUrl += "/";
        };
        mAppVersion = configuration.AppVersion;
        mClientUniqueId = configuration.ClientUniqueId;
        mDeploymentKey = configuration.DeploymentKey;
    }

    public CodePushRemotePackage queryUpdateWithCurrentPackage(CodePushLocalPackage currentPackage) {
        if (currentPackage == null || currentPackage.AppVersion == null || currentPackage.AppVersion.isEmpty()) {
            throw new IllegalArgumentException("Calling common acquisition SDK with incorrect package");
        }

        CodePushUpdateRequest updateRequest = new CodePushUpdateRequest(
                mDeploymentKey,
                currentPackage.AppVersion,
                currentPackage.PackageHash,
                false,
                currentPackage.Label,
                mClientUniqueId
        );


        try {
            final String requestUrl = mServerUrl + "updateCheck?" + CodePushUtils.getQueryStringFromObject(updateRequest);

            AsyncTask<Void, Void, CodePushRemotePackage> asyncTask = new AsyncTask<Void, Void, CodePushRemotePackage>() {
                @Override
                protected CodePushRemotePackage doInBackground(Void... params) {
                    try {
                        URL url = new URL(requestUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        if(connection.getResponseCode() == HttpsURLConnection.HTTP_OK){
                            InputStream inputStream = connection.getInputStream();
                            Scanner s = new Scanner(inputStream).useDelimiter("\\A");
                            String result = s.hasNext() ? s.next() : "";
                            CodePushUpdateResponseUpdateInfo updateInfo = CodePushUtils.convertStringToObject(result, CodePushUpdateResponse.class).UpdateInfo;
                            return new CodePushRemotePackage(
                                    updateInfo.AppVersion,
                                    mDeploymentKey,
                                    updateInfo.Description,
                                    false,
                                    updateInfo.IsMandatory,
                                    updateInfo.Label,
                                    updateInfo.PackageHash,
                                    updateInfo.PackageSize,
                                    updateInfo.DownloadUrl);
                        }
                    } catch (MalformedURLException exception) {

                    } catch (IOException exception) {

                    }
                    return null;
                }
            };

            return asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
        } catch (UnsupportedEncodingException exception) {

        } catch (ExecutionException exception) {

        } catch (InterruptedException exctption) {

        }
        return null;
    }

    public void reportStatusDeploy(CodePushLocalPackage deployedPackage, String status, String previousLabelOrAppVersion, String previousDeploymentKey) {
        throw new UnsupportedOperationException();
    }

    public void reportStatusDownload(CodePushLocalPackage downloadedPackage) {
        throw new UnsupportedOperationException();
    }
}
