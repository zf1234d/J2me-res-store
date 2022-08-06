package com.mBZo.jar

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.*
import java.io.File
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.multidex.MultiDex
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.nio.charset.Charset
import kotlin.collections.ArrayList


var archiveNum=0
var archiveB64C=""
var archiveVer=""
var otaUrl=""
const val netWorkRoot="https://dev.azure.com/CA0115/e189f55c-a98a-4d73-bc09-4a5b822b9563/_apis/git/repositories/589e5978-bff8-4f4d-a328-c045f4237299/items?path="




class MainActivity : AppCompatActivity(), View.OnClickListener  {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //布置viewpager2
        val mFragments = ArrayList<Fragment>()
        val viewpager: ViewPager2 = findViewById(R.id.home_page_tree)
        val nav: BottomNavigationView = findViewById(R.id.home_nav)
        val loading: ProgressBar = findViewById(R.id.progressBar2)
        val versionCode: Int = BuildConfig.VERSION_CODE
        mFragments.add(ArchiveFragment())
        mFragments.add(HomeFragment())
        mFragments.add(settingrootFragment())
        viewpager.offscreenPageLimit = 3
        viewpager.adapter = MainFragmentPagerAdapter(this, mFragments)
        //绑定底栏和viewpager
        //设置默认主页为第二个
        nav.menu.getItem(1).isChecked = true
        viewpager.currentItem = 1
        //底栏按钮监听
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_search -> {
                    viewpager.currentItem = 0
                }
                R.id.nav_main -> {
                    viewpager.currentItem = 1
                }
                R.id.nav_setting -> {
                    viewpager.currentItem = 2
                }
            }
            return@setOnItemSelectedListener true
        }
        //viewpager滑动监听
        viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                nav.menu.getItem(position).isChecked = true
            }
        })
        //结束了，这样就绑定好了

        //联网获取软件配置信息
        fun updateConfig() {
            fun updateConfigSave(uc1: String, uc2: String, uc3: String, uc4: String, uc5: String, data: String) {
                runOnUiThread {
                    val btn1: MaterialCardView = findViewById(R.id.btn1)
                    btn1.setOnClickListener(this)
                    val loadImg: ImageView = findViewById(R.id.state1)
                    val loadInfo: TextView = findViewById(R.id.state2)
                    val noticeCard: MaterialCardView = findViewById(R.id.state4)
                    val noticeInfo: TextView = findViewById(R.id.state5)
                    if (versionCode > uc4.toInt()) {
                        if (!File("${filesDir.absolutePath}/mBZo/java/list/0.list").exists()){
                            lazyWriteFile("${filesDir.absolutePath}/mBZo/java/list/","0.list","000000")
                        }
                        val localAuth = File("${filesDir.absolutePath}/mBZo/java/list/0.list").readText()
                        if (localAuth == uc3) {
                            noticeInfo.text = uc1
                            noticeCard.visibility = View.VISIBLE
                            archiveNum = uc2.toInt()
                            archiveB64C = data
                            nowReadArchiveList(this)
                        }
                        else{
                            loadInfo.text = getString(R.string.findNewArchive)
                            Glide.with(this).load(R.drawable.ic_baseline_update_24).into(loadImg)
                            noticeInfo.text = uc1
                            noticeCard.visibility = View.VISIBLE
                            loading.visibility = View.GONE
                            archiveNum = uc2.toInt()
                            archiveB64C = data
                            archiveVer = uc3
                        }
                    }
                    else{
                        loadInfo.text = resources.getString(R.string.findNewApp)
                        Glide.with(this).load(R.drawable.ic_baseline_update_24).into(loadImg)
                        noticeInfo.text = uc1
                        noticeCard.visibility = View.VISIBLE
                        loading.visibility = View.GONE
                        otaUrl = uc5
                    }
                }
            }
            Thread {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("$netWorkRoot/jarlist/ready.set")
                        .build()
                    val response = client.newCall(request).execute()
                    var data = response.body.string()
                    val uc1 = data.substringAfter("公告板★\r\n").substringBefore("\r\n☆公告板")
                    val uc2 = data.substringAfter("热更新★").substringBefore("☆热更新")
                    val uc3 = data.substringAfter("有效日期★").substringBefore("☆有效日期")
                    val uc4 = data.substringAfter("云端版号★").substringBefore("☆云端版号")
                    val uc5 = data.substringAfter("更新地址★").substringBefore("☆更新地址")
                    data = data.substringAfter("{裁剪线}")//data转换
                    updateConfigSave(uc1,uc2,uc3,uc4,uc5,data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
        updateConfig()
        //检查更新结束
    }
    //点击事件
    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btn1 -> {
                val loadInfo: TextView = findViewById(R.id.state2)
                //同步库存
                if (loadInfo.text == getString(R.string.findNewArchive)){
                    syncArchive(this,getString(R.string.findNewArchive),R.drawable.ic_baseline_update_24)
                }
                else if (loadInfo.text == "已连接"){
                    MaterialAlertDialogBuilder(this)
                        .setPositiveButton("更改库存"){_,_ -> syncArchive(this,"已连接",R.drawable.ic_baseline_check_24)}
                        .show()
                }
            }
        }
    }
}


