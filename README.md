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

3. Initialize the SDK using clientKey.

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

#### `getDetails(showKey: String, callback: GetDetailsCallback)`

Get detailed information about a specific show.

- Parameters:
    - `showKey`: The unique identifier of the show.
    - `callback`: An optional callback that will be called once the show details are fetched or an error is thrown.

```kotlin
Show.getDetails(showKey) { error, show -> }
```

#### `getStatus(showKey: String, callback: GetStatusShowCallback)`

Get the current event of a show.

- Parameters:
    - `showKey`: The unique identifier of the show.
    - `callback`: A optional callback that will be called once the show status is fetched or an error is thrown.

```kotlin
Show.getStatus(showKey) { error, show -> }
```

#### `getProducts(showKey: String, preLive: Boolean, callback: ((APIClientError?, List<ProductModel>?) -> Unit)`

Get the products of a specfic show.

- Parameters:
    - `showKey`: The unique identifier of the show.
    - `isEntrance`: An optional flag indicating whether the request is related to pre products. Default is false.
    - `callback`: A callback that will be called once the show products is fetched or an error is thrown.

```kotlin
Show.getProducts(showKey, preLive) { error, show -> }
```

The Product object has a 'variants' of type JSONArray that is structured as such:

```json
[
    {
      "id": 1234,
      "sku": "",
      "is_master": false,
      "is_default": false,
      "weight": "0.0",
      "width": null,
      "height": null,
      "depth": null,
      "deleted_at": null,
      "created_at": "2020-12-02T23:39:08.000Z",
      "updated_at": "2024-03-22T22:50:04.000Z",
      "position": 1,
      "limit_per_order": null,
      "count_on_hand": 123,
      "price": "123.12345",
      "display_price": "$12.34",
      "cost_price": null,
      "cost_currency": "USD",
      "product_id": 123,
      "metadata": null,
      "source_images": null,
      "has_geo": false,
      "allow_international": false,
      "exchange_name": "",
      "affiliate_link": null,
      "original_price": "30.0",
      "external_customer_rating": null,
      "external_number_of_reviews": null,
      "external_offer_id": null,
      "options": [],
      "buyer_price": "123.12345",
      "allowed_countries": null,
      "promotions": [],
      "shipping_method": null,
      "images": []
    }
]
```

## Chat

### Overview

The TSL Android SDK provides methods for fetching user and chat details of a specific show, enabling you to chat as a guest or as a registered user in your app.

### Methods

#### `Chat(jwt: String, isGuest: Boolean, callback: ((String?, UserTokenModel?))`

Initialize the Chat feature.

- Parameters:
    - `context`: The application context.
    - `jwt`: The JWT token for authentication.
    - `isGuest`: Indicates whether the user is a guest.
    - `showKey`: The unique identifier of the show.
    - `callback`: An optional callback function to be invoked upon completion.

```kotlin
Chat(jwt, isGuest, showKey) { errorMessage, userTokenModel -> }
```

#### `publish(message: String, callback: ((String?, String?)))`

Publish a message to chat.

- Parameters:
    - `message`: The message that the user wants to send.
    - `callback`: A optional callback that will be called once the message is sent or an error is thrown.

```kotlin
 Chat.publish(message) { error, timetoken -> }
```

#### `subscribe(callback: ChatCallback)`

Subscribe to a chat to get notified when there are changes.

- Parameters:
    - `callback`: A callback that will be called once there's a change.

```kotlin
 Chat.subscribe(object : Chat.ChatCallback {
    override fun onMessageReceived(message: MessageModel) {}
})
```

#### `getChatMessages(count: Int, start: Long?, callback: (List<MessageModel>?, Long?, String?))`

Get the chat history.

- Parameters:
    - `count`: The number of messages to fetch. Defaults to 25.
    - `start`: The starting time token for fetching messages. Used for pagination.
    - `callback`: An optional callback to return messages, the next start token, or an error.

```kotlin
Chat.getChatMessages { messageList, nextStartToken, error -> }
```

#### `updateUser(newJwt: String, isGuest: Boolean, callback: ((String?, UserTokenModel?))`

Update the current user.

- Parameters:
    - `count`: The new JWT token
    - `isGuest`: Indicates whether the user is a guest.
    - `callback`: An optional callback function to be invoked upon completion.

```kotlin
Chat.updateUser(jwt, isGuest) { errorMessage, userTokenModel -> }
```

#### `clean()`

Cleans all the connections involved with chat.


```kotlin
Chat.clean()
```

#### `countMessages(callback: (Map<String, Long>?) -> Unit)`

Count unread messages.

- Parameters:
    - `callback`: Callback to return the count of unread messages.

```kotlin
Chat.countMessages { unreadCount -> }
```

#### `deleteMessage(timeToken: String, callback: ((Boolean, String?) -> Unit)?)`

Delete a message.

- Parameters:
    - `timeToken`: The time token of the message to be deleted.
    - `callback`: An optional callback to return success or error.

```kotlin
Chat.deleteMessage(timeToken) { isSuccess, error -> }
```

### Listeners

#### `onMessageReceived(message: MessageModel)`

Called when a new message is received.

- Parameters:
    - `message`: The `MessageModel` instance containing the message details.

```kotlin
Chat.subscribe(object : Chat.ChatCallback { override fun onMessageReceived(message: MessageModel) {})
```

#### `onMessageDeleted(messageId: Int)`

Called when a message is deleted.

- Parameters:
    - `messageId`: The `id` of the message that has been deleted.

```kotlin
Chat.subscribe(object : Chat.ChatCallback { override fun onMessageDeleted(messageId: Int) {})
```

#### `onStatusChange(error: String)`

Called when a there is an error.

- Parameters:
    - `error`: The error message as a string.

```kotlin
Chat.subscribe(object : Chat.ChatCallback { override fun onStatusChange(error: String) {})
```


## Collect

### Overview

The TSL Android SDK provides methods for tracking user interactions within your app, allowing you to collect analytics on various user actions related to a show.

### Methods

#### `TalkShopLive.Collect(show: ShowModel)`

Initialize the `Collect` instance for a specific show.

- Parameters:
    - `show`: The `ShowModel` object containing show details.

```kotlin
val collectInstance = TalkShopLive.Collect(show)
```

### `collect(eventName: CollectorActions)`

Track a user interaction event.

- Parameters:
    - `eventName`: The event to be collected, defined in the `CollectorActions` enum.

```kotlin
collectInstance.collect(eventName = CollectorActions.VIDEO_PLAY)
```

## Support
If you **need help** or have a **general question**, contact <support@talkshoplive.com>.
