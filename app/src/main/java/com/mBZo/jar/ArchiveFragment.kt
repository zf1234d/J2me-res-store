package com.mBZo.jar

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mBZo.jar.adapter.ArchiveRecyclerAdapter
import com.mBZo.jar.databinding.FragmentArchiveBinding
import com.mBZo.jar.tool.ArchiveItem
import com.mBZo.jar.tool.FileLazy
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ArchiveFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ArchiveFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    private var _binding: FragmentArchiveBinding? = null
    private val binding: FragmentArchiveBinding get() = _binding!!
    private val activity: Activity get() = requireActivity()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.archiveToolbar.inflateMenu(R.menu.archive_toolbar_menu)
        val searchView: SearchView = binding.archiveToolbar.menu.findItem(R.id.toolbar_search).actionView as SearchView
        //加载库存
        loadArchive()
        val gameList = (binding.recyclerArchive.adapter as ArchiveRecyclerAdapter).getList()
        //搜索功能
        val filterList = arrayListOf<ArchiveItem>()
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList.clear()
                for (i in gameList) {
                    if (newText!=null && i.name.contains(newText)){
                        filterList.add(i)
                    }
                }
                val adapter = ArchiveRecyclerAdapter(activity,filterList)
                binding.recyclerArchive.adapter = adapter
                binding.recyclerArchive.adapter?.notifyDataSetChanged()
                return true
            }
        })
        searchView.addOnAttachStateChangeListener(object :View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(v: View) {
                binding.archiveAppbar.setExpanded(false)
                binding.recyclerArchive.isNestedScrollingEnabled = false
            }

            override fun onViewDetachedFromWindow(v: View) {
                searchView.onActionViewCollapsed()
                binding.recyclerArchive.isNestedScrollingEnabled = true
            }
        })
    }

    fun loadArchive(){
        try {
            val list = arrayListOf<ArchiveItem>()
            if (File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").exists().not()){
                FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").writeNew()
            }
            val lines = FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").readLines()
            var name = "";var nameFC = "";var from = ""
            for (str in lines){
                if (str.contains("\"name\"")){
                    name=str.substringAfter("\"name\":\"").substringBefore("\"")
                }
                else if (str.contains("\"nameFc\"")){
                    nameFC=str.substringAfter("\"nameFc\":\"").substringBefore("\"")
                }
                else if (str.contains("\"from\"")){
                    from=str.substringAfter("\"from\":\"").substringBefore("\"")
                }
                else if (str.contains("\"url\"")){
                    list.add(ArchiveItem(name,nameFC,from,str.substringAfter("\"url\":\"").substringBefore("\"")))
                }
            }
            binding.recyclerArchive.layoutManager = LinearLayoutManager(activity)
            binding.recyclerArchive.adapter = ArchiveRecyclerAdapter(activity, list)
            val searchView = binding.archiveToolbar.menu.findItem(R.id.toolbar_search).actionView as SearchView
            searchView.queryHint = "在${list.size}个项目中搜索"
        }catch (e:Exception){}
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ArchiveFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ArchiveFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

