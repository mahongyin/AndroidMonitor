package com.lygttpod.monitor.ui.sqlite

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.lygttpod.monitor.MonitorHelper
import com.lygttpod.monitor.R
import com.lygttpod.monitor.databinding.FragmentSpListBinding


class SqliteFileListFragment : Fragment() {

    private lateinit var binding: FragmentSpListBinding
    private var adapter: SqliteAdapter? = null
    private var handle: Handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sp_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSpListBinding.bind(view)
        initView()
        initData()
    }

    private fun initData() {
        val list = MonitorHelper.getSqliteFilesData()
        adapter?.setData(list)
    }

    private fun initView() {
        binding.swipeRefresh.setOnRefreshListener {
            handle.postDelayed({
                binding.swipeRefresh.isRefreshing = false
                initData()
            }, 1000)
        }
        adapter = SqliteAdapter()
        adapter?.onItemClick = {
            startActivity(TbListActivity.buildIntent(requireContext(), it.path))
        }
        binding.recyclerVew.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
    }
}