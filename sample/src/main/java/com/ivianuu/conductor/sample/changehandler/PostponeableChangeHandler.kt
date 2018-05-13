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

package com.ivianuu.conductor.sample.changehandler

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.internal.ClassUtils

/**
 * A [ControllerChangeHandler] which is postponable
 */
class PostponeableChangeHandler : ControllerChangeHandler {

    override val isReusable: Boolean
        get() = false

    override var removesFromViewOnPush: Boolean
        get() = wrappedChangeHandler.removesFromViewOnPush
        set(value) { wrappedChangeHandler.removesFromViewOnPush = value }

    lateinit var wrappedChangeHandler: ControllerChangeHandler
        private set

    private var postponedNum = 0
    private var postponedChangeData: ChangeData? = null

    constructor()

    constructor(wrappedChangeHandler: ControllerChangeHandler) {
        this.wrappedChangeHandler = wrappedChangeHandler
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeCompletedListener
    ) {
        if (postponedNum == 0) {
            startPostponedChangeInternal(ChangeData(
                container, from, to, isPush, changeListener))
        } else {
            postponedChangeData = ChangeData(container, from,
                to, isPush, changeListener)
        }
    }

    private data class ChangeData(
        val container: ViewGroup,
        val from: View?,
        val to: View?,
        val isPush: Boolean,
        val changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    )

    override fun saveToBundle(bundle: Bundle) {
        super.saveToBundle(bundle)
        val savedState = Bundle()
        bundle.putString(KEY_CLASS_NAME, javaClass.name)
        wrappedChangeHandler.saveToBundle(savedState)
        bundle.putBundle(KEY_SAVED_STATE, savedState)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        val className = bundle.getString(KEY_CLASS_NAME) ?: throw IllegalStateException()
        wrappedChangeHandler = ClassUtils
            .newInstance<ControllerChangeHandler>(className) ?: throw IllegalStateException()
        wrappedChangeHandler.restoreFromBundle(bundle.getBundle(KEY_SAVED_STATE))
    }

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)
        wrappedChangeHandler.onAbortPush(newHandler, newTop)
    }

    override fun completeImmediately() {
        super.completeImmediately()
        wrappedChangeHandler.completeImmediately()
    }

    fun postponeChange() {
        postponedNum++
    }

    fun startPostponedChange() {
        postponedNum--
        val postponedChangeData = postponedChangeData
        if (postponedNum == 0 && postponedChangeData != null) {
            startPostponedChangeInternal(postponedChangeData)
            this.postponedChangeData = null
        }
    }

    private fun startPostponedChangeInternal(changeData: ChangeData) {
        wrappedChangeHandler.performChange(changeData.container,
            changeData.from, changeData.to, changeData.isPush,
            object : ControllerChangeHandler.ControllerChangeCompletedListener {
                override fun onChangeCompleted() {
                    changeData.changeListener.onChangeCompleted()
                }
            })
    }

    private companion object {
        private const val KEY_CLASS_NAME = "PostponableChangeHandler.className"
        private const val KEY_SAVED_STATE = "PostponableChangeHandler.savedState"
    }
}