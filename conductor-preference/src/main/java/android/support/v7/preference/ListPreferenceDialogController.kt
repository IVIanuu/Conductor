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

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import java.util.*

class ListPreferenceDialogController : PreferenceDialogController() {

    private var mClickedDialogEntryIndex: Int = 0
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference

            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array."
                )
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
        putCharSequenceArray(outState, SAVE_STATE_ENTRIES, mEntries!!)
        putCharSequenceArray(outState, SAVE_STATE_ENTRY_VALUES, mEntryValues!!)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
        mEntries = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRIES)
        mEntryValues = getCharSequenceArray(savedInstanceState, SAVE_STATE_ENTRY_VALUES)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)

        builder.setSingleChoiceItems(
            mEntries, mClickedDialogEntryIndex
        ) { dialog, which ->
            mClickedDialogEntryIndex = which

            /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
            this@ListPreferenceDialogController.onClick(
                dialog,
                DialogInterface.BUTTON_POSITIVE
            )
            dialog.dismiss()
        }

        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = listPreference
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = mEntryValues!![mClickedDialogEntryIndex].toString()
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    companion object {

        private val SAVE_STATE_INDEX = "ListPreferenceDialogController.index"
        private val SAVE_STATE_ENTRIES = "ListPreferenceDialogController.entries"
        private val SAVE_STATE_ENTRY_VALUES = "ListPreferenceDialogController.entryValues"

        fun newInstance(key: String): ListPreferenceDialogController {
            val controller = ListPreferenceDialogController()
            controller.args.putString(PreferenceDialogController.ARG_KEY, key)
            return controller
        }

        private fun putCharSequenceArray(out: Bundle, key: String, entries: Array<CharSequence>) {
            val stored = ArrayList<String>(entries.size)

            for (cs in entries) {
                stored.add(cs.toString())
            }

            out.putStringArrayList(key, stored)
        }

        private fun getCharSequenceArray(`in`: Bundle, key: String): Array<CharSequence>? {
            val stored = `in`.getStringArrayList(key)

            return stored?.toTypedArray()
        }
    }

}
