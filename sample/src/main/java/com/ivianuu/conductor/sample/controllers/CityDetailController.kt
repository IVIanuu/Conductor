package com.ivianuu.conductor.sample.controllers

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.KtViewHolder
import kotlinx.android.synthetic.main.controller_city_detail.*
import kotlinx.android.synthetic.main.controller_target_display.*
import kotlinx.android.synthetic.main.row_city_detail.*

class CityDetailController(args: Bundle) : BaseController(args) {

    override val layoutRes: Int
        get() = R.layout.controller_city_detail

    private val imageDrawableRes = args.getInt(KEY_IMAGE)
    override val title = args.getString(KEY_TITLE)

    constructor(imageDrawableRes: Int, title: String) : this(
        bundleOf(
            KEY_IMAGE to imageDrawableRes,
            KEY_TITLE to title
        )
    )

    override fun onViewCreated(view: View) {
        with(recycler_view) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = CityDetailAdapter(
                LayoutInflater.from(view.context),
                title!!,
                imageDrawableRes,
                LIST_ROWS,
                title
            )
        }
    }

    internal class CityDetailAdapter(
        private val inflater: LayoutInflater,
        private val title: String, private val imageDrawableRes: Int,
        private val details: Array<String>,
        transitionNameBase: String
    ) : RecyclerView.Adapter<KtViewHolder>() {
        private val imageViewTransitionName = inflater.context.resources.getString(
            R.string.transition_tag_image_named,
            transitionNameBase
        )
        private val textViewTransitionName = inflater.context.resources.getString(
            R.string.transition_tag_title_named,
            transitionNameBase
        )

        override fun getItemCount() = 1 + details.size

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                VIEW_TYPE_HEADER
            } else {
                VIEW_TYPE_DETAIL
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KtViewHolder {
            return if (viewType == VIEW_TYPE_HEADER) {
                HeaderViewHolder(inflater.inflate(R.layout.row_city_header, parent, false))
            } else {
                DetailViewHolder(inflater.inflate(R.layout.row_city_detail, parent, false))
            }
        }

        override fun onBindViewHolder(holder: KtViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_HEADER) {
                (holder as HeaderViewHolder).bind(
                    imageDrawableRes,
                    title,
                    imageViewTransitionName,
                    textViewTransitionName
                )
            } else {
                (holder as DetailViewHolder).bind(details[position - 1])
            }
        }

        internal class HeaderViewHolder(itemView: View) : KtViewHolder(itemView) {

            fun bind(
                imageDrawableRes: Int, title: String,
                imageTransitionName: String,
                textViewTransitionName: String
            ) {
                image_view.setImageResource(imageDrawableRes)
                text_view.text = title

                ViewCompat.setTransitionName(image_view, imageTransitionName)
                ViewCompat.setTransitionName(text_view, textViewTransitionName)
            }
        }

        internal class DetailViewHolder(itemView: View) : KtViewHolder(itemView) {
            fun bind(detail: String) {
                text_view.text = detail
            }
        }

        companion object {

            private const val VIEW_TYPE_HEADER = 0
            private const val VIEW_TYPE_DETAIL = 1
        }
    }

    companion object {

        private const val KEY_TITLE = "CityDetailController.title"
        private const val KEY_IMAGE = "CityDetailController.image"

        private val LIST_ROWS = arrayOf(
            "• This is a city.",
            "• There's some cool stuff about it.",
            "• But really this is just a demo, not a city guide app.",
            "• This demo is meant to show some nice transitions, as long as you're on Lollipop or later.",
            "• You should have seen some sweet shared element transitions using the ImageView and the TextView in the \"header\" above.",
            "• This transition utilized some callbacks to ensure all the necessary rows in the recycler_view were laid about before the transition occurred.",
            "• Just adding some more lines so it scrolls now...\n\n\n\n\n\n\nThe end."
        )
    }
}
