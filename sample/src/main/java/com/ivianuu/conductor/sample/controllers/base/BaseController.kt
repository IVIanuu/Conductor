package com.ivianuu.conductor.sample.controllers.base

import android.arch.lifecycle.ViewModelStore
import android.arch.lifecycle.ViewModelStoreOwner
import android.content.Context
import android.os.Bundle
import android.support.v7.app.ActionBar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.sample.MainActivity
import com.ivianuu.conductor.sample.d
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController : Controller, LayoutContainer, ViewModelStoreOwner {

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

    private val vmStore = ViewModelStore()

    protected constructor() {}

    protected constructor(args: Bundle) : super(args) {}

    init {
        d { "init" }
    }

    override fun onCreate() {
        super.onCreate()
        d { "on create" }
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        d { "on context available" }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        d { "on create view" }
        val view = inflater.inflate(layoutRes, container, false)
        containerView = view
        onViewCreated(view)
        return view
    }

    override fun onAttach(view: View) {
        setTitle()
        super.onAttach(view)
        d { "on attach" }
    }

    override fun onDetach(view: View) {
        d { "on detach" }
        super.onDetach(view)
    }

    override fun onDestroy() {
        vmStore.clear()
        super.onDestroy()
        d { "on destroy" }
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
        d { "on destroy view" }
        clearFindViewByIdCache()
        containerView = null
        super.onDestroyView(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        d { "on save instance state" }
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        d { "on restore view state" }
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        d { "on save view state" }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        d { "on restore instance state" }
    }

    override fun onContextUnavailable() {
        super.onContextUnavailable()
        d { "on context unavailable" }
    }

    override fun getViewModelStore() = vmStore

    protected open fun onViewCreated(view: View) { }
}
