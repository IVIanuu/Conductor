package com.ivianuu.conductor

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.ViewGroup
import com.ivianuu.conductor.internal.LifecycleHandler
import com.ivianuu.conductor.internal.ensureMainThread

/**
 * Point of initial interaction with Conductor. Used to attach a [Router] to your Activity.
 */
object Conductor {

    /**
     * Conductor will create a [Router] that has been initialized for your Activity and containing ViewGroup.
     * If an existing [Router] is already associated with this Activity/ViewGroup pair, either in memory
     * or in the savedInstanceState, that router will be used and rebound instead of creating a new one with
     * an empty backstack.
     */
    @JvmStatic
    fun attachRouter(
        activity: FragmentActivity,
        container: ViewGroup,
        savedInstanceState: Bundle?
    ): Router {
        ensureMainThread()

        val lifecycleHandler = LifecycleHandler.install(activity)

        val router = lifecycleHandler.getRouter(container, savedInstanceState)
        router.rebindIfNeeded()

        return router
    }

}
