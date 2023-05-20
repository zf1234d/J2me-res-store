package com.mBZo.jar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.viewpager2.widget.ViewPager2
import com.mBZo.jar.tool.FileLazy
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http.RequestLine
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //设置左右滑动
        val viewpagerSdSet: Preference? = findPreference("viewpagerIsBad")
        val viewpager: ViewPager2? = activity?.findViewById(R.id.home_page_tree)
        viewpagerSdSet?.setOnPreferenceChangeListener { _, newValue ->
            viewpager?.isUserInputEnabled = (newValue as Boolean).not()
            return@setOnPreferenceChangeListener true
        }
        //下载管理和自动安装
        val downloadManager: Preference? = findPreference("downloadManager")
        val downloadAutoInstall: Preference? = findPreference("downloadAutoInstall")
        val downloader: SwitchPreferenceCompat? = findPreference("smartDownloader")
        downloadManager?.isVisible = (downloader?.isChecked == true)
        downloadAutoInstall?.isVisible = (downloader?.isChecked == true)
        downloader?.setOnPreferenceChangeListener { _, newValue ->
            downloadManager?.isVisible = newValue as Boolean
            downloadAutoInstall?.isVisible = newValue
            return@setOnPreferenceChangeListener true
        }
        //判断是否有下载管理是否为空，为空不可点
        downloadManager?.setOnPreferenceClickListener {
            if (downloadManager.summary!="无下载内容"){
                val fileList = FileLazy(view.context.filesDir.absolutePath+"/DlLog/").listFiles()
                if (fileList!=null && fileList.isNotEmpty()){
                    val intent = Intent(activity,DownloadActivity::class.java)
                    activity?.startActivity(intent)
                }
                else{
                    downloadManager.summary = "无下载内容"
                    Thread{
                        Thread.sleep(300)
                        activity?.runOnUiThread {
                            downloadManager.summary = ""
                        }
                    }.start()
                }
            }
            return@setOnPreferenceClickListener true
        }
        //

        //


    }
}

