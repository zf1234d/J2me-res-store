package com.mBZo.jar

import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import kotlin.concurrent.thread

class StoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)
        //找组件
        val title = findViewById<TextView>(R.id.storeTitle)
        val copyFrom = findViewById<TextView>(R.id.storeFrom)
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
//链接统一处理
fun web2download(activity: AppCompatActivity,link: String){
    if (link.contains("52emu") && link.contains("xid=1")){
        //这是一个特殊情况，用于处理52emu中的高速下载，使其变得可用
        contentFormat(activity,null,null,null,null,null,null,true)
        Snackbar.make(activity.findViewById(R.id.floatingActionButton),"检测到特殊链接，正在重处理",Snackbar.LENGTH_LONG).show()
        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .followRedirects(false)
                    .build()
                val request = Request.Builder()
                    .url(link)
                    .build()
                val response = client.newCall(request).execute()
                var finLink = response.header("Location")
                if (finLink != null) {
                    finLink = "https://wwr.lanzoux.com/${finLink.substringAfter("https://pan.lanzou.com/tp/")}"
                    lanzouApi(activity as StoreActivity,"web2download",finLink,"")
                }
                else{
                    Snackbar.make(activity.findViewById(R.id.floatingActionButton),"未能修复52emu的高速下载",Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                web2download(activity,link)
            }
            //
        }.start()
        //没想到挺长的
    }
    else{
        //这里是正常的下载，获取相关参数，决定是否需要内置下载器
        val spfRecord: SharedPreferences = activity.getSharedPreferences("com.mBZo.jar_preferences", MODE_PRIVATE)
        val downloader = spfRecord.getBoolean("smartDownloader",false)

        //下载判断开始，用户是否使用内置下载
        if (downloader){
            //内置下载处理函数
            fun downloadFileBy(url:String,filename:String,size:Long,mimeType:String){
                if (filename.subSequence(filename.length-4,filename.length)==".jar"){
                    MaterialAlertDialogBuilder(activity)
                        .setMessage("$filename\n\n大小：${size/1024} kb")
                        .setPositiveButton("开始下载"){_,_->
                            val waitingDownloadDialog =MaterialAlertDialogBuilder(activity)
                                .setMessage("$filename\n\n大小：${size/1024} kb\n\n已经开始下载")
                                .setCancelable(false)
                                .show()
                            val uri: Uri = Uri.parse(url)
                            val request: DownloadManager.Request = DownloadManager.Request(uri)
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            request.setDescription("正在下载...")
                            request.setTitle(filename)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            request.setMimeType(mimeType)
                            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val downloadId = downloadManager.enqueue(request)
                            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                            val receiver: BroadcastReceiver = object : BroadcastReceiver(){
                                override fun onReceive(context: Context?, intent: Intent?) {
                                    val myDownloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                                    if (downloadId == myDownloadId) {
                                        val downloadFileUri = downloadManager.getUriForDownloadedFile(downloadId)
                                        if (downloadFileUri != null) {
                                            //下载完成，询问打开文件
                                            waitingDownloadDialog.dismiss()
                                            val installDialog = MaterialAlertDialogBuilder(activity)
                                                .setMessage("$filename\n\n大小：${size/1024} kb\n\n下载完成")
                                                .setNeutralButton("忽略") {_,_ ->}
                                                .setPositiveButton("打开") {_,_ ->}
                                                .setCancelable(false)
                                                .show()
                                            installDialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                                                val jarFile = File(downloadFileUri.toString())
                                                val mIntent = Intent(Intent.ACTION_VIEW)
                                                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    mIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    mIntent.setDataAndType(downloadFileUri,"application/java-archive")
                                                } else {
                                                    mIntent.setDataAndType(Uri.fromFile(jarFile),"application/java-archive")
                                                }
                                                activity.startActivity(mIntent)
                                            }
                                        }
                                    }
                                }
                            }
                            activity.registerReceiver(receiver, filter)
                        }
                        .setNegativeButton("通过外部软件下载"){_,_-> otherOpen(activity,url) }
                        .show()
                }
                else{
                    MaterialAlertDialogBuilder(activity)
                        .setMessage("$filename\n\n大小：${size / 1024} kb\n\n（内置下载器不允许下载此格式文件）")
                        .setPositiveButton("通过外部软件下载"){_,_-> otherOpen(activity, url) }
                        .show()
                }
                contentFormat(activity,null,null,null,null,null,null,false)
            }
            //显示加载进度条
            contentFormat(activity,null,null,null,null,null,null,true)
            //可能使用内置下载，判断链接符不符合内置下载文件要求
            val webview = activity.findViewById<WebView>(R.id.storeBrowser)
            webview.loadUrl(link)
            webview.webViewClient=WebViewClient()
            webview.setDownloadListener { url, _, contentDisposition, mimetype, contentLength ->
                //加载完成
                contentFormat(activity,null,null,null,null,null,null,false)
                //传参，然后去下载
                if (contentDisposition==""){
                    val filename = URLUtil.guessFileName(url,null,null)
                    downloadFileBy(url,filename,contentLength,mimetype)
                }
                else{
                    val filename = URLDecoder.decode(Regex("\"$").replace(contentDisposition.substringAfter(Regex("filename\\s*=\\s*\"?").find(contentDisposition)!!.value),""),"utf-8")
                    downloadFileBy(url,filename,contentLength,mimetype)
                }
            }
        }
        else{
            //用户关闭内置下载需求
            otherOpen(activity,link)
        }
    }
}

