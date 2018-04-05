package com.lkdont.soundeditor.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.lkdont.soundeditor.R
import com.lkdont.soundeditor.widget.OnItemClickListener
import kotlinx.android.synthetic.main.examples_list_frag.*
import org.greenrobot.eventbus.EventBus

/**
 * Examples List Page
 *
 * Created by kidonliang on 2018/3/4.
 */
class ExamplesListFrag : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.examples_list_frag, container, false)
        return view
    }

    private val examples = arrayOf(
            "Resample",
            "Decode"
    )

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        content_rv.layoutManager = LinearLayoutManager(context)
        val adapter = ExamplesAdapter(context, examples)
        content_rv.adapter = adapter
        adapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(v: View, position: Int) {
                when (examples[position]) {
                    "Resample" -> EventBus.getDefault().post(MainAct.FragmentEvent(ResampleFrag()))
                    "Decode" -> EventBus.getDefault().post(MainAct.FragmentEvent(DecodeFrag()))
//                    2 -> EventBus.getDefault().post(MainAct.FragmentEvent(RecordFrag()))
                }
            }
        })
    }

    private class ExampleItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView = itemView.findViewById(R.id.title_tv)
    }

    private class ExamplesAdapter(context: Context, val examples: Array<String>)
        : RecyclerView.Adapter<ExampleItemViewHolder>() {

        private val mInflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ExampleItemViewHolder {
            val itemView = mInflater.inflate(R.layout.example_item, parent, false)
            return ExampleItemViewHolder(itemView)
        }

        override fun getItemCount(): Int {
            return examples.size
        }

        override fun onBindViewHolder(holder: ExampleItemViewHolder?, position: Int) {
            holder?.titleTv?.text = examples[position]
            holder?.itemView?.setOnClickListener {
                mOnItemClickListener?.onItemClick(holder.itemView, position)
            }
        }

        private var mOnItemClickListener: OnItemClickListener? = null

        fun setOnItemClickListener(listener: OnItemClickListener?) {
            mOnItemClickListener = listener
        }

    }

}