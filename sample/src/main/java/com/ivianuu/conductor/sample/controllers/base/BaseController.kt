package com.ivianuu.conductor.sample.controllers.base

import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.sample.MainActivity
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : Controller, LayoutContainer {

    override var containerView: View? = null

    // Note: This is just a quick demo of how an ActionBar *can* be accessed, not necessarily how it *should*
    // be accessed. In a production app, this would use Dagger instead.
    protected val actionBar: ActionBar?
        get() {
            val actionBarProvider = activity as MainActivity?
            return actionBarProvider?.supportActionBar
        }

    protected open val title: String?
        get() = null

    protected abstract val layoutRes: Int

    protected constructor() {}

    protected constructor(args: Bundle) : super(args) {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(layoutRes, container, false)
        containerView = view
        onViewCreated(view)
        return view
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
    }

    protected fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.title != null) {
                return
            }
            parentController = parentController.parentController
        }

        val title = title
        val actionBar = actionBar
        if (title != null && actionBar != null) {
            actionBar.title = title
        }
    }

    override fun onDestroyView(view: View) {
        clearFindViewByIdCache()
        containerView = null
        super.onDestroyView(view)
    }

    protected abstract fun onViewCreated(view: View)
}
