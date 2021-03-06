package com.ivianuu.conductor

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.ViewGroup
import com.ivianuu.conductor.ControllerChangeHandler.ControllerChangeListener
import com.ivianuu.conductor.internal.LifecycleHandler
import com.ivianuu.conductor.internal.TransactionIndexer

class ActivityHostedRouter : Router() {

    override val activity: FragmentActivity?
        get() = lifecycleHandler?.lifecycleActivity

    override val hasHost get() = lifecycleHandler != null

    override val siblingRouters: List<Router>
        get() = lifecycleHandler?.routers ?: emptyList()

    override val rootRouter: Router
        get() = this

    override val transactionIndexer = TransactionIndexer()

    private var lifecycleHandler: LifecycleHandler? = null

    fun setHost(lifecycleHandler: LifecycleHandler, container: ViewGroup) {
        if (this.lifecycleHandler != lifecycleHandler || this.container != container) {
            if (this.container != null && this.container is ControllerChangeListener) {
                removeChangeListener(this.container as ControllerChangeListener)
            }

            if (container is ControllerChangeListener) {
                addChangeListener(container as ControllerChangeListener)
            }

            this.lifecycleHandler = lifecycleHandler
            this.container = container

            watchContainerAttach()
        }
    }

    override fun saveInstanceState(outState: Bundle) {
        super.saveInstanceState(outState)
        transactionIndexer.saveInstanceState(outState)
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        super.restoreInstanceState(savedInstanceState)
        transactionIndexer.restoreInstanceState(savedInstanceState)
    }

    public override fun invalidateOptionsMenu() {
        lifecycleHandler?.lifecycleActivity?.invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        lifecycleHandler?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onActivityDestroyed(activity: Activity) {
        super.onActivityDestroyed(activity)
        lifecycleHandler = null
    }

    override fun startActivity(intent: Intent) {
        lifecycleHandler?.startActivity(intent)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int
    ) {
        lifecycleHandler?.startActivityForResult(instanceId, intent, requestCode)
    }

    override fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        lifecycleHandler?.startActivityForResult(instanceId, intent, requestCode, options)
    }

    override fun startIntentSenderForResult(
        instanceId: String, intent: IntentSender, requestCode: Int, fillInIntent: Intent?,
        flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        lifecycleHandler?.startIntentSenderForResult(
            instanceId,
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options
        )
    }

    override fun registerForActivityResult(instanceId: String, requestCode: Int) {
        lifecycleHandler?.registerForActivityResult(instanceId, requestCode)
    }

    override fun unregisterForActivityResults(instanceId: String) {
        lifecycleHandler?.unregisterForActivityResults(instanceId)
    }

    override fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    ) {
        lifecycleHandler?.requestPermissions(instanceId, permissions, requestCode)
    }

}
