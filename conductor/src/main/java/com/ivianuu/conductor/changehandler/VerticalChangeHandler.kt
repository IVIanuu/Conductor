package com.ivianuu.conductor.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.ControllerChangeHandler

/**
 * An [AnimatorChangeHandler] that will slide either slide a new View up or slide an old View down,
 * depending on whether a push or pop change is happening.
 */
class VerticalChangeHandler : AnimatorChangeHandler {

    constructor() {}

    constructor(removesFromViewOnPush: Boolean) : super(removesFromViewOnPush) {}

    constructor(duration: Long) : super(duration) {}

    constructor(duration: Long, removesFromViewOnPush: Boolean) : super(
        duration,
        removesFromViewOnPush
    ) {
    }

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        val animator = AnimatorSet()
        val viewAnimators = mutableListOf<Animator>()

        if (isPush && to != null) {
            viewAnimators.add(
                ObjectAnimator.ofFloat<View>(
                    to,
                    View.TRANSLATION_Y,
                    to.height.toFloat(),
                    0f
                )
            )
        } else if (!isPush && from != null) {
            viewAnimators.add(
                ObjectAnimator.ofFloat<View>(
                    from,
                    View.TRANSLATION_Y,
                    from.height.toFloat()
                )
            )
        }

        animator.playTogether(viewAnimators)
        return animator
    }

    override fun resetFromView(from: View) {}

    override fun copy(): ControllerChangeHandler {
        return VerticalChangeHandler(animationDuration, removesFromViewOnPush())
    }

}
