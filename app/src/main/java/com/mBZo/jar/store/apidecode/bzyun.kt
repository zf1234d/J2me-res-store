package com.mBZo.jar.store.apidecode

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.store.contentFormat
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.concurrent.thread

fun apiDecodeBzyun(activity: StoreActivity, path: String, name: String) {
    val addPath = "https://od.bzyun.top/api/?path=/J2ME应用商店$path/$name"
    Thread {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(addPath)
                .build()
            val response = client.newCall(request).execute()
            //解析
            val data = JSONObject(response.body.string()).getJSONObject("folder").getJSONArray("value")
            val downLinkList = mutableListOf<String>()
            val downLinkNameList = mutableListOf<String>()
            val fileSizeList = mutableListOf<String>()
            val imagesList = mutableListOf<String>()
            var temp:String
            for (index in 1..data.length()){
                temp=data.getJSONObject(index-1).getString("name").toString()
                if (data.getJSONObject(index-1).getJSONObject("file").toString()!=""){//确认项目不是文件夹
                    //下载列表
                    if (temp.contains(name)){//确认是可下载文件
                        downLinkNameList.add(temp.substringAfter(name+"_"))
                        downLinkList.add("https://od.bzyun.top/api/raw/?path=/J2ME应用商店$path/$name/$temp")
                        fileSizeList.add("${data.getJSONObject(index-1).getString("size").toInt().div(1024)}kb")
                    }
                    //预览图
                    if (temp.contains("img") && temp.contains("ErrLog.txt").not()){imagesList.add("https://od.bzyun.top/api/raw/?path=/J2ME应用商店$path/$name/$temp") }
                    //图标
                    if (temp.contains("pic") && temp.contains("ErrLog.txt").not()){
                        contentFormat(activity,"https://od.bzyun.top/api/raw/?path=/J2ME应用商店/$path/$name/$temp",null,null,null,null,null,true)
                    }
                    //简介
                    if (temp=="gameInfo.html"){
                        val requestInfo = Request.Builder()
                            .url("https://od.bzyun.top/api/raw/?path=/J2ME应用商店$path/$name/$temp")
                            .build()
                        thread {
                            try {
                                val responseInfo = client.newCall(requestInfo).execute()
                                contentFormat(activity,null,null,null,null,null,responseInfo.body.string(),false)
                            } catch (_: Exception) { }
                        }
                    }
                    //一次判断结束
                }
            }//循环结束
            contentFormat(activity,null,imagesList,downLinkList,downLinkNameList,fileSizeList,null,true)
        } catch (e: Exception) {
            activity.runOnUiThread {
                MaterialAlertDialogBuilder(activity)
                    .setCancelable(false)
                    .setTitle("加载失败")
                    .setMessage("您的网络可能存在问题！")
                    .setNegativeButton("重试"){_,_ -> apiDecodeBzyun(activity,path,name) }
                    .setPositiveButton("退出"){_,_ -> activity.finish() }
                    .show()
            }
        }
    }.start()
}