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

package com.ivianuu.conductor.autodispose

import android.view.View
import com.ivianuu.autodispose.LifecycleScopeProvider
import com.ivianuu.autodispose.OutsideLifecycleException
import com.ivianuu.autodispose.autoDispose
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.autodispose.ControllerEvent.*
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.subjects.BehaviorSubject

/**
 * Controller lifecycle events
 */
enum class ControllerEvent {
    CREATE,
    CREATE_VIEW,
    ATTACH,
    DETACH,
    DESTROY_VIEW,
    DESTROY
}

/**
 * Scope provider for controllers
 */
class ControllerScopeProvider private constructor(controller: Controller) :
    LifecycleScopeProvider<ControllerEvent> {

    private val lifecycleSubject = BehaviorSubject.create<ControllerEvent>()

    init {
        val initialState = if (controller.isBeingDestroyed || controller.isDestroyed) {
            throw OutsideLifecycleException("Cannot bind to Controller lifecycle when outside of it.")
        } else if (controller.isAttached) {
            ATTACH
        } else if (controller.view != null) {
            CREATE_VIEW
        } else if (controller.activity != null) {
            CREATE
        } else {
            null
        }

        if (initialState != null) {
            lifecycleSubject.onNext(initialState)
        }

        controller.addLifecycleListener(object : Controller.LifecycleListener() {
            override fun preCreate(controller: Controller) {
                lifecycleSubject.onNext(CREATE)
            }

            override fun preCreateView(controller: Controller) {
                lifecycleSubject.onNext(CREATE_VIEW)
            }

            override fun preAttach(controller: Controller, view: View) {
                lifecycleSubject.onNext(ATTACH)
            }

            override fun preDetach(controller: Controller, view: View) {
                lifecycleSubject.onNext(DETACH)
            }

            override fun preDestroyView(controller: Controller, view: View) {
                lifecycleSubject.onNext(DESTROY_VIEW)
            }

            override fun preDestroy(controller: Controller) {
                lifecycleSubject.onNext(DESTROY)
            }
        })
    }

    override fun lifecycle() = lifecycleSubject

    override fun correspondingEvents() = CORRESPONDING_EVENTS

    override fun peekLifecycle(): ControllerEvent? {
        return lifecycleSubject.value
    }

    companion object {
        private val CORRESPONDING_EVENTS =
            Function<ControllerEvent, ControllerEvent> { lastEvent ->
                when (lastEvent) {
                    CREATE -> DESTROY
                    CREATE_VIEW -> DESTROY_VIEW
                    ATTACH -> DETACH
                    DETACH -> DESTROY
                    else -> throw OutsideLifecycleException("Cannot bind to Controller lifecycle when outside of it.")
                }
            }

        fun from(controller: Controller): ControllerScopeProvider {
            return ControllerScopeProvider(controller)
        }
    }
}


fun Disposable.autoDispose(controller: Controller) {
    autoDispose(ControllerScopeProvider.from(controller))
}

fun Disposable.autoDispose(controller: Controller, event: ControllerEvent) {
    autoDispose(ControllerScopeProvider.from(controller), event)
}