package com.mBZo.jar

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.viewpager2.widget.ViewPager2

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewpagerSdSet: Preference? = findPreference("viewpagerIsBad")
        val viewpager: ViewPager2? = activity?.findViewById(R.id.home_page_tree)
        //设置左右滑动
        viewpagerSdSet?.setOnPreferenceChangeListener { _, newValue ->
            viewpager?.isUserInputEnabled = (newValue as Boolean).not()
            return@setOnPreferenceChangeListener true
        }
    }
}

