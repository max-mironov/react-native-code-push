/*
 * This script creates a snapshot of the contents in the resource directory
 * by creating a map with the modified time of all the files in the directory
 * and saving it to a temp file. This snapshot is later referenced in 
 * "generatePackageHash.js" to figure out which files have changed or were
 * newly generated by the "react-native bundle" command.
 */

var fs = require("fs");
var path = require("path");

var getFilesInFolder = require("./getFilesInFolder");

var TEMP_FILE_PATH = path.join(require("os").tmpdir(), "CodePushResourcesMap.json");

var resourcesDir = process.argv[2];
var resourceFiles = [];

try {
    getFilesInFolder(resourcesDir, resourceFiles);
} catch(error) {
    var targetPathNotFoundExceptionMessage = "\nResources directory path does not exist.\n";
    targetPathNotFoundExceptionMessage += "Unable to find '" + resourcesDir;
    targetPathNotFoundExceptionMessage += "' directory. Please check version of Android Plugin for Gradle.";
    error.message += targetPathNotFoundExceptionMessage;
    throw error;
}

var fileToModifiedTimeMap = {};

resourceFiles.forEach(function(resourceFile) {
    fileToModifiedTimeMap[resourceFile.path.substring(resourcesDir.length)] = resourceFile.mtime.getTime();
});

fs.writeFile(TEMP_FILE_PATH, JSON.stringify(fileToModifiedTimeMap), function(err) {
    if (err) {
        throw err;
    }
}); 