package com.mBZo.jar

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class StoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)
        //设置导航栏颜色
        if (Build.VERSION.SDK_INT > 27){
            window.navigationBarColor = getColor(R.color.white2)
        }
        //

    }
}