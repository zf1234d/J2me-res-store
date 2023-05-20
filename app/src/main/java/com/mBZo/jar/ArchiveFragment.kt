package com.mBZo.jar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.mBZo.jar.adapter.ArchiveRecyclerAdapter

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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val filterList = mutableListOf<Array<String>>()
        val appbar: AppBarLayout = view.findViewById(R.id.archive_appbar)
        val toolbar: Toolbar = view.findViewById(R.id.archive_toolbar)
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_archive)
        toolbar.inflateMenu(R.menu.archive_toolbar_menu)
        val searchItem = toolbar.menu.findItem(R.id.toolbar_search)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
            @SuppressLint("NotifyDataSetChanged")
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList.clear()
                for (i in 1..gameList.size) {
                    if (newText!=null && gameList[i-1][0].contains(newText)){
                        filterList.add(gameList[i-1])
                    }
                }
                val adapter = ArchiveRecyclerAdapter(activity,filterList)
                recyclerView.adapter = adapter
                recyclerView.adapter?.notifyDataSetChanged()
                return true
            }
        })
        searchView.addOnAttachStateChangeListener(object :View.OnAttachStateChangeListener{
            override fun onViewAttachedToWindow(v: View) {
                appbar.setExpanded(false)
                recyclerView.isNestedScrollingEnabled = false
            }

            override fun onViewDetachedFromWindow(v: View) {
                searchView.onActionViewCollapsed()
                recyclerView.isNestedScrollingEnabled = true
            }
        })
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

