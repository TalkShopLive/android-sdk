package live.talkshop.sdk.core.show.models

enum class ShowType(val rawValue: String) {
    LEGACY("legacy"),
    V2("v2");

    companion object {
        fun from(rawValue: String?): ShowType {
            return values().firstOrNull { showType ->
                showType.rawValue.equals(rawValue, ignoreCase = true)
            } ?: LEGACY
        }
    }
}