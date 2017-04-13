package com.microsoft.codepush.react;

import com.google.gson.annotations.SerializedName;

/**
 * Created by sergey.akhalkov on 4/13/2017.
 */

public class CodePushUpdateResponse {
    @SerializedName("updateInfo")
    public CodePushUpdateResponseUpdateInfo UpdateInfo;
}
