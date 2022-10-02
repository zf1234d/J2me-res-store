package com.mBZo.jar

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HTTP_UNSUPPORTED_MEDIA_TYPE
import org.json.JSONObject
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
        //设置导航栏颜色
        if (Build.VERSION.SDK_INT > 27){
            window.navigationBarColor = getColor(R.color.white2)
        }
        //不知道干嘛
        title.text = name
        val copyFromText = "来源:$from"
        copyFrom.text = copyFromText
        //通过from判断解析方法吧，找不到对应from就返回不支持
        if (from != null) {//虽然做了防毒，但不这样写不能编译
            if (from.substringAfter("没空云",) != from){ apiDecodeBzyun(this,path,name) }//匹配规则，没空云（OneIndexApi）
            else if (from.substringAfter("Joyin的jar游戏下载站",) != from){ apiDecodeJoyin(this,path) }//匹配规则，Joyin (Lanzou)
            else if (from.substringAfter("52emu",) != from){ apiDecode52emu(this,path) }//匹配规则，52emu (专属混合规则)
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

//统一回调
fun contentFormat(activity: AppCompatActivity,iconLink: String?,imageList: List<String>?,linkList: List<String>?,linkNameList: List<String>?,about: String?,loading: Boolean) {//最后一个为true时停止加载
    activity.runOnUiThread {
        val info = activity.findViewById<TextView>(R.id.storeInfo)
        val icon = activity.findViewById<ImageView>(R.id.ico)
        val loadingProgressBar = activity.findViewById<ProgressBar>(R.id.storeLoadingMain)
        val recyclerView: RecyclerView = activity.findViewById(R.id.storeImages)
        val downloadFab: FloatingActionButton = activity.findViewById(R.id.floatingActionButton)
        //简介
        if (about != null){
            info.visibility = View.VISIBLE
            info.text = about
            info.setOnClickListener{
                MaterialAlertDialogBuilder(activity)
                    .setTitle("简介")
                    .setMessage(about)
                    .show()
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
                //跳转浏览器
                fun web2download(link: String){
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.addCategory(Intent.CATEGORY_BROWSABLE)
                        intent.data = Uri.parse(link)
                        startActivity(activity,intent,null)
                    } catch (e: Exception) {
                        Toast.makeText(activity,"当前手机未安装浏览器", Toast.LENGTH_SHORT).show()
                    }
                }
                //处理下载相关交互
                downloadFab.visibility = View.VISIBLE
                downloadFab.setOnClickListener {
                    if (linkNameList.size > 1) {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("下载列表")
                            .setItems(linkNameList.toTypedArray()){_,p ->
                                MaterialAlertDialogBuilder(activity)
                                    .setTitle("详情")
                                    .setMessage("\n${linkNameList[p]}")
                                    .setPositiveButton("立即下载"){_,_ -> web2download(linkList[p]) }
                                    .show()
                            }
                            .show()
                    }
                    else {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("详情")
                            .setMessage("\n${linkNameList[0]}")
                            .setPositiveButton("立即下载"){_,_ -> web2download(linkList[0]) }
                            .show()
                    }
                }
            }
        }
        //隐藏加载
        if (loading){ loadingProgressBar.visibility = View.INVISIBLE }
    }
}

