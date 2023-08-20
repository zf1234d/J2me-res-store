package com.mBZo.jar

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mBZo.jar.R.id
import com.mBZo.jar.databinding.ActivityMainBinding
import com.mBZo.jar.tool.attachDynamicColor
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory


const val netWorkRoot="https://dev.azure.com/CA0115/e189f55c-a98a-4d73-bc09-4a5b822b9563/_apis/git/repositories/589e5978-bff8-4f4d-a328-c045f4237299/items?path="
var onlineInfo = ""
//更新地址
val otaUrl={ onlineInfo.substringAfter("更新地址★").substringBefore("☆更新地址") }
//云端库存版本
val archiveCVer={ onlineInfo.substringAfter("有效日期★").substringBefore("☆有效日期") }
//云端库存数
val archiveCNum={ onlineInfo.substringAfter("热更新★").substringBefore("☆热更新").toInt() }
//云端软件版本
val appCVer={ onlineInfo.substringAfter("云端版号★").substringBefore("☆云端版号").toInt() }
//公告
val notice={ onlineInfo.substringAfter("公告板★\r\n").substringBefore("\r\n☆公告板") }


class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate).addOnViewCreatedListener(WindowInsetsHelper.LISTENER)
        super.onCreate(savedInstanceState)
        attachDynamicColor()
        binding = ActivityMainBinding.inflate(layoutInflater)
        AppCenter.start(application, AppCenterSecret, Analytics::class.java, Crashes::class.java)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        val sp: SharedPreferences = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)
        //布置viewpager2
        val mFragments = ArrayList<Fragment>()
        binding.homePageTree.offscreenPageLimit = 3
        mFragments.add(ArchiveFragment())
        mFragments.add(HomeFragment())
        mFragments.add(SettingRootFragment())
        binding.homePageTree.adapter = MainFragmentPagerAdapter(this, mFragments)
        //绑定底栏和viewpager,设置默认主页为第二个
        val startPage = sp.getString("startPage","home")
        if (startPage=="home"){
            binding.homePageTree.setCurrentItem(1,false)
            binding.homeNav.menu.getItem(1).isChecked = true
        }
        //底栏按钮监听
        binding.homeNav.setOnItemSelectedListener {
            when (it.itemId) {
                id.nav_search -> {
                    binding.homePageTree.currentItem = 0
                }
                id.nav_main -> {
                    binding.homePageTree.currentItem = 1
                }
                id.nav_setting -> {
                    binding.homePageTree.currentItem = 2
                }
            }
            return@setOnItemSelectedListener true
        }
        //viewpager滑动监听
        binding.homePageTree.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.homeNav.menu.getItem(position).isChecked = true
            }
        })
        //结束了，这样就绑定好了

        //允许左右滑动嘛？
        val usefulFun = sp.getBoolean("viewpagerIsBad",false)
        if (usefulFun) {
            binding.homePageTree.isUserInputEnabled = false
        }

        //附加
        binding.homeNav.getOrCreateBadge(id.nav_search).isVisible = false
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

    class MainFragmentPagerAdapter(fragmentActivity: FragmentActivity, private val mFragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return mFragments[position]
        }

        override fun getItemCount(): Int {
            return mFragments.size
        }
    }
}
