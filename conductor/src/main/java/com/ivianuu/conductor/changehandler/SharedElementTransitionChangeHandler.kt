package com.ivianuu.conductor.changehandler

import android.annotation.TargetApi
import android.app.SharedElementCallback
import android.graphics.Rect
import android.os.Build
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.transition.TransitionSet
import android.util.ArrayMap
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnPreDrawListener
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.internal.TransitionUtils

/**
 * A TransitionChangeHandler that facilitates using different Transitions for the entering view, the exiting view,
 * and shared elements between the two.
 */
// Much of this class is based on FragmentTransition.java and FragmentTransitionCompat21.java from the Android support library
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
abstract class SharedElementTransitionChangeHandler : TransitionChangeHandler() {

    // A map of from -> to names. Generally these will be the same.
    private val sharedElementNames = ArrayMap<String, String>()

    private val waitForTransitionNames = mutableListOf<String>()
    private val removedViews = mutableListOf<ViewParentPair>()

    private var exitTransition: Transition? = null
    private var enterTransition: Transition? = null
    private var sharedElementTransition: Transition? = null
    private var exitTransitionCallback: SharedElementCallback? = null
    private var enterTransitionCallback: SharedElementCallback? = null

    override fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition {
        exitTransition = getExitTransition(container, from, to, isPush)
        enterTransition = getEnterTransition(container, from, to, isPush)
        sharedElementTransition = getSharedElementTransition(container, from, to, isPush)
        exitTransitionCallback = getExitTransitionCallback(container, from, to, isPush)
        enterTransitionCallback = getEnterTransitionCallback(container, from, to, isPush)

        if (enterTransition == null && sharedElementTransition == null && exitTransition == null) {
            throw IllegalStateException("SharedElementTransitionChangeHandler must have at least one transaction.")
        }

        return mergeTransitions(isPush)
    }

