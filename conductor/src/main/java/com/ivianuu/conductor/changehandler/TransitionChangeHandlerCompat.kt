package com.ivianuu.conductor.changehandler

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.internal.ClassUtils

/**
 * A base [ControllerChangeHandler] that facilitates using [android.transition.Transition]s to replace Controller Views.
 * If the target device is running on a version of Android that doesn't support transitions, a fallback [ControllerChangeHandler] will be used.
 */
open class TransitionChangeHandlerCompat : ControllerChangeHandler {

    lateinit var changeHandler: ControllerChangeHandler

    override var removesFromViewOnPush: Boolean
        get() = changeHandler.removesFromViewOnPush
        set(value) { changeHandler.removesFromViewOnPush = value }

    constructor()

    /**
     * Constructor that takes a [TransitionChangeHandler] for use with compatible devices, as well as a fallback
     * [ControllerChangeHandler] for use with older devices.
     */
    constructor(
        transitionChangeHandler: TransitionChangeHandler,
        fallbackChangeHandler: ControllerChangeHandler
    ) {
        changeHandler = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            transitionChangeHandler
        } else {
            fallbackChangeHandler
        }
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) {
        changeHandler.performChange(container, from, to, isPush, changeListener)
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)

        bundle.putString(KEY_CHANGE_HANDLER_CLASS, changeHandler.javaClass.name)

        val stateBundle = Bundle()
        changeHandler.saveToBundle(stateBundle)
        bundle.putBundle(KEY_HANDLER_STATE, stateBundle)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)

        val className = bundle.getString(KEY_CHANGE_HANDLER_CLASS)
        changeHandler = ClassUtils.newInstance<ControllerChangeHandler>(className)!!
        changeHandler.restoreFromBundle(bundle.getBundle(KEY_HANDLER_STATE))
    }

    override fun copy(): ControllerChangeHandler {
        return TransitionChangeHandlerCompat().apply {
            changeHandler = this@TransitionChangeHandlerCompat.changeHandler.copy()
        }
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        changeHandler.onAbortPush(newHandler, newTop)
    }

    override fun completeImmediately() {
        changeHandler.completeImmediately()
    }

    companion object {
        private const val KEY_CHANGE_HANDLER_CLASS =
            "TransitionChangeHandlerCompat.changeHandler.class"
        private const val KEY_HANDLER_STATE = "TransitionChangeHandlerCompat.changeHandler.state"
    }

}
