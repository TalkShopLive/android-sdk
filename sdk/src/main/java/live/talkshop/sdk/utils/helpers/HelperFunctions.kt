package live.talkshop.sdk.utils.helpers

internal object HelperFunctions {
    fun parseInt(duration: String?): Int? {
        return if (isNotEmptyOrNull(duration)) {
            duration!!.toInt()
        } else {
            null
        }
    }

    fun isNotEmptyOrNull(string: String?): Boolean {
        return !string.isNullOrEmpty() && string != "null"
    }
}