package com.mBZo.jar

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
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
            if (from.substringAfter("没空云", "pass") != "pass"){ apiDecodeBzyun(this,path,name) }//匹配规则，没空云（OneIndexApi）
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
fun contentFormat(activity: AppCompatActivity,iconLink: String?,imageList: List<String>?,linkList: List<String>?,about: String?,loading: Boolean) {
    activity.runOnUiThread {
        val info = activity.findViewById<TextView>(R.id.storeInfo)
        val icon = activity.findViewById<ImageView>(R.id.ico)
        val loadingProgressBar = activity.findViewById<ProgressBar>(R.id.storeLoadingMain)
        val recyclerView: RecyclerView = activity.findViewById(R.id.storeImages)
        //简介
        if (about != null){
            info.visibility = View.VISIBLE
            info.text = about
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
        //隐藏加载
        if (loading){ loadingProgressBar.visibility = View.INVISIBLE }
    }
}

private fun apiDecodeBzyun(activity: StoreActivity, path: String?,name: String?) {
    if (path != null){
        val addPath = "https://od.bzyun.top/api/?path=/J2ME应用商店/$path/$name"
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
                val imagesList = mutableListOf<String>()
                var temp:String
                for (index in 1..data.length()){
                    temp=data.getJSONObject(index-1).getString("name").toString()
                    if (data.getJSONObject(index-1).getJSONObject("file").toString()!=""){//确认项目不是文件夹
                        if (name != null){
                            //下载列表
                            if (temp.substringAfter(name)!=temp){//确认是可下载文件
                                downLinkList.add(temp.substringAfter(name+"_"))
                            }
                            //预览图
                            if (temp.substringAfter("img")!=temp && temp.substringAfter("img")!="ErrLog.txt"){
                                imagesList.add("https://od.bzyun.top/api/raw/?path=/J2ME应用商店/$path/$name/$temp")
                            }
                            //图标
                            if (temp.substringAfter("pic")!=temp && temp.substringAfter("pic")!="ErrLog.txt"){contentFormat(activity,"https://od.bzyun.top/api/raw/?path=/J2ME应用商店/$path/$name/$temp",null,null,null,false)}
                            //简介
                            if (temp=="gameInfo.html"){
                                val requestInfo = Request.Builder()
                                    .url("https://od.bzyun.top/api/raw/?path=/J2ME应用商店/$path/$name/$temp")
                                    .build()
                                thread {
                                    val responseInfo = client.newCall(requestInfo).execute()
                                    contentFormat(activity,null,null,null,responseInfo.body.string(),true)
                                }
                            }
                            //一次判断结束
                        }
                    }
                }//循环结束
                contentFormat(activity,null,imagesList,downLinkList,null,false)
            } catch (e: Exception) {
                apiDecodeBzyun(activity, path,name)
            }
        }.start()
    }
}


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
