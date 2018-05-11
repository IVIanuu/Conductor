package com.ivianuu.conductor.sample.controllers


import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.BundleBuilder
import kotlinx.android.synthetic.main.controller_dialog.*

class DialogController(args: Bundle) : BaseController(args) {

    override val layoutRes = R.layout.controller_dialog

    constructor(title: CharSequence, description: CharSequence) : this(
        BundleBuilder(Bundle())
            .putCharSequence(KEY_TITLE, title)
            .putCharSequence(KEY_DESCRIPTION, description)
            .build()
    )
    override fun onViewCreated(view: View) {
        tv_title!!.text = args.getCharSequence(KEY_TITLE)
        tv_description!!.text = args.getCharSequence(KEY_DESCRIPTION)
        tv_description!!.movementMethod = LinkMovementMethod.getInstance()

        dismiss.setOnClickListener {  router!!.popController(this) }

        dialog_window.setOnClickListener { router!!.popController(this) }
    }

    companion object {

        private const val KEY_TITLE = "DialogController.title"
        private const val KEY_DESCRIPTION = "DialogController.description"
    }
}
