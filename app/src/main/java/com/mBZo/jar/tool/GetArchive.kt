package com.mBZo.jar.tool

import android.app.Activity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mBZo.jar.ArchiveFragment
import com.mBZo.jar.HomeFragment
import com.mBZo.jar.R
import com.mBZo.jar.archiveCVer
import com.mBZo.jar.netWorkRoot
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.io.inputstream.ZipInputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets


class GetArchive(
    private val activity: Activity?,
    private val archive: ArchiveFragment?,
    private val home: HomeFragment
){
    fun start() {
        if (activity!=null) {
            val value = home.getType()
            home.setLoading(true)
            home.setType(
                activity.getString(R.string.updateNewArchive),
                ContextCompat.getDrawable(activity,R.drawable.ic_baseline_query_builder_24)
            )
            Thread {
                getArchiveDetail(object :OkHttpListener{
                    override fun onSucceed(result: String) {
                        activity.runOnUiThread {
                            home.setLoading(false)
                            home.setType(value.first,value.second)
                            selectArchiveDetail(activity,result)
                        }
                    }
                })
            }.start()
        }
    }

    private fun selectArchiveDetail(activity: Activity, data: String) {
        val must = mutableListOf<String>()//必选path列表
        val path = mutableListOf<String>()//path列表
        val name = mutableListOf<String>()//name列表
        val select = mutableListOf<Boolean>()//选择结果列表
        for (index in data.split("\n")) {
            if (index.contains("{") && index.contains("//close").not()) {
                if (index.contains(",")) {
                    path.add(index.substringAfter("{").substringBefore(","))
                    name.add(index.substringAfter(",").substringBefore("}"))
                    select.add(false)
                } else {
                    must.add("${netWorkRoot}/jarlist/Archive/raw/${index.substringAfter("{").substringBefore("}")}.zip")
                }
            }
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("额外库存源(可以不选)")
            .setMultiChoiceItems(name.toTypedArray(), null) { _, index, state ->
                select[index] = state
            }
            .setPositiveButton("开始同步") { _, _ ->
                FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/0.list").writeNew(
                    "process"
                )
                FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/1.list").writeNew()
                for (pos in 0 until select.size){
                    if (select[pos]){
                        must.add("${netWorkRoot}/jarlist/Archive/raw/${path[pos]}.zip")
                    }
                }
                startDownloadArchive(activity,must)
            }
            .show()
    }


    private fun startDownloadArchive(activity: Activity, list: List<String>) {
        home.setLoading(true)
        home.setType(
            activity.getString(R.string.updateNewArchive),
            ContextCompat.getDrawable(activity,R.drawable.ic_baseline_query_builder_24)
        )
        Thread{
            val fileRn = File("${activity.filesDir.absolutePath}/mBZo/java/list/1.list")
            val savePath = "${activity.filesDir.absolutePath}/mBZo/java/list/"
            var tipTemp = ""
            for (i in list.indices){
                val saveName = list[i].substringAfterLast("/").substringBefore(".zip")
                downloadFile(list[i],"$savePath$saveName.zip",object :DownloadProgressListener{
                    override fun onProgress(totalBytes: Long, downloadedBytes: Long) {
                        val progress = (downloadedBytes/totalBytes * 100).toInt()
                        val tip = "${activity.getString(R.string.updateNewArchive)}[${i+1}($progress%)/${list.size}]"
                        if (tip!=tipTemp){
                            tipTemp = tip
                            activity.runOnUiThread {
                                home.setType(
                                    tip,
                                    ContextCompat.getDrawable(activity,R.drawable.ic_baseline_query_builder_24)
                                )
                            }
                        }
                    }
                    override fun onSucceed() {
                        val zipFile = ZipFile(File("$savePath$saveName.zip"))
                        zipFile.isRunInThread = false
                        for (index in zipFile.fileHeaders){
                            if (index.fileName=="$saveName.txt") {
                                val inputStream: ZipInputStream = zipFile.getInputStream(index)
                                //流转字符串
                                val result = ByteArrayOutputStream()
                                val buffer = ByteArray(1024)
                                var length: Int
                                while (
                                    inputStream.read(buffer).also { length = it } != -1
                                ) {
                                    result.write(buffer, 0, length)
                                }
                                //写入
                                fileRn.appendText("\n${result.toString(StandardCharsets.UTF_8.name())}")
                            }
                        }
                    }
                })
            }
            FileLazy("${activity.filesDir.absolutePath}/mBZo/java/list/0.list")
                .writeNew(archiveCVer.invoke())
            activity.runOnUiThread {
                home.setLoading(false)
                home.setType(
                    activity.getString(R.string.allReady),
                    ContextCompat.getDrawable(activity,R.drawable.ic_baseline_check_24)
                )
            }
            archive?.loadArchive()
        }.start()
    }


    private fun getArchiveDetail(listener: OkHttpListener){
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("$netWorkRoot/jarlist/Archive/setup.hanpi")
                .build()
            val response = client.newCall(request).execute()
            val data = response.body.string()
            listener.onSucceed(data)
        } catch (e: Exception) {
            getArchiveDetail(listener)
        }
    }
}