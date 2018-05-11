package com.ivianuu.conductor.internal

import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object TransitionUtils {

    fun findNamedViews(namedViews: MutableMap<String, View>, view: View) {
        if (view.visibility == View.VISIBLE) {
            val transitionName = view.transitionName
            if (transitionName != null) {
                namedViews[transitionName] = view
            }

            if (view is ViewGroup) {
                val childCount = view.childCount
                for (i in 0 until childCount) {
                    val child = view.getChildAt(i)
                    findNamedViews(namedViews, child)
                }
            }
        }
    }

    fun findNamedView(view: View, transitionName: String): View? {
        if (transitionName == view.transitionName) {
            return view
        }

        if (view is ViewGroup) {
            val childCount = view.childCount
            for (i in 0 until childCount) {
                val viewWithTransitionName = findNamedView(view.getChildAt(i), transitionName)
                if (viewWithTransitionName != null) {
                    return viewWithTransitionName
                }
            }
        }

        return null
    }

    fun setEpicenter(transition: Transition, view: View) {
        val epicenter = Rect()
        getBoundsOnScreen(view, epicenter)
        transition.epicenterCallback = object : Transition.EpicenterCallback() {
            override fun onGetEpicenter(transition: Transition): Rect {
                return epicenter
            }
        }
    }

    fun getBoundsOnScreen(view: View, epicenter: Rect) {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        epicenter.set(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    fun setTargets(transition: Transition, nonExistentView: View, sharedViews: MutableList<View>) {
        val views = transition.targets
        views.clear()
        val count = sharedViews.size
        for (i in 0 until count) {
            val view = sharedViews[i]
            bfsAddViewChildren(views, view)
        }
        views.add(nonExistentView)
        sharedViews.add(nonExistentView)
        addTargets(transition, sharedViews)
    }

    fun addTargets(transition: Transition, views: List<View>) {
        if (transition is TransitionSet) {
            val set = transition as TransitionSet?
            val numTransitions = set!!.transitionCount
            for (i in 0 until numTransitions) {
                val child = set.getTransitionAt(i)
                addTargets(child, views)
            }
        } else if (!hasSimpleTarget(transition)) {
            val targets = transition.targets
            if (isNullOrEmpty(targets)) {
                val numViews = views.size
                for (i in 0 until numViews) {
                    transition.addTarget(views[i])
                }
            }
        }
    }

    fun replaceTargets(transition: Transition, oldTargets: List<View>, newTargets: List<View>) {
        if (transition is TransitionSet) {
            val numTransitions = transition.transitionCount
            for (i in 0 until numTransitions) {
                val child = transition.getTransitionAt(i)
                replaceTargets(child, oldTargets, newTargets)
            }
        } else if (!TransitionUtils.hasSimpleTarget(transition)) {
            val targets = transition.targets
            if (targets != null && targets.size == oldTargets.size && targets.containsAll(oldTargets)) {
                val targetCount = newTargets.size
                for (i in 0 until targetCount) {
                    transition.addTarget(newTargets[i])
                }
                for (i in oldTargets.indices.reversed()) {
                    transition.removeTarget(oldTargets[i])
                }
            }
        }
    }

    private fun bfsAddViewChildren(views: MutableList<View>, startView: View) {
        val startIndex = views.size
        if (containedBeforeIndex(views, startView, startIndex)) {
            return  // This child is already in the list, so all its children are also.
        }
        views.add(startView)
        for (index in startIndex until views.size) {
            val view = views[index]
            if (view is ViewGroup) {
                val childCount = view.childCount
                for (childIndex in 0 until childCount) {
                    val child = view.getChildAt(childIndex)
                    if (!containedBeforeIndex(views, child, startIndex)) {
                        views.add(child)
                    }
                }
            }
        }
    }

    private fun containedBeforeIndex(views: List<View>, view: View, maxIndex: Int): Boolean {
        for (i in 0 until maxIndex) {
            if (views[i] == view) {
                return true
            }
        }
        return false
    }

    private fun hasSimpleTarget(transition: Transition): Boolean {
        return (!isNullOrEmpty(transition.targetIds)
                || !isNullOrEmpty(transition.targetNames)
                || !isNullOrEmpty(transition.targetTypes))
    }

    private fun isNullOrEmpty(list: List<*>?): Boolean {
        return list == null || list.isEmpty()
    }

    fun mergeTransitions(ordering: Int, vararg transitions: Transition?): TransitionSet {
        val transitionSet = TransitionSet()
        for (transition in transitions) {
            if (transition != null) {
                transitionSet.addTransition(transition)
            }
        }
        transitionSet.ordering = ordering
        return transitionSet
    }

}
