namespace CodePush.ReactNative
{
    enum SyncStatus
    {
        UP_TO_DATE, // The running app is up-to-date
        UPDATE_INSTALLED, // The app had an optional/mandatory update that was successfully downloaded and is about to be installed.
        UPDATE_IGNORED, // The app had an optional update and the end-user chose to ignore it
        UNKNOWN_ERROR,
        SYNC_IN_PROGRESS, // There is an ongoing "sync" operation in progress.
        CHECKING_FOR_UPDATE,
        AWAITING_USER_ACTION,
        DOWNLOADING_PACKAGE,
        INSTALLING_UPDATE
    }
}
