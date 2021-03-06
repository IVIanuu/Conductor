package com.ivianuu.conductor.internal

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup

internal class ViewAttachHandler(private val attachListener: ViewAttachListener) :
    OnAttachStateChangeListener {

    private var rootAttached = false
    private var childrenAttached = false
    private var activityStopped = false
    private var reportedState = ReportedState.VIEW_DETACHED
    private var childOnAttachStateChangeListener: OnAttachStateChangeListener? = null

    private enum class ReportedState {
        VIEW_DETACHED,
        ACTIVITY_STOPPED,
        ATTACHED
    }

    interface ViewAttachListener {
        fun onAttached()
        fun onDetached(fromActivityStop: Boolean)
        fun onViewDetachAfterStop()
    }

    private interface ChildAttachListener {
        fun onAttached()
    }

    override fun onViewAttachedToWindow(v: View) {
        if (rootAttached) {
            return
        }

        rootAttached = true
        listenForDeepestChildAttach(v, object : ChildAttachListener {
            override fun onAttached() {
                childrenAttached = true
                reportAttached()

            }
        })
    }

    override fun onViewDetachedFromWindow(v: View) {
        rootAttached = false
        if (childrenAttached) {
            childrenAttached = false
            reportDetached(false)
        }
    }

    fun listenForAttach(view: View) {
        view.addOnAttachStateChangeListener(this)
    }

    fun unregisterAttachListener(view: View) {
        view.removeOnAttachStateChangeListener(this)

        if (childOnAttachStateChangeListener != null && view is ViewGroup) {
            findDeepestChild(view).removeOnAttachStateChangeListener(
                childOnAttachStateChangeListener
            )
        }
    }

    fun onActivityStarted() {
        activityStopped = false
        reportAttached()
    }

    fun onActivityStopped() {
        activityStopped = true
        reportDetached(true)
    }

    internal fun reportAttached() {
        if (rootAttached && childrenAttached && !activityStopped && reportedState != ReportedState.ATTACHED) {
            reportedState = ReportedState.ATTACHED
            attachListener.onAttached()
        }
    }

    private fun reportDetached(detachedForActivity: Boolean) {
        val wasDetachedForActivity = reportedState == ReportedState.ACTIVITY_STOPPED

        reportedState = if (detachedForActivity) {
            ReportedState.ACTIVITY_STOPPED
        } else {
            ReportedState.VIEW_DETACHED
        }

        if (wasDetachedForActivity && !detachedForActivity) {
            attachListener.onViewDetachAfterStop()
        } else {
            attachListener.onDetached(detachedForActivity)
        }
    }

    private fun listenForDeepestChildAttach(view: View, attachListener: ChildAttachListener) {
        if (view !is ViewGroup) {
            attachListener.onAttached()
            return
        }

        if (view.childCount == 0) {
            attachListener.onAttached()
            return
        }

        childOnAttachStateChangeListener = object : OnAttachStateChangeListener {
            private var attached = false

            override fun onViewAttachedToWindow(v: View) {
                if (!attached) {
                    attached = true
                    attachListener.onAttached()
                    v.removeOnAttachStateChangeListener(this)
                    childOnAttachStateChangeListener = null
                }
            }

            override fun onViewDetachedFromWindow(v: View) {}
        }

        findDeepestChild(view)
            .addOnAttachStateChangeListener(childOnAttachStateChangeListener)
    }

    private fun findDeepestChild(viewGroup: ViewGroup): View {
        if (viewGroup.childCount == 0) {
            return viewGroup
        }

        val lastChild = viewGroup.getChildAt(viewGroup.childCount - 1)
        return if (lastChild is ViewGroup) {
            findDeepestChild(lastChild)
        } else {
            lastChild
        }
    }

}
