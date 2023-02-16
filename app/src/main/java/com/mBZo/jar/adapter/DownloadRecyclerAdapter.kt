package com.mBZo.jar.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mBZo.jar.BuildConfig
import com.mBZo.jar.DownloadActivity
import com.mBZo.jar.R
import com.mBZo.jar.store.installJar
import com.mBZo.jar.tool.isDestroy
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import zlc.season.downloadx.State
import zlc.season.downloadx.core.DownloadTask
import zlc.season.downloadx.download
import zlc.season.downloadx.utils.formatSize
import java.io.File
import java.util.*


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
            holder.chipShare.visibility = View.GONE
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
            holder.chipOpen.text = "取消"
            holder.chipOpen.setOnClickListener {
                notifyItemRemoved(position)
                if (fileList[position].exists()){
                    fileList[position].delete()
                }
                fileList.remove(fileList[position])
                downloadTask.remove()
                notifyItemRangeChanged(position,itemCount)
            }
            @SuppressLint("SetTextI18n")
            holder.chipShare.text = "${state.progress.downloadSizeStr()}/${state.progress.totalSizeStr()}"
            holder.chipShare.setOnClickListener {  }
            holder.chipDel.text = state.progress.percentStr()
            holder.chipDel.setOnClickListener {  }
            holder.loading.isIndeterminate = false
            holder.loading.progress = state.progress.percent().toInt()
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
                    val mIntent = Intent(Intent.ACTION_SEND)
                    mIntent.putExtra(Intent.EXTRA_SUBJECT, fileList[position].name)
                    mIntent.type = "application/java-archive"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        mIntent.putExtra(Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID +".fileProvider",fileDownloaded))
                    } else {
                        mIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileDownloaded))
                    }
                    holder.itemView.context.startActivity(mIntent)
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