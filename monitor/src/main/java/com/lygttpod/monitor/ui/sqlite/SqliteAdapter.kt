package com.lygttpod.monitor.ui.sqlite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lygttpod.monitor.R
import com.lygttpod.monitor.data.DbData
import com.lygttpod.monitor.databinding.ItemSpFileBinding

class SqliteAdapter : RecyclerView.Adapter<SqliteAdapter.SqliteFileViewHolder>() {

    private var list: List<DbData> = listOf()

    var onItemClick: ((DbData) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SqliteFileViewHolder {
        return SqliteFileViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_sp_file, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SqliteFileViewHolder, position: Int) {
        holder.bindData(list[position])
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(list[position])
        }
    }

    override fun getItemCount() = list.size

    fun setData(list: List<DbData>?) {
        this.list = list ?: listOf()
        notifyDataSetChanged()
    }

    inner class SqliteFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemSpFileBinding.bind(view)

        fun bindData(data: DbData) {
            binding.tvSpFileName.text = data.name
        }
    }
}