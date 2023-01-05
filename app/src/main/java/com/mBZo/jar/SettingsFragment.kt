package com.mBZo.jar

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.viewpager2.widget.ViewPager2

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
        downloadManager?.setOnPreferenceClickListener {
            val intent = Intent(activity,DownloadActivity::class.java)
            activity?.startActivity(intent)
            return@setOnPreferenceClickListener true
        }
        //

        //


    }
}

