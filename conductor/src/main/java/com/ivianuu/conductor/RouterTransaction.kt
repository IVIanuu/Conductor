package com.ivianuu.conductor

import android.os.Bundle

import com.ivianuu.conductor.internal.TransactionIndexer

/**
 * Metadata used for adding [Controller]s to a [Router].
 */
class RouterTransaction {

    val controller: Controller

    var tag: String?
        set(value) {
            if (!attachedToRouter) {
                field = value
            } else {
                throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
            }
        }

    var pushChangeHandler: ControllerChangeHandler?
        set(value) {
            if (!attachedToRouter) {
                field = value
            } else {
                throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
            }
        }
        get() {
            var handler = controller.overriddenPushHandler
            if (handler == null) {
                handler = field
            }
            return handler
        }

    var popChangeHandler: ControllerChangeHandler?
        set(value) {
            if (!attachedToRouter) {
                field = value
            } else {
                throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
            }
        }
        get() {
            var handler = controller.overriddenPopHandler
            if (handler == null) {
                handler = field
            }
            return handler
        }

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

    fun tag(tag: String?): RouterTransaction {
        this.tag = tag
        return this
    }

    fun pushChangeHandler(handler: ControllerChangeHandler?): RouterTransaction {
        pushChangeHandler = handler
        return this
    }

    fun popChangeHandler(handler: ControllerChangeHandler?): RouterTransaction {
        popChangeHandler = handler
        return this
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
