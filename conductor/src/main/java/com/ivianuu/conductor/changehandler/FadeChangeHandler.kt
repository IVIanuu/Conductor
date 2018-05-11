package com.ivianuu.conductor.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.ControllerChangeHandler

/**
 * An [AnimatorChangeHandler] that will cross fade two views
 */
open class FadeChangeHandler @JvmOverloads constructor(
    animationDuration: Long = DEFAULT_ANIMATION_DURATION,
    removesFromViewOnPush: Boolean = true
) : AnimatorChangeHandler(animationDuration, removesFromViewOnPush) {

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        val animator = AnimatorSet()
        if (to != null) {
            val start = if (toAddedToContainer) 0f else to.alpha
            animator.play(ObjectAnimator.ofFloat<View>(to, View.ALPHA, start, 1f))
        }

        if (from != null && (!isPush || removesFromViewOnPush)) {
            animator.play(ObjectAnimator.ofFloat<View>(from, View.ALPHA, 0f))
        }

        return animator
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f
    }

    override fun copy(): ControllerChangeHandler {
        return FadeChangeHandler(animationDuration, removesFromViewOnPush)
    }

    private companion object {
        private const val DEFAULT_ANIMATION_DURATION = 300L
    }
}
