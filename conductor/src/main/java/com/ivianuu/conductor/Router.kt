package com.ivianuu.conductor

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.annotation.UiThread
import android.view.*
import com.ivianuu.conductor.Controller.LifecycleListener
import com.ivianuu.conductor.ControllerChangeHandler.ChangeTransaction
import com.ivianuu.conductor.ControllerChangeHandler.ControllerChangeListener
import com.ivianuu.conductor.changehandler.SimpleSwapChangeHandler
import com.ivianuu.conductor.internal.NoOpControllerChangeHandler
import com.ivianuu.conductor.internal.ThreadUtils
import com.ivianuu.conductor.internal.TransactionIndexer
import java.util.*

/**
 * A Router implements navigation and backstack handling for [Controller]s. Router objects are attached
 * to Activity/containing ViewGroup pairs. Routers do not directly render or push Views to the container ViewGroup,
 * but instead defer this responsibility to the [ControllerChangeHandler] specified in a given transaction.
 */
abstract class Router {

    internal val backstack = Backstack()
    private val changeListeners = mutableListOf<ControllerChangeListener>()
    private val pendingControllerChanges = mutableListOf<ChangeTransaction>()
    internal val destroyingControllers = mutableListOf<Controller>()

    private var popsLastView = false
    internal var containerFullyAttached = false

    internal var container: ViewGroup? = null

    /**
     * Returns this Router's host Activity or `null` if it has either not yet been attached to
     * an Activity or if the Activity has been destroyed.
     */
    abstract val activity: Activity?

    val containerId: Int
        get() = container?.id ?: 0

    /**
     * Returns the number of [Controller]s currently in the backstack
     */
    val backstackSize: Int
        get() = backstack.size

    internal val controllers: List<Controller>
        get() {
            val controllers = mutableListOf<Controller>()

            val backstackIterator = backstack.reverseIterator()
            while (backstackIterator.hasNext()) {
                controllers.add(backstackIterator.next().controller)
            }

            return controllers
        }
    internal abstract val siblingRouters: List<Router>
    internal abstract val rootRouter: Router
    internal abstract val transactionIndexer: TransactionIndexer?

    internal abstract val hasHost: Boolean

