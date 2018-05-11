package com.ivianuu.conductor.sample.controllers

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES
import android.view.View
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.changehandler.ScaleFadeChangeHandler
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.widget.ElasticDragDismissFrameLayout
import com.ivianuu.conductor.sample.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback

@TargetApi(VERSION_CODES.LOLLIPOP)
class DragDismissController : BaseController() {

    private val dragDismissListener = object : ElasticDragDismissCallback() {
        override fun onDragDismissed() {
            overridePopHandler(ScaleFadeChangeHandler())
            requireRouter().popController(this@DragDismissController)
        }
    }

    override val layoutRes = R.layout.controller_drag_dismiss

    override val title: String?
        get() = "Drag to Dismiss"

    override fun onViewCreated(view: View) {
        (view as ElasticDragDismissFrameLayout).addListener(dragDismissListener)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)

        (view as ElasticDragDismissFrameLayout).removeListener(dragDismissListener)
    }
}
