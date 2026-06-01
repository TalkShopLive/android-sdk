# Changelog

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
