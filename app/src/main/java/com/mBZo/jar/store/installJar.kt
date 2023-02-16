package com.mBZo.jar.store

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mBZo.jar.BuildConfig
import com.mBZo.jar.tool.isDestroy
import java.io.File

@SuppressLint("QueryPermissionsNeeded")
fun installJar(activity: Activity, file: File) {
    try {
        val mIntent = Intent(Intent.ACTION_VIEW)
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            mIntent.setDataAndType(FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID +".fileProvider",file), "application/java-archive")
        } else {
            mIntent.setDataAndType(Uri.fromFile(file),"application/java-archive")
        }
        activity.startActivity(mIntent)
    } catch (e: Exception) {
        if (isDestroy(activity).not()){
            activity.runOnUiThread {
                Toast.makeText(activity,"没装模拟器啊，兄弟？", Toast.LENGTH_SHORT).show()
            }
        }
    }
}