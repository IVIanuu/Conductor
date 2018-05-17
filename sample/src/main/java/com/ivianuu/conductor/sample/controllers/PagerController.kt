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

package com.ivianuu.conductor.sample.controllers

import android.view.View
import com.ivianuu.conductor.Router
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.support.RouterPagerAdapter
import kotlinx.android.synthetic.main.controller_pager.*
import java.util.*

class PagerController : BaseController() {

    private val pagerAdapter = object : RouterPagerAdapter(this@PagerController) {
        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {
                val page = ChildController(
                    String.format(
                        Locale.getDefault(),
                        "Child #%d (Swipe to see more)",
                        position
                    ), PAGE_COLORS[position], true
                )
                router.setRoot(RouterTransaction.with(page))
            }
        }

        override fun getCount() = PAGE_COLORS.size

        override fun getPageTitle(position: Int) = "Page $position"
    }

    override val title: String
        get() = "ViewPager Demo"

    override val layoutRes = R.layout.controller_pager

    override fun onViewCreated(view: View) {
        view_pager.adapter = pagerAdapter
        tab_layout.setupWithViewPager(view_pager)
    }

    override fun onDestroyView(view: View) {
        if (!requireActivity().isChangingConfigurations) {
            view_pager.adapter = null
        }
        tab_layout.setupWithViewPager(null)
        super.onDestroyView(view)
    }

    companion object {
        private val PAGE_COLORS = intArrayOf(
            R.color.green_300,
            R.color.cyan_300,
            R.color.deep_purple_300,
            R.color.lime_300,
            R.color.red_300
        )
    }

}
