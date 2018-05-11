package com.ivianuu.conductor

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.ViewGroup
import com.ivianuu.conductor.ControllerChangeHandler.ControllerChangeListener
import com.ivianuu.conductor.internal.TransactionIndexer

internal class ControllerHostedRouter : Router {

    override val activity: FragmentActivity?
        get() = hostController?.activity

    override val hasHost: Boolean
        get() = hostController != null

    override val siblingRouters: List<Router>
        get() {
            val list = mutableListOf<Router>()
            hostController?.let {
                list.addAll(it.childRouters)
                it.router?.let {
                    list.addAll(it.siblingRouters)
                }
            }
            return list
        }

    override val rootRouter: Router
        get() = hostController?.router?.rootRouter ?: this

    override val transactionIndexer: TransactionIndexer?
        get() = rootRouter.transactionIndexer

    private var hostController: Controller? = null

    var hostId = 0
        private set
    var tag: String? = null
        private set

    internal var isDetachFrozen = false
        set(value) {
            field = value
            backstack.forEach { it.controller.isDetachFrozen = value }
        }

    constructor()

    constructor(hostId: Int, tag: String?) {
        this.hostId = hostId
        this.tag = tag
    }

    fun setHost(controller: Controller, container: ViewGroup) {
        if (hostController != controller || this.container != container) {
            removeHost()

            if (container is ControllerChangeListener) {
                addChangeListener(container)
            }

            hostController = controller
            this.container = container

            backstack.forEach { it.controller.parentController = controller }

            watchContainerAttach()
        }
    }

    fun removeHost() {
        val container = container
        if (container != null && container is ControllerChangeListener) {
            removeChangeListener(container)
        }

        destroyingControllers
            .filter { it.view != null }
            .map { it to it.requireView() }
            .forEach { it.first.detach(it.second, true, false) }

        backstack
            .map { it.controller }
            .filter { it.view != null }
            .map { it to it.requireView() }
            .forEach { it.first.detach(it.second, true, false) }

        prepareForContainerRemoval()

        hostController = null
        this.container = null
    }

    override fun destroy(popViews: Boolean) {
        isDetachFrozen = false
        super.destroy(popViews)
    }

    override fun pushToBackstack(entry: RouterTransaction) {
        if (isDetachFrozen) {
            entry.controller.isDetachFrozen = true
        }
        super.pushToBackstack(entry)
    }

    override fun setBackstack(
        newBackstack: List<RouterTransaction>,
        changeHandler: ControllerChangeHandler?
    ) {
        if (isDetachFrozen) {
            newBackstack.forEach { it.controller.isDetachFrozen = true }
        }
        super.setBackstack(newBackstack, changeHandler)
    }

    override fun onActivityDestroyed(activity: Activity) {
        super.onActivityDestroyed(activity)
        removeHost()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        hostController?.router?.onActivityResult(requestCode, resultCode, data)
    }

    public override fun invalidateOptionsMenu() {
        hostController?.router?.invalidateOptionsMenu()
    }

    override fun startActivity(intent: Intent) {
        hostController?.router?.startActivity(intent)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int
    ) {
        hostController?.router?.startActivityForResult(instanceId, intent, requestCode)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        hostController?.router?.startActivityForResult(
            instanceId, intent, requestCode, options
        )
    }

    override fun startIntentSenderForResult(
        instanceId: String,
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    ) {
        hostController?.router?.startIntentSenderForResult(
            instanceId, intent, requestCode,
            fillInIntent, flagsMask, flagsValues, extraFlags, options
        )
    }

    override fun registerForActivityResult(instanceId: String, requestCode: Int) {
        hostController?.router?.registerForActivityResult(instanceId, requestCode)
    }

    override fun unregisterForActivityResults(instanceId: String) {
        hostController?.router?.unregisterForActivityResults(instanceId)
    }

    override fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    ) {
        hostController?.router?.requestPermissions(instanceId, permissions, requestCode)
    }

    override fun saveInstanceState(outState: Bundle) {
        super.saveInstanceState(outState)
        outState.putInt(KEY_HOST_ID, hostId)
        outState.putString(KEY_TAG, tag)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        hostId = savedInstanceState.getInt(KEY_HOST_ID)
        tag = savedInstanceState.getString(KEY_TAG)
    }

    override fun setControllerRouter(controller: Controller) {
        super.setControllerRouter(controller)
        controller.parentController = hostController
    }

    companion object {
        private const val KEY_HOST_ID = "ControllerHostedRouter.hostId"
        private const val KEY_TAG = "ControllerHostedRouter.tag"
    }
}
