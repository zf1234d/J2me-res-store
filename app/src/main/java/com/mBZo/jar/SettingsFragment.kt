package com.mBZo.jar

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.DynamicColors
import com.mBZo.jar.tool.FileLazy
import com.mBZo.jar.tool.askReloadArchive
import com.mBZo.jar.tool.isPlay


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
        //动态主题
        val dynamicTheme: SwitchPreferenceCompat? = findPreference("dynamicTheme")
        if (DynamicColors.isDynamicColorAvailable()){
            dynamicTheme?.setOnPreferenceClickListener {
                askReloadArchive = true
                requireActivity().recreate()
                return@setOnPreferenceClickListener true
            }
        }
        else{
            dynamicTheme?.isEnabled = false
            dynamicTheme?.summary = "系统不支持此功能"
        }
        //下载管理和自动安装
        val downloadManager: Preference? = findPreference("downloadManager")
        val downloader: SwitchPreferenceCompat? = findPreference("smartDownloader")
        downloadManager?.isVisible = (downloader?.isChecked == true)
        downloader?.setOnPreferenceChangeListener { _, newValue ->
            downloadManager?.isVisible = newValue as Boolean
            return@setOnPreferenceChangeListener true
        }
        if (isPlay != false){
            downloader?.isChecked = false
            downloader?.isEnabled = false
            downloader?.summary = "谷歌play模式不允许使用该功能"
        }
        //判断是否有下载管理是否为空，为空不可点
        downloadManager?.setOnPreferenceClickListener {
            if (downloadManager.summary!="无下载内容"){
                val fileList = FileLazy(view.context.filesDir.absolutePath+"/DlLog/").listFiles()
                if (fileList!=null && fileList.isNotEmpty()){
                    val intent = Intent()
                    intent.setClass(requireContext(),DownloadActivity::class.java)
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


    }
}

