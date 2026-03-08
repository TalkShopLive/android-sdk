package live.talkshop.sdk.core.show.models

enum class ShowType(val rawValue: String) {
    LEGACY("legacy"),
    V2("v2");

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun from(rawValue: String?): ShowType {
            return entries.firstOrNull { it.rawValue.equals(rawValue, ignoreCase = true) }
                ?: LEGACY
        }
    }
}