package com.ivianuu.conductor

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.util.SparseArray
import android.view.*
import com.ivianuu.conductor.internal.ClassUtils
import com.ivianuu.conductor.internal.ViewAttachHandler
import com.ivianuu.conductor.internal.ViewAttachHandler.ViewAttachListener
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.util.*
import kotlin.collections.ArrayList

/**
 * A Controller manages portions of the UI. It is similar to an Activity or Fragment in that it manages its
 * own lifecycle and controls interactions between the UI and whatever logic is required. It is, however,
 * a much lighter weight component than either Activities or Fragments. While it offers several lifecycle
 * methods, they are much simpler and more predictable than those of Activities and Fragments.
 */
abstract class Controller @JvmOverloads protected constructor(args: Bundle? = null) {

    /**
     * Returns any arguments that were set in this Controller's constructor
     */
    val args = args ?: Bundle(javaClass.classLoader)

    private var viewState: Bundle? = null
    private var savedInstanceState: Bundle? = null

    /**
     * Returns whether or not this Controller is currently in the process of being destroyed.
     */
    var isBeingDestroyed = false
        private set
    /**
     * Returns whether or not this Controller has been destroyed.
     */
    var isDestroyed = false
        private set

    /**
     * Returns whether or not this Controller is currently attached to a host View.
     */
    var isAttached = false
        private set

    var hasOptionsMenu = false
        set(value) {
            val invalidate = isAttached && !optionsMenuHidden && field != value

            field = value

            if (invalidate) executeWithRouter { it.invalidateOptionsMenu() }
        }

    var optionsMenuHidden = false
        set(value) {
            val invalidate = isAttached && hasOptionsMenu && field != value

            field = value

            if (invalidate) executeWithRouter { it.invalidateOptionsMenu() }
        }

    internal var viewIsAttached = false
    internal var viewWasDetached = false

    var router: Router? = null
        internal set(value) {
            if (field != value) {
                field = value

                if (value != null) {
                    performOnRestoreInstanceState()

                    onRouterSetListeners.forEach { it.invoke(value) }
                    onRouterSetListeners.clear()
                }
            } else {
                performOnRestoreInstanceState()
            }
        }

    /**
     * Return this Controller's View or `null` if it has not yet been created or has been
     * destroyed.
     */
    var view: View? = null
        private set

    /**
     * Returns this Controller's parent Controller if it is a child Controller or `null` if
     * it has no parent.
     */
    var parentController: Controller? = null
        internal set

    /**
     * Returns this Controller's instance ID, which is generated when the instance is created and
     * retained across restarts.
     */
    var instanceId = UUID.randomUUID().toString()
        private set

    private var targetInstanceId: String? = null
    internal var needsAttach = false
    private var attachedToUnownedParent = false
    private var hasSavedViewState = false
    internal var isDetachFrozen = false
        set(value) {
            if (field != value) {
                field = value

                childRouters.forEach { it.isDetachFrozen = value }

                val view = view
                if (!value && view != null && viewWasDetached) {
                    detach(view, false, false, true)
                }
            }
        }

    /**
     * Returns the [ControllerChangeHandler] that should be used for pushing this Controller, or null
     * if the handler from the [RouterTransaction] should be used instead.
     */
    var overriddenPushHandler: ControllerChangeHandler? = null

    /**
     * Returns the [ControllerChangeHandler] that should be used for popping this Controller, or null
     * if the handler from the [RouterTransaction] should be used instead.
     */
    var overriddenPopHandler: ControllerChangeHandler? = null

    var retainViewMode = RetainViewMode.RELEASE_DETACH
        set(value) {
            field = value
            if (this.retainViewMode == RetainViewMode.RELEASE_DETACH && !isAttached) {
                removeViewReference(true)
            }
        }

    private var viewAttachHandler: ViewAttachHandler? = null
    internal val childRouters = mutableListOf<ControllerHostedRouter>()
    private val lifecycleListeners = mutableListOf<LifecycleListener>()
    private val requestedPermissions = mutableListOf<String>()
    private val onRouterSetListeners = mutableListOf<((Router) -> Unit)>()
    private var destroyedView: WeakReference<View>? = null
    private var isPerformingExitTransition = false
    private var isContextAvailable = false

