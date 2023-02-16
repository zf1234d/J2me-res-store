package com.mBZo.jar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mBZo.jar.R
import com.mBZo.jar.tool.imageLoad
import com.stfalcon.imageviewer.StfalconImageViewer

class ImgShowRecyclerAdapter(private val imgUrlList: List<String>) :
    RecyclerView.Adapter<ImgShowRecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = imgUrlList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val iconLink = imgUrlList[position]
        //显示
        imageLoad(holder.itemView as ImageView,iconLink)
        //点击
        holder.itemView.setOnClickListener {
            StfalconImageViewer.Builder(holder.itemView.context, imgUrlList) { view, image -> imageLoad(view,image) }
                .withStartPosition(position)
                .withTransitionFrom(holder.itemView as ImageView)
                .show()
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}