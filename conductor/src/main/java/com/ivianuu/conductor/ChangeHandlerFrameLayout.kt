package com.ivianuu.conductor

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ivianuu.conductor.ControllerChangeHandler.ControllerChangeListener

/**
 * A FrameLayout implementation that can be used to block user interactions while
 * [ControllerChangeHandler]s are performing changes. It is not required to use this
 * ViewGroup, but it can be helpful.
 */
class ChangeHandlerFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ControllerChangeListener {

    private var inProgressTransactionCount = 0

    override fun onInterceptTouchEvent(ev: MotionEvent) =
        inProgressTransactionCount > 0 || super.onInterceptTouchEvent(ev)

    override fun onChangeStarted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        inProgressTransactionCount++
    }

    override fun onChangeCompleted(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        inProgressTransactionCount--
    }

}
