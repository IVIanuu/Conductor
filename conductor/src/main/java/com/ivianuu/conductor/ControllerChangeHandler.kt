package com.ivianuu.conductor

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.changehandler.SimpleSwapChangeHandler
import com.ivianuu.conductor.internal.ClassUtils

/**
 * ControllerChangeHandlers are responsible for swapping the View for one Controller to the View
 * of another. They can be useful for performing animations and transitions between Controllers. Several
 * default ControllerChangeHandlers are included.
 */
abstract class ControllerChangeHandler {

    internal var forceRemoveViewOnPush= false
    private var hasBeenUsed= false

    /**
     * Returns whether or not this is a reusable ControllerChangeHandler. Defaults to false and should
     * ONLY be overridden if there are absolutely no side effects to using this handler more than once.
     * In the case that a handler is not reusable, it will be copied using the [.copy] method
     * prior to use.
     */
    open val isReusable: Boolean
        get() = false

    /**
     * Responsible for swapping Views from one Controller to another.
     */
    abstract fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeCompletedListener
    )

    init {
        ensureDefaultConstructor()
    }

    /**
     * Saves any data about this handler to a Bundle in case the application is killed.
     */
    open fun saveToBundle(bundle: Bundle) {}

    /**
     * Restores data that was saved in the [.saveToBundle] method.
     */
    open fun restoreFromBundle(bundle: Bundle) {}

    /**
     * Will be called on change handlers that push a controller if the controller being pushed is
     * popped before it has completed.
     */
    open fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {}

    /**
     * Will be called on change handlers that push a controller if the controller being pushed is
     * needs to be attached immediately, without any animations or transitions.
     */
    open fun completeImmediately() {}

    /**
     * Returns a copy of this ControllerChangeHandler. This method is internally used by the library, so
     * ensure it will return an exact copy of your handler if overriding. If not overriding, the handler
     * will be saved and restored from the Bundle format.
     */
    open fun copy(): ControllerChangeHandler {
        return fromBundle(toBundle())!!
    }

    internal fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(KEY_CLASS_NAME, javaClass.name)

        val savedState = Bundle()
        saveToBundle(savedState)
        bundle.putBundle(KEY_SAVED_STATE, savedState)

        return bundle
    }

    private fun ensureDefaultConstructor() {
        try {
            javaClass.getConstructor()
        } catch (e: Exception) {
            throw RuntimeException(javaClass.toString() + " does not have a default constructor.")
        }

    }

    open fun removesFromViewOnPush(): Boolean {
        return true
    }

    open fun setForceRemoveViewOnPush(force: Boolean) {
        forceRemoveViewOnPush = force
    }

    internal class ChangeTransaction(
        val to: Controller?,
        val from: Controller?,
        val isPush: Boolean,
        val container: ViewGroup?,
        val changeHandler: ControllerChangeHandler?,
        val listeners: List<ControllerChangeListener>
    )

    /**
     * A listener interface useful for allowing external classes to be notified of change events.
     */
    interface ControllerChangeListener {
        /**
         * Called when a [ControllerChangeHandler] has started changing [Controller]s
         */
        fun onChangeStarted(
            to: Controller?,
            from: Controller?,
            isPush: Boolean,
            container: ViewGroup,
            handler: ControllerChangeHandler
        )

        /**
         * Called when a [ControllerChangeHandler] has completed changing [Controller]s
         */
        fun onChangeCompleted(
            to: Controller?,
            from: Controller?,
            isPush: Boolean,
            container: ViewGroup,
            handler: ControllerChangeHandler
        )
    }

    /**
     * A simplified listener for being notified when the change is complete. This MUST be called by any custom
     * ControllerChangeHandlers in order to ensure that [Controller]s will be notified of this change.
     */
    interface ControllerChangeCompletedListener {
        /**
         * Called when the change is complete.
         */
        fun onChangeCompleted()
    }

    private data class ChangeHandlerData(
        val changeHandler: ControllerChangeHandler,
        val isPush: Boolean
    )

    companion object {
        private const val KEY_CLASS_NAME = "ControllerChangeHandler.className"
        private const val KEY_SAVED_STATE = "ControllerChangeHandler.savedState"

        private val inProgressChangeHandlers = mutableMapOf<String, ChangeHandlerData>()

        internal fun fromBundle(bundle: Bundle?): ControllerChangeHandler? {
            return if (bundle != null) {
                val className = bundle.getString(KEY_CLASS_NAME) ?: throw IllegalStateException()
                val changeHandler = ClassUtils.newInstance<ControllerChangeHandler>(className) ?: throw IllegalStateException()

                changeHandler.restoreFromBundle(bundle.getBundle(KEY_SAVED_STATE))
                changeHandler
            } else {
                null
            }
        }

        internal fun completeHandlerImmediately(controllerInstanceId: String): Boolean {
            val changeHandlerData = inProgressChangeHandlers[controllerInstanceId]
            if (changeHandlerData != null) {
                changeHandlerData.changeHandler.completeImmediately()
                inProgressChangeHandlers.remove(controllerInstanceId)
                return true
            }
            return false
        }

        private fun abortOrComplete(
            toAbort: Controller,
            newController: Controller?,
            newChangeHandler: ControllerChangeHandler
        ) {
            val changeHandlerData = inProgressChangeHandlers[toAbort.instanceId]
            if (changeHandlerData != null) {
                if (changeHandlerData.isPush) {
                    changeHandlerData.changeHandler.onAbortPush(newChangeHandler, newController)
                } else {
                    changeHandlerData.changeHandler.completeImmediately()
                }

                inProgressChangeHandlers.remove(toAbort.instanceId)
            }
        }

        internal fun executeChange(transaction: ChangeTransaction) {
            executeChange(
                transaction.to,
                transaction.from,
                transaction.isPush,
                transaction.container,
                transaction.changeHandler,
                transaction.listeners
            )
        }

        private fun executeChange(
            to: Controller?,
            from: Controller?,
            isPush: Boolean,
            container: ViewGroup?,
            inHandler: ControllerChangeHandler?,
            listeners: List<ControllerChangeListener>
        ) {
            if (container != null) {
                val handler = if (inHandler == null) {
                    SimpleSwapChangeHandler()
                } else if (inHandler.hasBeenUsed && !inHandler.isReusable) {
                    inHandler.copy()
                } else {
                    inHandler
                }
                handler.hasBeenUsed = true

                if (from != null) {
                    if (isPush) {
                        completeHandlerImmediately(from.instanceId)
                    } else {
                        abortOrComplete(from, to, handler)
                    }
                }

                if (to != null) {
                    inProgressChangeHandlers[to.instanceId] =
                            ChangeHandlerData(handler, isPush)
                }

                for (listener in listeners) {
                    listener.onChangeStarted(to, from, isPush, container, handler)
                }

                val toChangeType =
                    if (isPush) ControllerChangeType.PUSH_ENTER else ControllerChangeType.POP_ENTER
                val fromChangeType =
                    if (isPush) ControllerChangeType.PUSH_EXIT else ControllerChangeType.POP_EXIT

                val toView: View?
                if (to != null) {
                    toView = to.inflate(container)
                    to.changeStarted(handler, toChangeType)
                } else {
                    toView = null
                }

                val fromView: View?
                if (from != null) {
                    fromView = from.view
                    from.changeStarted(handler, fromChangeType)
                } else {
                    fromView = null
                }

                handler.performChange(
                    container,
                    fromView,
                    toView,
                    isPush,
                    object : ControllerChangeCompletedListener {
                        override fun onChangeCompleted() {
                            from?.changeEnded(handler, fromChangeType)

                            if (to != null) {
                                inProgressChangeHandlers.remove(to.instanceId)
                                to.changeEnded(handler, toChangeType)
                            }

                            for (listener in listeners) {
                                listener.onChangeCompleted(to, from, isPush, container, handler)
                            }

                            if (handler.forceRemoveViewOnPush && fromView != null) {
                                val fromParent = fromView.parent
                                if (fromParent != null && fromParent is ViewGroup) {
                                    fromParent.removeView(fromView)
                                }
                            }

                            if (handler.removesFromViewOnPush() && from != null) {
                                from.needsAttach = false
                            }
                        }
                    })
            }
        }
    }

}
