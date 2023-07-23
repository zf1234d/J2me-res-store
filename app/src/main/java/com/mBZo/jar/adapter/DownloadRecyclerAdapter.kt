package com.mBZo.jar.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mBZo.jar.BuildConfig
import com.mBZo.jar.DownloadActivity
import com.mBZo.jar.R
import com.mBZo.jar.tool.DownloadProgressListener
import com.mBZo.jar.tool.downloadFile
import com.mBZo.jar.tool.formatSize
import com.mBZo.jar.tool.installJar
import com.mBZo.jar.tool.isDestroy
import java.io.File
import java.util.*


class DownloadRecyclerAdapter(
    private val activity: DownloadActivity,
    private val fileList: MutableList<File>
) :
    RecyclerView.Adapter<DownloadRecyclerAdapter.ViewHolder>() {
    override fun getItemCount(): Int = fileList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener {  }
        //文件名
        holder.taskName.text = fileList[holder.adapterPosition].name
        //判断是否已下载
        val downloadedFilePath = activity.filesDir.absolutePath+"/Download/"+fileList[holder.adapterPosition].name
        if (File(downloadedFilePath.substringBeforeLast("/")).exists().not()){
            File(downloadedFilePath.substringBeforeLast("/")).mkdirs()
        }
        if (File(downloadedFilePath).exists()){
            viewSucceed(holder,holder.adapterPosition)
        }
        else{
            //下载监听
            Thread{
                downloadFile(fileList[position].readText(),activity.filesDir.absolutePath+"/Download/"+fileList[holder.adapterPosition].name,object :DownloadProgressListener{
                    override fun onProgress(totalBytes: Long, downloadedBytes: Long) {
                        viewInDownloading(holder,holder.adapterPosition,downloadedBytes,totalBytes)
                    }
                    override fun onSucceed() {
                        viewSucceed(holder,holder.adapterPosition)
                    }
                })
            }.start()
        }

        //onBindViewHolder
    }

//    private fun viewFailed(downloadTask: DownloadTask, holder: ViewHolder, position: Int) {
//        activity.runOnUiThread {
//            holder.chipOpen.text = "重试"
//            holder.chipDel.text = "删除"
//            holder.chipOpen.setOnClickListener {
//                downloadTask.start()
//            }
//            holder.chipDel.setOnClickListener {
//                notifyItemRemoved(position)
//                if (fileList[position].exists()){
//                    fileList[position].delete()
//                }
//                fileList.remove(fileList[position])
//                downloadTask.remove()
//                notifyItemRangeChanged(position,itemCount)
//            }
//            holder.loading.visibility = View.GONE
//            holder.chipOpen.visibility = View.VISIBLE
//            holder.chipShare.visibility = View.GONE
//            holder.chipDel.visibility = View.VISIBLE
//        }
//    }

    private fun viewInDownloading(
        holder: ViewHolder,
        position: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        activity.runOnUiThread {
            holder.chipOpen.visibility = View.GONE
//            holder.chipOpen.text = "取消"
//            holder.chipOpen.setOnClickListener {
//                notifyItemRemoved(position)
//                if (fileList[position].exists()){
//                    fileList[position].delete()
//                }
//                fileList.remove(fileList[position])
//                downloadTask.remove()
//                notifyItemRangeChanged(position,itemCount)
//            }
            @SuppressLint("SetTextI18n")
            holder.chipShare.text = "${downloadedBytes.formatSize()}/${totalBytes.formatSize()}"
            holder.chipShare.setOnClickListener {  }

            val progress = (downloadedBytes.toDouble() / totalBytes.toDouble() * 100).toInt()
            val progressStr = "$progress%"
            holder.chipDel.text = progressStr
            holder.chipDel.setOnClickListener {  }
            holder.loading.isIndeterminate = false
            holder.loading.progress = progress
            holder.chipOpen.visibility = View.VISIBLE
            holder.chipShare.visibility = View.VISIBLE
            holder.chipDel.visibility = View.VISIBLE
        }
    }

    private fun viewSucceed(holder: ViewHolder, position: Int) {
        activity.runOnUiThread {
            val fileDownloaded = File(activity.filesDir.absolutePath+"/Download/"+fileList[position].name)
            holder.chipOpen.text = "安装"
            holder.chipOpen.setOnClickListener{
                installJar(activity,fileDownloaded)
            }
            holder.chipShare.text = "分享"
            holder.chipShare.setOnClickListener {
                try {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID +".fileProvider",fileDownloaded)
                    } else {
                        Uri.fromFile(fileDownloaded)
                    }
                    ShareCompat.IntentBuilder(activity)
                        .setChooserTitle(fileList[position].name)
                        .setType("*/*")
                        .setStream(uri)
                        .startChooser()
                } catch (e: Exception) {
                    if (isDestroy(activity).not()){
                        activity.runOnUiThread {
                            Toast.makeText(activity,"未找到支持分享的软件", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
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
            holder.chipShare.visibility = View.VISIBLE
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
        val chipShare: Chip = view.findViewById(R.id.chipShare)
        val chipDel: Chip = view.findViewById(R.id.chipDel)
    }
}