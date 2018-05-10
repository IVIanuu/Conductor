package com.ivianuu.conductor.sample.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.Router
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.NavigationDemoController.DisplayUpMode
import com.ivianuu.conductor.sample.controllers.base.BaseController

class MultipleChildRouterController : BaseController() {

    private val childContainers = mutableListOf<ViewGroup>()

    override val title: String?
        get() = "Child Router Demo"

    override val layoutRes: Int
        get() = R.layout.controller_multiple_child_routers

    override fun onViewCreated(view: View) {
        childContainers.add(view.findViewById(R.id.container_0))
        childContainers.add(view.findViewById(R.id.container_1))
        childContainers.add(view.findViewById(R.id.container_2))

        for (childContainer in childContainers) {
            val childRouter = getChildRouter(childContainer).setPopsLastView(false)
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
