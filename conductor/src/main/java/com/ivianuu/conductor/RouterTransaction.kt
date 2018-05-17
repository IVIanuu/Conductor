package com.ivianuu.conductor

import android.os.Bundle

import com.ivianuu.conductor.internal.TransactionIndexer

/**
 * Metadata used for adding [Controller]s to a [Router].
 */
class RouterTransaction {

    private val controller: Controller
    private var tag: String? = null
    private var pushChangeHandler: ControllerChangeHandler?
    private var popChangeHandler: ControllerChangeHandler?

    private var attachedToRouter = false
    internal var transactionIndex = INVALID_INDEX

    private constructor(controller: Controller) {
        this.controller = controller
        this.tag = null
        this.pushChangeHandler = null
        this.popChangeHandler = null
    }

    internal constructor(bundle: Bundle) {
        controller = Controller.newInstance(bundle.getBundle(KEY_VIEW_CONTROLLER_BUNDLE))
        tag = bundle.getString(KEY_TAG)
        pushChangeHandler =
                ControllerChangeHandler.fromBundle(bundle.getBundle(KEY_PUSH_TRANSITION))
        popChangeHandler =
                ControllerChangeHandler.fromBundle(bundle.getBundle(KEY_POP_TRANSITION))
        transactionIndex = bundle.getInt(KEY_INDEX)
        attachedToRouter = bundle.getBoolean(KEY_ATTACHED_TO_ROUTER)
    }

    internal fun onAttachedToRouter() {
        attachedToRouter = true
    }

    fun controller() = controller

    fun tag(tag: String?): RouterTransaction {
        if (!attachedToRouter) {
            this.tag = tag
        } else {
            throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
        }
        return this
    }

    fun tag() = tag

    fun pushChangeHandler(handler: ControllerChangeHandler?): RouterTransaction {
        if (!attachedToRouter) {
            pushChangeHandler = handler
        } else {
            throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
        }
        return this
    }

    fun pushChangeHandler(): ControllerChangeHandler? {
        var handler = controller.overriddenPushHandler
        if (handler == null) {
            handler = pushChangeHandler
        }
        return handler
    }

    fun popChangeHandler(handler: ControllerChangeHandler?): RouterTransaction {
        if (!attachedToRouter) {
            popChangeHandler = handler
        } else {
            throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
        }
        return this
    }

    fun popChangeHandler(): ControllerChangeHandler? {
        var handler = controller.overriddenPopHandler
        if (handler == null) {
            handler = popChangeHandler
        }
        return handler
    }

    internal fun ensureValidIndex(indexer: TransactionIndexer?) {
        if (indexer == null) throw IllegalStateException("indexer is null")
        if (transactionIndex == INVALID_INDEX) {
            transactionIndex = indexer.nextIndex()
        }
    }

    internal fun saveInstanceState(): Bundle {
        val bundle = Bundle()

        bundle.putBundle(KEY_VIEW_CONTROLLER_BUNDLE, controller.saveInstanceState())

        pushChangeHandler?.let { bundle.putBundle(KEY_PUSH_TRANSITION, it.toBundle()) }
        popChangeHandler?.let { bundle.putBundle(KEY_POP_TRANSITION, it.toBundle()) }

        bundle.putString(KEY_TAG, tag)
        bundle.putInt(KEY_INDEX, transactionIndex)
        bundle.putBoolean(KEY_ATTACHED_TO_ROUTER, attachedToRouter)

        return bundle
    }

    companion object {
        private const val INVALID_INDEX = -1

        private const val KEY_VIEW_CONTROLLER_BUNDLE = "RouterTransaction.controller.bundle"
        private const val KEY_PUSH_TRANSITION = "RouterTransaction.pushControllerChangeHandler"
        private const val KEY_POP_TRANSITION = "RouterTransaction.popControllerChangeHandler"
        private const val KEY_TAG = "RouterTransaction.tag"
        private const val KEY_INDEX = "RouterTransaction.transactionIndex"
        private const val KEY_ATTACHED_TO_ROUTER = "RouterTransaction.attachedToRouter"

        @JvmStatic
        fun with(controller: Controller): RouterTransaction {
            return RouterTransaction(controller)
        }
    }

}
