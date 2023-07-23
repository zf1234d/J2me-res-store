package com.mBZo.jar.store.apidecode

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.store.storeManage
import com.mBZo.jar.tool.isDestroy
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

fun apiDecodeBzyun(activity: StoreActivity, path: String, name: String ,failedNum: Int = 0) {
    val maxFailed = 3
    val rootUrl = "https://alist.bzyun.top"
    Thread {
        try {
            val requestBody: FormBody = FormBody.Builder()
                .add("path","/分享/bzyun/J2ME应用商店$path/$name")
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
            val result = response.body.string()
            //解析
            val data = JSONObject(result).getJSONObject("data").getJSONArray("content")
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
                        downLinkList.add("${rootUrl}/d/分享/bzyun/J2ME应用商店$path/$name/$indexName")
                        fileSizeList.add("${data.getJSONObject(index).getString("size").toInt().div(1024)}kb")
                    }
                    //预览图
                    if (indexName.contains("img") && indexName.contains("ErrLog.txt").not()){imagesList.add("${rootUrl}/d/J2ME应用商店$path/$name/$indexName") }
                    //图标
                    if (indexName.contains("pic") && indexName.contains("ErrLog.txt").not()){
                        storeManage(activity,"${rootUrl}/d/分享/bzyun/J2ME应用商店$path/$name/$indexName", loading = true)
                    }
                    //简介
                    if (indexName=="gameInfo.html"){
                        val requestInfo = Request.Builder()
                            .url("${rootUrl}/d/分享/bzyun/J2ME应用商店$path/$name/$indexName")
                            .build()
                        Thread {
                            try {
                                val responseInfo = client.newCall(requestInfo).execute()
                                storeManage(activity,about = responseInfo.body.string(), loading = false)
                            } catch (_: Exception) { }
                        }.start()
                    }
                    //一次判断结束
                }
            }//循环结束
            storeManage(activity,null,imagesList,downLinkList,downLinkNameList,fileSizeList, loading = true)
        } catch (e: Exception) {
            if (isDestroy(activity).not()){
                if (failedNum<maxFailed){
                    apiDecodeBzyun(activity,path,name,failedNum+1)
                }
                else{
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
            }
        }
    }.start()
}