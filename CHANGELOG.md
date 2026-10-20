# Changelog

## [2.2.0] — 2026-10-19

Parity with iOS 4.2.0: video views at the first frame, engaged views and anonymous views under DNT.

### Added

**Video views: `VideoViewTracker` (`core/videoviews/`)**
Views are now counted at the first rendered frame (`VIDEO_VIEW`, `view_rule: first_frame`), and engaged views after 3 s of watch time (`VIDEO_ENGAGED_VIEW`, `view_rule: engaged_3s`), matching web and embed. The host app forwards its player events to the tracker hooks (`onSourceChanged`, `onFirstFrame`, `onPlaybackStateChanged`, `onPlaybackEnded`) and the SDK applies the rule. App background / foreground is detected by the SDK from `TalkShopLive.initialize`. Picture in Picture does not count as background (the video stays on screen) and is detected too, so unlike iOS there is no PiP hook.

- New `CollectorActions.VIDEO_ENGAGED_VIEW` (category `PROCESS`). This adds an entry to a public enum: a host app with an exhaustive `when` over `CollectorActions` needs a new branch or an `else`.
- New `meta.view_rule` on view events.
- New `VideoViewSource` and `ShowModel.liveAssetId` (sent as `meta.event_id`). `liveAssetId` is a new last constructor parameter with a default: Kotlin source compatible, but already-compiled code calling the `ShowModel` constructor or `copy` must be rebuilt.
- New `VideoViewTracker.release()`, called when the player screen goes away (Android has no `deinit`).

### Changed

**Anonymous views under DNT (`Collector.kt`)**
With `dnt = true`, `VIDEO_VIEW` and `VIDEO_ENGAGED_VIEW` are still sent, anonymously: `meta.anonymous = true`, no `user_id`, `page_metrics` or `aspect`, and only `show_id`, `event_id`, `streaming_content_key`, `store_id`, `video_status`, `view_rule` and `external` in `meta`. All other events stay blocked under DNT, as before.

**`Show.getStatus` no longer counts a view (`ShowProvider.kt`)**
It used to call `incrementView` the first time a live show status was fetched, without any playback. Views now come only from `VIDEO_VIEW`.

### Tests added

`VideoViewStateMachineTests.kt`: 21 offline JUnit tests for the video view rule (`VIDEO_VIEW`, `VIDEO_ENGAGED_VIEW`, new play / same play, background). The state machine runs on a manual clock, so the tests need no network and do not wait for real time.

### Version

- SDK version defined once in `Constants.SDK_VERSION` (used for the analytics `version`) → `2.2.0`
- `sdk/build.gradle.kts` Maven coordinate → `2.2.0`
- `jitpack.yml` and `pom.xml` → `2.2.0`
- `sdk-release.aar` rebuilt from the 2.2.0 sources (the committed one was still the 2.1.0 build). JitPack publishes the committed AAR.

---

## [2.1.1] — 2026-05-29

Parity hotfix matching iOS 4.1.1. Aligns Android SDK with all critical and non-critical fixes shipped to ios-sdk for the Walmart retest.

### Critical fixes

**`KEY_CHAT_ID` had wrong JSON key — `Keys.kt:48`**
Was `"chatId"`, must be `"chat_id"`. The Chat API's `FederatedTokenResponseDto` returns the field as `chat_id` (snake_case, verified against `chat-api`). Every v2 session was decoding `chatId = null`.

**Force-unwrap crash on null token parser — `APICalls.kt:197`**
`UserTokenParser.fromJsonString(response.body)!!` would throw `KotlinNullPointerException` if the parser returned `null` on malformed JSON. Replaced with `?: return Either.Error(getError(APIClientError.USER_TOKEN_EXCEPTION))`.

### Non-critical fixes

**v2 metadata-failure visibility — `PubNubListeners.kt`**
The message was already being delivered on metadata failure (correct), but the error was silently swallowed. Added `Logging.print` so debug builds surface the failure with the affected UUID.

**Four stray `println` calls bypassed `isDebugMode`**
- `ChatProvider.kt:173`
- `PubNubListeners.kt:119, 132, 138`

All replaced with `Logging.print` (gated by `isDebugMode`). Lines 119 and 132 were leaking full PubNub message payloads to stdout in release builds.

**Redundant JSON body on v2 DELETE calls — `APICalls.kt`**
`deleteMessage` and `deleteAction` v2 branches sent `{"showId": X}` as body. The Chat API uses `@Query()` only — no `@Body()` decorator on either endpoint (verified). Body was serialized for nothing. Removed.

### Debug logging added

- `PubNubListeners` v2 metadata fetch — logs the UUID whose metadata fetch failed before delivering the message without sender context.

### Tests added

`HotfixValidationTests.kt` — 10 offline JUnit tests covering all the above. No network required.

### Version

- `build.gradle.kts` Maven coordinate → `2.1.1`
- `Collector.kt` analytics version → `2.1.1`

---

## [2.1.0] — 2026-04 (previous release)

- Chat 2.0: federated user token flow via `POST /api/v1/tokens/federated-user`
- v2 DELETE endpoints for messages and message actions
- User metadata from PubNub directly for v2 shows
- Shoppettes module + pagination

## [2.0.x] — earlier releases

- Initial PubNub Kotlin SDK integration (8.0.0)
- Chat send/delete/like/unlike
- Show details, status, products
- Collector analytics
