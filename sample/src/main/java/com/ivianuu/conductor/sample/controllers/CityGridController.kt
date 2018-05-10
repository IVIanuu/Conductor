package com.ivianuu.conductor.sample.controllers

import android.graphics.PorterDuff.Mode
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.FadeChangeHandler
import com.ivianuu.conductor.changehandler.TransitionChangeHandlerCompat
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.changehandler.CityGridSharedElementTransitionChangeHandler
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.BundleBuilder
import com.ivianuu.conductor.sample.util.KtViewHolder
import kotlinx.android.synthetic.main.controller_city_grid.*
import kotlinx.android.synthetic.main.row_city_grid.view.*
import kotlinx.android.synthetic.main.row_city_header.*

import java.util.ArrayList

class CityGridController(args: Bundle) : BaseController(args) {

    override val title = args.getString(KEY_TITLE)
    private val dotColor = args.getInt(KEY_DOT_COLOR)
    private val fromPosition = args.getInt(KEY_FROM_POSITION)

    override val layoutRes: Int
        get() = R.layout.controller_city_grid

    constructor(title: String, dotColor: Int, fromPosition: Int) : this(
        BundleBuilder(Bundle())
            .putString(KEY_TITLE, title)
            .putInt(KEY_DOT_COLOR, dotColor)
            .putInt(KEY_FROM_POSITION, fromPosition)
            .build()
    )

    override fun onViewCreated(view: View) {
        tv_title.text = title
        img_dot.drawable.setColorFilter(
            ContextCompat.getColor(activity!!, dotColor),
            Mode.SRC_ATOP
        )

        ViewCompat.setTransitionName(
            tv_title,
            resources!!.getString(R.string.transition_tag_title_indexed, fromPosition)
        )
        ViewCompat.setTransitionName(
            img_dot,
            resources!!.getString(R.string.transition_tag_dot_indexed, fromPosition)
        )

        recycler_view.setHasFixedSize(true)
        recycler_view.layoutManager = GridLayoutManager(view.context, 2)
        recycler_view.adapter = CityGridAdapter(LayoutInflater.from(view.context), CITY_MODELS)
    }

    internal fun onModelRowClick(model: CityModel?) {
        val imageTransitionName =
            resources!!.getString(R.string.transition_tag_image_named, model!!.title)
        val titleTransitionName =
            resources!!.getString(R.string.transition_tag_title_named, model.title)

        val names = ArrayList<String>()
        names.add(imageTransitionName)
        names.add(titleTransitionName)

        router!!.pushController(
            RouterTransaction.with(CityDetailController(model.drawableRes, model.title))
                .pushChangeHandler(
                    TransitionChangeHandlerCompat(
                        CityGridSharedElementTransitionChangeHandler(names),
                        FadeChangeHandler()
                    )
                )
                .popChangeHandler(
                    TransitionChangeHandlerCompat(
                        CityGridSharedElementTransitionChangeHandler(names),
                        FadeChangeHandler()
                    )
                )
        )
    }

    internal inner class CityGridAdapter(
        private val inflater: LayoutInflater,
        private val items: Array<CityModel>
    ) : RecyclerView.Adapter<CityGridAdapter.ViewHolder>() {

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_city_grid, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        internal inner class ViewHolder(itemView: View) : KtViewHolder(itemView) {

            private var model: CityModel? = null

            fun bind(item: CityModel) {
                with(itemView) {
                    row_root.setOnClickListener { onModelRowClick(model) }
                    model = item
                    img_city.setImageResource(item.drawableRes)
                    tv_title.text = item.title

                    ViewCompat.setTransitionName(
                        tv_title,
                        resources!!.getString(R.string.transition_tag_title_named, model!!.title)
                    )
                   ViewCompat.setTransitionName(
                        img_city,
                        resources!!.getString(R.string.transition_tag_image_named, model!!.title)
                    )
                }
            }

        }
    }

    data class CityModel(var drawableRes: Int, var title: String)

    companion object {

        private const val KEY_TITLE = "CityGridController.title"
        private const val KEY_DOT_COLOR = "CityGridController.dotColor"
        private const val KEY_FROM_POSITION = "CityGridController.position"

        private val CITY_MODELS = arrayOf(
            CityModel(R.drawable.chicago, "Chicago"),
            CityModel(R.drawable.jakarta, "Jakarta"),
            CityModel(R.drawable.london, "London"),
            CityModel(R.drawable.sao_paulo, "Sao Paulo"),
            CityModel(R.drawable.tokyo, "Tokyo")
        )
    }
}
