package com.ivianuu.conductor.sample.controllers


import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.os.bundleOf
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import kotlinx.android.synthetic.main.controller_dialog.*

class DialogController(args: Bundle) : BaseController(args) {

    override val layoutRes = R.layout.controller_dialog

    constructor(title: CharSequence, description: CharSequence) : this(
        bundleOf(
            KEY_TITLE to title,
            KEY_DESCRIPTION to description
        )
    )
    override fun onViewCreated(view: View) {
        tv_title!!.text = args.getCharSequence(KEY_TITLE)
        tv_description!!.text = args.getCharSequence(KEY_DESCRIPTION)
        tv_description!!.movementMethod = LinkMovementMethod.getInstance()

        dismiss.setOnClickListener {  requireRouter().popController(this) }

        dialog_window.setOnClickListener { requireRouter().popController(this) }
    }

    companion object {
        private const val KEY_TITLE = "DialogController.title"
        private const val KEY_DESCRIPTION = "DialogController.description"
    }
}
