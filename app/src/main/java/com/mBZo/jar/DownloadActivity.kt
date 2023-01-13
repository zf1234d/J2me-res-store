package com.mBZo.jar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.adapter.DownloadRecyclerAdapter
import java.io.File

class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        if (File(filesDir.absolutePath+"/Download/").exists().not()){
            //下载路径
            File(filesDir.absolutePath+"/Download/").mkdirs()
        }
        if (File(filesDir.absolutePath+"/DlLog/").exists().not()){
            //日志路径
            File(filesDir.absolutePath+"/DlLog/").mkdirs()
        }
        //适配器
        val fileList = File(filesDir.absolutePath+"/DlLog/").listFiles()
        if (fileList!=null){
            val recyclerView: RecyclerView = findViewById(R.id.recycler_download)
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            if (fileList.isEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle("无下载内容")
                    .setPositiveButton("退出") {_,_ -> finish() }
                    .show()
            }
            val adapter = DownloadRecyclerAdapter(this,fileList.toMutableList())
            recyclerView.adapter = adapter
        }
    }
}