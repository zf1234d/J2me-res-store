package com.mBZo.jar.store

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mBZo.jar.R
import com.mBZo.jar.adapter.ImgShowRecyclerAdapter
import com.mBZo.jar.isDestroy

fun contentFormat(activity: AppCompatActivity, iconLink: String?, imageList: List<String>?, linkList: List<String>?, linkNameList: List<String>?, fileSizeList: List<String>?, about: String?, loading: Boolean) {//最后一个为true时停止加载
    activity.runOnUiThread {
        val info = activity.findViewById<TextView>(R.id.storeInfo)
        val icon = activity.findViewById<ImageView>(R.id.ico)
        val loadingProgressBar = activity.findViewById<ProgressBar>(R.id.storeLoadingMain)
        val recyclerView: RecyclerView = activity.findViewById(R.id.storeImages)
        val downloadFab: FloatingActionButton = activity.findViewById(R.id.floatingActionButton)
        val spfRecord: SharedPreferences = activity.getSharedPreferences("com.mBZo.jar_preferences",
            Context.MODE_PRIVATE
        )
        val downloader = spfRecord.getBoolean("smartDownloader",true)
        //简介
        if (about != null){
            info.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                info.text = Html.fromHtml(about.replace("\n","<br>"), Html.FROM_HTML_MODE_LEGACY)
            } else {
                info.text = Html.fromHtml(about.replace("\n","<br>"))
            }
        }
        //图标
        if (iconLink != null){
            icon.visibility = View.VISIBLE
            if (isDestroy(activity).not()){
                Glide.with(activity).load(iconLink).into(icon)
            }
        }
        //预览图
        if (imageList != null) {
            if (imageList.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                //设置recyclerView
                val layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL,false)
                recyclerView.layoutManager = layoutManager
                val adapter = ImgShowRecyclerAdapter(activity,imageList)
                recyclerView.adapter = adapter
            }
        }
        //下载链接
        if (linkNameList != null && linkNameList.isNotEmpty()) {
            if (linkList != null && linkList.isNotEmpty()) {
                //处理下载相关交互
                downloadFab.visibility = View.VISIBLE
                val linkNameListA = mutableListOf<String>()
                if (fileSizeList != null && fileSizeList.isNotEmpty()){
                    for (p in 1..linkNameList.size){
                        linkNameListA.add("[${fileSizeList[p-1]}] ${linkNameList[p-1]}")
                    }
                }
                else{
                    linkNameListA.addAll(linkNameList)
                }
                downloadFab.setOnClickListener {
                    if (linkNameList.size>1) {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("下载列表")
                            .setItems(linkNameListA.toTypedArray()){_,p ->
                                if (downloader){
                                    WebViewListen2Download(activity,linkList[p])
                                }
                                else{
                                    if (fileSizeList != null && fileSizeList.isNotEmpty()){
                                        MaterialAlertDialogBuilder(activity)
                                            .setTitle("详情")
                                            .setMessage("\n${linkNameList[p]}\n大小：${fileSizeList[p]}")
                                            .setPositiveButton("立即下载"){_,_ -> WebViewListen2Download(activity,linkList[p]) }
                                            .show()
                                    }
                                    else {
                                        MaterialAlertDialogBuilder(activity)
                                            .setTitle("详情")
                                            .setMessage("\n${linkNameList[p]}")
                                            .setPositiveButton("立即下载"){_,_ -> WebViewListen2Download(activity,linkList[p]) }
                                            .show()
                                    }
                                }
                            }
                            .show()
                    }
                    else {
                        if (downloader){
                            WebViewListen2Download(activity,linkList[0])
                        }
                        else{
                            MaterialAlertDialogBuilder(activity)
                                .setTitle("详情")
                                .setMessage("\n${linkNameList[0]}")
                                .setPositiveButton("立即下载"){_,_ -> WebViewListen2Download(activity,linkList[0]) }
                                .show()
                        }
                    }
                }
            }
        }
        //隐藏加载
        if (loading){ loadingProgressBar.visibility = View.VISIBLE }
        else { loadingProgressBar.visibility = View.INVISIBLE }
    }
}