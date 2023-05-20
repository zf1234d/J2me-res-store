package com.mBZo.jar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.R.*
import com.mBZo.jar.adapter.ArchiveRecyclerAdapter
import com.mBZo.jar.tool.FileLazy
import com.mBZo.jar.tool.imageLoad
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.inputstream.ZipInputStream
import okhttp3.*
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory
import zlc.season.downloadx.download
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.schedule


var isPlay: Boolean? = null//play检测
var onlineInfo = ""
var archiveNum=0//本地的库存数
val gameList = mutableListOf<Array<String>>()
val otaUrl={ onlineInfo.substringAfter("更新地址★").substringBefore("☆更新地址") }
val archiveVer={ onlineInfo.substringAfter("有效日期★").substringBefore("☆有效日期") }//云端库存版本
val cloudVer={ onlineInfo.substringAfter("云端版号★").substringBefore("☆云端版号").toInt() }//云端软件版本
const val netWorkRoot="https://dev.azure.com/CA0115/e189f55c-a98a-4d73-bc09-4a5b822b9563/_apis/git/repositories/589e5978-bff8-4f4d-a328-c045f4237299/items?path="


class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate).addOnViewCreatedListener(WindowInsetsHelper.LISTENER)
        super.onCreate(savedInstanceState)
        AppCenter.start(application, AppCenterSecret, Analytics::class.java, Crashes::class.java)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(layout.activity_main)
        val spfRecord: SharedPreferences = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
        //布置viewpager2
        val mFragments = ArrayList<Fragment>()
        val viewpager: ViewPager2 = findViewById(id.home_page_tree)
        val nav: BottomNavigationView = findViewById(id.home_nav)
        val loading: ProgressBar = findViewById(id.progressBar2)
        viewpager.offscreenPageLimit = 3
        mFragments.add(ArchiveFragment())
        mFragments.add(HomeFragment())
        mFragments.add(SettingRootFragment())
        viewpager.adapter = MainFragmentPagerAdapter(this, mFragments)
        //绑定底栏和viewpager
        //设置默认主页为第二个
        val startPage = spfRecord.getString("startPage","home")
        if (startPage=="home"){
            viewpager.setCurrentItem(1,false)
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
                    val loadImg: ImageView = findViewById(id.state1)
                    val loadInfo: TextView = findViewById(id.state2)
                    val noticeCard: MaterialCardView = findViewById(id.state4)
                    val noticeInfo: TextView = findViewById(id.state5)
                    //不同情况共通加载内容
                    noticeCard.visibility = View.VISIBLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        noticeInfo.text = Html.fromHtml(uc1.replace("\n","<br>"),Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        @Suppress("DEPRECATION")
                        noticeInfo.text = Html.fromHtml(uc1.replace("\n","<br>"))
                    }
                    noticeInfo.movementMethod = LinkMovementMethod.getInstance()
                    archiveNum = uc2.toInt()
                    //不同情况差异加载内容
                    if (BuildConfig.VERSION_CODE >= cloudVer.invoke()) {
                        if (!File("${filesDir.absolutePath}/mBZo/java/list/0.list").exists()){
                            FileLazy("${filesDir.absolutePath}/mBZo/java/list/0.list").writeNew("000000")
                        }
                        val localAuth = FileLazy("${filesDir.absolutePath}/mBZo/java/list/0.list").read()
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
                            imageLoad(this,loadImg,drawable.ic_baseline_update_24)
                            loading.visibility = View.GONE
                        }
                    }
                    else{
                        when (isPlay) {
                            true -> {
                                nowReadArchiveList(this)
                            }
                            null -> {
                                updateConfigSave(uc1,uc2)
                            }
                            else -> {
                                if (startPage=="search"){
                                    nav.menu.getItem(1).isChecked = true
                                }
                                loadInfo.text = resources.getString(string.findNewApp)
                                imageLoad(this,loadImg,drawable.ic_baseline_update_24)
                                loading.visibility = View.GONE
                                nowReadArchiveList(this,false)
                            }
                        }
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
                    updateConfig()
                }
            }.start()
        }
        updateConfig()
        //检查更新结束
    }
    //按键事件
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val nav: BottomNavigationView = findViewById(id.home_nav)
        val viewpager: ViewPager2 = findViewById(id.home_page_tree)
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val spfRecord: SharedPreferences = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
            val keepApp = spfRecord.getBoolean("keepActivity",true)
            if (viewpager.currentItem!=1){
                //关闭搜索页的搜索栏
                val toolbar: Toolbar = findViewById(id.archive_toolbar)
                val searchItem: MenuItem? = toolbar.menu.findItem(id.toolbar_search)
                searchItem?.collapseActionView()
                //回到主页面
                viewpager.setCurrentItem(1, false)
                nav.menu.getItem(1).isChecked = true
            }
            else{
                if (keepApp) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.addCategory(Intent.CATEGORY_HOME)
                    startActivity(intent)
                }
                else {
                    finish()
                }
            }
            return true
        }
        else{
            return super.onKeyDown(keyCode, event)
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


//库存更新
@OptIn(DelicateCoroutinesApi::class)
fun syncArchive(activity: Activity?, type: String, typeImg: Int) {
    if (activity!=null) {
        val loadImg: ImageView = activity.findViewById(id.state1)
        val loadInfo: TextView = activity.findViewById(id.state2)
        val loading: ProgressBar = activity.findViewById(id.progressBar2)
        val savePath = "${activity.filesDir.absolutePath}/mBZo/java/list/"
        var tip: String
        var client: OkHttpClient
        var request: Request
        var response: Response
        loadInfo.text = activity.getString(string.updateNewArchive)
        imageLoad(activity,loadImg,drawable.ic_baseline_query_builder_24)
        loading.visibility = View.VISIBLE
        //批量下载一串路径
        fun processDownloadArchive(path: MutableList<String>, process: Int) {
            val saveName = path[process].substringAfterLast("/").substringBefore(".zip")
            val downloadTask = GlobalScope.download(path[process], "$saveName.zip", savePath)
            downloadTask.state()
                .onEach { state ->
                    if (downloadTask.isSucceed()) {
                        try {
                            //回UI
                            activity.runOnUiThread {
                                tip = "${activity.getString(string.updateNewArchive)}[${process + 1}(读取)/${path.size}]"
                                val zipFile = ZipFile(File("$savePath$saveName.zip"))
                                val fileRn = File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list")
                                loadInfo.text = tip
                                zipFile.isRunInThread = false
                                for (index in zipFile.fileHeaders){
                                    if (index.fileName=="$saveName.txt") {
                                        val inputStream: ZipInputStream =
                                            zipFile.getInputStream(index)
                                        //流转字符串
                                        val result = ByteArrayOutputStream()
                                        val buffer = ByteArray(1024)
                                        var length: Int
                                        while (
                                            inputStream.read(buffer).also { length = it } != -1
                                        ) {
                                            result.write(buffer, 0, length)
                                        }
                                        //写入
                                        fileRn.appendText("\n${result.toString(StandardCharsets.UTF_8.name())}")
                                    }
                                }
                                downloadTask.remove()
                                if (process + 1 >= path.size) {
                                    tip = "${activity.getString(string.updateNewArchive)}[${process + 1}(载入)/${path.size}]"
                                    loadInfo.text = tip
                                    path.clear()
                                    FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/0.list")
                                        .writeNew(archiveVer.invoke())
                                    nowReadArchiveList(activity)
                                } else {
                                    tip = "${activity.getString(string.updateNewArchive)}[${process + 1}(请求)/${path.size}]"
                                    loadInfo.text = tip
                                    processDownloadArchive(path, process + 1)
                                }
                            }
                        } catch (e: Exception) {
                            activity.runOnUiThread {
                                downloadTask.remove()
                                loadInfo.text = activity.getString(string.findNewArchiveUnexpectedStop)
                            }
                        }
                    } else if (downloadTask.isStarted()) {
                        if (state.progress.totalSize != (0).toLong()) {
                            activity.runOnUiThread {
                                tip = "${activity.getString(string.updateNewArchive)}[${process}(${
                                    state.progress.percent().toInt()
                                }%)/${path.size}]"
                                loadInfo.text = tip
                            }
                        }
                    } else if (downloadTask.isFailed()) {
                        downloadTask.start()
                    }
                }.launchIn(GlobalScope)
            downloadTask.start()
        }

        //收集需要下载的条目
        fun startDownloadArchive(
            pathList: MutableList<String>,
            addonPathList: MutableList<String>,
            stateList: MutableList<Boolean>
        ) {
            loading.visibility = View.VISIBLE
            loadInfo.text = activity.getString(string.updateNewArchive)
            imageLoad(activity,loadImg,drawable.ic_baseline_query_builder_24)
            Thread {
                val finPathList = mutableListOf<String>()
                finPathList.clear()
                //拼接默认仓库
                for (index in pathList) {
                    finPathList.add("${netWorkRoot}/jarlist/Archive/raw/${index}.zip")
                }
                //拼接额外资源
                for (index in 1..stateList.size) {
                    if (stateList[index - 1]) {
                        finPathList.add("${netWorkRoot}/jarlist/Archive/raw/${addonPathList[index - 1]}.zip")
                    }
                }
                //传入路径批量下载
                tip = "${activity.getString(string.updateNewArchive)}[0/${finPathList.size}]"
                activity.runOnUiThread {
                    loadInfo.text = tip
                    pathList.clear()
                    addonPathList.clear()
                    stateList.clear()
                    processDownloadArchive(finPathList, 0)
                }
            }.start()
        }

        //询问数量
        fun askAnoArchSelect(data: String) {
            val list0 = mutableListOf<String>()//不可选泽path列表
            val list1 = mutableListOf<String>()//path列表
            val list2 = mutableListOf<String>()//namespace列表
            val list3 = mutableListOf<Boolean>()//选择结果列表
            for (index in data.split("\n")) {
                if (index.contains("{") && index.contains("//close").not()) {
                    if (index.contains(",")) {
                        list1.add(index.substringAfter("{").substringBefore(","))
                        list2.add(index.substringAfter(",").substringBefore("}"))
                        list3.add(false)
                    } else {
                        list0.add(index.substringAfter("{").substringBefore("}"))
                    }
                }
            }
            activity.runOnUiThread {
                loadInfo.text = type
                imageLoad(activity,loadImg,typeImg)
                loading.visibility = View.GONE
                MaterialAlertDialogBuilder(activity)
                    .setTitle("额外库存源(可以不选)")
                    .setMultiChoiceItems(
                        list2.toTypedArray(),
                        null
                    ) { _, index, state -> list3[index] = state }
                    .setPositiveButton("开始同步") { _, _ ->
                        FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/0.list").writeNew(
                            "process"
                        )
                        FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").writeNew()
                        list2.clear()
                        startDownloadArchive(list0, list1, list3)
                    }
                    .show()
            }
        }
        //网络线程
        Thread {
            try {
                client = OkHttpClient()
                request = Request.Builder()
                    .url("$netWorkRoot/jarlist/Archive/setup.hanpi")
                    .build()
                response = client.newCall(request).execute()
                val data = response.body.string()
                askAnoArchSelect(data)
            } catch (e: Exception) {
                syncArchive(activity, type, typeImg)
            }
        }.start()
    }
}

