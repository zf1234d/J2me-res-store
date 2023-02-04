package com.mBZo.jar.adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mBZo.jar.R
import com.mBZo.jar.StoreActivity

class ArchiveRecyclerAdapter(private val activity: Activity?, private val nameList: List<String>, private  val fromList: List<String>, private val pathList: List<String>) :
    RecyclerView.Adapter<ArchiveRecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_archive, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = nameList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemName = nameList[position]
        val itemFrom = fromList[position]
        //显示
        holder.name.text = itemName
        holder.from.text = itemFrom
        //点击
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StoreActivity::class.java)
            intent.putExtra("name",nameList[position])
            intent.putExtra("from",fromList[position])
            intent.putExtra("path",pathList[position])
            activity?.startActivity(intent)
        }
    }


    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.archiveItemName)
        val from: TextView = itemView.findViewById(R.id.archiveItemFrom)
    }
}