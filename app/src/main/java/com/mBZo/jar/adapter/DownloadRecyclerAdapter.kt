package com.mBZo.jar.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mBZo.jar.DownloadActivity
import com.mBZo.jar.R
import com.mBZo.jar.store.installJar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import zlc.season.downloadx.State
import zlc.season.downloadx.core.DownloadTask
import zlc.season.downloadx.download
import zlc.season.downloadx.utils.formatSize
import java.io.File


class DownloadRecyclerAdapter(
    private val activity: DownloadActivity,
    private val fileList: MutableList<File>
) :
    RecyclerView.Adapter<DownloadRecyclerAdapter.ViewHolder>() {
    override fun getItemCount(): Int = fileList.size

    @OptIn(DelicateCoroutinesApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {  }
        //文件名
        holder.taskName.text = fileList[position].name
        //判断是否已下载
        val downloadedFilePath = activity.filesDir.absolutePath+"/Download/"+fileList[position].name
        if (File(downloadedFilePath).exists()){
            viewSucceed(holder,position)
        }
        else{
            //下载监听
            val downloadTask = GlobalScope.download(fileList[position].readText(),fileList[position].name,activity.filesDir.absolutePath+"/Download/")
            downloadTask.state()
                .onEach {
                    if (downloadTask.isSucceed()){
                        viewSucceed(holder,position)
                    }
                    else if (downloadTask.isStarted()){
                        if (it.progress.totalSize!=(0).toLong()){
                            viewInDownloading(downloadTask,holder,position,it)
                        }
                    }
                    else{
                        viewFailed(downloadTask,holder,position)
                    }
                }.launchIn(GlobalScope)
            downloadTask.start()
        }

        //onBindViewHolder
    }

    private fun viewFailed(downloadTask: DownloadTask, holder: ViewHolder, position: Int) {
        activity.runOnUiThread {
            holder.chipOpen.text = "重试"
            holder.chipDel.text = "删除"
            holder.chipOpen.setOnClickListener {
                downloadTask.start()
            }
            holder.chipDel.setOnClickListener {
                notifyItemRemoved(position)
                if (fileList[position].exists()){
                    fileList[position].delete()
                }
                fileList.remove(fileList[position])
                downloadTask.remove()
                notifyItemRangeChanged(position,itemCount)
            }
            holder.loading.visibility = View.GONE
            holder.chipOpen.visibility = View.VISIBLE
            holder.chipDel.visibility = View.VISIBLE
        }
    }

    private fun viewInDownloading(
        downloadTask: DownloadTask,
        holder: ViewHolder,
        position: Int,
        state: State
    ) {
        activity.runOnUiThread {
            holder.chipDel.text = "取消"
            holder.chipDel.setOnClickListener {
                notifyItemRemoved(position)
                if (fileList[position].exists()){
                    fileList[position].delete()
                }
                fileList.remove(fileList[position])
                downloadTask.remove()
                notifyItemRangeChanged(position,itemCount)
            }
            holder.loading.isIndeterminate = false
            holder.loading.progress = state.progress.percent().toInt()
            holder.chipDel.visibility = View.VISIBLE
        }
    }

    private fun viewSucceed(holder: ViewHolder, position: Int) {
        activity.runOnUiThread {
            holder.chipOpen.text = "安装"
            holder.chipOpen.setOnClickListener{
                installJar(activity,File(activity.filesDir.absolutePath+"/Download/"+fileList[position].name))
            }
            @SuppressLint("SetTextI18n")
            holder.chipDel.text = "删除(${File(activity.filesDir.absolutePath+"/Download/"+fileList[position].name).length().formatSize()})"
            holder.chipDel.setOnClickListener {
                notifyItemRemoved(position)
                val downloadedFile = File(activity.filesDir.absolutePath+"/Download/"+fileList[position].name)
                if (fileList[position].exists()){
                    fileList[position].delete()
                }
                fileList.remove(fileList[position])
                if (downloadedFile.exists()){
                    downloadedFile.delete()
                }
                notifyItemRangeChanged(position,itemCount)
            }
            holder.loading.visibility = View.GONE
            holder.chipOpen.visibility = View.VISIBLE
            holder.chipDel.visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
            = ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false))
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskName: TextView = view.findViewById(R.id.name)
        val loading: ProgressBar = view.findViewById(R.id.loading)
        val chipOpen: Chip = view.findViewById(R.id.chipOpen)
        val chipDel: Chip = view.findViewById(R.id.chipDel)
    }
}