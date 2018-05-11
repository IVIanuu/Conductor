package com.ivianuu.conductor.sample.controllers

import android.content.Intent
import android.graphics.PorterDuff.Mode
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.URLSpan
import android.util.Log
import android.view.*
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.ControllerChangeType
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.FadeChangeHandler
import com.ivianuu.conductor.changehandler.TransitionChangeHandlerCompat
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.changehandler.ArcFadeMoveChangeHandlerCompat
import com.ivianuu.conductor.sample.changehandler.FabToDialogTransitionChangeHandler
import com.ivianuu.conductor.sample.controllers.NavigationDemoController.DisplayUpMode
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.KtViewHolder
import kotlinx.android.synthetic.main.controller_home.*
import kotlinx.android.synthetic.main.row_home.*

class HomeController : BaseController() {

    override val title: String?
        get() = "Conductor Demos"

    enum class DemoModel(val title: String, val color: Int) {
        NAVIGATION("Navigation Demos", R.color.red_300),
        TRANSITIONS("Transition Demos", R.color.blue_grey_300),
        SHARED_ELEMENT_TRANSITIONS("Shared Element Demos", R.color.purple_300),
        CHILD_CONTROLLERS("Child Controllers", R.color.orange_300),
        MULTIPLE_CHILD_ROUTERS("Multiple Child Routers", R.color.deep_orange_300),
        MASTER_DETAIL("Master Detail", R.color.grey_300),
        DRAG_DISMISS("Drag Dismiss", R.color.lime_300)
    }

    override val layoutRes = R.layout.controller_home

    init {
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View) {
        recycler_view.setHasFixedSize(true)
        recycler_view.layoutManager = LinearLayoutManager(view.context)
        recycler_view.adapter = HomeAdapter(
            LayoutInflater.from(view.context),
            DemoModel.values()
        )

        fab.setOnClickListener { onFabClicked(true) }
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        outState.putInt(KEY_FAB_VISIBILITY, fab!!.visibility)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        fab!!.visibility = savedViewState.getInt(KEY_FAB_VISIBILITY)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home, menu)
    }

    override fun onChangeStarted(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        setOptionsMenuHidden(!changeType.isEnter)

        if (changeType.isEnter) {
            setTitle()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.about) {
            onFabClicked(false)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onFabClicked(fromFab: Boolean) {
        val details =
            SpannableString("A small, yet full-featured framework that allows building View-based Android applications")
        details.setSpan(
            AbsoluteSizeSpan(16, true),
            0,
            details.length,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE
        )

        val url = "https://github.com/bluelinelabs/Conductor"
        val link = SpannableString(url)
        link.setSpan(object : URLSpan(url) {
            override fun onClick(widget: View) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }, 0, link.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        val description = SpannableStringBuilder()
        description.append(details)
        description.append("\n\n")
        description.append(link)

        val pushHandler = if (fromFab) TransitionChangeHandlerCompat(
            FabToDialogTransitionChangeHandler(),
            FadeChangeHandler(removesFromViewOnPush = false)
        ) else FadeChangeHandler(removesFromViewOnPush = false)
        val popHandler = if (fromFab) TransitionChangeHandlerCompat(
            FabToDialogTransitionChangeHandler(),
            FadeChangeHandler()
        ) else FadeChangeHandler()

        requireRouter()
            .pushController(
                RouterTransaction.with(DialogController("Conductor", description))
                    .pushChangeHandler(pushHandler)
                    .popChangeHandler(popHandler)
            )

    }

    fun onModelRowClick(model: DemoModel?, position: Int) {
        when (model) {
            HomeController.DemoModel.NAVIGATION -> requireRouter().pushController(
                RouterTransaction.with(
                    NavigationDemoController(
                        0,
                        DisplayUpMode.SHOW_FOR_CHILDREN_ONLY
                    )
                )
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
                    .tag(NavigationDemoController.TAG_UP_TRANSACTION)
            )
            HomeController.DemoModel.TRANSITIONS -> requireRouter().pushController(
                TransitionDemoController.getRouterTransaction(
                    0,
                    this
                )
            )
            HomeController.DemoModel.CHILD_CONTROLLERS -> requireRouter().pushController(
                RouterTransaction.with(ParentController())
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
            HomeController.DemoModel.SHARED_ELEMENT_TRANSITIONS -> {
                val titleSharedElementName =
                    resources!!.getString(R.string.transition_tag_title_indexed, position)
                val dotSharedElementName =
                    resources!!.getString(R.string.transition_tag_dot_indexed, position)

                requireRouter().pushController(
                    RouterTransaction.with(CityGridController(model.title, model.color, position))
                        .pushChangeHandler(
                            ArcFadeMoveChangeHandlerCompat(
                                titleSharedElementName,
                                dotSharedElementName
                            )
                        )
                        .popChangeHandler(
                            ArcFadeMoveChangeHandlerCompat(
                                titleSharedElementName,
                                dotSharedElementName
                            )
                        )
                )
            }
            HomeController.DemoModel.DRAG_DISMISS -> requireRouter().pushController(
                RouterTransaction.with(DragDismissController())
                    .pushChangeHandler(FadeChangeHandler(removesFromViewOnPush = false))
                    .popChangeHandler(FadeChangeHandler())
            )
            HomeController.DemoModel.MULTIPLE_CHILD_ROUTERS -> requireRouter().pushController(
                RouterTransaction.with(MultipleChildRouterController())
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
            HomeController.DemoModel.MASTER_DETAIL -> requireRouter().pushController(
                RouterTransaction.with(MasterDetailListController())
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
        }
    }

    internal inner class HomeAdapter(
        private val inflater: LayoutInflater,
        private val items: Array<DemoModel>
    ) : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_home, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position, items[position])
        }

        internal inner class ViewHolder(itemView: View) : KtViewHolder(itemView) {

            private var model: DemoModel? = null

            fun bind(position: Int, item: DemoModel) {
                model = item
                tv_title!!.text = item.title
                img_dot!!.drawable.setColorFilter(
                    ContextCompat.getColor(activity!!, item.color),
                    Mode.SRC_ATOP
                )

                ViewCompat.setTransitionName(
                    tv_title,
                    resources!!.getString(R.string.transition_tag_title_indexed, position)
                )
                ViewCompat.setTransitionName(
                    img_dot,
                    resources!!.getString(R.string.transition_tag_dot_indexed, position)
                )

                row_root.setOnClickListener { onModelRowClick(model, position) }
            }

        }
    }

    companion object {
        private const val KEY_FAB_VISIBILITY = "HomeController.fabVisibility"
    }
}
