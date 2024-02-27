package live.talkshop.sdk.utils.helpers

object HelperFunctions {
    fun parseInt(duration: String?): Int? {
        return if (duration.isNullOrEmpty()) {
            duration!!.toInt()
        } else {
            null
        }
    }
}