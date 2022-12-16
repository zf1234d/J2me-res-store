package com.mBZo.jar

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
        //下载路径
        val downloadPath: Preference? = findPreference("downloadPath")
        downloadPath?.summary = view.context.getExternalFilesDir("download").toString().substringAfter(Environment.getExternalStorageDirectory().toString()+"/")
        val downloader: SwitchPreferenceCompat? = findPreference("smartDownloader")
        downloadPath?.isVisible = (downloader?.isChecked == true)
        downloader?.setOnPreferenceChangeListener { _, newValue ->
            downloadPath?.isVisible = newValue as Boolean
            return@setOnPreferenceChangeListener true
        }
        //




    }
}

