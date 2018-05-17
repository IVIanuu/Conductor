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

package android.support.v7.preference

import android.os.Bundle
import android.support.annotation.RestrictTo
import android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP
import android.view.View
import android.widget.EditText

class EditTextPreferenceDialogController : PreferenceDialogController() {

    private var mEditText: EditText? = null

    private var mText: CharSequence? = null

    private val editTextPreference: EditTextPreference
        get() = preference as EditTextPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mText = editTextPreference.text
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TEXT, mText)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        mEditText = view.findViewById<View>(android.R.id.edit) as EditText

        if (mEditText == null) {
            throw IllegalStateException("Dialog view must contain an EditText with id" + " @android:id/edit")
        }

        mEditText!!.setText(mText)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        mEditText = null
    }

    /** @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    override fun needInputMethod(): Boolean {
        // We want the input method to show, if possible, when dialog is displayed
        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val value = mEditText!!.text.toString()
            if (editTextPreference.callChangeListener(value)) {
                editTextPreference.text = value
            }
        }
    }

    companion object {

        private val SAVE_STATE_TEXT = "EditTextPreferenceDialogController.text"

        fun newInstance(key: String): EditTextPreferenceDialogController {
            val controller = EditTextPreferenceDialogController()
            controller.args.putString(PreferenceDialogController.ARG_KEY, key)
            return controller
        }
    }

}