//读仓库
@SuppressLint("NotifyDataSetChanged")
fun nowReadArchiveList(activity: Activity,successConnect: Boolean = true) {
    Thread {
        var count = 0
        //清空list中遗留的数据
        gameList.clear()
        //读文件并转换成列表然后循环判断类型
        if (File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").exists().not()){
            FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").writeNew()
        }
        val archiveFileLines = FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").readLines()
        var nameTemp="";var nameFCTemp="";var fromTemp=""
        for (str in archiveFileLines){
            if (str.contains("\"name\"")){
                nameTemp=str.substringAfter("\"name\":\"").substringBefore("\"")
                count++
            }
            else if (str.contains("\"nameFc\"")){
                nameFCTemp=str.substringAfter("\"nameFc\":\"").substringBefore("\"")
            }
            else if (str.contains("\"from\"")){
                fromTemp=str.substringAfter("\"from\":\"").substringBefore("\"")
            }
            else if (str.contains("\"url\"")){
                gameList.add(arrayOf(nameTemp,nameFCTemp,fromTemp,str.substringAfter("\"url\":\"").substringBefore("\"")))
            }
        }
        if (nameFCTemp!=""){
            gameList.sortBy { it[1] }
        }

        val loading: ProgressBar = activity.findViewById(id.progressBar2)
        val loadImg: ImageView = activity.findViewById(id.state1)
        val loadInfo: TextView = activity.findViewById(id.state2)
        val recyclerView: RecyclerView = activity.findViewById(id.recycler_archive)
        val btmNav: BottomNavigationView = activity.findViewById(id.home_nav)
        val layoutManager = LinearLayoutManager(activity)
        val adapter = ArchiveRecyclerAdapter(activity, gameList)
        activity.runOnUiThread {
            //改变界面
            if (successConnect){
                loadInfo.text = activity.getString(string.allReady)
                imageLoad(activity,loadImg,drawable.ic_baseline_check_24)
                loading.visibility = View.GONE
            }
            //设置recyclerView
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            btmNav.getOrCreateBadge(id.nav_search).maxCharacterCount = 7
            btmNav.getOrCreateBadge(id.nav_search).number = count
        }
    }.start()
}





