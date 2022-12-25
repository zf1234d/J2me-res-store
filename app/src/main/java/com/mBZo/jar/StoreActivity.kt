package com.mBZo.jar

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mBZo.jar.store.WebViewListen2Download
import com.mBZo.jar.store.contentFormat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.concurrent.thread


class StoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)
        //找组件
        val title = findViewById<TextView>(R.id.storeTitle)
        val copyFrom = findViewById<TextView>(R.id.storeFrom)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.inflateMenu(R.menu.store_toolbar_menu)
            toolbar.setOnMenuItemClickListener {
                val intent = Intent(this,DownloadActivity::class.java)
                startActivity(intent)
                return@setOnMenuItemClickListener true
            }
        }
        //读参数
        val name = intent.getStringExtra("name")
        val from = intent.getStringExtra("from")
        val path = intent.getStringExtra("path")
        //不知道干嘛
        title.text = name
        val copyFromText = "来源:$from"
        copyFrom.text = copyFromText
        //通过from判断解析方法吧，找不到对应from就返回不支持
        if (name != null && from != null && path != null) {//虽然做了防毒，但不这样写不能编译
            if (from.contains("没空云")){ apiDecodeBzyun(this,path,name) }//匹配规则，没空云（OneIndexApi）
            else if (from.contains("Joyin的jar游戏下载站")){ apiDecodeJoyin(this,path) }//匹配规则，Joyin (Lanzou)
            else if (from.contains("52emu")){ apiDecode52emu(this,path) }//匹配规则，52emu (专属混合规则)
            else if (from=="损坏") {//库存本身出意外都是在这里解决
                MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setMessage("当前资源损坏，截图向我反馈")
                    .setPositiveButton("退出"){ _,_ -> this.finish() }
                    .show()
            }
            else {//查了一圈也不认识
                MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setMessage("暂不支持解析该仓库内容，请等待更新")
                    .setPositiveButton("退出"){ _,_ -> this.finish() }
                    .show()
            }
        }
        else{//如果说，真的有毒的话，给一个提示
            MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setMessage("您触发了意料之外的情况，无法提供内容")
                .setPositiveButton("退出"){ _,_ -> this.finish() }
                .show()
        }
        //应该没啥要写了吧
    }
}


//跳转浏览器
fun otherOpen(activity: AppCompatActivity,url:String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(activity,intent,null)
    } catch (e: Exception) {
        Toast.makeText(activity,"未找到可用软件", Toast.LENGTH_SHORT).show()
    }
}


//解析的前置模块
fun isDestroy(mActivity: Activity?): Boolean {
    return mActivity == null || mActivity.isFinishing || mActivity.isDestroyed
}



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
                    contentFormat(activity,null,null,null,null,null,null,false)
                    WebViewListen2Download(activity,finLink)
                }
            }
        }catch (e: Exception) {
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
    }.start()
}


//各种解析
private fun apiDecode52emu(activity: StoreActivity, path: String) {
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
                Snackbar.make(activity.findViewById(R.id.floatingActionButton),"暂无下载地址",Snackbar.LENGTH_LONG).show()
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
                    contentFormat(activity,null,imagesList,null,null,null,info,false)
                }
                else{
                    contentFormat(activity,null,imagesList,downLinkList,downLinkNameList,null,info,false)
                }
            }
        }catch (e: Exception) {
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
    }.start()
}

private fun apiDecodeJoyin(activity: StoreActivity, path: String) {
    lanzouApi(activity,"only",path,"1234")
}

private fun apiDecodeBzyun(activity: StoreActivity, path: String,name: String) {
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
                    if (temp.contains("pic") && temp.contains("ErrLog.txt").not()){contentFormat(activity,"https://od.bzyun.top/api/raw/?path=/J2ME应用商店/$path/$name/$temp",null,null,null,null,null,true)}
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
                    .setNegativeButton("重试"){_,_ -> apiDecodeBzyun(activity, path,name) }
                    .setPositiveButton("退出"){_,_ -> activity.finish() }
                    .show()
            }
        }
    }.start()
}
//***解析部分到此为止***

