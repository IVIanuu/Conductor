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

package com.ivianuu.conductor.sample.util

import android.os.Bundle
import android.view.View
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.sample.d

/**
 * Logs the lifecycle of controllers
 */
class LoggingLifecycleListener : Controller.LifecycleListener() {

    override fun postContextAvailable(controller: Controller) {
        super.postContextAvailable(controller)
        controller.d { "on context available" }
    }

    override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        super.onRestoreInstanceState(controller, savedInstanceState)
        controller.d { "on restore instance state" }
    }

    override fun postCreateView(controller: Controller, view: View) {
        super.postCreateView(controller, view)
        controller.d { "on create view" }
    }

    override fun onRestoreViewState(controller: Controller, savedViewState: Bundle) {
        super.onRestoreViewState(controller, savedViewState)
        controller.d { "restore view state" }
    }

    override fun postAttach(controller: Controller, view: View) {
        super.postAttach(controller, view)
        controller.d { "on attach" }
    }

    override fun postDetach(controller: Controller, view: View) {
        super.postDetach(controller, view)
        controller.d { "on detach" }
    }

    override fun postDestroyView(controller: Controller) {
        super.postDestroyView(controller)
        controller.d { "on destroy view" }
    }

    override fun onSaveViewState(controller: Controller, outState: Bundle) {
        super.onSaveViewState(controller, outState)
        controller.d { "on save view state" }
    }

    override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        super.onSaveInstanceState(controller, outState)
        controller.d { "on save instance state" }
    }

    override fun postContextUnavailable(controller: Controller) {
        super.postContextUnavailable(controller)
        controller.d { "context unavailable" }
    }

    override fun postDestroy(controller: Controller) {
        super.postDestroy(controller)
        controller.d { "on destroy" }
    }
}