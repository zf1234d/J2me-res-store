package com.mBZo.jar.store.apidecode

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.isDestroy
import com.mBZo.jar.otherOpen
import com.mBZo.jar.store.WebViewListen2Download
import com.mBZo.jar.store.contentFormat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


fun lanzouApi(activity: StoreActivity,type: String,url: String,pwd: String) {
    //蓝奏云解析kotlin版 by.mBZo ver1.2
    var finLink="";var data="";var about: String
    Thread {
        try {
            //第一次请求
            val client = OkHttpClient()
            var request = Request.Builder()
                .url(url)
                .build()
            var response = client.newCall(request).execute()
            finLink = response.body.string()
            if(finLink.contains("passwd").not()) {
                //无密码
                //获取信息
                val info = finLink.split("<span class=\"p7\">")
                about = "${info[1].split("<br>")[0]}\n${info[2].split("<br>")[0]}\n${info[3].split("<br>")[0]}\n${info[4].split("<br>")[0]}\n${info[5].split("<br>")[0]}".replace("</span>","",ignoreCase = true).replace("<font>","",ignoreCase = true).replace("</font>","",ignoreCase = true)
                //拼接二次请求连接
                finLink = "https://wwr.lanzoux.com${finLink.split("</iframe>")[1].split("src=\"")[1].split("\"")[0]}"
                //无密码状态二次请求
                request = Request.Builder()
                    .url(finLink)
                    .build()
                response = client.newCall(request).execute()
                //拼接最终请求数据
                val lanzouRaw = Regex("//.*\n").replace(response.body.string(),"")
                finLink = lanzouRaw
                data = finLink.substringAfter("data : { ").substringBefore(" },")
                finLink=""
                for (index in data.split(",")){
                    finLink+="${index.substringAfter("\'").substringBefore("\':")}=" +
                            if (index.contains("\':\'")) {
                                index.substringAfter(":\'").substringBefore("\'")
                            }
                            else {
                                if (Regex("^[0-9]*\$").matches(index.substringAfter("\':"))) {
                                    index.substringAfter("\':")
                                }
                                else {
                                    lanzouRaw.substringAfter("var ${index.substringAfter("\':")} = '").substringBefore("';")
                                }
                            } + "&"
                }
                finLink = finLink.substringBeforeLast("&")
            }
            else {
                //有密码,拼接请求数据
                about = "${finLink.split("<meta name=\"description\" content=\"")[1].split("|\"")[0]}\n上传时间：${finLink.substringAfter("\"n_file_infos\">").substringBefore("</span>")}"
                finLink = "${finLink.split("data : \'")[1].split("\'+pwd,")[0]}$pwd"
            }
            //发起最终请求
            request = Request.Builder()
                .url("https://wwr.lanzoux.com/ajaxm.php")
                .header("referer", url)
                .post(finLink.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()
            response = client.newCall(request).execute()
            finLink = response.body.string()
            finLink = "${JSONObject(finLink).getString("dom")}/file/${JSONObject(finLink).getString("url")}"
            activity.runOnUiThread{
                if (type == "only"){
                    //作为唯一信息来源
                    val linkNameList = listOf(about)
                    val linkList = listOf(finLink)
                    contentFormat(activity,null,null,linkList,linkNameList,null,about,false)
                }
                else if (type == "web2download"){
                    //传回web2download
                    contentFormat(activity, loading = false)
                    WebViewListen2Download(activity,finLink)
                }
            }
        }catch (e: Exception) {
            if (isDestroy(activity).not()){
                activity.runOnUiThread {
                    MaterialAlertDialogBuilder(activity)
                        .setCancelable(false)
                        .setTitle("加载失败")
                        .setMessage("您的网络可能存在问题！\n若多次重试均无效则不排除蓝奏云api变动的可能性。")
                        .setNegativeButton("重试") {_,_ -> lanzouApi(activity,type,url,pwd) }
                        .setPositiveButton("退出") {_,_ -> activity.finish()}
                        .setNeutralButton("显示日志") {_,_ ->
                            MaterialAlertDialogBuilder(activity)
                                .setCancelable(false)
                                .setTitle("失败详情")
                                .setMessage("auto:$data\n" +
                                        "finLink:\n$finLink")
                                .setNegativeButton("仍然重试") {_,_ -> lanzouApi(activity,type,url,pwd) }
                                .setPositiveButton("退出") {_,_ -> activity.finish()}
                                .setNeutralButton("原始网页") {_,_ -> otherOpen(activity,url) ;activity.finish()}
                                .show()
                        }
                        .show()
                }
            }

        }
    }.start()
}



fun apiDecodeJoyin(activity: StoreActivity, path: String) {
    lanzouApi(activity,"only",path,"1234")
}

fun apiDecodeEjJava(activity: StoreActivity, path: String) {
    lanzouApi(activity,"only",path,"")
}