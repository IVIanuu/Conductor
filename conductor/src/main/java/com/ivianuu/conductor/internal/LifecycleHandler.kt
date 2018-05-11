package com.ivianuu.conductor.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.arch.lifecycle.ViewModelStore
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.SparseArray
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import com.ivianuu.conductor.ActivityHostedRouter
import com.ivianuu.conductor.Router
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList

class LifecycleHandler : Fragment(), ActivityLifecycleCallbacks {

    lateinit var lifecycleActivity: FragmentActivity

    private var hasRegisteredCallbacks = false
    private var destroyed = false
    private var attached = false
    private var permissionRequestMap = SparseArray<String>()
    private var activityRequestMap = SparseArray<String>()
    private var pendingPermissionRequests = mutableListOf<PendingPermissionRequest>()

    private val routerMap = mutableMapOf<Int, ActivityHostedRouter>()

    val routers: List<Router>
        get() = routerMap.values.toList()

    init {
        setHasOptionsMenu(true)
        retainInstance = true
    }

    fun getRouter(container: ViewGroup, savedInstanceState: Bundle?): Router {
        return routerMap.getOrPut(container.id) {
            ActivityHostedRouter(this, container).apply {
                if (savedInstanceState != null) {
                    val routerSavedState =
                        savedInstanceState.getBundle(KEY_ROUTER_STATE_PREFIX + containerId)
                    if (routerSavedState != null) {
                        restoreInstanceState(routerSavedState)
                    }
                }
            }
        }
    }

