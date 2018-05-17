package com.ivianuu.conductor.sample.controllers

import android.view.View
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.NavigationDemoController.DisplayUpMode
import com.ivianuu.conductor.sample.controllers.base.BaseController
import kotlinx.android.synthetic.main.controller_multiple_child_routers.*

class MultipleChildRouterController : BaseController() {

    override val title: String?
        get() = "Child Router Demo"

    override val layoutRes: Int
        get() = R.layout.controller_multiple_child_routers

    override fun onViewCreated(view: View) {
        for (childContainer in listOf(container_0, container_1, container_2)) {
            val childRouter = getChildRouter(childContainer).apply {
                popsLastView = false
            }
            if (!childRouter.hasRootController()) {
                childRouter.setRoot(
                    RouterTransaction.with(
                        NavigationDemoController(
                            0,
                            DisplayUpMode.HIDE
                        )
                    )
                )
            }
        }
    }

}
