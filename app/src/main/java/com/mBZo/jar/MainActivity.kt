package com.mBZo.jar

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.multidex.MultiDex
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mBZo.jar.R.*
import okhttp3.*
import java.io.File
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule


var name = mutableListOf<String>()
var from = mutableListOf<String>()
var path = mutableListOf<String>()
var onlineInfo = ""
var archiveNum=0//本地的库存数
val archiveB64C= { onlineInfo.substringAfter("{裁剪线}") }//库存列表
val otaUrl={ onlineInfo.substringAfter("更新地址★").substringBefore("☆更新地址") }
val archiveVer={ onlineInfo.substringAfter("有效日期★").substringBefore("☆有效日期") }//云端库存版本
val cloudVer={ onlineInfo.substringAfter("云端版号★").substringBefore("☆云端版号").toInt() }//云端软件版本
const val netWorkRoot="https://dev.azure.com/CA0115/e189f55c-a98a-4d73-bc09-4a5b822b9563/_apis/git/repositories/589e5978-bff8-4f4d-a328-c045f4237299/items?path="



class MainActivity : AppCompatActivity(), View.OnClickListener  {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)
        val spfRecord: SharedPreferences = getSharedPreferences("com.mBZo.jar_preferences", MODE_PRIVATE)
        //布置viewpager2
        val mFragments = ArrayList<Fragment>()
        val viewpager: ViewPager2 = findViewById(id.home_page_tree)
        val nav: BottomNavigationView = findViewById(id.home_nav)
        val loading: ProgressBar = findViewById(id.progressBar2)
        mFragments.add(ArchiveFragment())
        mFragments.add(HomeFragment())
        mFragments.add(settingrootFragment())
        viewpager.offscreenPageLimit = 2
        viewpager.adapter = MainFragmentPagerAdapter(this, mFragments)
        //绑定底栏和viewpager
        if (Build.VERSION.SDK_INT >=27){//设置导航栏颜色
            window.navigationBarColor = getColor(color.navigationBarColor)
        }
        //设置默认主页为第二个
        val startPage = spfRecord.getString("startPage","home")
        if (startPage=="home"){
            viewpager.setCurrentItem(1, false)
            nav.menu.getItem(1).isChecked = true
        }
        //底栏按钮监听
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                id.nav_search -> {
                    viewpager.currentItem = 0
                }
                id.nav_main -> {
                    viewpager.currentItem = 1
                }
                id.nav_setting -> {
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


        //允许左右滑动嘛？
        val usefulFun = spfRecord.getBoolean("viewpagerIsBad",false)
        if (usefulFun) {
            viewpager.isUserInputEnabled = false
        }

        //联网获取软件配置信息
        fun updateConfig() {
            fun updateConfigSave(uc1: String, uc2: String) {
                runOnUiThread {
                    val btn1: MaterialCardView = findViewById(id.btn1)
                    btn1.setOnClickListener(this)
                    val loadImg: ImageView = findViewById(id.state1)
                    val loadInfo: TextView = findViewById(id.state2)
                    val noticeCard: MaterialCardView = findViewById(id.state4)
                    val noticeInfo: TextView = findViewById(id.state5)
                    //不同情况共通加载内容
                    noticeCard.visibility = View.VISIBLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        noticeInfo.text = Html.fromHtml(uc1.replace("\n","<br>"),Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        noticeInfo.text = Html.fromHtml(uc1.replace("\n","<br>"))
                    }
                    noticeInfo.movementMethod = LinkMovementMethod.getInstance()
                    archiveNum = uc2.toInt()
                    //不同情况差异加载内容
                    if (BuildConfig.VERSION_CODE >= cloudVer.invoke()) {
                        if (!File("${filesDir.absolutePath}/mBZo/java/list/0.list").exists()){
                            lazyWriteFile("${filesDir.absolutePath}/mBZo/java/list/","0.list","000000")
                        }
                        val localAuth = File("${filesDir.absolutePath}/mBZo/java/list/0.list").readText()
                        if (localAuth == archiveVer.invoke()) {
                            nowReadArchiveList(this)
                        }
                        else{
                            if (startPage=="search") {
                                viewpager.setCurrentItem(1, false)
                                nav.menu.getItem(1).isChecked = true
                            }
                            if (localAuth == "process") {
                                loadInfo.text = getString(string.findNewArchiveUnexpectedStop)
                            }
                            else {
                                loadInfo.text = getString(string.findNewArchive)
                            }
                            Glide.with(this).load(drawable.ic_baseline_update_24).into(loadImg)
                            loading.visibility = View.GONE
                        }
                    }
                    else{
                        if (startPage=="search"){
                            viewpager.setCurrentItem(1, false)
                            nav.menu.getItem(1).isChecked = true
                        }
                        loadInfo.text = resources.getString(string.findNewApp)
                        Glide.with(this).load(drawable.ic_baseline_update_24).into(loadImg)
                        loading.visibility = View.GONE
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
                    val data = response.body.string()
                    onlineInfo = data
                    val uc1 = data.substringAfter("公告板★\r\n").substringBefore("\r\n☆公告板")
                    val uc2 = data.substringAfter("热更新★").substringBefore("☆热更新")
                    updateConfigSave(uc1,uc2)
                } catch (e: Exception) {
                    //出错自动重试
                    Timer().schedule(5) {
                        updateConfig()
                    }
                }
            }.start()
        }
        updateConfig()
        //检查更新结束
    }
    //点击事件
    //TODO 要加功能
    override fun onClick(p0: View?) {
        when (p0?.id) {
            id.btn1 -> {
                val loadInfo: TextView = findViewById(id.state2)
                //同步库存
                when (loadInfo.text) {
                    getString(string.findNewArchive),getString(string.findNewArchiveUnexpectedStop) -> {
                        syncArchive(this,getString(string.findNewArchive), drawable.ic_baseline_update_24)
                    }
                    getString(string.findNewApp) -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addCategory(Intent.CATEGORY_BROWSABLE)
                            intent.data = Uri.parse(otaUrl.invoke())
                            startActivity(intent,null)
                        } catch (e: Exception) {
                            Toast.makeText(this,getString(string.notFindBrowser),Toast.LENGTH_SHORT).show()
                        }
                    }
                    getString(string.allReady) -> {
                        MaterialAlertDialogBuilder(this)
                            .setMessage("版本\n${BuildConfig.VERSION_CODE} (云端${cloudVer.invoke()})\n\n库存\n${File("${filesDir.absolutePath}/mBZo/java/list/0.list").readText()}\n\n通道\n${BuildConfig.BUILD_TYPE}\n\n系统\n${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT})\n\n目标\n${this.applicationInfo.targetSdkVersion}")
                            .setPositiveButton("更改库存"){_,_ -> syncArchive(this,getString(string.allReady), drawable.ic_baseline_check_24)}
                            .show()
                    }
                    else -> { }
                }
            }
        }
    }
    //按键事件
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val searchBar: TextInputEditText? = findViewById(id.search_bar)
            if (searchBar?.hasFocus() == true){
                searchBar.clearFocus()
                return false
            }
            else {
                val spfRecord: SharedPreferences = getSharedPreferences("com.mBZo.jar_preferences", MODE_PRIVATE)
                val keepApp = spfRecord.getBoolean("keepActivity",true)
                if (keepApp) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.addCategory(Intent.CATEGORY_HOME)
                    startActivity(intent)
                    return true
                }
                else {
                    finish()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
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
    val loadImg: ImageView = activity.findViewById(id.state1)
    val loadInfo: TextView = activity.findViewById(id.state2)
    val loading: ProgressBar = activity.findViewById(id.progressBar2)
    var tip: String
    var process = 0
    var client: OkHttpClient
    var request: Request
    var response: Response
    loadInfo.text = activity.getString(string.updateNewArchive)
    Glide.with(activity).load(drawable.ic_baseline_query_builder_24).into(loadImg)
    loading.visibility = View.VISIBLE
    //批量下载一串路径
    fun processDownloadArchive(urlPath: Array<String>) {
        Thread {
            fun getArchive(url: String){
                try {
                    client = OkHttpClient.Builder()
                    .connectTimeout(2,TimeUnit.SECONDS)
                    .readTimeout(3,TimeUnit.SECONDS)
                    .build()
                    request = Request.Builder()
                    .url(url)
                    .build()
                    response = client.newCall(request).execute()
                    val data = response.body.string()
                    File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").appendText("\n${dcBase64(data)}", Charset.forName("UTF-8"))
                    process += 1
                        //时间可能很长啊，记录个进度，没毛病啊
                    activity.runOnUiThread {
                        tip = "${activity.getString(string.updateNewArchive)}($process/${urlPath.size})"
                        loadInfo.text = tip
                        //下载完成事件
                        if (process == urlPath.size){
                            lazyWriteFile("${activity.filesDir.absolutePath}/mBZo/java/list/","0.list", archiveVer.invoke())
                            nowReadArchiveList(activity)
                        }
                    }
                } catch (e: Exception) {
                    getArchive(url)
                }
            }
            for (str in urlPath){
                getArchive(str)
            }
        }.start()
    }
    //下载仓库  参数为（路径，数量，是否下载）
    fun startDownloadArchive(list1: Array<String?>, list2: Array<Int?>, list4: Array<Boolean?>) {
        loading.visibility = View.VISIBLE
        loadInfo.text = activity.getString(string.updateNewArchive)
        Glide.with(activity).load(drawable.ic_baseline_query_builder_24).into(loadImg)
        //kotlin不支持数组变长度，创建一个动态改变数组长度的fun
        fun append(arr: Array<String>, element: String): Array<String> {
            val list: MutableList<String> = arr.toMutableList()
            list.add(element)
            return list.toTypedArray()
        }
        var urlPath = arrayOf<String>()
        //拼接默认仓库
        var localArchiveB64C = archiveB64C.invoke()
        for (index in 1..archiveNum){
            urlPath=append(urlPath,dcBase64(localArchiveB64C.substringAfter("{").substringBefore("}")))
            localArchiveB64C = localArchiveB64C.substringAfter("}")
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
        tip = "${activity.getString(string.updateNewArchive)}(0/${urlPath.size})"
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
                .setTitle("额外库存源(可以不选)")
                .setMultiChoiceItems(list3,null){_,index,state ->  list4[index] = state}
                .setPositiveButton("开始同步"){ _,_ ->
                    lazyWriteFile("${activity.filesDir.absolutePath}/mBZo/java/list/","0.list","process")
                    lazyWriteFile("${activity.filesDir.absolutePath}/mBZo/java/list/","1.list","")
                    startDownloadArchive(list1,list2,list4)
                }
                .show()
        }
    }
    //网络线程
    Thread {
        try {
            client = OkHttpClient()
            request = Request.Builder()
                .url("$netWorkRoot/jarlist/expandlist/set.hanpi")
                .build()
            response = client.newCall(request).execute()
            val data = response.body.string()
            askAnoArchSelect(data)
        } catch (e: Exception) {
            syncArchive(activity,type, typeImg)
        }
    }.start()
}