    /**
     * This should be called by the host Activity when its onActivityResult method is called if the instanceId
     * of the controller that called startActivityForResult is not known.
     *
     * @param requestCode The Activity's onActivityResult requestCode
     * @param resultCode  The Activity's onActivityResult resultCode
     * @param data        The Activity's onActivityResult data
     */
    abstract fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    /**
     * This should be called by the host Activity when its onRequestPermissionsResult method is called. The call will be forwarded
     * to the [Controller] with the instanceId passed in.
     *
     * @param instanceId   The instanceId of the Controller to which this result should be forwarded
     * @param requestCode  The Activity's onRequestPermissionsResult requestCode
     * @param permissions  The Activity's onRequestPermissionsResult permissions
     * @param grantResults The Activity's onRequestPermissionsResult grantResults
     */
    fun onRequestPermissionsResult(
        instanceId: String,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val controller = getControllerWithInstanceId(instanceId)
        controller?.requestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * This should be called by the host Activity when its onBackPressed method is called. The call will be forwarded
     * to its top [Controller]. If that controller doesn't handle it, then it will be popped.
     *
     * @return Whether or not a back action was handled by the Router
     */
    @UiThread
    fun handleBack(): Boolean {
        ThreadUtils.ensureMainThread()

        if (!backstack.isEmpty) {
            if (backstack.peek()!!.controller.handleBack()) {
                return true
            } else if (popCurrentController()) {
                return true
            }
        }

        return false
    }

    /**
     * Pops the top [Controller] from the backstack
     *
     * @return Whether or not this Router still has controllers remaining on it after popping.
     */
    @UiThread
    fun popCurrentController(): Boolean {
        ThreadUtils.ensureMainThread()

        val transaction = backstack.peek()
                ?: throw IllegalStateException("Trying to pop the current controller when there are none on the backstack.")
        return popController(transaction.controller)
    }

    /**
     * Pops the passed [Controller] from the backstack
     *
     * @param controller The controller that should be popped from this Router
     * @return Whether or not this Router still has controllers remaining on it after popping.
     */
    @UiThread
    fun popController(controller: Controller): Boolean {
        ThreadUtils.ensureMainThread()

        val topTransaction = backstack.peek()
        val poppingTopController =
            topTransaction != null && topTransaction.controller == controller

        if (poppingTopController) {
            trackDestroyingController(backstack.pop())
            performControllerChange(backstack.peek(), topTransaction, false)
        } else {
            var removedTransaction: RouterTransaction? = null
            var nextTransaction: RouterTransaction? = null
            for (transaction in backstack) {
                if (transaction.controller == controller) {
                    if (controller.isAttached) {
                        trackDestroyingController(transaction)
                    }
                    backstack.remove(transaction)
                    removedTransaction = transaction
                } else if (removedTransaction != null) {
                    if (!transaction.controller.isAttached) {
                        nextTransaction = transaction
                    }
                    break
                }
            }

            if (removedTransaction != null) {
                performControllerChange(nextTransaction, removedTransaction, false)
            }
        }

        return if (popsLastView) {
            topTransaction != null
        } else {
            !backstack.isEmpty
        }
    }

    /**
     * Pushes a new [Controller] to the backstack
     *
     * @param transaction The transaction detailing what should be pushed, including the [Controller],
     * and its push and pop [ControllerChangeHandler], and its tag.
     */
    @UiThread
    fun pushController(transaction: RouterTransaction) {
        ThreadUtils.ensureMainThread()

        val from = backstack.peek()
        pushToBackstack(transaction)
        performControllerChange(transaction, from, true)
    }

    /**
     * Replaces this Router's top [Controller] with a new [Controller]
     *
     * @param transaction The transaction detailing what should be pushed, including the [Controller],
     * and its push and pop [ControllerChangeHandler], and its tag.
     */
    @UiThread
    fun replaceTopController(transaction: RouterTransaction) {
        ThreadUtils.ensureMainThread()

        val topTransaction = backstack.peek()
        if (!backstack.isEmpty) {
            trackDestroyingController(backstack.pop())
        }

        val handler = transaction.pushChangeHandler()
        if (topTransaction != null) {
            val oldHandlerRemovedViews =
                topTransaction.pushChangeHandler() == null || topTransaction.pushChangeHandler()!!.removesFromViewOnPush()
            val newHandlerRemovesViews = handler == null || handler.removesFromViewOnPush()
            if (!oldHandlerRemovedViews && newHandlerRemovesViews) {
                for (visibleTransaction in getVisibleTransactions(backstack.iterator())) {
                    performControllerChange(null, visibleTransaction, true, handler)
                }
            }
        }

        pushToBackstack(transaction)

        handler?.setForceRemoveViewOnPush(true)
        performControllerChange(transaction.pushChangeHandler(handler), topTransaction, true)
    }

    internal open fun destroy(popViews: Boolean) {
        popsLastView = true
        val poppedControllers = backstack.popAll()
        trackDestroyingControllers(poppedControllers)

        if (popViews && poppedControllers.isNotEmpty()) {
            val topTransaction = poppedControllers[0]
            topTransaction.controller.addLifecycleListener(object : LifecycleListener() {
                override fun onChangeEnd(
                    controller: Controller,
                    changeHandler: ControllerChangeHandler,
                    changeType: ControllerChangeType
                ) {
                    if (changeType == ControllerChangeType.POP_EXIT) {
                        for (i in poppedControllers.size - 1 downTo 1) {
                            val transaction = poppedControllers[i]
                            performControllerChange(
                                null,
                                transaction,
                                true,
                                SimpleSwapChangeHandler()
                            )
                        }
                    }
                }
            })

            performControllerChange(null, topTransaction, false, topTransaction.popChangeHandler())
        }
    }

    /**
     * If set to true, this router will handle back presses by performing a change handler on the last controller and view
     * in the stack. This defaults to false so that the developer can either finish its containing Activity or otherwise
     * hide its parent view without any strange artifacting.
     */
    fun setPopsLastView(popsLastView: Boolean): Router {
        this.popsLastView = popsLastView
        return this
    }

    /**
     * Pops all [Controller]s until only the root is left
     *
     * @return Whether or not any [Controller]s were popped in order to get to the root transaction
     */
    @UiThread
    fun popToRoot(): Boolean {
        ThreadUtils.ensureMainThread()

        return popToRoot(null)
    }

    /**
     * Pops all [Controller] until only the root is left
     *
     * @param changeHandler The [ControllerChangeHandler] to handle this transaction
     * @return Whether or not any [Controller]s were popped in order to get to the root transaction
     */
    @UiThread
    fun popToRoot(changeHandler: ControllerChangeHandler?): Boolean {
        ThreadUtils.ensureMainThread()

        return if (backstack.size > 1) {
            popToTransaction(backstack.root()!!, changeHandler)
            true
        } else {
            false
        }
    }

    /**
     * Pops all [Controller]s until the Controller with the passed tag is at the top
     *
     * @param tag The tag being popped to
     * @return Whether or not any [Controller]s were popped in order to get to the transaction with the passed tag
     */
    @UiThread
    fun popToTag(tag: String): Boolean {
        ThreadUtils.ensureMainThread()

        return popToTag(tag, null)
    }

    /**
     * Pops all [Controller]s until the [Controller] with the passed tag is at the top
     *
     * @param tag           The tag being popped to
     * @param changeHandler The [ControllerChangeHandler] to handle this transaction
     * @return Whether or not the [Controller] with the passed tag is now at the top
     */
    @UiThread
    fun popToTag(tag: String, changeHandler: ControllerChangeHandler?): Boolean {
        ThreadUtils.ensureMainThread()

        for (transaction in backstack) {
            if (tag == transaction.tag()) {
                popToTransaction(transaction, changeHandler)
                return true
            }
        }
        return false
    }

    /**
     * Sets the root Controller. If any [Controller]s are currently in the backstack, they will be removed.
     *
     * @param transaction The transaction detailing what should be pushed, including the [Controller],
     * and its push and pop [ControllerChangeHandler], and its tag.
     */
    @UiThread
    fun setRoot(transaction: RouterTransaction) {
        ThreadUtils.ensureMainThread()

        val transactions = listOf(transaction)
        setBackstack(transactions, transaction.pushChangeHandler())
    }

    /**
     * Returns the hosted Controller with the given instance id or `null` if no such
     * Controller exists in this Router.
     *
     * @param instanceId The instance ID being searched for
     */
    fun getControllerWithInstanceId(instanceId: String): Controller? {
        for (transaction in backstack) {
            val controllerWithId = transaction.controller.findController(instanceId)
            if (controllerWithId != null) {
                return controllerWithId
            }
        }
        return null
    }

    /**
     * Returns the hosted Controller that was pushed with the given tag or `null` if no
     * such Controller exists in this Router.
     *
     * @param tag The tag being searched for
     */
    fun getControllerWithTag(tag: String): Controller? {
        for (transaction in backstack) {
            if (tag == transaction.tag()) {
                return transaction.controller
            }
        }
        return null
    }

    /**
     * Returns the current backstack, ordered from root to most recently pushed.
     */
    fun getBackstack(): List<RouterTransaction> {
        val list = mutableListOf<RouterTransaction>()
        val backstackIterator = backstack.reverseIterator()
        while (backstackIterator.hasNext()) {
            list.add(backstackIterator.next())
        }
        return list
    }

    /**
     * Sets the backstack, transitioning from the current top controller to the top of the new stack (if different)
     * using the passed [ControllerChangeHandler]
     *
     * @param newBackstack  The new backstack
     * @param changeHandler An optional change handler to be used to handle the root view of transition
     */
    @UiThread
    open fun setBackstack(
        newBackstack: List<RouterTransaction>,
        changeHandler: ControllerChangeHandler?
    ) {
        ThreadUtils.ensureMainThread()

        val oldTransactions = getBackstack()
        val oldVisibleTransactions = getVisibleTransactions(backstack.iterator())

        removeAllExceptVisibleAndUnowned()
        ensureOrderedTransactionIndices(newBackstack)

        backstack.setBackstack(newBackstack)

        // Ensure all new controllers have a valid router set
        val backstackIterator = backstack.reverseIterator()
        while (backstackIterator.hasNext()) {
            val transaction = backstackIterator.next()
            transaction.onAttachedToRouter()
            setControllerRouter(transaction.controller)
        }

        if (newBackstack.isNotEmpty()) {
            val reverseNewBackstack = newBackstack.reversed()
            val newVisibleTransactions = getVisibleTransactions(reverseNewBackstack.iterator())
            val newRootRequiresPush =
                !(newVisibleTransactions.isNotEmpty() && oldTransactions.contains(
                    newVisibleTransactions[0]
                ))

            val visibleTransactionsChanged =
                !backstacksAreEqual(newVisibleTransactions, oldVisibleTransactions)
            if (visibleTransactionsChanged) {
                val oldRootTransaction =
                    if (oldVisibleTransactions.isNotEmpty()) oldVisibleTransactions[0] else null
                val newRootTransaction = newVisibleTransactions[0]

                // Replace the old root with the new one
                if (oldRootTransaction == null || oldRootTransaction.controller != newRootTransaction.controller) {
                    // Ensure the existing root controller is fully pushed to the view hierarchy
                    if (oldRootTransaction != null) {
                        ControllerChangeHandler.completeHandlerImmediately(oldRootTransaction.controller.instanceId)
                    }
                    performControllerChange(
                        newRootTransaction,
                        oldRootTransaction,
                        newRootRequiresPush,
                        changeHandler
                    )
                }

                // Remove all visible controllers that were previously on the backstack
                for (i in oldVisibleTransactions.size - 1 downTo 1) {
                    val transaction = oldVisibleTransactions[i]
                    if (!newVisibleTransactions.contains(transaction)) {
                        val localHandler = changeHandler?.copy() ?: SimpleSwapChangeHandler()
                        localHandler.setForceRemoveViewOnPush(true)
                        ControllerChangeHandler.completeHandlerImmediately(transaction.controller.instanceId)
                        performControllerChange(
                            null,
                            transaction,
                            newRootRequiresPush,
                            localHandler
                        )
                    }
                }

                // Add any new controllers to the backstack
                for (i in 1 until newVisibleTransactions.size) {
                    val transaction = newVisibleTransactions[i]
                    if (!oldVisibleTransactions.contains(transaction)) {
                        performControllerChange(
                            transaction,
                            newVisibleTransactions[i - 1],
                            true,
                            transaction.pushChangeHandler()
                        )
                    }
                }
            }

        }

        // Destroy all old controllers that are no longer on the backstack. We don't do this when we initially
        // set the backstack to prevent the possibility that they'll be destroyed before the controller
        // change handler runs.
        for (oldTransaction in oldTransactions) {
            var contains = false
            for (newTransaction in newBackstack) {
                if (oldTransaction.controller == newTransaction.controller) {
                    contains = true
                    break
                }
            }

            if (!contains) {
                oldTransaction.controller.destroy()
            }
        }
    }

    /**
     * Returns whether or not this Router has a root [Controller]
     */
    fun hasRootController(): Boolean {
        return backstackSize > 0
    }

    /**
     * Adds a listener for all of this Router's [Controller] change events
     *
     * @param changeListener The listener
     */
    fun addChangeListener(changeListener: ControllerChangeListener) {
        if (!changeListeners.contains(changeListener)) {
            changeListeners.add(changeListener)
        }
    }

    /**
     * Removes a previously added listener
     *
     * @param changeListener The listener to be removed
     */
    fun removeChangeListener(changeListener: ControllerChangeListener) {
        changeListeners.remove(changeListener)
    }

    /**
     * Attaches this Router's existing backstack to its container if one exists.
     */
    @UiThread
    fun rebindIfNeeded() {
        ThreadUtils.ensureMainThread()

        val backstackIterator = backstack.reverseIterator()
        while (backstackIterator.hasNext()) {
            val transaction = backstackIterator.next()

            if (transaction.controller.needsAttach) {
                performControllerChange(transaction, null, true, SimpleSwapChangeHandler(false))
            }
        }
    }

    fun onActivityResult(instanceId: String, requestCode: Int, resultCode: Int, data: Intent?) {
        val controller = getControllerWithInstanceId(instanceId)
        controller?.onActivityResult(requestCode, resultCode, data)
    }

    fun onActivityStarted(activity: Activity) {
        for (transaction in backstack) {
            transaction.controller.activityStarted(activity)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onActivityStarted(activity)
            }
        }
    }

