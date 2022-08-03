package com.mBZo.jar

import android.annotation.SuppressLint
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
import okhttp3.*
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //布置viewpager2
        val mFragments = ArrayList<Fragment>()
        val viewpager: ViewPager2 = findViewById(R.id.home_page_tree)
        val nav: BottomNavigationView = findViewById(R.id.home_nav)
        val loading: ProgressBar = findViewById(R.id.progressBar2)
        val versionCode: Int = BuildConfig.VERSION_CODE;
        val versionName: String = BuildConfig.VERSION_NAME;
        mFragments.add(ArchiveFragment())
        mFragments.add(HomeFragment())
        mFragments.add(settingrootFragment())
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

        //获取软件版本
        @SuppressLint("SetTextI18n")
        fun getverinfo() {
            try {
                //重复的来获取
                val versioncard: TextView = findViewById(R.id.state3)
                versioncard.text = "$versionName ($versionCode)"
            } catch (e: Exception) {
                Timer().schedule(50) {
                    getverinfo()
                }
            }
        }
        getverinfo()
        //联网获取软件配置信息
        fun updateConfigSave(uc1: String, uc2: String, uc3: String, uc4: String, uc5: String, data: String) {
            runOnUiThread {
                val loadImg: ImageView = findViewById(R.id.state1)
                val loadInfo: TextView = findViewById(R.id.state2)
                val noticeCard: MaterialCardView = findViewById(R.id.state4)
                val noticeInfo: TextView = findViewById(R.id.state5)
                if (versionCode > uc4.toInt()) {
                    if (!File("${filesDir.absolutePath}/mBZo/java/list/0.list").exists()){
                        easyWriteFile("${filesDir.absolutePath}/mBZo/java/list/","0.list","000000")
                    }
                    val localAuth = File("${filesDir.absolutePath}/mBZo/java/list/0.list",).readText()
                    if (localAuth == uc3) {
                        loadInfo.text = "已连接"
                        Glide.with(this).load(R.drawable.ic_baseline_check_24).into(loadImg);
                        noticeInfo.text = uc1
                        noticeCard.visibility = View.VISIBLE
                        loading.visibility = View.GONE
                    }
                    else{
                        loadInfo.text = "发现新库存，点击同步"
                        Glide.with(this).load(R.drawable.ic_baseline_update_24).into(loadImg);
                        noticeInfo.text = uc1
                        noticeCard.visibility = View.VISIBLE
                        loading.visibility = View.GONE
                    }
                }
                else{
                    loadInfo.text = "发现新版本，点击更新"
                    Glide.with(this).load(R.drawable.ic_baseline_update_24).into(loadImg);
                    noticeInfo.text = uc1
                    noticeCard.visibility = View.VISIBLE
                    loading.visibility = View.GONE
                }
            }
        }
        fun updateConfig() {
            Thread {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://dev.azure.com/CA0115/e189f55c-a98a-4d73-bc09-4a5b822b9563/_apis/git/repositories/589e5978-bff8-4f4d-a328-c045f4237299/items?path=/jarlist/ready.set")
                        .build()
                    val response = client.newCall(request).execute()
                    var data = response.body!!.string()
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


fun easyWriteFile(path: String,name: String,content: String){
    File(path).mkdirs()
    File(name).writeText(content)
}