    /**
     * Returns the host Activity of this Controller's [Router] or `null` if this
     * Controller has not yet been attached to an Activity or if the Activity has been destroyed.
     */
    val activity: FragmentActivity?
        get() = router?.activity

    /**
     * Returns the Resources from the host Activity or `null` if this Controller has not
     * yet been attached to an Activity or if the Activity has been destroyed.
     */
    val resources: Resources?
        get() = activity?.resources

    /**
     * Returns the Application Context derived from the host Activity or `null` if this Controller
     * has not yet been attached to an Activity or if the Activity has been destroyed.
     */
    val applicationContext: Context?
        get() = activity?.applicationContext

    /**
     * Returns the target Controller that was set with the [.setTargetController]
     * method or `null` if this Controller has no target.
     */
    /**
     * Optional target for this Controller. One reason this could be used is to send results back to the Controller
     * that started this one. Target Controllers are retained across instances. It is recommended
     * that Controllers enforce that their target Controller conform to a specific Interface.
     */
    var targetController: Controller?
        get() {
            val targetInstanceId = targetInstanceId
            return if (targetInstanceId != null) {
                requireRouter().rootRouter.getControllerWithInstanceId(targetInstanceId)
            } else {
                null
            }
        }
        set(target) {
            if (targetInstanceId != null) {
                throw RuntimeException("Target controller already set. A controller's target may only be set once.")
            }

            targetInstanceId = target?.instanceId
        }

    init {
        ensureRequiredConstructor()
    }

    /**
     * Called when the controller is ready to display its view. A valid view must be returned. The standard body
     * for this method will be `return inflater.inflate(R.layout.my_layout, container, false);`, plus
     * any binding code.
     */
    protected abstract fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View

    /**
     * Retrieves the child [Router] for the given container/tag combination. If no child router for
     * this container exists yet, it will be created. Note that multiple routers should not exist
     * in the same container unless a lot of care is taken to maintain order between them. Avoid using
     * the same container unless you have a great reason to do so (ex: ViewPagers).
     */
    @JvmOverloads
    fun getChildRouter(container: ViewGroup, tag: String? = null): Router {
        return getChildRouter(container, tag, true)!!
    }

    /**
     * Retrieves the child [Router] for the given container/tag combination. Note that multiple
     * routers should not exist in the same container unless a lot of care is taken to maintain order
     * between them. Avoid using the same container unless you have a great reason to do so (ex: ViewPagers).
     * The only time this method will return `null` is when the child router does not exist prior
     * to calling this method and the createIfNeeded parameter is set to false.
     */
    fun getChildRouter(container: ViewGroup, tag: String?, createIfNeeded: Boolean): Router? {
        val containerId = container.id

        var childRouter = childRouters
            .firstOrNull { it.hostId == containerId && tag == it.tag }

        if (childRouter == null) {
            if (createIfNeeded) {
                childRouter = ControllerHostedRouter(container.id, tag)
                childRouter.setHost(this, container)
                childRouters.add(childRouter)

                if (isPerformingExitTransition) {
                    childRouter.isDetachFrozen = true
                }
            }
        } else if (!childRouter.hasHost) {
            childRouter.setHost(this, container)
            childRouter.rebindIfNeeded()
        }

        return childRouter
    }

    /**
     * Removes a child [Router] from this Controller. When removed, all Controllers currently managed by
     * the [Router] will be destroyed.
     */
    fun removeChildRouter(childRouter: Router) {
        if (childRouter is ControllerHostedRouter && childRouters.remove(childRouter)) {
            childRouter.destroy(true)
        }
    }

    internal fun findController(instanceId: String): Controller? {
        if (this.instanceId == instanceId) {
            return this
        }

        return childRouters
            .map { it.getControllerWithInstanceId(instanceId) }
            .firstOrNull()
    }

    /**
     * Returns all of this Controller's child Routers
     */
    fun getChildRouters(): List<Router> {
        return childRouters.toList()
    }

