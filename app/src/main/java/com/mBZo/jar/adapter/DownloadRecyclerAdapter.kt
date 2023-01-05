package com.mBZo.jar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mBZo.jar.DownloadActivity
import com.mBZo.jar.R
import com.mBZo.jar.store.installJar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import zlc.season.downloadx.State
import zlc.season.downloadx.download
import java.io.File


class DownloadRecyclerAdapter(
    private val activity: DownloadActivity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val fileList: MutableList<File>
) :
    RecyclerView.Adapter<DownloadRecyclerAdapter.ViewHolder>() {
    override fun getItemCount(): Int = fileList.size

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
            val downloadTask = lifecycleScope.download(fileList[position].readText(),fileList[position].name,activity.filesDir.absolutePath+"/Download/")
            downloadTask.state()
                .onEach {
                    if (downloadTask.isSucceed()){
                        viewSucceed(holder,position)
                    }
                    else if (downloadTask.isStarted()){
                        if (it.progress.totalSize!=(0).toLong()){
                            viewInDownloading(holder,it)
                        }
                    }
                    else if (downloadTask.isFailed()){
                        viewFailed(holder,position)
                    }

                }.launchIn(lifecycleScope)
            downloadTask.start()
        }

        //onBindViewHolder
    }

    private fun viewFailed(holder: ViewHolder, position: Int) {
        activity.runOnUiThread {
            holder.chipOpen.text = "重试"
            holder.chipDel.text = "删除"
            holder.chipDel.setOnClickListener {
                notifyItemRemoved(position)
                val baseDownloadPath = activity.filesDir.absolutePath+"/Download/"+fileList[position].name
                if (fileList[position].exists()){
                    fileList[position].delete()
                }
                fileList.remove(fileList[position])
                //占位文件
                if (File("$baseDownloadPath.download").exists()){
                    File("$baseDownloadPath.download").delete()
                }
                //下载器缓存
                if (File("$baseDownloadPath.tmp").exists()){
                    File("$baseDownloadPath.tmp").delete()
                }
                notifyItemChanged(position)
            }
            holder.loading.visibility = View.GONE
            holder.chipOpen.visibility = View.VISIBLE
            holder.chipDel.visibility = View.VISIBLE
        }
    }

    private fun viewInDownloading(holder: ViewHolder, state: State) {
        activity.runOnUiThread {
            holder.loading.isIndeterminate = false
            holder.loading.progress = state.progress.percent().toInt()
        }
    }

    private fun viewSucceed(holder: ViewHolder, position: Int) {
        activity.runOnUiThread {
            holder.chipOpen.text = "安装"
            holder.chipOpen.setOnClickListener{
                installJar(activity,File(activity.filesDir.absolutePath+"/Download/"+fileList[position].name))
            }
            holder.chipDel.text = "删除"
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