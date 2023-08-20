package com.mBZo.jar.store

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.R
import com.mBZo.jar.adapter.ImgShowRecyclerAdapter
import com.mBZo.jar.tool.imageLoad

fun storeManage(activity: Activity, iconLink: String?=null, imageList: List<String>?=null, linkList: List<String>?=null, linkNameList: List<String>?=null, fileSizeList: List<String>?=null, about: String?=null, loading: Boolean) {
    //loading为false时停止加载
    val info = activity.findViewById<TextView>(R.id.storeInfo)
    val icon = activity.findViewById<ImageView>(R.id.ico)
    val loadingProgressBar = activity.findViewById<ProgressBar>(R.id.storeLoadingMain)
    val recyclerView: RecyclerView = activity.findViewById(R.id.storeImages)
    val downloadButton: MaterialButton = activity.findViewById(R.id.storeDownload)
    val sp: SharedPreferences = activity.getSharedPreferences("com.mBZo.jar_preferences", Context.MODE_PRIVATE)
    val smartDownloader = sp.getBoolean("smartDownloader",false)
    activity.runOnUiThread {
        //简介
        if (about != null){
            info.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                info.text = Html.fromHtml(about.replace("\n","<br>"), Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                info.text = Html.fromHtml(about.replace("\n","<br>"))
            }
            info.movementMethod = LinkMovementMethod.getInstance()
        }
        //图标
        if (iconLink != null){
            icon.visibility = View.VISIBLE
            imageLoad(activity,icon,iconLink)
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
        if (linkNameList?.isNotEmpty() == true) {
            if (linkList?.isNotEmpty() == true) {
                //处理下载相关交互
                downloadButton.visibility = View.VISIBLE
                downloadButton.text = "下载"
                if (linkNameList.size>1) {
                    var linkDialogShowNameList = mutableListOf<String>()
                    if (fileSizeList?.isNotEmpty() == true){
                        for (p in 1..linkNameList.size){
                            linkDialogShowNameList.add("[${fileSizeList[p-1]}] ${linkNameList[p-1]}")
                        }
                    }
                    else{
                        linkDialogShowNameList = linkNameList.toMutableList()
                    }
                    downloadButton.setOnClickListener {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("下载列表")
                            .setItems(linkDialogShowNameList.toTypedArray()){_,p ->
                                if (smartDownloader){
                                    WebViewListen2Download(activity,linkList[p])
                                }
                                else{
                                    if (!fileSizeList.isNullOrEmpty()){
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
                }
                else{
                    downloadButton.setOnClickListener {
                        if (smartDownloader){
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