//读仓库
@SuppressLint("NotifyDataSetChanged")
fun nowReadArchiveList(activity: AppCompatActivity) {
    val loading: ProgressBar = activity.findViewById(id.progressBar2)
    val loadImg: ImageView = activity.findViewById(id.state1)
    val loadInfo: TextView = activity.findViewById(id.state2)
    val filterNameList = mutableListOf<String>()
    val filterFromList = mutableListOf<String>()
    val filterPathList = mutableListOf<String>()
    val searchBar: TextInputEditText = activity.findViewById(id.search_bar)
    val recyclerView: RecyclerView = activity.findViewById(id.recyclerView)
    val btmNav: BottomNavigationView = activity.findViewById(id.home_nav)
    var count = 0
    //清空list中遗留的数据
    name = mutableListOf()
    from = mutableListOf()
    path = mutableListOf()
    //改变界面
    loadInfo.text = activity.getString(string.allReady)
    Glide.with(activity).load(drawable.ic_baseline_check_24).into(loadImg)
    loading.visibility = View.GONE
    //读文件并转换成列表然后循环判断类型
    for (str in File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").readLines()){
        if (str.contains("\"name\"")){
            name.add(str.substringAfter("\"name\":\"").substringBefore("\""))
            count++//计数
            //这里加个容错，防止库存里有只因汤
            //实现方式是计算数组长度，正常来说name长度应该是比from和path大1，所以如果大2就是有缺失
            if (name.size-from.size == 2){
                from.add("损坏")
            }
            if (name.size-path.size == 2){
                path.add("损坏")
            }
        }
        else if (str.contains("\"from\"")){  from.add(str.substringAfter("\"from\":\"").substringBefore("\""))  }
        else if (str.contains("\"url\"")){  path.add(str.substringAfter("\"url\":\"").substringBefore("\""))  }

    }

    //设置recyclerView
    filterNameList.addAll(name)
    filterFromList.addAll(from)
    filterPathList.addAll(path)
    val layoutManager = LinearLayoutManager(activity)
    recyclerView.layoutManager = layoutManager
    val adapter = RecyclerAdapter(activity,filterNameList,filterFromList,filterPathList)
    recyclerView.adapter = adapter
    btmNav.getOrCreateBadge(id.nav_search).number = count
    btmNav.getOrCreateBadge(id.nav_search).maxCharacterCount = 6
    //动态读取nav高度，防止遮挡recycler
    recyclerView.setPadding(0,0,0,btmNav.height)

    //设置searchBar
    searchBar.addTextChangedListener {
        filterNameList.clear()
        filterFromList.clear()
        filterPathList.clear()
        for (i in 1..name.size) {
            if (name[i-1].contains(searchBar.text.toString())){
                filterNameList.add(name[i-1])
                filterFromList.add(from[i-1])
                filterPathList.add(path[i-1])
            }
        }
        adapter.notifyDataSetChanged()
    }
}



//解码base64
fun dcBase64(string: String): String {
    return  String(Base64.decode(string.toByteArray(),Base64.NO_WRAP))
}

//仓库的RecyclerView
class RecyclerAdapter(private val activity: AppCompatActivity,private val nameList: List<String>,private  val fromList: List<String>,private val pathList: List<String>) :
    RecyclerView.Adapter<RecyclerAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(layout.recycler_item, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int = nameList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val itemName = nameList[position]
        val itemFrom = fromList[position]
        //显示
        holder.name.text = itemName
        holder.from.text = itemFrom
        //点击
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StoreActivity::class.java)
            intent.putExtra("name",nameList[position])
            intent.putExtra("from",fromList[position])
            intent.putExtra("path",pathList[position])
            activity.startActivity(intent)
        }
    }


    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(id.archiveItemName)
        val from: TextView = itemView.findViewById(id.archiveItemFrom)
    }
}



