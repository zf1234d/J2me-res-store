package com.mBZo.jar.store.apidecode

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.store.storeManage
import com.mBZo.jar.tool.isDestroy
import okhttp3.OkHttpClient
import okhttp3.Request

fun apiDecodeIniche(activity: StoreActivity, path: String) {
    Thread {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(path)
                .build()
            val response = client.newCall(request).execute()
            var stepDate=response.body.string()
            //简介
            stepDate = stepDate.substringAfter("<div class=\"game_info\">")
            val gameInfoRude = stepDate
                .substringBefore("<ul class=")
                .replace("\n","")
                .split("<span>")
            var gameInfo = ""
            for (index in gameInfoRude){
                if (index.contains("：")){
                    if (index.contains("厂商") || index.contains("分类") || index.contains("平台") || index.contains("容量") || index.contains("版本") || index.contains("下载") || index.contains("分辨率") || index.contains("单机联机") || index.contains("官方简介")){
                        gameInfo += index.replace("</span>","") + "\n"
                    }
                }
            }
            //图片
            stepDate = stepDate
                .substringAfter("<ul class=")
                .substringAfter(">")
                .substringBefore("</ul>")
            val imageList = mutableListOf<String>()
            if (stepDate.contains("img src")){
                val imageRudeList = stepDate.split("<li><img src=\"")
                for (index in imageRudeList){
                    imageList.add("https://iniche.cn"+index.substringBefore("\""))
                }
            }
            //下载
            val link = List(1) {path}
            val linkName = List(1) {"你知道的，这是什么并不重要"}
            storeManage(activity,null,imageList,link,linkName,null,gameInfo,false)
        } catch (e: Exception) {
            if (isDestroy(activity).not()){
                activity.runOnUiThread {
                    MaterialAlertDialogBuilder(activity)
                        .setCancelable(false)
                        .setTitle("加载失败")
                        .setMessage("您的网络可能存在问题！")
                        .setNegativeButton("重试"){_,_ -> apiDecodeIniche(activity,path) }
                        .setPositiveButton("退出"){_,_ -> activity.finish() }
                        .show()
                }
            }
        }
    }.start()
}