class MainFragmentPagerAdapter(fragmentActivity: FragmentActivity, private val mFragments: List<Fragment>
) : FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return mFragments[position]
    }

    override fun getItemCount(): Int {
        return mFragments.size
    }
}


fun lazyWriteFile(path: String,name: String,content: String){
    File(path).mkdirs()
    File("$path/$name").writeText(content)
}


//库存更新
private fun syncArchive(activity: AppCompatActivity,type: String,typeImg: Int) {
    val loadImg: ImageView = activity.findViewById(R.id.state1)
    val loadInfo: TextView = activity.findViewById(R.id.state2)
    val loading: ProgressBar = activity.findViewById(R.id.progressBar2)
    var tip: String
    loadInfo.text = activity.getString(R.string.updateNewArchive)
    Glide.with(activity).load(R.drawable.ic_baseline_query_builder_24).into(loadImg)
    loading.visibility = View.VISIBLE
    lazyWriteFile("${activity.filesDir.absolutePath}/mBZo/java/list/","1.list","")
    //批量下载一串路径
    fun processDownloadArchive(urlPath: Array<String>) {
        var process = 0
        fun getOkHttp2File(url: String){
            fun saveGetAsFile(data: String) {
                activity.runOnUiThread {
                    File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").appendText(dcBase64(data),
                        Charset.defaultCharset())
                    process += 1
                    //时间可能很长啊，记录个进度，没毛病啊
                    tip = "${activity.getString(R.string.updateNewArchive)}($process/${urlPath.size})"
                    loadInfo.text = tip
                    //下载完成事件
                    if (process == urlPath.size){
                        lazyWriteFile("${activity.filesDir.absolutePath}/mBZo/java/list/","0.list", archiveVer)
                        nowReadArchiveList(activity)
                    }
                }
            }
            Thread {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    val response = client.newCall(request).execute()
                    val data = response.body.string()
                    saveGetAsFile(data)
                } catch (e: Exception) {
                    getOkHttp2File(url)
                }
            }.start()
        }
        for (str in urlPath){
            getOkHttp2File(str)
        }
    }
    //下载仓库  参数为（路径，数量，是否下载）
    fun startDownloadArchive(list1: Array<String?>, list2: Array<Int?>, list4: Array<Boolean?>) {
        loading.visibility = View.VISIBLE
        loadInfo.text = activity.getString(R.string.updateNewArchive)
        Glide.with(activity).load(R.drawable.ic_baseline_query_builder_24).into(loadImg)
        //kotlin不支持数组变长度，创建一个动态改变数组长度的fun
        fun append(arr: Array<String>, element: String): Array<String> {
            val list: MutableList<String> = arr.toMutableList()
            list.add(element)
            return list.toTypedArray()
        }
        var urlPath = arrayOf<String>()
        //拼接默认仓库
        for (index in 1..archiveNum){
            urlPath=append(urlPath,dcBase64(archiveB64C.substringAfter("{").substringBefore("}")))
            archiveB64C = archiveB64C.substringAfter("}")
        }
        //拼接额外资源
        for (index in 1..list2.size){
            if (list4[index-1] == true){
                for(index0 in 1..list2[index-1]!!){
                    urlPath=append(urlPath,"$netWorkRoot/jarlist/expandlist/${list1[index-1]}/${list1[index-1]}_$index0")
                }
            }
        }
        //传入路径批量下载
        tip = "${activity.getString(R.string.updateNewArchive)}(0/${urlPath.size})"
        loadInfo.text = tip
        processDownloadArchive(urlPath)
    }
    //询问数量
    fun askAnoArchSelect(data: String) {
        activity.runOnUiThread{
            loadInfo.text = type
            Glide.with(activity).load(typeImg).into(loadImg)
            loading.visibility = View.GONE
            var temp =data
            val list1: Array<String?> = arrayOfNulls (temp.substringAfter("list=").substringBefore("\r\n").toInt())
            val list2: Array<Int?> = arrayOfNulls (temp.substringAfter("list=").substringBefore("\r\n").toInt())
            val list3: Array<String?> = arrayOfNulls (temp.substringAfter("list=").substringBefore("\r\n").toInt())
            val list4: Array<Boolean?> = arrayOfNulls (temp.substringAfter("list=").substringBefore("\r\n").toInt())
            for (index in 1..temp.substringAfter("list=").substringBefore("\r\n").toInt()){
                list3[index-1] = temp.substringAfter("{").substringBefore("}")
                list1[index-1] = list3[index-1]?.substringAfter("{")?.substringBefore(",")?.let { dcBase64(it) }
                list2[index-1] = list3[index-1]?.substringAfter(",")?.substringBefore(",")?.toInt()
                list3[index-1] = list3[index-1]?.substringAfterLast(",")?.substringBefore("}")?.let { dcBase64(it) }
                list4[index-1] = false
                temp = temp.substringAfter("}")
            }
            MaterialAlertDialogBuilder(activity)
                .setTitle("可额外添加这些来源的库存")
                .setMultiChoiceItems(list3,null){_,index,state ->  list4[index] = state}
                .setPositiveButton("开始同步"){ _,_ -> startDownloadArchive(list1,list2,list4)}
                .show()
        }
    }
    //网络线程
    Thread {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$netWorkRoot/jarlist/expandlist/set.hanpi")
                .build()
            val response = client.newCall(request).execute()
            val data = response.body.string()
            askAnoArchSelect(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}

//读仓库
fun nowReadArchiveList(activity: AppCompatActivity) {
    val loading: ProgressBar = activity.findViewById(R.id.progressBar2)
    val loadImg: ImageView = activity.findViewById(R.id.state1)
    val loadInfo: TextView = activity.findViewById(R.id.state2)
    val recyclerView: RecyclerView = activity.findViewById(R.id.recyclerView)
    loadInfo.text = "已连接"
    Glide.with(activity).load(R.drawable.ic_baseline_check_24).into(loadImg)
    loading.visibility = View.GONE
    val textList = arrayListOf("我的关注","通知开关", "我的徽章", "意见反馈", "我要投稿",
        "我的关注","通知开关", "我的徽章", "意见反馈", "我要投稿",
        "我的关注","通知开关","我的徽章","意见反馈","我要投稿")
    //设置recyclerView
    val layoutManager = LinearLayoutManager(activity)
    recyclerView.layoutManager = layoutManager
    val adapter = RecyclerAdapter(textList)
    recyclerView.adapter = adapter
}



//解码base64
fun dcBase64(string: String): String {
    return  String(Base64.decode(string.toByteArray(),Base64.NO_WRAP))
}

//仓库的RecyclerView
class RecyclerAdapter(private val textList: ArrayList<String>) :
    RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = textList.size ?: 0

    override fun onBindViewHolder(holder: RecyclerAdapter.MyViewHolder, position: Int) {
        val textpos = textList[position]
        holder.title.text = textpos
        holder.itemView.setOnClickListener {
            Toast.makeText(holder.itemView.context, "${holder.title.text}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.archiveItemName)
    }
}
