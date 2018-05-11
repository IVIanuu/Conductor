package com.ivianuu.conductor.sample.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.transition.*
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.changehandler.SharedElementTransitionChangeHandler
import java.util.*

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CityGridSharedElementTransitionChangeHandler : SharedElementTransitionChangeHandler {

    private val names: ArrayList<String>

    constructor() {
        names = ArrayList()
    }

    constructor(waitForTransitionNames: List<String>) {
        names = ArrayList(waitForTransitionNames)
    }

    override fun saveToBundle(bundle: Bundle) {
        bundle.putStringArrayList(KEY_WAIT_FOR_TRANSITION_NAMES, names)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        val savedNames = bundle.getStringArrayList(KEY_WAIT_FOR_TRANSITION_NAMES)
        if (savedNames != null) {
            names.addAll(savedNames)
        }
    }

    override fun getExitTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? {
        return if (isPush) {
            Explode()
        } else {
            Slide(Gravity.BOTTOM)
        }
    }

    override fun getSharedElementTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? {
        return TransitionSet().addTransition(ChangeBounds()).addTransition(ChangeClipBounds())
            .addTransition(ChangeTransform())
    }

    override fun getEnterTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition? {
        return if (isPush) {
            Slide(Gravity.BOTTOM)
        } else {
            Explode()
        }
    }

    override fun configureSharedElements(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ) {
        for (name in names) {
            addSharedElement(name)
            waitOnSharedElementNamed(name)
        }
    }

    companion object {

        private val KEY_WAIT_FOR_TRANSITION_NAMES =
            "CityGridSharedElementTransitionChangeHandler.names"
    }

}
