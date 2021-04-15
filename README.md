# TestFairyUploader

## About
**TestFairyUploader** is a command-line tool designed for automated uploading of APKs, IPAs and deobfuscation files to TestFairy.

This tool is using *TestFairy Upload API*. You can read more about it [here](https://docs.testfairy.com/API/Upload_API.html).

## TestFairyUploader options

**TestFairyUploader** has few option which can be combined and they are executed in following order:
- Print tool version to stdout and exit
- Upload app and deobfuscation file

##### Print tool version to stdout and exit
```bash
-version
```
##### Options
```bash
-apiKey 1234567890
-httpTimeout 300 #Optional. Http timeout in seconds (default: 120)
-retryCount 3 #Optional. Application id of your app (default: 0)
-appFilePath "path/to/app.apk" #Required. Path to APK or IPA file
-symbolsFilePath "path/to/symbols.zip" #Optional. Path to deobfuscation file
-releaseNotesFilePath "path/to/release_notes.txt" #Optional. Path to file with release notes for testers
-testersGroups "internal,beta" #Optional. Comma separated testers groups (value "all" will cover all groups)
-tags "iap,test" #Optional. Comma separated tags
-notifyTesters #Optional. If specified, testers will be notified via email
-autoUpdate #Optional. If specified, app will be automatically updated on testers devices (only for apps with TestFairy SDK)
```

## Releases
Latest release: [1.0.0](https://github.com/chorobochrontochor/TestFairyUploader/releases)