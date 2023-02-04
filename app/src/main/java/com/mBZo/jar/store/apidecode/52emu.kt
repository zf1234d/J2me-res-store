package com.mBZo.jar.store.apidecode

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mBZo.jar.R
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.isDestroy
import com.mBZo.jar.store.contentFormat
import okhttp3.OkHttpClient
import okhttp3.Request

fun apiDecode52emu(activity: StoreActivity, path: String) {
    Thread {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://java.52emu.cn/xq.php?id=$path")
                .build()
            val response = client.newCall(request).execute()
            var info = response.body.string()
            val downLinkList = mutableListOf<String>()
            val downLinkNameList = mutableListOf<String>()
            val imagesList = mutableListOf<String>()
            //解析下载地址
            if (info.contains("暂无下载地址")) {
                Snackbar.make(activity.findViewById(R.id.storeDownload),"暂无下载地址", Snackbar.LENGTH_LONG).show()
            }
            else if (info.contains("【下载地址】")){
                for (index in info.substringAfter("【下载地址】：<a href=\'").substringBefore("<hr />")
                    .split("</a>|<a href=\'")) {
                    downLinkNameList.add(index.substringAfter("\'>").substringBefore("</a>"))
                    downLinkList.add("http://java.52emu.cn/" + index.substringBefore("\'>"))
                }
            }
            //解析预览图
            for (index in info.split("img")){
                if (index.contains("src=\"")){
                    imagesList.add(index.substringAfter("src=\"").substringBefore("\">"))
                }
            }
            //解析简介
            if (info.contains("游戏简介")){
                info = info.substringAfter("【游戏简介】").substringAfter("</h3>").substringBefore("<hr />")
            }
            else{
                info = "未找到简介"
            }
            activity.runOnUiThread {
                if (downLinkList.size == 0){
                    contentFormat(activity,null,imagesList,about = info, loading = false)
                }
                else{
                    contentFormat(activity,null,imagesList,downLinkList,downLinkNameList,null,info,false)
                }
            }
        }catch (e: Exception) {
            if (isDestroy(activity).not()){
                activity.runOnUiThread {
                    MaterialAlertDialogBuilder(activity)
                        .setCancelable(false)
                        .setTitle("加载失败")
                        .setMessage("您的网络可能存在问题！")
                        .setNegativeButton("重试"){_,_ -> apiDecode52emu(activity,path) }
                        .setPositiveButton("退出"){_,_ -> activity.finish() }
                        .show()
                }
            }
        }
    }.start()
}