    override fun prepareForTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition,
        isPush: Boolean,
        onTransitionPreparedListener: TransitionChangeHandler.OnTransitionPreparedListener
    ) {
        val listener = object : TransitionChangeHandler.OnTransitionPreparedListener {
            override fun onPrepared() {
                configureTransition(container, from, to, transition, isPush)
                onTransitionPreparedListener.onPrepared()
            }
        }

        configureSharedElements(container, from, to, isPush)

        if (to != null && to.parent == null && waitForTransitionNames.size > 0) {
            waitOnAllTransitionNames(to, listener)
            container.addView(to)
        } else {
            listener.onPrepared()
        }
    }

    override fun executePropertyChanges(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition?,
        isPush: Boolean
    ) {
        if (to != null && removedViews.size > 0) {
            to.visibility = View.VISIBLE
            removedViews.forEach { it.parent.addView(it.view) }
            removedViews.clear()
        }

        super.executePropertyChanges(container, from, to, transition, isPush)
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)

        removedViews.clear()
    }

    internal fun configureTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition,
        isPush: Boolean
    ) {
        val nonExistentView = View(container.context)

        val fromSharedElements = mutableListOf<View>()
        val toSharedElements = mutableListOf<View>()

        configureSharedElements(
            container,
            nonExistentView,
            to,
            from,
            isPush,
            fromSharedElements,
            toSharedElements
        )

        val exitingViews = if (exitTransition != null) configureEnteringExitingViews(
            exitTransition!!,
            from,
            fromSharedElements,
            nonExistentView
        ) else null
        if (exitingViews == null || exitingViews.isEmpty()) {
            exitTransition = null
        }

        if (enterTransition != null) {
            enterTransition!!.addTarget(nonExistentView)
        }

        val enteringViews = mutableListOf<View>()
        scheduleRemoveTargets(
            transition,
            enterTransition,
            enteringViews,
            exitTransition,
            exitingViews,
            sharedElementTransition,
            toSharedElements
        )
        scheduleTargetChange(
            container,
            to,
            nonExistentView,
            toSharedElements,
            enteringViews,
            exitingViews!!.toMutableList()
        )

        setNameOverrides(container, toSharedElements)
        scheduleNameReset(container, toSharedElements)
    }

    private fun waitOnAllTransitionNames(
        to: View,
        onTransitionPreparedListener: TransitionChangeHandler.OnTransitionPreparedListener
    ) {
        val onPreDrawListener = object : OnPreDrawListener {
            internal var addedSubviewListeners = false

            override fun onPreDraw(): Boolean {
                val foundViews = mutableListOf<View>()
                var allViewsFound = true
                for (transitionName in waitForTransitionNames) {
                    val namedView = TransitionUtils.findNamedView(to, transitionName)
                    if (namedView != null) {
                        foundViews.add(namedView)
                    } else {
                        allViewsFound = false
                        break
                    }
                }

                if (allViewsFound && !addedSubviewListeners) {
                    addedSubviewListeners = true
                    waitOnChildTransitionNames(to, foundViews, this, onTransitionPreparedListener)
                }

                return false
            }
        }

        to.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
    }

    internal fun waitOnChildTransitionNames(
        to: View,
        foundViews: List<View>,
        parentPreDrawListener: OnPreDrawListener,
        onTransitionPreparedListener: TransitionChangeHandler.OnTransitionPreparedListener
    ) {
        for (view in foundViews) {
            OneShotPreDrawListener.add(true, view, Runnable {
                waitForTransitionNames.remove(view.transitionName)

                removedViews.add(ViewParentPair(view, view.parent as ViewGroup))
                (view.parent as ViewGroup).removeView(view)

                if (waitForTransitionNames.size == 0) {
                    to.viewTreeObserver.removeOnPreDrawListener(parentPreDrawListener)
                    to.visibility = View.INVISIBLE
                    onTransitionPreparedListener.onPrepared()
                }
            })
        }
    }

    private fun scheduleTargetChange(
        container: ViewGroup,
        to: View?,
        nonExistentView: View,
        toSharedElements: List<View>,
        enteringViews: MutableList<View>,
        exitingViews: MutableList<View>?
    ) {
        OneShotPreDrawListener.add(true, container, Runnable {
            if (enterTransition != null) {
                enterTransition!!.removeTarget(nonExistentView)
                val views = configureEnteringExitingViews(
                    enterTransition!!,
                    to,
                    toSharedElements,
                    nonExistentView
                )
                enteringViews.addAll(views)
            }

            if (exitingViews != null) {
                if (exitTransition != null) {
                    val tempExiting = mutableListOf<View>()
                    tempExiting.add(nonExistentView)
                    TransitionUtils.replaceTargets(exitTransition!!, exitingViews, tempExiting)
                }
                exitingViews.clear()
                exitingViews.add(nonExistentView)
            }
        })
    }

    private fun mergeTransitions(isPush: Boolean): Transition {
        val overlap =
            enterTransition == null || exitTransition == null || allowTransitionOverlap(isPush)

        if (overlap) {
            return TransitionUtils.mergeTransitions(
                TransitionSet.ORDERING_TOGETHER,
                exitTransition,
                enterTransition,
                sharedElementTransition
            )
        } else {
            val staggered = TransitionUtils.mergeTransitions(
                TransitionSet.ORDERING_SEQUENTIAL,
                exitTransition,
                enterTransition
            )
            return TransitionUtils.mergeTransitions(
                TransitionSet.ORDERING_TOGETHER,
                staggered,
                sharedElementTransition
            )
        }
    }

    internal fun configureEnteringExitingViews(
        transition: Transition,
        view: View?,
        sharedElements: List<View>,
        nonExistentView: View
    ): List<View> {
        val viewList = mutableListOf<View>()
        if (view != null) {
            captureTransitioningViews(viewList, view)
        }
        viewList.removeAll(sharedElements)
        if (!viewList.isEmpty()) {
            viewList.add(nonExistentView)
            TransitionUtils.addTargets(transition, viewList)
        }
        return viewList
    }

    private fun configureSharedElements(
        container: ViewGroup, nonExistentView: View, to: View?, from: View?,
        isPush: Boolean, fromSharedElements: MutableList<View>, toSharedElements: MutableList<View>
    ) {

        if (to == null || from == null) {
            return
        }

        val capturedFromSharedElements = captureFromSharedElements(from)

        if (sharedElementNames.isEmpty()) {
            sharedElementTransition = null
        } else if (capturedFromSharedElements != null) {
            fromSharedElements.addAll(capturedFromSharedElements.values)
        }

        if (enterTransition == null && exitTransition == null && sharedElementTransition == null) {
            return
        }

        callSharedElementStartEnd(capturedFromSharedElements, true)

        val toEpicenter: Rect?
        if (sharedElementTransition != null) {
            toEpicenter = Rect()
            TransitionUtils.setTargets(
                sharedElementTransition!!,
                nonExistentView,
                fromSharedElements
            )
            setFromEpicenter(capturedFromSharedElements)
            if (enterTransition != null) {
                enterTransition!!.epicenterCallback = object : Transition.EpicenterCallback() {
                    override fun onGetEpicenter(transition: Transition): Rect? {
                        return if (toEpicenter.isEmpty) {
                            null
                        } else toEpicenter
                    }
                }
            }
        } else {
            toEpicenter = null
        }

        OneShotPreDrawListener.add(true, container, Runnable {
            val capturedToSharedElements = captureToSharedElements(to, isPush)

            if (capturedToSharedElements != null) {
                toSharedElements.addAll(capturedToSharedElements.values)
                toSharedElements.add(nonExistentView)
            }

            callSharedElementStartEnd(capturedToSharedElements, false)
            sharedElementTransition?.let { sharedElementTransition ->
                sharedElementTransition.targets.clear()
                sharedElementTransition.targets.addAll(toSharedElements)
                TransitionUtils.replaceTargets(
                    sharedElementTransition,
                    fromSharedElements,
                    toSharedElements
                )

                val toEpicenterView = getToEpicenterView(capturedToSharedElements)
                if (toEpicenterView != null && toEpicenter != null) {
                    TransitionUtils.getBoundsOnScreen(toEpicenterView, toEpicenter)
                }
            }
        })
    }

    internal fun getToEpicenterView(toSharedElements: ArrayMap<String, View>?): View? {
        return if (enterTransition != null && sharedElementNames.size > 0 && toSharedElements != null) {
            toSharedElements[sharedElementNames.valueAt(0)]
        } else null
    }

    private fun setFromEpicenter(fromSharedElements: ArrayMap<String, View>?) {
        if (sharedElementNames.size > 0 && fromSharedElements != null) {
            val fromEpicenterView = fromSharedElements[sharedElementNames.keyAt(0)]

            if (fromEpicenterView != null) {
                sharedElementTransition?.let { TransitionUtils.setEpicenter(it, fromEpicenterView) }
                exitTransition?.let { TransitionUtils.setEpicenter(it, fromEpicenterView) }
            }
        }
    }

    internal fun captureToSharedElements(to: View?, isPush: Boolean): ArrayMap<String, View>? {
        if (sharedElementNames.isEmpty() || sharedElementTransition == null || to == null) {
            sharedElementNames.clear()
            return null
        }

        val toSharedElements = ArrayMap<String, View>()
        TransitionUtils.findNamedViews(toSharedElements, to)
        for (removedView in removedViews) {
            toSharedElements[removedView.view.transitionName] = removedView.view
        }

        val names = sharedElementNames.values.toList()

        toSharedElements.retainAll(names)
        if (enterTransitionCallback != null) {
            enterTransitionCallback!!.onMapSharedElements(names, toSharedElements)
            for (i in names.indices.reversed()) {
                val name = names[i]
                val view = toSharedElements[name]
                if (view == null) {
                    val key = findKeyForValue(sharedElementNames, name)
                    if (key != null) {
                        sharedElementNames.remove(key)
                    }
                } else if (name != view.transitionName) {
                    val key = findKeyForValue(sharedElementNames, name)
                    if (key != null) {
                        sharedElementNames[key] = view.transitionName
                    }
                }
            }
        } else {
            for (i in sharedElementNames.size - 1 downTo 0) {
                val targetName = sharedElementNames.valueAt(i)
                if (!toSharedElements.containsKey(targetName)) {
                    sharedElementNames.removeAt(i)
                }
            }
        }
        return toSharedElements
    }

    private fun findKeyForValue(map: ArrayMap<String, String>, value: String): String? {
        val numElements = map.size
        for (i in 0 until numElements) {
            if (value == map.valueAt(i)) {
                return map.keyAt(i)
            }
        }
        return null
    }

    private fun captureFromSharedElements(from: View): ArrayMap<String, View>? {
        if (sharedElementNames.isEmpty() || sharedElementTransition == null) {
            sharedElementNames.clear()
            return null
        }

        val fromSharedElements = ArrayMap<String, View>()
        TransitionUtils.findNamedViews(fromSharedElements, from)

        val names = sharedElementNames.keys.toList()

        fromSharedElements.retainAll(names)
        if (exitTransitionCallback != null) {
            exitTransitionCallback!!.onMapSharedElements(names, fromSharedElements)
            for (i in names.indices.reversed()) {
                val name = names[i]
                val view = fromSharedElements[name]
                if (view == null) {
                    sharedElementNames.remove(name)
                } else if (name != view.transitionName) {
                    val targetValue = sharedElementNames.remove(name)
                    sharedElementNames[view.transitionName] = targetValue
                }
            }
        } else {
            sharedElementNames.retainAll(fromSharedElements.keys)
        }
        return fromSharedElements
    }

    private fun callSharedElementStartEnd(
        sharedElements: ArrayMap<String, View>?,
        isStart: Boolean
    ) {
        if (enterTransitionCallback != null) {
            val count = sharedElements?.size ?: 0
            val views = mutableListOf<View>()
            val names = mutableListOf<String>()
            for (i in 0 until count) {
                names.add(sharedElements!!.keyAt(i))
                views.add(sharedElements.valueAt(i))
            }
            if (isStart) {
                enterTransitionCallback!!.onSharedElementStart(names, views, null)
            } else {
                enterTransitionCallback!!.onSharedElementEnd(names, views, null)
            }
        }
    }

    private fun captureTransitioningViews(transitioningViews: MutableList<View>, view: View) {
        if (view.visibility == View.VISIBLE) {
            if (view is ViewGroup) {
                if (view.isTransitionGroup) {
                    transitioningViews.add(view)
                } else {
                    val count = view.childCount
                    for (i in 0 until count) {
                        val child = view.getChildAt(i)
                        captureTransitioningViews(transitioningViews, child)
                    }
                }
            } else {
                transitioningViews.add(view)
            }
        }
    }

    private fun scheduleRemoveTargets(
        overallTransition: Transition,
        enterTransition: Transition?, enteringViews: List<View>?,
        exitTransition: Transition?, exitingViews: List<View>?,
        sharedElementTransition: Transition?, toSharedElements: List<View>?
    ) {
        overallTransition.addListener(object : TransitionListener {
            override fun onTransitionStart(transition: Transition) {
                if (enterTransition != null && enteringViews != null) {
                    TransitionUtils.replaceTargets(enterTransition, enteringViews, emptyList())
                }
                if (exitTransition != null && exitingViews != null) {
                    TransitionUtils.replaceTargets(exitTransition, exitingViews, emptyList())
                }
                if (sharedElementTransition != null && toSharedElements != null) {
                    TransitionUtils.replaceTargets(sharedElementTransition, toSharedElements, emptyList())
                }
            }

            override fun onTransitionEnd(transition: Transition) {}

            override fun onTransitionCancel(transition: Transition) {}

            override fun onTransitionPause(transition: Transition) {}

            override fun onTransitionResume(transition: Transition) {}
        })
    }

    private fun setNameOverrides(container: View, toSharedElements: List<View>) {
        OneShotPreDrawListener.add(true, container, Runnable {
            val numSharedElements = toSharedElements.size
            for (i in 0 until numSharedElements) {
                val view = toSharedElements[i]
                val name = view.transitionName
                if (name != null) {
                    val inName = findKeyForValue(sharedElementNames, name)
                    view.transitionName = inName
                }
            }
        })
    }

    private fun scheduleNameReset(container: ViewGroup, toSharedElements: List<View>) {
        OneShotPreDrawListener.add(true, container, Runnable {
            val numSharedElements = toSharedElements.size
            for (i in 0 until numSharedElements) {
                val view = toSharedElements[i]
                val name = view.transitionName
                val inName = sharedElementNames[name]
                view.transitionName = inName
            }
        })
    }

    /**
     * Will be called when views are ready to have their shared elements configured. Within this method one of the addSharedElement methods
     * should be called for each shared element that will be used. If one or more of these shared elements will not instantly be available in
     * the incoming view (for ex, in a recycler_view), waitOnSharedElementNamed can be called to delay the transition until everything is available.
     */
    abstract fun configureSharedElements(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    )

    /**
     * Should return the transition that will be used on the exiting ("from") view, if one is desired.
     */
    abstract fun getExitTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition?

    /**
     * Should return the transition that will be used on shared elements between the from and to views.
     */
    abstract fun getSharedElementTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition?

    /**
     * Should return the transition that will be used on the entering ("to") view, if one is desired.
     */
    abstract fun getEnterTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition?

    /**
     * Should return a callback that can be used to customize transition behavior of the shared element transition for the "from" view.
     */
    open fun getExitTransitionCallback(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): SharedElementCallback? {
        return null
    }

    /**
     * Should return a callback that can be used to customize transition behavior of the shared element transition for the "to" view.
     */
    open fun getEnterTransitionCallback(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): SharedElementCallback? {
        return null
    }

    /**
     * Should return whether or not the the exit transition and enter transition should overlap. If true,
     * the enter transition will start as soon as possible. Otherwise, the enter transition will wait until the
     * completion of the exit transition. Defaults to true.
     */
    open fun allowTransitionOverlap(isPush: Boolean): Boolean {
        return true
    }

    /**
     * Used to register an element that will take part in the shared element transition.
     */
    protected open fun addSharedElement(name: String) {
        sharedElementNames[name] = name
    }

    /**
     * Used to register an element that will take part in the shared element transition. Maps the name used in the
     * "from" view to the name used in the "to" view if they are not the same.
     */
    protected open fun addSharedElement(fromName: String, toName: String) {
        sharedElementNames[fromName] = toName
    }

    /**
     * Used to register an element that will take part in the shared element transition. Maps the name used in the
     * "from" view to the name used in the "to" view if they are not the same.
     */
    protected open fun addSharedElement(sharedElement: View, toName: String) {
        val transitionName = sharedElement.transitionName
                ?: throw IllegalArgumentException("Unique transitionNames are required for all sharedElements")
        sharedElementNames[transitionName] = toName
    }

    /**
     * The transition will be delayed until the view with the name passed in is available in the "to" hierarchy. This is
     * particularly useful for views that don't load instantly, like recycler_views. Note that using this method can
     * potentially lock up your app indefinitely if the view never loads!
     */
    protected open fun waitOnSharedElementNamed(name: String) {
        if (!sharedElementNames.values.contains(name)) {
            throw IllegalStateException("Can't wait on a shared element that hasn't been registered using addSharedElement")
        }
        waitForTransitionNames.add(name)
    }

    private class OneShotPreDrawListener private constructor(
        private val preDrawReturnValue: Boolean,
        private val view: View,
        private val runnable: Runnable
    ) : OnPreDrawListener, View.OnAttachStateChangeListener {
        private var viewTreeObserver: ViewTreeObserver? = null

        init {
            viewTreeObserver = view.viewTreeObserver
        }

        override fun onPreDraw(): Boolean {
            removeListener()
            runnable.run()
            return preDrawReturnValue
        }

        private fun removeListener() {
            if (viewTreeObserver!!.isAlive) {
                viewTreeObserver!!.removeOnPreDrawListener(this)
            } else {
                view.viewTreeObserver.removeOnPreDrawListener(this)
            }
            view.removeOnAttachStateChangeListener(this)
        }

        override fun onViewAttachedToWindow(v: View) {
            viewTreeObserver = v.viewTreeObserver
        }

        override fun onViewDetachedFromWindow(v: View) {
            removeListener()
        }

        companion object {

            fun add(
                preDrawReturnValue: Boolean,
                view: View,
                runnable: Runnable
            ): OneShotPreDrawListener {
                val listener = OneShotPreDrawListener(preDrawReturnValue, view, runnable)
                view.viewTreeObserver.addOnPreDrawListener(listener)
                view.addOnAttachStateChangeListener(listener)
                return listener
            }
        }

    }

    internal class ViewParentPair internal constructor(
        internal val view: View,
        internal val parent: ViewGroup
    )

}
