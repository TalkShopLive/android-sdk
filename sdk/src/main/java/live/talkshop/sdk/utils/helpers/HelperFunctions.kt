package live.talkshop.sdk.utils.helpers

object HelperFunctions {
    fun parseInt(duration: String?): Int? {
        return if (isNotEmptyOrNull(duration)) {
            duration!!.toInt()
        } else {
            null
        }
    }

    fun isNotEmptyOrNull(string: String?): Boolean {
        return !string.isNullOrEmpty() && !string.equals("null")
    }
}