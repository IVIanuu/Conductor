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

package com.ivianuu.conductor.support

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.PagerAdapter
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.Router
import java.util.*

/**
 * An adapter for ViewPagers that uses Routers as pages
 */
abstract class RouterPagerAdapter(private val host: Controller) : PagerAdapter() {

    private var maxPagesToStateSave = Integer.MAX_VALUE
    private var savedPages = SparseArray<Bundle>()
    private val visibleRouters = SparseArray<Router>()
    private var savedPageHistory = mutableListOf<Int>()
    private var currentPrimaryRouter: Router? = null

    /**
     * Called when a router is instantiated. Here the router's root should be set if needed.
     */
    abstract fun configureRouter(router: Router, position: Int)

    /**
     * Sets the maximum number of pages that will have their states saved. When this number is exceeded,
     * the page that was state saved least recently will have its state removed from the save data.
     */
    fun setMaxPagesToStateSave(maxPagesToStateSave: Int) {
        if (maxPagesToStateSave < 0) {
            throw IllegalArgumentException("Only positive integers may be passed for maxPagesToStateSave.")
        }

        this.maxPagesToStateSave = maxPagesToStateSave

        ensurePagesSaved()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val name = makeRouterName(container.id, getItemId(position))

        val router = host.getChildRouter(container, name)
        if (!router.hasRootController()) {
            val routerSavedState = savedPages.get(position)

            if (routerSavedState != null) {
                router.restoreInstanceState(routerSavedState)
                savedPages.remove(position)
            }
        }

        router.rebindIfNeeded()
        configureRouter(router, position)

        if (router != currentPrimaryRouter) {
            for (transaction in router.getBackstack()) {
                transaction.controller().optionsMenuHidden = true
            }
        }

        visibleRouters.put(position, router)
        return router
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val router = `object` as Router

        val savedState = Bundle()
        router.saveInstanceState(savedState)
        savedPages.put(position, savedState)

        savedPageHistory.remove(position)
        savedPageHistory.add(position)

        ensurePagesSaved()

        host.removeChildRouter(router)

        visibleRouters.remove(position)
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        val router = `object` as Router
        if (router != currentPrimaryRouter) {
            if (currentPrimaryRouter != null) {
                for (transaction in currentPrimaryRouter!!.getBackstack()) {
                    transaction.controller().optionsMenuHidden = true
                }
            }
            for (transaction in router.getBackstack()) {
                transaction.controller().optionsMenuHidden = false
            }
            currentPrimaryRouter = router
        }
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        val router = `object` as Router
        val backstack = router.getBackstack()
        for (transaction in backstack) {
            if (transaction.controller().view == view) {
                return true
            }
        }
        return false
    }

    override fun saveState(): Parcelable? {
        val bundle = Bundle()
        bundle.putSparseParcelableArray(KEY_SAVED_PAGES, savedPages)
        bundle.putInt(KEY_MAX_PAGES_TO_STATE_SAVE, maxPagesToStateSave)
        bundle.putIntegerArrayList(KEY_SAVE_PAGE_HISTORY, ArrayList(savedPageHistory))
        return bundle
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        val bundle = state as Bundle?
        if (bundle != null) {
            savedPages = bundle.getSparseParcelableArray(KEY_SAVED_PAGES)
            maxPagesToStateSave = bundle.getInt(KEY_MAX_PAGES_TO_STATE_SAVE)
            savedPageHistory = bundle.getIntegerArrayList(KEY_SAVE_PAGE_HISTORY)
        }
    }

    /**
     * Returns the already instantiated Router in the specified position or `null` if there
     * is no router associated with this position.
     */
    fun getRouter(position: Int): Router? {
        return visibleRouters.get(position)
    }

    fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private fun ensurePagesSaved() {
        while (savedPages.size() > maxPagesToStateSave) {
            val positionToRemove = savedPageHistory.removeAt(0)
            savedPages.remove(positionToRemove)
        }
    }

    companion object {
        private const val KEY_SAVED_PAGES = "RouterPagerAdapter.savedStates"
        private const val KEY_MAX_PAGES_TO_STATE_SAVE = "RouterPagerAdapter.maxPagesToStateSave"
        private const val KEY_SAVE_PAGE_HISTORY = "RouterPagerAdapter.savedPageHistory"

        private fun makeRouterName(viewId: Int, id: Long): String {
            return viewId.toString() + ":" + id
        }
    }

}