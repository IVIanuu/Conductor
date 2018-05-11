package com.ivianuu.conductor.changehandler

import android.os.Bundle
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup

import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler

/**
 * A [ControllerChangeHandler] that will instantly swap Views with no animations or transitions.
 */
open class SimpleSwapChangeHandler @JvmOverloads constructor(
    removesFromViewOnPush: Boolean = true
) : ControllerChangeHandler(), OnAttachStateChangeListener {

    override val isReusable: Boolean
        get() = true

    private var canceled = false
    private var container: ViewGroup? = null
    private var changeListener: ControllerChangeHandler.ControllerChangeCompletedListener? = null

    init {
        this.removesFromViewOnPush = removesFromViewOnPush
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putBoolean(KEY_REMOVES_FROM_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_ON_PUSH)
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)
        canceled = true
    }

    override fun completeImmediately() {
        if (changeListener != null) {
            changeListener?.onChangeCompleted()
            changeListener = null

            container?.removeOnAttachStateChangeListener(this)
            container = null
        }
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) {
        if (!canceled) {
            if (from != null && (!isPush || removesFromViewOnPush)) {
                container.removeView(from)
            }

            if (to != null && to.parent == null) {
                container.addView(to)
            }
        }

        if (container.windowToken != null) {
            changeListener.onChangeCompleted()
        } else {
            this.changeListener = changeListener
            this.container = container
            container.addOnAttachStateChangeListener(this)
        }

    }

    override fun onViewAttachedToWindow(v: View) {
        v.removeOnAttachStateChangeListener(this)

        if (changeListener != null) {
            changeListener?.onChangeCompleted()
            changeListener = null
            container = null
        }
    }

    override fun onViewDetachedFromWindow(v: View) {}

    override fun copy(): ControllerChangeHandler {
        return SimpleSwapChangeHandler(removesFromViewOnPush)
    }

    companion object {
        private const val KEY_REMOVES_FROM_ON_PUSH = "SimpleSwapChangeHandler.removesFromViewOnPush"
    }
}
