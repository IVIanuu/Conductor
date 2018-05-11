package com.ivianuu.conductor.sample.controllers

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivianuu.conductor.RouterTransaction
import com.ivianuu.conductor.changehandler.HorizontalChangeHandler
import com.ivianuu.conductor.sample.R
import com.ivianuu.conductor.sample.controllers.base.BaseController
import com.ivianuu.conductor.sample.util.KtViewHolder
import kotlinx.android.synthetic.main.controller_master_detail_list.*
import kotlinx.android.synthetic.main.row_home.*

class MasterDetailListController : BaseController() {

    private var selectedIndex: Int = 0
    private var twoPaneView= false

    override val title: String?
        get() = "Master/Detail Flow"

    override val layoutRes = R.layout.controller_master_detail_list

    enum class DetailItemModel private constructor(
        internal var title: String,
        internal var detail: String,
        internal var backgroundColor: Int
    ) {
        ONE(
            "Item 1",
            "This is a quick demo of master/detail flow using Conductor. In portrait mode you'll see a standard list. In landscape, you'll see a two-pane layout.",
            R.color.green_300
        ),
        TWO("Item 2", "This is another item.", R.color.cyan_300),
        THREE("Item 3", "Wow, a 3rd item!", R.color.deep_purple_300)
    }

    override fun onViewCreated(view: View) {
        (recycler_view as RecyclerView).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter =  DetailItemAdapter(
                LayoutInflater.from(view.context),
                DetailItemModel.values()
            )
        }

        twoPaneView = detail_container != null
        if (twoPaneView) {
            onRowSelected(selectedIndex)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_SELECTED_INDEX, selectedIndex)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        selectedIndex = savedInstanceState.getInt(KEY_SELECTED_INDEX)
    }

    internal fun onRowSelected(index: Int) {
        selectedIndex = index

        val model = DetailItemModel.values()[index]
        val controller = ChildController(model.detail, model.backgroundColor, true)

        if (twoPaneView) {
            getChildRouter(detail_container!!).setRoot(RouterTransaction.with(controller))
        } else {
            requireRouter().pushController(
                RouterTransaction.with(controller)
                    .pushChangeHandler(HorizontalChangeHandler())
                    .popChangeHandler(HorizontalChangeHandler())
            )
        }
    }

    internal inner class DetailItemAdapter(
        private val inflater: LayoutInflater,
        private val items: Array<DetailItemModel>
    ) : RecyclerView.Adapter<DetailItemAdapter.ViewHolder>() {

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.row_detail_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        internal inner class ViewHolder(itemView: View) : KtViewHolder(itemView) {

            fun bind(item: DetailItemModel, position: Int) {
                tv_title!!.text = item.title

                if (twoPaneView && position == selectedIndex) {
                    row_root!!.setBackgroundColor(
                        ContextCompat.getColor(
                            row_root!!.context,
                            R.color.grey_400
                        )
                    )
                } else {
                    row_root!!.setBackgroundColor(
                        ContextCompat.getColor(
                            row_root!!.context,
                            android.R.color.transparent
                        )
                    )
                }

                row_root.setOnClickListener {
                    onRowSelected(position)
                    notifyDataSetChanged()
                }
            }

        }
    }

    companion object {

        private const val KEY_SELECTED_INDEX = "MasterDetailListController.selectedIndex"
    }

}