//解析的前置模块
fun lanzouApi(activity: StoreActivity,type: String,url: String,pwd: String) {
    //蓝奏云解析kotlin版 by.mBZo ver1.0
    Thread {
        try {
            //第一次请求
            val client = OkHttpClient()
            var request = Request.Builder()
                .url(url)
                .build()
            var response = client.newCall(request).execute()
            var finLink = response.body.string()
            if(finLink.substringAfter("passwd")==finLink) {
                //无密码
                //获取信息
                val info = finLink.split("<span class=\"p7\">")
                apiTemp1 = "${info[1].split("<br>")[0]}\n${info[2].split("<br>")[0]}\n${info[3].split("<br>")[0]}\n${info[4].split("<br>")[0]}\n${info[5].split("<br>")[0]}".replace("</span>","",ignoreCase = true).replace("<font>","",ignoreCase = true).replace("</font>","",ignoreCase = true)
                //拼接二次请求连接
                finLink = "https://wwr.lanzoux.com${finLink.split("</iframe>")[1].split("src=\"")[1].split("\"")[0]}"
                //无密码状态二次请求
                request = Request.Builder()
                    .url(finLink)
                    .build()
                response = client.newCall(request).execute()
                //拼接最终请求数据
                finLink = response.body.string()
                val data1 = finLink.split("var ajaxdata = '")[1].split("';")[0]
                val data2 = finLink.split("var vsign = '")[1].split("';")[0]
                val data3 = finLink.split("var awebsigna = '")[1].split("';")[0]
                val data4 = finLink.split("var cwebsignkeyc = '")[1].split("';")[0]
                finLink = "action=downprocess&signs=$data1&sign=$data2&websign=$data3&websignkey=$data4&ves=1"
            }
            else {
                //有密码,拼接请求数据
                apiTemp1 = finLink.split("<meta name=\"description\" content=\"")[1].split("|\"")[0]
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
                    val linkNameList = listOf<String>(apiTemp1)
                    val linkList = listOf<String>(finLink)
                    contentFormat(activity,null,null,linkList,linkNameList,apiTemp1,true)
                }
                else if (type == "link"){
                    //只要链接
                    apiTemp1 = finLink
                }
            }
        }catch (e: Exception) {
            lanzouApi(activity,type,url,pwd)
        }
    }.start()
}

//公共变量
var apiTemp1 = ""

//各种解析
private fun apiDecode52emu(activity: StoreActivity, path: String?) {
    if (path !=null){
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
                apiTemp1 = "" //清除公共变量内的旧内容
                //解析下载地址
                for (index in info.substringAfter("【下载地址】：<a href=\'").substringBefore("<hr />").split("</a>|<a href=\'")){
                    downLinkNameList.add(index.substringAfter("\'>").substringBefore("</a>"))
                    downLinkList.add("http://java.52emu.cn/"+index.substringBefore("\'>"))
                }
                //解析预览图
                for (index in info.split("img")){
                    if (index.substringAfter("src=\"")!=index){
                        imagesList.add(index.substringAfter("src=\"").substringBefore("\">"))
                    }
                }
                //解析简介
                if (info.substringAfter("游戏简介")!=info){
                    info = info.substringAfter("【游戏简介】").substringAfter("</h3>").substringBefore("<hr />")
                }
                else{
                    info = "未找到简介"
                }
                activity.runOnUiThread {
                    contentFormat(activity,null,imagesList,downLinkList,downLinkNameList,info,true)
                }
            }catch (e: Exception) {
                apiDecode52emu(activity,path)
            }
        }.start()
    }
}

private fun apiDecodeJoyin(activity: StoreActivity, path: String?) {
    if (path != null) {
        lanzouApi(activity,"only",path,"1234")
    }
}

private fun apiDecodeBzyun(activity: StoreActivity, path: String?,name: String?) {
    if (path != null){
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
                        if (name != null){
                            //下载列表
                            if (temp.substringAfter(name)!=temp){//确认是可下载文件
                                downLinkNameList.add(temp.substringAfter(name+"_"))
                                downLinkList.add("https://od.bzyun.top/api/raw/?path=/J2ME应用商店$path/$name/$temp")
                                fileSizeList.add(data.getJSONObject(index-1).getString("name").toString())
                            }
                            //预览图
                            if (temp.substringAfter("img")!=temp && temp.substringAfter("img")!="ErrLog.txt"){
                                imagesList.add("https://od.bzyun.top/api/raw/?path=/J2ME应用商店$path/$name/$temp")
                            }
                            //图标
                            if (temp.substringAfter("pic")!=temp && temp.substringAfter("pic")!="ErrLog.txt"){contentFormat(activity,"https://od.bzyun.top/api/raw/?path=/J2ME应用商店/$path/$name/$temp",null,null,null,null,false)}
                            //简介
                            if (temp=="gameInfo.html"){
                                val requestInfo = Request.Builder()
                                    .url("https://od.bzyun.top/api/raw/?path=/J2ME应用商店$path/$name/$temp")
                                    .build()
                                thread {
                                    val responseInfo = client.newCall(requestInfo).execute()
                                    contentFormat(activity,null,null,null,null,responseInfo.body.string(),true)
                                }
                            }
                            //一次判断结束
                        }
                    }
                }//循环结束
                contentFormat(activity,null,imagesList,downLinkList,downLinkNameList,null,false)
            } catch (e: Exception) {
                apiDecodeBzyun(activity, path,name)
            }
        }.start()
    }
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