    fun onActivityResumed(activity: Activity) {
        for (transaction in backstack) {
            transaction.controller.activityResumed(activity)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onActivityResumed(activity)
            }
        }
    }

    fun onActivityPaused(activity: Activity) {
        for (transaction in backstack) {
            transaction.controller.activityPaused(activity)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onActivityPaused(activity)
            }
        }
    }

    fun onActivityStopped(activity: Activity) {
        for (transaction in backstack) {
            transaction.controller.activityStopped(activity)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onActivityStopped(activity)
            }
        }
    }

    open fun onActivityDestroyed(activity: Activity) {
        prepareForContainerRemoval()
        changeListeners.clear()

        for (transaction in backstack) {
            transaction.controller.activityDestroyed(activity)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onActivityDestroyed(activity)
            }
        }

        for (index in destroyingControllers.indices.reversed()) {
            val controller = destroyingControllers[index]
            controller.activityDestroyed(activity)

            for (childRouter in controller.getChildRouters()) {
                childRouter.onActivityDestroyed(activity)
            }
        }

        container = null
    }

    internal fun prepareForHostDetach() {
        for (transaction in backstack) {
            if (ControllerChangeHandler.completeHandlerImmediately(transaction.controller.instanceId)) {
                transaction.controller.needsAttach = true
            }
            transaction.controller.prepareForHostDetach()
        }
    }

    open fun saveInstanceState(outState: Bundle) {
        prepareForHostDetach()

        val backstackState = Bundle()
        backstack.saveInstanceState(backstackState)

        outState.putParcelable(KEY_BACKSTACK, backstackState)
        outState.putBoolean(KEY_POPS_LAST_VIEW, popsLastView)
    }

    open fun restoreInstanceState(savedInstanceState: Bundle) {
        val backstackBundle = savedInstanceState.getParcelable<Bundle>(KEY_BACKSTACK)

        backstack.restoreInstanceState(backstackBundle)
        popsLastView = savedInstanceState.getBoolean(KEY_POPS_LAST_VIEW)

        val backstackIterator = backstack.reverseIterator()
        while (backstackIterator.hasNext()) {
            setControllerRouter(backstackIterator.next().controller)
        }
    }

    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        for (transaction in backstack) {
            transaction.controller.createOptionsMenu(menu, inflater)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onCreateOptionsMenu(menu, inflater)
            }
        }
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        for (transaction in backstack) {
            transaction.controller.prepareOptionsMenu(menu)

            for (childRouter in transaction.controller.getChildRouters()) {
                childRouter.onPrepareOptionsMenu(menu)
            }
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        for (transaction in backstack) {
            if (transaction.controller.optionsItemSelected(item)) {
                return true
            }

            for (childRouter in transaction.controller.getChildRouters()) {
                if (childRouter.onOptionsItemSelected(item)) {
                    return true
                }
            }
        }
        return false
    }

    private fun popToTransaction(
        transaction: RouterTransaction,
        changeHandler: ControllerChangeHandler?
    ) {
        var changeHandler = changeHandler
        if (backstack.size > 0) {
            val topTransaction = backstack.peek()

            val updatedBackstack = mutableListOf<RouterTransaction>()
            val backstackIterator = backstack.reverseIterator()
            while (backstackIterator.hasNext()) {
                val existingTransaction = backstackIterator.next()
                updatedBackstack.add(existingTransaction)
                if (existingTransaction == transaction) {
                    break
                }
            }

            if (changeHandler == null) {

                changeHandler = topTransaction!!.popChangeHandler()
            }

            setBackstack(updatedBackstack, changeHandler)
        }
    }

    internal fun watchContainerAttach() {
        container?.post { containerFullyAttached = true }
    }

    internal fun prepareForContainerRemoval() {
        containerFullyAttached = false
        container?.setOnHierarchyChangeListener(null)
    }

    fun handleRequestedPermission(permission: String): Boolean? {
        for (transaction in backstack) {
            if (transaction.controller.didRequestPermission(permission)) {
                return transaction.controller.shouldShowRequestPermissionRationale(permission)
            }
        }
        return null
    }

    private fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean
    ) {
        if (isPush && to != null) {
            to.onAttachedToRouter()
        }

        val changeHandler = when {
            isPush -> to!!.pushChangeHandler()
            from != null -> from.popChangeHandler()
            else -> null
        }

        performControllerChange(to, from, isPush, changeHandler)
    }

    internal fun performControllerChange(
        to: RouterTransaction?,
        from: RouterTransaction?,
        isPush: Boolean,
        changeHandler: ControllerChangeHandler?
    ) {
        var changeHandler = changeHandler
        val toController = to?.controller
        val fromController = from?.controller
        var forceDetachDestroy = false

        if (to != null) {
            to.ensureValidIndex(transactionIndexer!!)
            setControllerRouter(toController!!)
        } else if (backstack.size == 0 && !popsLastView) {
            // We're emptying out the backstack. Views get weird if you transition them out, so just no-op it. The host
            // Activity or controller should be handling this by finishing or at least hiding this view.
            changeHandler = NoOpControllerChangeHandler()
            forceDetachDestroy = true
        }

        performControllerChange(toController, fromController, isPush, changeHandler)

        if (forceDetachDestroy && fromController != null && fromController.view != null) {
            fromController.detach(fromController.view!!, true, false)
        }
    }

    private fun performControllerChange(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        changeHandler: ControllerChangeHandler?
    ) {
        if (isPush && to != null && to.isDestroyed) {
            throw IllegalStateException("Trying to push a controller that has already been destroyed. (" + to.javaClass.simpleName + ")")
        }

        val transaction = ChangeTransaction(
            to,
            from,
            isPush,
            container,
            changeHandler,
            ArrayList(changeListeners)
        )

        if (pendingControllerChanges.size > 0) {
            // If we already have changes queued up (awaiting full container attach), queue this one up as well so they don't happen
            // out of order.
            pendingControllerChanges.add(transaction)
        } else if (from != null && (changeHandler == null || changeHandler.removesFromViewOnPush()) && !containerFullyAttached) {
            // If the change handler will remove the from view, we have to make sure the container is fully attached first so we avoid NPEs
            // within ViewGroup (details on issue #287). Post this to the container to ensure the attach is complete before we try to remove
            // anything.
            pendingControllerChanges.add(transaction)
            container?.post { performPendingControllerChanges() }
        } else {
            ControllerChangeHandler.executeChange(transaction)
        }
    }

    internal fun performPendingControllerChanges() {
        // We're intentionally using dynamic size checking (list.size()) here so we can account for changes
        // that occur during this loop (ex: if a controller is popped from within onAttach)
        for (i in pendingControllerChanges.indices) {
            ControllerChangeHandler.executeChange(pendingControllerChanges[i])
        }
        pendingControllerChanges.clear()
    }

    protected open fun pushToBackstack(entry: RouterTransaction) {
        backstack.push(entry)
    }

    private fun trackDestroyingController(transaction: RouterTransaction) {
        if (!transaction.controller.isDestroyed) {
            destroyingControllers.add(transaction.controller)

            transaction.controller.addLifecycleListener(object : LifecycleListener() {
                override fun postDestroy(controller: Controller) {
                    destroyingControllers.remove(controller)
                }
            })
        }
    }

    private fun trackDestroyingControllers(transactions: List<RouterTransaction>) {
        for (transaction in transactions) {
            trackDestroyingController(transaction)
        }
    }

    private fun removeAllExceptVisibleAndUnowned() {
        val views = mutableListOf<View>()

        for (transaction in getVisibleTransactions(backstack.iterator())) {
            if (transaction.controller.view != null) {
                views.add(transaction.controller.view!!)
            }
        }

        for (router in siblingRouters) {
            if (router.container == container) {
                addRouterViewsToList(router, views)
            }
        }

        val childCount = container?.childCount ?: 0
        for (i in childCount - 1 downTo 0) {
            val child = container!!.getChildAt(i)
            if (!views.contains(child)) {
                container!!.removeView(child)
            }
        }
    }

    // Swap around transaction indices to ensure they don't get thrown out of order by the
    // developer rearranging the backstack at runtime.
    private fun ensureOrderedTransactionIndices(backstack: List<RouterTransaction>) {
        val indices = mutableListOf<Int>()
        for (transaction in backstack) {
            transaction.ensureValidIndex(transactionIndexer!!)
            indices.add(transaction.transactionIndex)
        }

        indices.sort()

        for (i in backstack.indices) {
            backstack[i].transactionIndex = indices[i]
        }
    }

    private fun addRouterViewsToList(router: Router, list: MutableList<View>) {
        for (controller in router.controllers) {
            if (controller.view != null) {
                list.add(controller.view!!)
            }

            for (child in controller.getChildRouters()) {
                addRouterViewsToList(child, list)
            }
        }
    }

    private fun getVisibleTransactions(backstackIterator: Iterator<RouterTransaction>): List<RouterTransaction> {
        val transactions = mutableListOf<RouterTransaction>()
        while (backstackIterator.hasNext()) {
            val transaction = backstackIterator.next()
            transactions.add(transaction)

            if (transaction.pushChangeHandler() == null || transaction.pushChangeHandler()!!.removesFromViewOnPush()) {
                break
            }
        }

        transactions.reverse()
        return transactions
    }

    private fun backstacksAreEqual(
        lhs: List<RouterTransaction>,
        rhs: List<RouterTransaction>
    ): Boolean {
        if (lhs.size != rhs.size) {
            return false
        }

        for (i in rhs.indices) {
            if (rhs[i].controller() != lhs[i].controller()) {
                return false
            }
        }

        return true
    }

    internal open fun setControllerRouter(controller: Controller) {
        controller.router = this
    }

    internal abstract fun invalidateOptionsMenu()
    internal abstract fun startActivity(intent: Intent)
    internal abstract fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int
    )

    internal abstract fun startActivityForResult(
        instanceId: String,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    )

    @Throws(IntentSender.SendIntentException::class)
    internal abstract fun startIntentSenderForResult(
        instanceId: String,
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    )

    internal abstract fun registerForActivityResult(instanceId: String, requestCode: Int)
    internal abstract fun unregisterForActivityResults(instanceId: String)
    internal abstract fun requestPermissions(
        instanceId: String,
        permissions: Array<String>,
        requestCode: Int
    )

    companion object {
        private const val KEY_BACKSTACK = "Router.backstack"
        private const val KEY_POPS_LAST_VIEW = "Router.popsLastView"
    }

}
