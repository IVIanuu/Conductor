package com.ivianuu.conductor

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.ViewGroup
import com.ivianuu.conductor.internal.LifecycleHandler
import com.ivianuu.conductor.internal.ThreadUtils

/**
 * Point of initial interaction with Conductor. Used to attach a [Router] to your Activity.
 */
object Conductor {

    /**
     * Conductor will create a [Router] that has been initialized for your Activity and containing ViewGroup.
     * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
     * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
     * an empty backstack.
     *
     * @param activity The Activity that will host the [Router] being attached.
     * @param container The ViewGroup in which the [Router]'s [Controller] views will be hosted
     * @param savedInstanceState The savedInstanceState passed into the hosting Activity's onCreate method. Used
     * for restoring the Router's state if possible.
     * @return A fully configured [Router] instance for use with this Activity/ViewGroup pair.
     */
    @JvmStatic
    fun attachRouter(
        activity: FragmentActivity,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): Router {
        ThreadUtils.ensureMainThread()

        val lifecycleHandler = LifecycleHandler.install(activity)

        val router = lifecycleHandler.getRouter(container, savedInstanceState)
        router.rebindIfNeeded()

        return router
    }

}
