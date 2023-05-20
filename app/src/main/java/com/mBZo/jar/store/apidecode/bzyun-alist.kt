package com.mBZo.jar.store.apidecode

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.store.contentFormat
import com.mBZo.jar.tool.isDestroy
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.concurrent.thread

fun apiDecodeBzyunCn(activity: StoreActivity, path: String, name: String) {
    val rootUrl = "https://store.j2me.bzyun.top"
    Thread {
        try {
            val requestBody: FormBody = FormBody.Builder()
                .add("path","/J2ME应用商店$path/$name")
                .add("password","")
                .add("page","1")
                .add("per_page","100")
                .add("refresh","false")
                .build()
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("${rootUrl}/api/fs/list")
                .post(requestBody)
                .build()
            val response = client.newCall(request).execute()
            //解析
            val data = JSONObject(response.body.string()).getJSONObject("data").getJSONArray("content")
            val downLinkList = mutableListOf<String>()
            val downLinkNameList = mutableListOf<String>()
            val fileSizeList = mutableListOf<String>()
            val imagesList = mutableListOf<String>()
            for (index in 0 until  data.length()){
                val indexName=data.getJSONObject(index).getString("name").toString()
                if (data.getJSONObject(index).getBoolean("is_dir").not()){//确认项目不是文件夹
                    //下载列表
                    if (indexName.contains(name.substringBefore("_"))){//确认是可下载文件
                        downLinkNameList.add(indexName.substringAfterLast("_"))
                        downLinkList.add("${rootUrl}/d/J2ME应用商店$path/$name/$indexName")
                        fileSizeList.add("${data.getJSONObject(index).getString("size").toInt().div(1024)}kb")
                    }
                    //预览图
                    if (indexName.contains("img") && indexName.contains("ErrLog.txt").not()){imagesList.add("${rootUrl}/d/J2ME应用商店$path/$name/$indexName") }
                    //图标
                    if (indexName.contains("pic") && indexName.contains("ErrLog.txt").not()){
                        contentFormat(activity,"${rootUrl}/d/J2ME应用商店$path/$name/$indexName", loading = true)
                    }
                    //简介
                    if (indexName=="gameInfo.html"){
                        val requestInfo = Request.Builder()
                            .url("${rootUrl}/d/J2ME应用商店$path/$name/$indexName")
                            .build()
                        thread {
                            try {
                                val responseInfo = client.newCall(requestInfo).execute()
                                contentFormat(activity,about = responseInfo.body.string(), loading = false)
                            } catch (_: Exception) { }
                        }
                    }
                    //一次判断结束
                }
            }//循环结束
            contentFormat(activity,null,imagesList,downLinkList,downLinkNameList,fileSizeList, loading = true)
        } catch (e: Exception) {
            if (isDestroy(activity).not()){
                activity.runOnUiThread {
                    MaterialAlertDialogBuilder(activity)
                        .setCancelable(false)
                        .setTitle("加载失败")
                        .setMessage("您的网络可能存在问题！")
                        .setNegativeButton("重试"){_,_ -> apiDecodeBzyunCn(activity,path,name) }
                        .setPositiveButton("退出"){_,_ -> activity.finish() }
                        .show()
                }
            }
        }
    }.start()
}