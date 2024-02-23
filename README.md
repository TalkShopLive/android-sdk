# TSL Android SDK

This is the official TSL Android SDK repository.

TSL takes care of the infrastructure and APIs needed for live streaming of different types of shows for various channels. Work on your app's logic and let TSL handle live streaming of shows, sending and receiving messages, and reactions.

* [Requirements](#requirements)
* [Get Keys](#get-keys)
* [Set Up Your Project](#set-up-your-project)
* [Shows](#shows)
* [Support](#support)

## Requirements

* Android Studio 2022+
* Android 24+

The TSL Android SDK contains external dependencies of PubNub SDK.

## Get Keys

You will need the publish and subscribe keys to authenticate your app. Get your keys from the backend.

## Set Up Your Project

1. You have can include Jitpack to your project by adding the following to the root build.gradle

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

2. Add TSL to your project by including the following dependency to your app gradle file.

```groovy
implementation "com.github.TalkShopLive:android-sdk:versionTag"
```

2. Initialize the SDK using clientKey:

- Parameters:
       - `context`: Application context.
       - `clientKey`: Given secured client key.
       - `debugMode`: Print console logs if true.
       - `testMode`: Switch to staging if true.
       - `dnt`: Switch to do not track.
       - `callback`: Optional callback when the authentication is successful or not.
```kotlin
 TalkShopLive.initialize(context: Context, clientKey: String, debugMode: Boolean, testMode: Boolean, dnt: Boolean)
```

## Shows

### Overview

The TSL Android SDK provides methods for fetching details of a specific show and its current event, enabling you to get show and current event details in your app.

### Methods

#### `getDetails(showId:String, callback: GetDetailsCallback)`

Get detailed information about a specific show.

- Parameters:
    - `showId`: The unique identifier of the show.
    - `GetDetailsCallback`: A callback that will be called once the show details are fetched or an error is thrown.

```kotlin
Show.getDetails(productKey, object : Show.GetDetailsCallback {
    override fun onSuccess(showObject: ShowObject) {}
    override fun onError(error: String) {}
})
```

#### `getStatus(showId:String, callback: GetStatusShowCallback)`

Get the current event of a show.

- Parameters:
    - `showId`: The unique identifier of the show.
    - `GetStatusShowCallback`: A callback that will be called once the show status is fetched or an error is thrown.

```kotlin
Show.getStatus(productKey, object : Show.GetStatusShowCallback {
    override fun onSuccess(status: String) {}
    override fun onError(error: String) {}
})
```

## Support

If you **need help** or have a **general question**, contact <support@talkshoplive.com>.