    /**
     * Called when this Controller's View is being destroyed. This should overridden to unbind the View
     * from any local variables.
     */
    protected open fun onDestroyView(view: View) {}

    /**
     * Called when this Controller begins the process of being swapped in or out of the host view.
     */
    protected open fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    /**
     * Called when this Controller completes the process of being swapped in or out of the host view.
     */
    protected open fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
    }

    /**
     * Called when this Controller has a Context available to it. This will happen very early on in the lifecycle
     * (before a view is created). If the host activity is re-created (ex: for orientation change), this will be
     * called again when the new context is available.
     */
    protected open fun onContextAvailable(context: Context) {}

    /**
     * Called when this Controller's Context is no longer available. This can happen when the Controller is
     * destroyed or when the host Activity is destroyed.
     */
    protected open fun onContextUnavailable() {}

    /**
     * Called when this Controller is attached to its host ViewGroup
     */
    protected open fun onAttach(view: View) {}

    /**
     * Called when this Controller is detached from its host ViewGroup
     */
    protected open fun onDetach(view: View) {}

    /**
     * Called when this Controller has been destroyed.
     */
    protected open fun onDestroy() {}

    /**
     * Called when this Controller's host Activity is started
     */
    protected open fun onActivityStarted(activity: Activity) {}

    /**
     * Called when this Controller's host Activity is resumed
     */
    protected open fun onActivityResumed(activity: Activity) {}

    /**
     * Called when this Controller's host Activity is paused
     */
    protected open fun onActivityPaused(activity: Activity) {}

    /**
     * Called when this Controller's host Activity is stopped
     */
    protected open fun onActivityStopped(activity: Activity) {}

    /**
     * Called to save this Controller's View state. As Views can be detached and destroyed as part of the
     * Controller lifecycle (ex: when another Controller has been pushed on top of it), care should be taken
     * to save anything needed to reconstruct the View.
     */
    protected open fun onSaveViewState(view: View, outState: Bundle) {}

    /**
     * Restores data that was saved in the [.onSaveViewState] method. This should be overridden
     * to restore the View's state to where it was before it was destroyed.
     */
    protected open fun onRestoreViewState(view: View, savedViewState: Bundle) {}

    /**
     * Called to save this Controller's state in the event that its host Activity is destroyed.
     */
    protected open fun onSaveInstanceState(outState: Bundle) {}

    /**
     * Restores data that was saved in the [.onSaveInstanceState] method. This should be overridden
     * to restore this Controller's state to where it was before it was destroyed.
     */
    protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {}

    /**
     * Calls startActivity(Intent) from this Controller's host Activity.
     */
    fun startActivity(intent: Intent) {
        executeWithRouter { it.startActivity(intent) }
    }

    /**
     * Calls startActivityForResult(Intent, int) from this Controller's host Activity.
     */
    fun startActivityForResult(intent: Intent, requestCode: Int) {
        executeWithRouter { it.startActivityForResult(instanceId, intent, requestCode) }
    }

    /**
     * Calls startActivityForResult(Intent, int, Bundle) from this Controller's host Activity.
     */
    fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        executeWithRouter { it.startActivityForResult(instanceId, intent, requestCode, options) }
    }

    /**
     * Calls startIntentSenderForResult(IntentSender, int, Intent, int, int, int, Bundle) from this Controller's host Activity.
     */
    fun startIntentSenderForResult(
        intent: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int,
        flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        executeWithRouter {
            it.startIntentSenderForResult(
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
    }

    /**
     * Registers this Controller to handle onActivityResult responses. Calling this method is NOT
     * necessary when calling [.startActivityForResult]
     */
    fun registerForActivityResult(requestCode: Int) {
        executeWithRouter { it.registerForActivityResult(instanceId, requestCode) }
    }

    /**
     * Should be overridden if this Controller has called startActivityForResult and needs to handle
     * the result.
     */
    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    /**
     * Calls requestPermission(String[], int) from this Controller's host Activity. Results for this request,
     * including [.shouldShowRequestPermissionRationale] and
     * [.onRequestPermissionsResult] will be forwarded back to this Controller by the system.
     */
    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissions(permissions: Array<String>, requestCode: Int) {
        requestedPermissions.addAll(Arrays.asList(*permissions))
        executeWithRouter { it.requestPermissions(instanceId, permissions, requestCode) }
    }

    /**
     * Gets whether you should show UI with rationale for requesting a permission.
     * {@see android.app.Activity#shouldShowRequestPermissionRationale(String)}
     */
    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return Build.VERSION.SDK_INT >= 23
                && activity?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    /**
     * Should be overridden if this Controller has requested runtime permissions and needs to handle the user's response.
     */
    open fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
    }

    /**
     * Returns the view if attached or throws an exception
     */
    fun requireView() = view ?: throw IllegalStateException("view is not attached")

    /**
     * Returns the activity if attached or throws an exception
     */
    fun requireActivity() = activity ?: throw IllegalStateException("activity is not attached")

    /**
     * Returns the resources if attached or throws an exception
     */
    fun requireResources() = resources ?: throw IllegalStateException("activity is not attached")

    /**
     * Returns the router if attached or throws an exception
     */
    fun requireRouter() = router ?: throw IllegalStateException("router is not attached")

    /**
     * Should be overridden if this Controller needs to handle the back button being pressed.
     */
    open fun handleBack(): Boolean {
        return childRouters
            .flatMap { it.backstack }
            .sortedByDescending { it.transactionIndex }
            .map { it.controller() }
            .any { it.isAttached && it.requireRouter().handleBack() }
    }

    /**
     * Adds a listener for all of this Controller's lifecycle events
     */
    fun addLifecycleListener(lifecycleListener: LifecycleListener) {
        if (!lifecycleListeners.contains(lifecycleListener)) {
            lifecycleListeners.add(lifecycleListener)
        }
    }

    /**
     * Removes a previously added lifecycle listener
     */
    fun removeLifecycleListener(lifecycleListener: LifecycleListener) {
        lifecycleListeners.remove(lifecycleListener)
    }

    private fun notifyLifecycleListeners(action: (LifecycleListener) -> Unit) {
        val lifecycleListeners = lifecycleListeners.toList()
        lifecycleListeners.forEach(action)
    }

    /**
     * Adds option items to the host Activity's standard options menu. This will only be called if
     * [.setHasOptionsMenu] has been called.
     */
    open fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {}

    /**
     * Prepare the screen's options menu to be displayed. This is called directly before showing the
     * menu and can be used modify its contents.
     */
    open fun onPrepareOptionsMenu(menu: Menu) {}

    /**
     * Called when an option menu item has been selected by the user.
     */
    open fun onOptionsItemSelected(item: MenuItem): Boolean {
        return false
    }

    internal fun prepareForHostDetach() {
        needsAttach = needsAttach || isAttached

        childRouters.forEach { it.prepareForHostDetach() }
    }

    internal fun didRequestPermission(permission: String): Boolean {
        return requestedPermissions.contains(permission)
    }

    internal fun requestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        requestedPermissions.removeAll(Arrays.asList(*permissions))
        onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    internal fun onContextAvailable() {
        val context = router?.activity

        if (context != null && !isContextAvailable) {
            notifyLifecycleListeners { it.preContextAvailable(this) }

            isContextAvailable = true
            onContextAvailable(context)

            notifyLifecycleListeners { it.postContextAvailable(this) }
        }

        childRouters.forEach { it.onContextAvailable() }
    }

    private fun executeWithRouter(action: (Router) -> Unit) {
        val router = router
        if (router != null) {
            action.invoke(router)
        } else {
            onRouterSetListeners.add(action)
        }
    }

    internal fun activityStarted(activity: Activity) {
        viewAttachHandler?.onActivityStarted()
        onActivityStarted(activity)
    }

    internal fun activityResumed(activity: Activity) {
        val view = view
        if (!isAttached && view != null && viewIsAttached) {
            attach(view)
        } else if (isAttached) {
            needsAttach = false
            hasSavedViewState = false
        }

        onActivityResumed(activity)
    }

    internal fun activityPaused(activity: Activity) {
        onActivityPaused(activity)
    }

    internal fun activityStopped(activity: Activity) {
        viewAttachHandler?.onActivityStopped()
        onActivityStopped(activity)
    }

    internal fun activityDestroyed(activity: Activity) {
        if (activity.isChangingConfigurations) {
            view?.let { detach(it, true, false, false) }
        } else {
            destroy(true)
        }

        if (isContextAvailable) {
            notifyLifecycleListeners { it.preContextUnavailable(this, activity) }

            isContextAvailable = false
            onContextUnavailable()

            notifyLifecycleListeners { it.postContextUnavailable(this) }
        }
    }

    internal fun attach(view: View) {
        attachedToUnownedParent = router == null || view.parent != requireRouter().container
        if (attachedToUnownedParent) return

        hasSavedViewState = false

        notifyLifecycleListeners { it.preAttach(this, view) }

        isAttached = true
        needsAttach = false

        onAttach(view)

        if (hasOptionsMenu && !optionsMenuHidden) {
            requireRouter().invalidateOptionsMenu()
        }

        notifyLifecycleListeners { it.postAttach(this, view) }
    }

    internal fun detach(view: View, forceViewRefRemoval: Boolean, blockViewRefRemoval: Boolean, canRetainChildViews: Boolean) {
        if (!attachedToUnownedParent) {
            childRouters.forEach { it.prepareForHostDetach() }
        }

        val removeViewRef = !blockViewRefRemoval
                && (forceViewRefRemoval || retainViewMode == RetainViewMode.RELEASE_DETACH || isBeingDestroyed)

        if (isAttached) {
            notifyLifecycleListeners { it.preDetach(this, view) }

            isAttached = false
            onDetach(view)

            if (hasOptionsMenu && !optionsMenuHidden) {
                router?.invalidateOptionsMenu()
            }

            notifyLifecycleListeners { it.postDetach(this, view) }
        }

        if (removeViewRef) {
            removeViewReference(canRetainChildViews)
        }
    }

    private fun removeViewReference(canRetainChildViews: Boolean) {
        val view = view
        if (view != null) {
            if (!isBeingDestroyed && !hasSavedViewState) {
                saveViewState(view)
            }

            notifyLifecycleListeners { it.preDestroyView(this, view) }

            onDestroyView(view)

            viewAttachHandler?.unregisterAttachListener(view)
            viewAttachHandler = null
            viewIsAttached = false

            if (isBeingDestroyed) {
                destroyedView = WeakReference(view)
            }
            this.view = null

            notifyLifecycleListeners { it.postDestroyView(this) }

            childRouters.forEach { it.removeHost(canRetainChildViews) }
        }

        if (isBeingDestroyed) {
            performDestroy()
        }
    }

    internal fun inflate(parent: ViewGroup): View {
        val oldView = view
        if (oldView != null
            && oldView.parent != null
            && oldView.parent != parent) {
            val forceRemoval = retainViewMode == RetainViewMode.RELEASE_DETACH
            detach(oldView, forceRemoval, false, true)
            if (retainViewMode != RetainViewMode.RETAIN_DETACH) {
                removeViewReference(true)
            } else {
                // were retaining our view to make sure that were getting attached to the new container
                // we have to remove the view from the old one
                (oldView.parent as ViewGroup).removeView(oldView)
            }
        }

        if (view == null) {
            notifyLifecycleListeners { it.preCreateView(this) }

            val view = onCreateView(LayoutInflater.from(activity), parent, viewState)
            this.view = view

            if (view == parent) {
                throw IllegalStateException("Controller's onCreateView method returned the parent ViewGroup. Perhaps you forgot to pass false for LayoutInflater.inflate's attachToRoot parameter?")
            }

            notifyLifecycleListeners { it.postCreateView(this, view) }

            restoreViewState(view)

            val viewAttachHandler = ViewAttachHandler(object : ViewAttachListener {
                override fun onAttached() {
                    viewIsAttached = true
                    viewWasDetached = false
                    attach(view)
                }

                override fun onDetached(fromActivityStop: Boolean) {
                    viewIsAttached = false
                    viewWasDetached = true

                    if (!isDetachFrozen) {
                        detach(view, false, fromActivityStop, true)
                    }
                }

                override fun onViewDetachAfterStop() {
                    if (!isDetachFrozen) {
                        detach(view, false, false, true)
                    }
                }
            })

            viewAttachHandler.listenForAttach(view)
            this.viewAttachHandler = viewAttachHandler
        } else if (retainViewMode == RetainViewMode.RETAIN_DETACH) {
            restoreChildControllerHosts()
        }

        return view!!
    }

    private fun restoreChildControllerHosts() {
        childRouters
            .filter { !it.hasHost }
            .forEach { childRouter ->
                val containerView = view?.findViewById<View>(childRouter.hostId)
                if (containerView != null && containerView is ViewGroup) {
                    childRouter.setHost(this, containerView)
                    childRouter.rebindIfNeeded()
                }
            }
    }

    private fun performDestroy() {
        if (isContextAvailable) {
            notifyLifecycleListeners { it.preContextUnavailable(this, requireActivity()) }

            isContextAvailable = false
            onContextUnavailable()

            notifyLifecycleListeners { it.postContextUnavailable(this) }
        }

        if (!isDestroyed) {
            notifyLifecycleListeners { it.preDestroy(this) }

            isDestroyed = true

            onDestroy()

            parentController = null

            notifyLifecycleListeners { it.postDestroy(this) }
        }
    }

    internal fun destroy() {
        destroy(false)
    }

    private fun destroy(removeViews: Boolean) {
        isBeingDestroyed = true

        router?.unregisterForActivityResults(instanceId)

        childRouters.forEach { it.destroy(false) }

        if (!isAttached) {
            removeViewReference(false)
        } else if (removeViews) {
            view?.let { detach(it, true, false, false) }
        }
    }

    private fun saveViewState(view: View) {
        hasSavedViewState = true

        val viewState = Bundle(javaClass.classLoader)

        val hierarchyState = SparseArray<Parcelable>()
        view.saveHierarchyState(hierarchyState)
        viewState.putSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY, hierarchyState)

        val stateBundle = Bundle(javaClass.classLoader)
        onSaveViewState(view, stateBundle)
        viewState.putBundle(KEY_VIEW_STATE_BUNDLE, stateBundle)

        notifyLifecycleListeners { it.onSaveViewState(this, viewState) }

        this.viewState = viewState
    }

    private fun restoreViewState(view: View) {
        val viewState = viewState
        if (viewState != null) {
            view.restoreHierarchyState(viewState.getSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY))
            val savedViewState = viewState.getBundle(KEY_VIEW_STATE_BUNDLE)
            savedViewState.classLoader = javaClass.classLoader
            onRestoreViewState(view, savedViewState)

            restoreChildControllerHosts()

            notifyLifecycleListeners { it.onRestoreViewState(this, viewState) }
        }
    }

    internal fun saveInstanceState(): Bundle {
        if (!hasSavedViewState) {
            view?.let(this::saveViewState)
        }

        val outState = Bundle()
        outState.putString(KEY_CLASS_NAME, javaClass.name)
        outState.putBundle(KEY_VIEW_STATE, viewState)
        outState.putBundle(KEY_ARGS, args)
        outState.putString(KEY_INSTANCE_ID, instanceId)
        outState.putString(KEY_TARGET_INSTANCE_ID, targetInstanceId)
        outState.putStringArrayList(KEY_REQUESTED_PERMISSIONS, ArrayList(requestedPermissions))
        outState.putBoolean(KEY_NEEDS_ATTACH, needsAttach || isAttached)
        outState.putInt(KEY_RETAIN_VIEW_MODE, retainViewMode.ordinal)

        overriddenPushHandler?.let { outState.putBundle(KEY_OVERRIDDEN_PUSH_HANDLER, it.toBundle()) }
        overriddenPopHandler?.let { outState.putBundle(KEY_OVERRIDDEN_POP_HANDLER, it.toBundle()) }

        val childRouterBundles = childRouters
            .map {
                val bundle = Bundle()
                it.saveInstanceState(bundle)
                bundle
            }

        outState.putParcelableArrayList(KEY_CHILD_ROUTERS, ArrayList(childRouterBundles))

        val savedState = Bundle(javaClass.classLoader)
        onSaveInstanceState(savedState)

        notifyLifecycleListeners { it.onSaveInstanceState(this, savedState) }

        outState.putBundle(KEY_SAVED_STATE, savedState)

        return outState
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        with(savedInstanceState) {
            viewState = getBundle(KEY_VIEW_STATE)?.apply {
                classLoader = this@Controller.javaClass.classLoader
            }

            instanceId = getString(KEY_INSTANCE_ID)
            targetInstanceId = getString(KEY_TARGET_INSTANCE_ID)
            requestedPermissions.addAll(getStringArrayList(KEY_REQUESTED_PERMISSIONS))

            overriddenPushHandler = ControllerChangeHandler.fromBundle(
                getBundle(KEY_OVERRIDDEN_PUSH_HANDLER)
            )
            overriddenPopHandler = ControllerChangeHandler.fromBundle(
                getBundle(KEY_OVERRIDDEN_POP_HANDLER)
            )

            needsAttach = getBoolean(KEY_NEEDS_ATTACH)
            retainViewMode = RetainViewMode.values()[getInt(KEY_RETAIN_VIEW_MODE, 0)]

            val childBundles = getParcelableArrayList<Bundle>(KEY_CHILD_ROUTERS)

            childBundles
                .map { childBundle ->
                    ControllerHostedRouter().apply {
                        restoreInstanceState(childBundle)
                    }
                }
                .forEach { childRouters.add(it) }
        }

        this.savedInstanceState = savedInstanceState.getBundle(KEY_SAVED_STATE)?.also {
            it.classLoader = javaClass.classLoader
        }

        performOnRestoreInstanceState()
    }

    private fun performOnRestoreInstanceState() {
        val savedInstanceState = savedInstanceState
        if (savedInstanceState != null && router != null) {
            onRestoreInstanceState(savedInstanceState)

            notifyLifecycleListeners { it.onRestoreInstanceState(this, savedInstanceState) }

            this.savedInstanceState = null
        }
    }

    internal fun changeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        if (!changeType.isEnter) {
            isPerformingExitTransition = true
            childRouters.forEach { it.isDetachFrozen = true }
        }

        onChangeStarted(changeHandler, changeType)

        notifyLifecycleListeners { it.onChangeStart(this, changeHandler, changeType) }
    }

    internal fun changeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        if (!changeType.isEnter) {
            isPerformingExitTransition = false
            childRouters.forEach { it.isDetachFrozen = false }
        }

        onChangeEnded(changeHandler, changeType)

        notifyLifecycleListeners { it.onChangeEnd(this, changeHandler, changeType) }

        val destroyedView = destroyedView
        if (isBeingDestroyed && !viewIsAttached && !isAttached && destroyedView != null) {
            val view = destroyedView.get()
            val container = requireRouter().container
            if (container != null && view != null && view.parent == container) {
                container.removeView(view)
            }
            this.destroyedView = null
        }
    }

    internal fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isAttached && hasOptionsMenu && !optionsMenuHidden) {
            onCreateOptionsMenu(menu, inflater)
        }
    }

    internal fun prepareOptionsMenu(menu: Menu) {
        if (isAttached && hasOptionsMenu && !optionsMenuHidden) {
            onPrepareOptionsMenu(menu)
        }
    }

    internal fun optionsItemSelected(item: MenuItem): Boolean {
        return isAttached && hasOptionsMenu && !optionsMenuHidden && onOptionsItemSelected(item)
    }

    private fun ensureRequiredConstructor() {
        val constructors = javaClass.constructors
        if (getBundleConstructor(constructors) == null && getDefaultConstructor(constructors) == null) {
            throw RuntimeException(javaClass.toString() + " does not have a constructor that takes a Bundle argument or a default constructor. Controllers must have one of these in order to restore their states.")
        }
    }

    /** Modes that will influence when the Controller will allow its view to be destroyed  */
    enum class RetainViewMode {
        /** The Controller will release its reference to its view as soon as it is detached.  */
        RELEASE_DETACH,
        /** The Controller will retain its reference to its view when detached.  */
        RETAIN_DETACH
    }

    /** Allows external classes to listen for lifecycle events in a Controller  */
    abstract class LifecycleListener {

        open fun preContextAvailable(controller: Controller) {}
        open fun postContextAvailable(controller: Controller) {}

        open fun preCreateView(controller: Controller) {}
        open fun postCreateView(controller: Controller, view: View) {}

        open fun preAttach(controller: Controller, view: View) {}
        open fun postAttach(controller: Controller, view: View) {}

        open fun preDetach(controller: Controller, view: View) {}
        open fun postDetach(controller: Controller, view: View) {}

        open fun preDestroyView(controller: Controller, view: View) {}
        open fun postDestroyView(controller: Controller) {}

        open fun preContextUnavailable(controller: Controller, context: Context) {}
        open fun postContextUnavailable(controller: Controller) {}

        open fun preDestroy(controller: Controller) {}
        open fun postDestroy(controller: Controller) {}

        open fun onSaveInstanceState(controller: Controller, outState: Bundle) {}
        open fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {}

        open fun onSaveViewState(controller: Controller, outState: Bundle) {}
        open fun onRestoreViewState(controller: Controller, savedViewState: Bundle) {}

        open fun onChangeStart(
            controller: Controller,
            changeHandler: ControllerChangeHandler,
            changeType: ControllerChangeType
        ) {
        }

        open fun onChangeEnd(
            controller: Controller,
            changeHandler: ControllerChangeHandler,
            changeType: ControllerChangeType
        ) {
        }

    }

    companion object {
        private const val KEY_CLASS_NAME = "Controller.className"
        private const val KEY_VIEW_STATE = "Controller.viewState"
        private const val KEY_CHILD_ROUTERS = "Controller.childRouters"
        private const val KEY_SAVED_STATE = "Controller.savedState"
        private const val KEY_INSTANCE_ID = "Controller.instanceId"
        private const val KEY_TARGET_INSTANCE_ID = "Controller.target.instanceId"
        private const val KEY_ARGS = "Controller.args"
        private const val KEY_NEEDS_ATTACH = "Controller.needsAttach"
        private const val KEY_REQUESTED_PERMISSIONS = "Controller.requestedPermissions"
        private const val KEY_OVERRIDDEN_PUSH_HANDLER = "Controller.overriddenPushHandler"
        private const val KEY_OVERRIDDEN_POP_HANDLER = "Controller.overriddenPopHandler"
        private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
        private const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
        private const val KEY_RETAIN_VIEW_MODE = "Controller.retainViewMode"

        internal fun newInstance(bundle: Bundle): Controller {
            val className = bundle.getString(KEY_CLASS_NAME)

            val cls =
                ClassUtils.classForName<Controller>(className, false)!!
            val constructors = cls.constructors
            val bundleConstructor = getBundleConstructor(constructors)

            val args = bundle.getBundle(KEY_ARGS)?.apply {
                classLoader = cls.classLoader
            }

            val controller: Controller
            try {
                if (bundleConstructor != null) {
                    controller = bundleConstructor.newInstance(args) as Controller
                } else {

                    controller = getDefaultConstructor(constructors)?.newInstance() as Controller

                    // Restore the args that existed before the last process death
                    if (args != null) {
                        controller.args.putAll(args)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(
                    "An exception occurred while creating a new instance of " + className + ". " + e.message,
                    e
                )
            }

            controller.restoreInstanceState(bundle)
            return controller
        }

        private fun getDefaultConstructor(constructors: Array<Constructor<*>>): Constructor<*>? {
            return constructors.firstOrNull { it.parameterTypes.isEmpty() }
        }

        private fun getBundleConstructor(constructors: Array<Constructor<*>>): Constructor<*>? {
            return constructors.firstOrNull {
                it.parameterTypes.size == 1 && it.parameterTypes[0] == Bundle::class.java
            }
        }
    }

}