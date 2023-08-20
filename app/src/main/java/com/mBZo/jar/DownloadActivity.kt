package com.mBZo.jar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.adapter.DownloadRecyclerAdapter
import com.mBZo.jar.tool.FileLazy
import com.mBZo.jar.tool.attachDynamicColor
import java.io.File

class DownloadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachDynamicColor()
        setContentView(R.layout.activity_download)
        //适配器
        val fileList = FileLazy(filesDir.absolutePath+"/DlLog/").listFiles()
        if (fileList!=null && fileList.isNotEmpty()){
            //按照时间，从新到旧排序
            val modifiedFileList = mutableListOf<File>()
            modifiedFileList.add(fileList[0])
            for (index in 1 until  fileList.size){
                for (i in 0 until modifiedFileList.size){
                    if (fileList[index].lastModified()>=modifiedFileList[i].lastModified()){
                        modifiedFileList.add(i,fileList[index])
                        break
                    }
                    else{
                        if (i+1==modifiedFileList.size){
                            modifiedFileList.add(fileList[index])
                        }
                    }
                }
            }
            //将排序完成的列表送入布局
            val recyclerView: RecyclerView = findViewById(R.id.recycler_download)
            val layoutManager = LinearLayoutManager(this)
            recyclerView.layoutManager = layoutManager
            val adapter = DownloadRecyclerAdapter(this,modifiedFileList.toMutableList())
            recyclerView.adapter = adapter
        }
        else{
            MaterialAlertDialogBuilder(this)
                .setOnDismissListener { finish() }
                .setTitle("无下载内容")
                .setPositiveButton("退出") {_,_ ->  }
                .show()
        }
    }
}