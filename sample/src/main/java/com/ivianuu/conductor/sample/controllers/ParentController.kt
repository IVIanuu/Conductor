package com.ivianuu.conductor.sample.controllers

import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.ControllerChangeType
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.FadeChangeHandler
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.ColorUtil

class ParentController : BaseController() {
    private var finishing= false
    private var hasShownAll= false

    override val title: String?
        get() = "Parent/Child Demo"

    override val layoutRes: Int
        get() = R.layout.controller_parent

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)

        if (changeType == ControllerChangeType.PUSH_ENTER) {
            addChild(0)
        }
    }

    private fun addChild(index: Int) {
        val frameId =
            resources!!.getIdentifier("child_content_" + (index + 1), "id", activity!!.packageName)
        val container = view!!.findViewById<View>(frameId) as ViewGroup
        val childRouter = getChildRouter(container).apply {
            popsLastView = true
        }

        if (!childRouter.hasRootController()) {
            val childController = ChildController(
                "Child Controller #$index",
                ColorUtil.getMaterialColor(resources!!, index),
                false
            )

            childController.addLifecycleListener(object : Controller.LifecycleListener() {
                override fun onChangeEnd(
                    controller: Controller,
                    changeHandler: ControllerChangeHandler,
                    changeType: ControllerChangeType
                ) {
                    if (!isBeingDestroyed) {
                        if (changeType == ControllerChangeType.PUSH_ENTER && !hasShownAll) {
                            if (index < NUMBER_OF_CHILDREN - 1) {
                                addChild(index + 1)
                            } else {
                                hasShownAll = true
                            }
                        } else if (changeType == ControllerChangeType.POP_EXIT) {
                            if (index > 0) {
                                removeChild(index - 1)
                            } else {
                                requireRouter().popController(this@ParentController)
                            }
                        }
                    }
                }
            })

            childRouter.setRoot(
                RouterTransaction.with(childController)
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
        }
    }

    private fun removeChild(index: Int) {
        val childRouters = getChildRouters()
        if (index < childRouters.size) {
            removeChildRouter(childRouters[index])
        }
    }

    override fun handleBack(): Boolean {
        var childControllers = 0
        for (childRouter in getChildRouters()) {
            if (childRouter.hasRootController()) {
                childControllers++
            }
        }

        return if (childControllers != NUMBER_OF_CHILDREN || finishing) {
            true
        } else {
            finishing = true
            super.handleBack()
        }
    }

    companion object {
        private const val NUMBER_OF_CHILDREN = 5
    }

}
