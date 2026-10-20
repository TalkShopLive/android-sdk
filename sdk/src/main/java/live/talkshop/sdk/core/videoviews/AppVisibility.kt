package live.talkshop.sdk.core.videoviews

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tells video view trackers when the app goes to the background and comes back.
 *
 * Background = no started activity left (rotation does not count). Foreground = an activity
 * starts or resumes again. Picture-in-picture is not background: the activity stays started
 * and the video stays on screen.
 * Registered once from `TalkShopLive.initialize`. Lifecycle callbacks run on the main thread.
 */
internal object AppVisibility : Application.ActivityLifecycleCallbacks {

    interface Listener {
        fun onAppBackgrounded()
        fun onAppForegrounded()
    }

    @Volatile
    private var registered = false

    @Volatile
    var isBackground = false
        private set

    private val startedActivities = HashSet<Int>()
    private val listeners = CopyOnWriteArrayList<WeakReference<Listener>>()

    fun register(context: Context) {
        if (registered) return
        val application = context.applicationContext as? Application ?: return
        synchronized(this) {
            if (registered) return
            application.registerActivityLifecycleCallbacks(this)
            registered = true
        }
    }

    /** Adds a listener (held weakly) and tells it right away if the app is in the background. */
    fun addListener(listener: Listener) {
        listeners.add(WeakReference(listener))
        if (isBackground) {
            listener.onAppBackgrounded()
        }
    }

    fun removeListener(listener: Listener) {
        listeners.removeAll { it.get() == null || it.get() === listener }
    }

    private fun setBackground(background: Boolean) {
        if (isBackground == background) return
        isBackground = background
        listeners.forEach { reference ->
            val listener = reference.get()
            if (listener == null) {
                listeners.remove(reference)
            } else if (background) {
                listener.onAppBackgrounded()
            } else {
                listener.onAppForegrounded()
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities.add(System.identityHashCode(activity))
        setBackground(false)
    }

    override fun onActivityResumed(activity: Activity) {
        setBackground(false)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.remove(System.identityHashCode(activity))
        // Late registration: an activity started before init is unknown, so an empty set
        // after its stop still means nothing is visible.
        if (startedActivities.isEmpty() && !activity.isChangingConfigurations) {
            setBackground(true)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        startedActivities.remove(System.identityHashCode(activity))
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