    private fun registerActivityListener(activity: FragmentActivity) {
        this.lifecycleActivity = activity

        if (!hasRegisteredCallbacks) {
            hasRegisteredCallbacks = true
            activity.application.registerActivityLifecycleCallbacks(this)

            // Since Fragment transactions are async, we have to keep an <Activity, LifecycleHandler> map in addition
            // to trying to find the LifecycleHandler fragment in the Activity to handle the case of the developer
            // trying to immediately get > 1 router in the same Activity. See issue #299.
            activeLifecycleHandlers[activity] = this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            val permissionParcel = savedInstanceState.getParcelable<StringSparseArrayParceler>(
                KEY_PERMISSION_REQUEST_CODES
            )
            permissionRequestMap = permissionParcel?.stringSparseArray ?: SparseArray()

            val activityParcel = savedInstanceState.getParcelable<StringSparseArrayParceler>(
                KEY_ACTIVITY_REQUEST_CODES
            )
            activityRequestMap = activityParcel?.stringSparseArray ?: SparseArray()

            val pendingRequests =
                savedInstanceState.getParcelableArrayList<PendingPermissionRequest>(
                    KEY_PENDING_PERMISSION_REQUESTS
                )
            pendingPermissionRequests = pendingRequests ?: mutableListOf()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(
            KEY_PERMISSION_REQUEST_CODES,
            StringSparseArrayParceler(permissionRequestMap)
        )
        outState.putParcelable(
            KEY_ACTIVITY_REQUEST_CODES,
            StringSparseArrayParceler(activityRequestMap)
        )
        outState.putParcelableArrayList(KEY_PENDING_PERMISSION_REQUESTS, ArrayList(pendingPermissionRequests))
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleActivity.application.unregisterActivityLifecycleCallbacks(this)
        activeLifecycleHandlers.remove(lifecycleActivity)
        destroyRouters()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        destroyed = false
        setAttached()
    }

    override fun onDetach() {
        super.onDetach()

        attached = false
        destroyRouters()
    }

    private fun setAttached() {
        if (!attached) {
            attached = true

            for (i in pendingPermissionRequests.indices.reversed()) {
                val request = pendingPermissionRequests.removeAt(i)
                requestPermissions(request.instanceId,
                    request.permissions.toTypedArray(), request.requestCode)
            }
        }
    }

    private fun destroyRouters() {
        if (!destroyed) {
            destroyed = true

            for (router in routerMap.values) {
                router.onActivityDestroyed(lifecycleActivity)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val instanceId = activityRequestMap.get(requestCode)
        if (instanceId != null) {
            for (router in routerMap.values) {
                router.onActivityResult(instanceId, requestCode, resultCode, data)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val instanceId = permissionRequestMap.get(requestCode)
        if (instanceId != null) {
            for (router in routerMap.values) {
                router.onRequestPermissionsResult(
                    instanceId,
                    requestCode,
                    permissions,
                    grantResults
                )
            }
        }
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        for (router in routerMap.values) {
            val handled = router.handleRequestedPermission(permission)
            if (handled != null) {
                return handled
            }
        }
        return super.shouldShowRequestPermissionRationale(permission)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        for (router in routerMap.values) {
            router.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        for (router in routerMap.values) {
            router.onPrepareOptionsMenu(menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        for (router in routerMap.values) {
            if (router.onOptionsItemSelected(item)) {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun registerForActivityResult(instanceId: String, requestCode: Int) {
        activityRequestMap.put(requestCode, instanceId)
    }

    fun unregisterForActivityResults(instanceId: String) {
        for (i in activityRequestMap.size() - 1 downTo 0) {
            if (instanceId == activityRequestMap.get(activityRequestMap.keyAt(i))) {
                activityRequestMap.removeAt(i)
            }
        }
    }

    fun startActivityForResult(instanceId: String, intent: Intent, requestCode: Int) {
        registerForActivityResult(instanceId, requestCode)
        startActivityForResult(intent, requestCode)
    }

    fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ) {
        registerForActivityResult(instanceId, requestCode)
        startActivityForResult(intent, requestCode, options)
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Throws(IntentSender.SendIntentException::class)
    fun startIntentSenderForResult(
        instanceId: String, intent: IntentSender, requestCode: Int,
        fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int,
        options: Bundle?
    ) {
        registerForActivityResult(instanceId, requestCode)
        startIntentSenderForResult(
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options
        )
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissions(instanceId: String, permissions: Array<String>, requestCode: Int) {
        if (attached) {
            permissionRequestMap.put(requestCode, instanceId)
            requestPermissions(permissions, requestCode)
        } else {
            pendingPermissionRequests.add(
                PendingPermissionRequest(
                    instanceId,
                    permissions.toList(),
                    requestCode
                )
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {

    }

    override fun onActivityStarted(activity: Activity) {
        if (this.lifecycleActivity == activity) {
            for (router in routerMap.values) {
                router.onActivityStarted(activity)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (this.lifecycleActivity == activity) {
            for (router in routerMap.values) {
                router.onActivityResumed(activity)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (this.lifecycleActivity == activity) {
            for (router in routerMap.values) {
                router.onActivityPaused(activity)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (this.lifecycleActivity == activity) {
            for (router in routerMap.values) {
                router.onActivityStopped(activity)
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        if (this.lifecycleActivity == activity) {
            for (router in routerMap.values) {
                val bundle = Bundle()
                router.saveInstanceState(bundle)
                outState.putBundle(KEY_ROUTER_STATE_PREFIX + router.containerId, bundle)
            }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activeLifecycleHandlers.remove(activity)
    }

    @Parcelize
    internal data class PendingPermissionRequest(
        val instanceId: String,
        val permissions: List<String>,
        val requestCode: Int
    ) : Parcelable

    companion object {
        private const val FRAGMENT_TAG = "com.ivianuu.conductor.LifecycleHandler"

        private const val KEY_PENDING_PERMISSION_REQUESTS =
            "LifecycleHandler.pendingPermissionRequests"
        private const val KEY_PERMISSION_REQUEST_CODES = "LifecycleHandler.permissionRequests"
        private const val KEY_ACTIVITY_REQUEST_CODES = "LifecycleHandler.activityRequests"
        private const val KEY_ROUTER_STATE_PREFIX = "LifecycleHandler.routerState"

        private val activeLifecycleHandlers = mutableMapOf<Activity, LifecycleHandler>()

        fun install(activity: FragmentActivity): LifecycleHandler {
            var lifecycleHandler = findInActivity(activity)
            if (lifecycleHandler == null) {
                lifecycleHandler = LifecycleHandler()
                activity.supportFragmentManager.beginTransaction()
                    .add(lifecycleHandler, FRAGMENT_TAG)
                    .commit()
            }
            lifecycleHandler.registerActivityListener(activity)
            return lifecycleHandler
        }

        private fun findInActivity(activity: FragmentActivity): LifecycleHandler? {
            var lifecycleHandler: LifecycleHandler? = activeLifecycleHandlers[activity]
            if (lifecycleHandler == null) {
                lifecycleHandler =
                        activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as LifecycleHandler?
            }

            return lifecycleHandler
        }
    }
}
