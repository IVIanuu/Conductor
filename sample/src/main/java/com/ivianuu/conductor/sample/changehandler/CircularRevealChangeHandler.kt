package com.ivianuu.conductor.sample.changehandler

import android.animation.Animator
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup

import com.ivianuu.conductor.changehandler.AnimatorChangeHandler

/**
 * An [AnimatorChangeHandler] that will perform a circular reveal
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class CircularRevealChangeHandler : AnimatorChangeHandler {

    private var cx = 0
    private var cy = 0

    constructor()

    /**
     * Constructor that will create a circular reveal from the center of the fromView parameter.
     */
    constructor(
        fromView: View,
        containerView: View,
        removesFromViewOnPush: Boolean
    ) : this(fromView, containerView, AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION, removesFromViewOnPush)

    /**
     * Constructor that will create a circular reveal from the center of the fromView parameter.
     */
    @JvmOverloads constructor(
        fromView: View,
        containerView: View,
        duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
        removesFromViewOnPush: Boolean = true
    ) : super(duration, removesFromViewOnPush) {

        val fromLocation = IntArray(2)
        fromView.getLocationInWindow(fromLocation)

        val containerLocation = IntArray(2)
        containerView.getLocationInWindow(containerLocation)

        val relativeLeft = fromLocation[0] - containerLocation[0]
        val relativeTop = fromLocation[1] - containerLocation[1]

        cx = fromView.width / 2 + relativeLeft
        cy = fromView.height / 2 + relativeTop
    }

    /**
     * Constructor that will create a circular reveal from the center point passed in.
     */
    constructor(cx: Int, cy: Int, removesFromViewOnPush: Boolean) : this(
        cx,
        cy,
        AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
        removesFromViewOnPush
    ) {

    }

    /**
     * Constructor that will create a circular reveal from the center point passed in.
     */
    @JvmOverloads constructor(
        cx: Int,
        cy: Int,
        duration: Long = AnimatorChangeHandler.DEFAULT_ANIMATION_DURATION,
        removesFromViewOnPush: Boolean = true
    ) : super(duration, removesFromViewOnPush) {
        this.cx = cx
        this.cy = cy
    }

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ): Animator {
        val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        var animator: Animator? = null
        if (isPush && to != null) {
            animator = ViewAnimationUtils.createCircularReveal(to, cx, cy, 0f, radius)
        } else if (!isPush && from != null) {
            animator = ViewAnimationUtils.createCircularReveal(from, cx, cy, radius, 0f)
        }
        return animator!!
    }

    override fun resetFromView(from: View) {}

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        bundle.putInt(KEY_CX, cx)
        bundle.putInt(KEY_CY, cy)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        cx = bundle.getInt(KEY_CX)
        cy = bundle.getInt(KEY_CY)
    }

    companion object {
        private const val KEY_CX = "CircularRevealChangeHandler.cx"
        private const val KEY_CY = "CircularRevealChangeHandler.cy"
    }
}