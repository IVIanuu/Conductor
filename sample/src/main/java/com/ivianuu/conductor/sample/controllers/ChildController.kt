package com.ivianuu.conductor.sample.controllers

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import androidx.core.os.bundleOf
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController

import kotlinx.android.synthetic.main.controller_child.*

class ChildController(args: Bundle) : BaseController(args) {

    override val layoutRes: Int
        get() = R.layout.controller_child

    constructor(title: String, backgroundColor: Int, colorIsResId: Boolean) : this(
        bundleOf(
            KEY_TITLE to title,
            KEY_BG_COLOR to backgroundColor,
            KEY_COLOR_IS_RES to colorIsResId
        )
    )

    override fun onViewCreated(view: View) {
        tv_title.text = args.getString(KEY_TITLE)

        var bgColor = args.getInt(KEY_BG_COLOR)
        if (args.getBoolean(KEY_COLOR_IS_RES)) {
            bgColor = ContextCompat.getColor(activity!!, bgColor)
        }
        view.setBackgroundColor(bgColor)
    }

    companion object {
        private const val KEY_TITLE = "ChildController.title"
        private const val KEY_BG_COLOR = "ChildController.bgColor"
        private const val KEY_COLOR_IS_RES = "ChildController.colorIsResId"
    }
}
