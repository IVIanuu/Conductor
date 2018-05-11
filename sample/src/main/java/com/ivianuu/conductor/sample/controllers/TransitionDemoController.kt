package com.ivianuu.conductor.sample.controllers

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.FadeChangeHandler
import com.ivianuu.conductor.changehandler.HorizontalChangeHandler
import com.ivianuu.conductor.changehandler.VerticalChangeHandler
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.changehandler.ArcFadeMoveChangeHandlerCompat
import com.ivianuu.conductor.sample.changehandler.CircularRevealChangeHandlerCompat
import com.ivianuu.conductor.sample.changehandler.FlipChangeHandler
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.BundleBuilder
import kotlinx.android.synthetic.main.controller_transition_demo.*

class TransitionDemoController(args: Bundle) : BaseController(args) {

    private val transitionDemo: TransitionDemo

    override val title: String?
        get() = "Transition Demos"

    enum class TransitionDemo(val title: String, val layoutId: Int, val colorId: Int) {
        VERTICAL(
            "Vertical Slide Animation",
            R.layout.controller_transition_demo,
            R.color.blue_grey_300
        ),
        CIRCULAR(
            "Circular Reveal Animation (on Lollipop and above, else Fade)",
            R.layout.controller_transition_demo,
            R.color.red_300
        ),
        FADE("Fade Animation", R.layout.controller_transition_demo, R.color.blue_300),
        FLIP("Flip Animation", R.layout.controller_transition_demo, R.color.deep_orange_300),
        HORIZONTAL(
            "Horizontal Slide Animation",
            R.layout.controller_transition_demo,
            R.color.green_300
        ),
        ARC_FADE(
            "Arc/Fade Shared Element Transition (on Lollipop and above, else Fade)",
            R.layout.controller_transition_demo_shared,
            0
        ),
        ARC_FADE_RESET(
            "Arc/Fade Shared Element Transition (on Lollipop and above, else Fade)",
            R.layout.controller_transition_demo,
            R.color.pink_300
        );


        companion object {

            fun fromIndex(index: Int): TransitionDemo {
                return TransitionDemo.values()[index]
            }
        }
    }

    constructor(index: Int) : this(
        BundleBuilder(Bundle())
            .putInt(KEY_INDEX, index)
            .build()
    ) {
    }

    init {
        transitionDemo = TransitionDemo.fromIndex(args.getInt(KEY_INDEX))
    }

    override val layoutRes: Int
        get() = transitionDemo.layoutId

    override fun onViewCreated(view: View) {
        if (transitionDemo.colorId != 0 && bg_view != null) {
            bg_view!!.setBackgroundColor(ContextCompat.getColor(activity!!, transitionDemo.colorId))
        }

        val nextIndex = transitionDemo.ordinal + 1
        var buttonColor = 0
        if (nextIndex < TransitionDemo.values().size) {
            buttonColor = TransitionDemo.fromIndex(nextIndex).colorId
        }
        if (buttonColor == 0) {
            buttonColor = TransitionDemo.fromIndex(0).colorId
        }

        btn_next!!.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                activity!!,
                buttonColor
            )
        )
        tv_title!!.text = transitionDemo.title

        btn_next.setOnClickListener {
            val nextIndex = transitionDemo.ordinal + 1

            if (nextIndex < TransitionDemo.values().size) {
                router!!.pushController(getRouterTransaction(nextIndex, this))
            } else {
                router!!.popToRoot()
            }

        }
    }

    fun getChangeHandler(from: Controller): ControllerChangeHandler {
        when (transitionDemo) {
            TransitionDemoController.TransitionDemo.VERTICAL -> return VerticalChangeHandler()
            TransitionDemoController.TransitionDemo.CIRCULAR -> {
                val demoController = from as TransitionDemoController
                return CircularRevealChangeHandlerCompat(
                    demoController.btn_next!!,
                    demoController.containerView!!
                )
            }
            TransitionDemoController.TransitionDemo.FADE -> return FadeChangeHandler()
            TransitionDemoController.TransitionDemo.FLIP -> return FlipChangeHandler()
            TransitionDemoController.TransitionDemo.ARC_FADE -> return ArcFadeMoveChangeHandlerCompat(
                from.resources!!.getString(R.string.transition_tag_dot),
                from.resources!!.getString(R.string.transition_tag_title)
            )
            TransitionDemoController.TransitionDemo.ARC_FADE_RESET -> return ArcFadeMoveChangeHandlerCompat(
                from.resources!!.getString(R.string.transition_tag_dot),
                from.resources!!.getString(R.string.transition_tag_title)
            )
            TransitionDemoController.TransitionDemo.HORIZONTAL -> return HorizontalChangeHandler()
            else -> throw IllegalStateException()
        }
    }

    companion object {

        private const val KEY_INDEX = "TransitionDemoController.index"

        fun getRouterTransaction(index: Int, fromController: Controller): RouterTransaction {
            val toController = TransitionDemoController(index)

            return RouterTransaction.with(toController)
                .pushChangeHandler(toController.getChangeHandler(fromController))
                .popChangeHandler(toController.getChangeHandler(fromController))
        }
    }

}
