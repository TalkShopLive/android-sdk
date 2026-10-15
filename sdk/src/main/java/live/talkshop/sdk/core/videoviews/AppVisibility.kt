package live.talkshop.sdk.core.videoviews

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.core.app.OnPictureInPictureModeChangedProvider
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tells video view trackers when the app goes to the background and comes back.
 *
 * Background = no started activity left (rotation does not count), or an activity
 * entered picture-in-picture. Foreground = an activity starts or resumes again.
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
    private val pipConsumers = HashMap<Int, Consumer<PictureInPictureModeChangedInfo>>()
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
        if (!activity.isInPictureInPictureMode) {
            setBackground(false)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        observePictureInPicture(activity)
        setBackground(false)
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity.isInPictureInPictureMode) {
            setBackground(true)
        }
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
        val id = System.identityHashCode(activity)
        startedActivities.remove(id)
        val consumer = pipConsumers.remove(id) ?: return
        try {
            (activity as? OnPictureInPictureModeChangedProvider)
                ?.removeOnPictureInPictureModeChangedListener(consumer)
        } catch (e: LinkageError) {
            // androidx.core without the PiP listener API: nothing was registered.
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    /**
     * The order of onPause and the PiP flag is not guaranteed on every API level,
     * so activities that report PiP changes are also observed directly.
     */
    private fun observePictureInPicture(activity: Activity) {
        val id = System.identityHashCode(activity)
        if (pipConsumers.containsKey(id)) return
        try {
            val provider = activity as? OnPictureInPictureModeChangedProvider ?: return
            val consumer = Consumer<PictureInPictureModeChangedInfo> { info ->
                if (info.isInPictureInPictureMode) {
                    setBackground(true)
                }
            }
            provider.addOnPictureInPictureModeChangedListener(consumer)
            pipConsumers[id] = consumer
        } catch (e: LinkageError) {
            // androidx.core without the PiP listener API: the onPause check is used alone.
        }
    }
}
