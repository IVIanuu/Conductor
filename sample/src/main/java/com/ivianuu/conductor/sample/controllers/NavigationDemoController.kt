package com.ivianuu.conductor.sample.controllers

import android.os.Bundle
import android.view.View
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.ControllerChangeType
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.HorizontalChangeHandler
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.BundleBuilder
import com.ivianuu.conductor.sample.util.ColorUtil
import kotlinx.android.synthetic.main.controller_navigation_demo.*

class NavigationDemoController(args: Bundle) : BaseController(args) {

    private val index: Int
    private val displayUpMode: DisplayUpMode

    override val title: String?
        get() = "Navigation Demos"

    override val layoutRes = R.layout.controller_navigation_demo

    enum class DisplayUpMode {
        SHOW,
        SHOW_FOR_CHILDREN_ONLY,
        HIDE;

        val displayUpModeForChild: DisplayUpMode
            get() {
                when (this) {
                    HIDE -> return HIDE
                    else -> return SHOW
                }
            }
    }

    constructor(index: Int, displayUpMode: DisplayUpMode) : this(
        BundleBuilder(Bundle())
            .putInt(KEY_INDEX, index)
            .putInt(KEY_DISPLAY_UP_MODE, displayUpMode.ordinal)
            .build()
    ) {
    }

    init {
        index = args.getInt(KEY_INDEX)
        displayUpMode = DisplayUpMode.values()[args.getInt(KEY_DISPLAY_UP_MODE)]
    }

    override fun onViewCreated(view: View) {
        if (displayUpMode != DisplayUpMode.SHOW) {
            view.findViewById<View>(R.id.btn_up).visibility = View.GONE
        }

        view.setBackgroundColor(ColorUtil.getMaterialColor(resources!!, index))
        tv_title!!.text = resources!!.getString(R.string.navigation_title, index)

        btn_next.setOnClickListener {
            router!!.pushController(
                RouterTransaction.with(
                    NavigationDemoController(
                        index + 1,
                        displayUpMode.displayUpModeForChild
                    )
                )
                    .pushChangeHandler(HorizontalChangeHandler())
                    .popChangeHandler(HorizontalChangeHandler())
            )
        }

        btn_up.setOnClickListener {
            router!!.popToTag(TAG_UP_TRANSACTION)

        }

        btn_pop_to_root.setOnClickListener {
            router!!.popToRoot()
        }
    }

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)

        setButtonsEnabled(true)
    }

    override fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeStarted(changeHandler, changeType)

        setButtonsEnabled(false)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val view = view
        if (view != null) {
            view.findViewById<View>(R.id.btn_next).isEnabled = enabled
            view.findViewById<View>(R.id.btn_up).isEnabled = enabled
            view.findViewById<View>(R.id.btn_pop_to_root).isEnabled = enabled
        }
    }

    companion object {

        const val TAG_UP_TRANSACTION = "NavigationDemoController.up"

        private const val KEY_INDEX = "NavigationDemoController.index"
        private const val KEY_DISPLAY_UP_MODE = "NavigationDemoController.displayUpMode"
    }
}
