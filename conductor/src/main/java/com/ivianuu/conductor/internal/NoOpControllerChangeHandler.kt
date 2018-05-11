package com.ivianuu.conductor.internal

import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.ControllerChangeHandler

internal class NoOpControllerChangeHandler : ControllerChangeHandler() {

    override val isReusable: Boolean
        get() = true

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) {
        changeListener.onChangeCompleted()
    }

    override fun copy() = NoOpControllerChangeHandler()

}
