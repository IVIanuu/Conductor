package com.ivianuu.conductor

import android.os.Bundle

import com.ivianuu.conductor.internal.TransactionIndexer

/**
 * Metadata used for adding [Controller]s to a [Router].
 */
class RouterTransaction {

    internal val controller: Controller
    private var tag: String? = null

    private var pushControllerChangeHandler: ControllerChangeHandler? = null
    private var popControllerChangeHandler: ControllerChangeHandler? = null
    private var attachedToRouter = false
    internal var transactionIndex = INVALID_INDEX

    private constructor(controller: Controller) {
        this.controller = controller
    }

    internal constructor(bundle: Bundle) {
        controller = Controller.newInstance(bundle.getBundle(KEY_VIEW_CONTROLLER_BUNDLE))
        pushControllerChangeHandler =
                ControllerChangeHandler.fromBundle(bundle.getBundle(KEY_PUSH_TRANSITION))
        popControllerChangeHandler =
                ControllerChangeHandler.fromBundle(bundle.getBundle(KEY_POP_TRANSITION))
        tag = bundle.getString(KEY_TAG)
        transactionIndex = bundle.getInt(KEY_INDEX)
        attachedToRouter = bundle.getBoolean(KEY_ATTACHED_TO_ROUTER)
    }

    internal fun onAttachedToRouter() {
        attachedToRouter = true
    }

    fun controller(): Controller {
        return controller
    }

    fun tag(): String? {
        return tag
    }

    fun tag(tag: String?): RouterTransaction {
        if (!attachedToRouter) {
            this.tag = tag
            return this
        } else {
            throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
        }
    }

    fun pushChangeHandler(): ControllerChangeHandler? {
        var handler = controller.overriddenPushHandler
        if (handler == null) {
            handler = pushControllerChangeHandler
        }
        return handler
    }

    fun pushChangeHandler(handler: ControllerChangeHandler?): RouterTransaction {
        if (!attachedToRouter) {
            pushControllerChangeHandler = handler
            return this
        } else {
            throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
        }
    }

    fun popChangeHandler(): ControllerChangeHandler? {
        var handler = controller.overriddenPopHandler
        if (handler == null) {
            handler = popControllerChangeHandler
        }
        return handler
    }

    fun popChangeHandler(handler: ControllerChangeHandler?): RouterTransaction {
        if (!attachedToRouter) {
            popControllerChangeHandler = handler
            return this
        } else {
            throw RuntimeException(javaClass.simpleName + "s can not be modified after being added to a Router.")
        }
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

        pushControllerChangeHandler?.let { bundle.putBundle(KEY_PUSH_TRANSITION, it.toBundle()) }
        popControllerChangeHandler?.let { bundle.putBundle(KEY_POP_TRANSITION, it.toBundle()) }

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