//统一回调
fun contentFormat(activity: AppCompatActivity,iconLink: String?,imageList: List<String>?,linkList: List<String>?,linkNameList: List<String>?,fileSizeList: List<String>?,about: String?,loading: Boolean) {//最后一个为true时停止加载
    activity.runOnUiThread {
        val info = activity.findViewById<TextView>(R.id.storeInfo)
        val icon = activity.findViewById<ImageView>(R.id.ico)
        val loadingProgressBar = activity.findViewById<ProgressBar>(R.id.storeLoadingMain)
        val recyclerView: RecyclerView = activity.findViewById(R.id.storeImages)
        val downloadFab: FloatingActionButton = activity.findViewById(R.id.floatingActionButton)
        val spfRecord: SharedPreferences = activity.getSharedPreferences("com.mBZo.jar_preferences", MODE_PRIVATE)
        val downloader = spfRecord.getBoolean("smartDownloader",false)
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
            Glide.with(activity).load(iconLink).into(icon)
        }
        //预览图
        if (imageList != null) {
            if (imageList.isNotEmpty()) {
                recyclerView.visibility = View.VISIBLE
                //设置recyclerView
                val layoutManager = LinearLayoutManager(activity,RecyclerView.HORIZONTAL,false)
                recyclerView.layoutManager = layoutManager
                val adapter = ImgShowRecyclerAdapter(activity,imageList)
                recyclerView.adapter = adapter
            }
        }
        //下载链接
        if (linkNameList != null) {
            if (linkList != null) {
                //处理下载相关交互
                downloadFab.visibility = View.VISIBLE
                val linkNameListA = mutableListOf<String>()
                if (fileSizeList != null){
                    for (p in 1..linkNameList.size){
                        linkNameListA.add("[${fileSizeList[p-1]}] ${linkNameList[p-1]}")
                    }
                }
                else{
                    linkNameListA.addAll(linkNameList)
                }
                downloadFab.setOnClickListener {
                    if (linkNameList.size > 1) {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("下载列表")
                            .setItems(linkNameListA.toTypedArray()){_,p ->
                                if (downloader){
                                    web2download(activity,linkList[p])
                                }
                                else{
                                    if (fileSizeList != null){
                                        MaterialAlertDialogBuilder(activity)
                                            .setTitle("详情")
                                            .setMessage("\n${linkNameList[p]}\n大小：${fileSizeList[p]}")
                                            .setPositiveButton("立即下载"){_,_ -> web2download(activity,linkList[p]) }
                                            .show()
                                    }
                                    else {
                                        MaterialAlertDialogBuilder(activity)
                                            .setTitle("详情")
                                            .setMessage("\n${linkNameList[p]}")
                                            .setPositiveButton("立即下载"){_,_ -> web2download(activity,linkList[p]) }
                                            .show()
                                    }
                                }
                            }
                            .show()
                    }
                    else {
                        if (downloader){
                            web2download(activity,linkList[0])
                        }
                        else{
                            MaterialAlertDialogBuilder(activity)
                                .setTitle("详情")
                                .setMessage("\n${linkNameList[0]}")
                                .setPositiveButton("立即下载"){_,_ -> web2download(activity,linkList[0]) }
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

//解析的前置模块
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
                    web2download(activity,finLink)
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
            else {
                for (index in info.substringAfter("【下载地址】：<a href=\'").substringBefore("<hr />")
                    .split("</a>|<a href=\'")) {
                    downLinkNameList.add(index.substringAfter("\'>").substringBefore("</a>"))
                    downLinkList.add("http://java.52emu.cn/" + index.substringBefore("\'>"))
                }
            }
            //解析预览图
            for (index in info.split("img")){
                if (index.substringAfter("src=\"")!=index){
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
                            val responseInfo = client.newCall(requestInfo).execute()
                            contentFormat(activity,null,null,null,null,null,responseInfo.body.string(),false)
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

//预览图显示
class ImgShowRecyclerAdapter(private val activity: AppCompatActivity, private val imgUrlList: List<String>) :
    RecyclerView.Adapter<ImgShowRecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.img_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = imgUrlList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val iconLink = imgUrlList[position]
        //显示
        //holder.name.text =
        Glide.with(activity).load(iconLink).into(holder.images)
        //点击
        holder.itemView.setOnClickListener {   }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val images: ImageView = itemView.findViewById(R.id.imagesShow)
    }
}
