package com.mBZo.jar.adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mBZo.jar.R
import com.mBZo.jar.StoreActivity

class ArchiveRecyclerAdapter(private val activity: Activity?, private val list: MutableList<Array<String>>) :
    RecyclerView.Adapter<ArchiveRecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val nav: BottomNavigationView? = activity?.findViewById(R.id.home_nav)
        nav?.getOrCreateBadge(R.id.nav_search)?.number = itemCount
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_archive, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemName = list[position][0]
        val itemFrom = list[position][2]
        //显示
        holder.name.text = itemName
        holder.from.text = itemFrom
        //点击
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StoreActivity::class.java)
            intent.putExtra("name",list[position][0])
            intent.putExtra("from",list[position][2])
            intent.putExtra("path",list[position][3])
            activity?.startActivity(intent)
        }
    }


    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.archiveItemName)
        val from: TextView = itemView.findViewById(R.id.archiveItemFrom)
    }
}