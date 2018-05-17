/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.conductor.support

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.Router
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.SimpleSwapChangeHandler


/**
 * A controller that displays a dialog window, floating on top of its activity's window.
 * This is a wrapper over [Dialog] object like [android.app.DialogFragment].
 */
abstract class DialogController : Controller {

    protected var dialog: Dialog? = null
        private set
    private var dismissed: Boolean = false

    /**
     * Convenience constructor for use when no arguments are needed.
     */
    protected constructor() : super(null)

    /**
     * Constructor that takes arguments that need to be retained across restarts.
     */
    protected constructor(args: Bundle) : super(args)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        dialog = onCreateDialog(savedViewState).apply {
            ownerActivity = requireActivity()
            setOnDismissListener { dismissDialog() }
            if (savedViewState != null) {
                val dialogState = savedViewState.getBundle(SAVED_DIALOG_STATE_TAG)
                if (dialogState != null) {
                    onRestoreInstanceState(dialogState)
                }
            }
        }

        return View(requireActivity())
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        val dialogState = dialog!!.onSaveInstanceState()
        outState.putBundle(SAVED_DIALOG_STATE_TAG, dialogState)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        dialog?.show()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        dialog?.hide()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
    }

    /**
     * Display the dialog, create a transaction and pushing the controller.
     */
    fun showDialog(router: Router, tag: String? = null) {
        dismissed = false
        router.pushController(
            RouterTransaction.with(this)
                .pushChangeHandler(SimpleSwapChangeHandler(false))
                .popChangeHandler(SimpleSwapChangeHandler(false))
                .tag(tag)
        )
    }

    /**
     * Dismiss the dialog and pop this controller
     */
    open fun dismissDialog() {
        if (dismissed) {
            return
        }
        requireRouter().popController(this)
        dismissed = true
    }

    /**
     * Build your own custom Dialog container such as an [android.app.AlertDialog]
     */
    protected abstract fun onCreateDialog(savedViewState: Bundle?): Dialog

    companion object {
        private const val SAVED_DIALOG_STATE_TAG = "android:savedDialogState"
    }
}