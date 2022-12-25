package com.mBZo.jar.store

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.mBZo.jar.BuildConfig
import java.io.File

fun installJar(activity: Activity, file: File) {
    val mIntent = Intent(Intent.ACTION_VIEW)
    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        mIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        mIntent.setDataAndType(FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID +".fileProvider",file), "application/java-archive")
    } else {
        mIntent.setDataAndType(Uri.fromFile(file),"application/java-archive")
    }
    activity.startActivity(mIntent)
}