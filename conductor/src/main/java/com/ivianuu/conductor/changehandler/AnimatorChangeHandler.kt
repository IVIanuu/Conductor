package com.ivianuu.conductor.changehandler

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler

/**
 * A base [ControllerChangeHandler] that facilitates using [android.animation.Animator]s to replace Controller Views
 */
abstract class AnimatorChangeHandler(
    duration: Long,
    internal var removesFromViewOnPush: Boolean
) : ControllerChangeHandler() {

    var animationDuration = 0L
        private set
    internal var canceled = false
    internal var needsImmediateCompletion = false
    private var completed = false
    internal var animator: Animator? = null
    private var onAnimationReadyOrAbortedListener: OnAnimationReadyOrAbortedListener? = null

    constructor() : this(DEFAULT_ANIMATION_DURATION, true) {}

    constructor(removesFromViewOnPush: Boolean) : this(
        DEFAULT_ANIMATION_DURATION,
        removesFromViewOnPush
    ) {
    }

    constructor(duration: Long) : this(duration, true) {}

    init {
        animationDuration = duration
    }

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putLong(KEY_DURATION, animationDuration)
        bundle.putBoolean(KEY_REMOVES_FROM_ON_PUSH, removesFromViewOnPush)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        animationDuration = bundle.getLong(KEY_DURATION)
        removesFromViewOnPush = bundle.getBoolean(KEY_REMOVES_FROM_ON_PUSH)
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)

        canceled = true
        if (animator != null) {
            animator?.cancel()
        } else if (onAnimationReadyOrAbortedListener != null) {
            onAnimationReadyOrAbortedListener?.onReadyOrAborted()
        }
    }

    override fun completeImmediately() {
        super.completeImmediately()

        needsImmediateCompletion = true
        if (animator != null) {
            animator?.end()
        } else if (onAnimationReadyOrAbortedListener != null) {
            onAnimationReadyOrAbortedListener?.onReadyOrAborted()
        }
    }

    override fun removesFromViewOnPush(): Boolean {
        return removesFromViewOnPush
    }

    /**
     * Should be overridden to return the Animator to use while replacing Views.
     *
     * @param container The container these Views are hosted in.
     * @param from The previous View in the container or `null` if there was no Controller before this transition
     * @param to The next View that should be put in the container or `null` if no Controller is being transitioned to
     * @param isPush True if this is a push transaction, false if it's a pop.
     * @param toAddedToContainer True if the "to" view was added to the container as a part of this ChangeHandler. False if it was already in the hierarchy.
     */
    protected abstract fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator

    /**
     * Will be called after the animation is complete to reset the View that was removed to its pre-animation state.
     */
    protected abstract fun resetFromView(from: View)

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) {
        var readyToAnimate = true
        val addingToView = to != null && to.parent == null

        if (addingToView) {
            if (isPush || from == null) {
                container.addView(to)
            } else if (to?.parent == null) {
                container.addView(to, container.indexOfChild(from))
            }

            if (to!!.width <= 0 && to.height <= 0) {
                readyToAnimate = false
                onAnimationReadyOrAbortedListener = OnAnimationReadyOrAbortedListener(
                    container,
                    from,
                    to,
                    isPush,
                    true,
                    changeListener
                )
                to.viewTreeObserver.addOnPreDrawListener(onAnimationReadyOrAbortedListener)
            }
        }

        if (readyToAnimate) {
            performAnimation(container, from, to, isPush, addingToView, changeListener)
        }
    }

    internal fun complete(
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener,
        animatorListener: AnimatorListener?
    ) {
        if (!completed) {
            completed = true
            changeListener.onChangeCompleted()
        }

        if (animator != null) {
            if (animatorListener != null) {
                animator?.removeListener(animatorListener)
            }
            animator?.cancel()
            animator = null
        }

        onAnimationReadyOrAbortedListener = null
    }

    internal fun performAnimation(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) {
        if (canceled) {
            complete(changeListener, null)
            return
        }
        if (needsImmediateCompletion) {
            if (from != null && (!isPush || removesFromViewOnPush)) {
                container.removeView(from)
            }
            complete(changeListener, null)
            if (isPush && from != null) {
                resetFromView(from)
            }
            return
        }

        animator = getAnimator(container, from, to, isPush, toAddedToContainer)

        if (animationDuration > 0) {
            animator?.duration = animationDuration
        }

        animator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {
                if (from != null && (!isPush || removesFromViewOnPush) && needsImmediateCompletion) {
                    container.removeView(from)
                }

                complete(changeListener, this)
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!canceled && animator != null) {
                    if (from != null && (!isPush || removesFromViewOnPush)) {
                        container.removeView(from)
                    }

                    complete(changeListener, this)

                    if (isPush && from != null) {
                        resetFromView(from)
                    }
                }
            }
        })

        animator?.start()
    }

    private inner class OnAnimationReadyOrAbortedListener internal constructor(
        internal val container: ViewGroup,
        internal val from: View?,
        internal val to: View?,
        internal val isPush: Boolean,
        internal val addingToView: Boolean,
        internal val changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) : ViewTreeObserver.OnPreDrawListener {
        private var hasRun= false

        override fun onPreDraw(): Boolean {
            onReadyOrAborted()

            return true
        }

        internal fun onReadyOrAborted() {
            if (!hasRun) {
                hasRun = true

                if (to != null) {
                    val observer = to.viewTreeObserver
                    if (observer.isAlive) {
                        observer.removeOnPreDrawListener(this)
                    }
                }

                performAnimation(container, from, to, isPush, addingToView, changeListener)
            }
        }

    }

    companion object {
        private const val KEY_DURATION = "AnimatorChangeHandler.duration"
        private const val KEY_REMOVES_FROM_ON_PUSH = "AnimatorChangeHandler.removesFromViewOnPush"

        const val DEFAULT_ANIMATION_DURATION= -1L
    }

}
