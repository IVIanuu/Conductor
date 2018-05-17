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

package com.ivianuu.conductor.sample.controllers

import android.view.View
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.VerticalChangeHandler
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.ColorUtil
import com.ivianuu.conductor.sample.util.LoggingLifecycleListener
import kotlinx.android.synthetic.main.controller_test.*
import kotlinx.android.synthetic.main.controller_test_child.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class TestController : BaseController() {

    override val layoutRes = R.layout.controller_test

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        val childRouter = getChildRouter(child_container)
        if (!childRouter.hasRootController()) {
            childRouter.setRoot(RouterTransaction.with(TestChildController()))
        }

        up.setOnClickListener {
            requireRouter()
                .pushController(
                    RouterTransaction.with(MultipleChildRouterController())
                        .pushChangeHandler(VerticalChangeHandler())
                        .popChangeHandler(VerticalChangeHandler())
                )
        }
    }

}

class TestChildController : BaseController() {
    override val layoutRes = R.layout.controller_test_child

    init {
        retainViewMode = RetainViewMode.RETAIN_DETACH
        addLifecycleListener(LoggingLifecycleListener())
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        bg_view.setBackgroundColor(
            ColorUtil.getMaterialColor(resources!!, listOf(1, 2, 3, 4).shuffled().first())
        )
    }
}