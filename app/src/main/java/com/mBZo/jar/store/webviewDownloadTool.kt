package com.mBZo.jar.store

import android.annotation.SuppressLint
import android.content.*
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mBZo.jar.R
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.lanzouApi
import com.mBZo.jar.otherOpen
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import okhttp3.Request
import zlc.season.downloadx.download
import java.io.File
import java.net.URLDecoder

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetJavaScriptEnabled")
class WebViewListen2Download(activity: AppCompatActivity, link: String){
    init {
        if (link.contains("52emu") && link.contains("xid=1")){
        //这是一个特殊情况，用于处理52emu中的高速下载，使其变得可用
        contentFormat(activity,null,null,null,null,null,null,true)
        Snackbar.make(activity.findViewById(R.id.floatingActionButton),"检测到特殊链接，正在重处理", Snackbar.LENGTH_LONG).show()
        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .followRedirects(false)
                    .build()
                val request = Request.Builder()
                    .url(link)
                    .build()
                val response = client.newCall(request).execute()
                var finLink = response.header("Location")
                if (finLink != null) {
                    finLink = "https://wwr.lanzoux.com/${finLink.substringAfter("https://pan.lanzou.com/tp/")}"
                    lanzouApi(activity as StoreActivity,"web2download",finLink,"")
                }
                else{
                    activity.runOnUiThread {
                        Snackbar.make(activity.findViewById(R.id.floatingActionButton),"未能修复52emu的高速下载",
                            Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    Snackbar.make(activity.findViewById(R.id.floatingActionButton),"下载出错，请检查网络状况",
                        Snackbar.LENGTH_LONG).show()
                }
            }
        }.start()
        //52emu针对处理部分结束
    }
    else{
        //这里是正常的下载，获取相关参数，决定是否需要内置下载器
        val spfRecord: SharedPreferences = activity.getSharedPreferences("com.mBZo.jar_preferences", Context.MODE_PRIVATE)
        val downloader = spfRecord.getBoolean("smartDownloader",true)

        //下载判断开始，用户是否使用内置下载
        if (downloader){
            //内置下载处理函数

            //显示加载进度条
            contentFormat(activity,null,null,null,null,null,null,true)
            //可能使用内置下载，判断链接符不符合内置下载文件要求
            val webviewDialog = MaterialAlertDialogBuilder(activity)
                .setTitle("正在验证")
                .setView(R.layout.dialog_webview)
                .show()
            val webview: WebView? = webviewDialog.findViewById(R.id.webview)
            webview?.loadUrl(link)
            webview?.webViewClient= WebViewClient()
            webview?.settings?.javaScriptEnabled = true
            webview?.setDownloadListener { url, _, contentDisposition, mimetype, contentLength ->
                //成功获得下载链接,关闭webview窗口并隐藏加载进度条
                webviewDialog.dismiss()
                contentFormat(activity,null,null,null,null,null,null,false)
                //传参，然后去下载
                if (contentDisposition==""){
                    val filename = URLUtil.guessFileName(url,null,null)
                    downloadFileBy(activity,url,filename,contentLength,mimetype)
                }
                else{
                    val filename = URLDecoder.decode(Regex("\"$").replace(contentDisposition.substringAfter(Regex("filename\\s*=\\s*\"?").find(contentDisposition)!!.value),""),"utf-8")
                    downloadFileBy(activity, url, filename, contentLength, mimetype)
                }
            }
        }
        else{
            //用户关闭内置下载需求
            otherOpen(activity,link)
        }
    }

    }


    private fun downloadFileBy(
        activity: AppCompatActivity,
        url: String,
        filename: String,
        size: Long,
        mimeType: String)
    {
        if (filename.subSequence(filename.length-4,filename.length)==".jar"){
            MaterialAlertDialogBuilder(activity)
                .setMessage("$filename\n\n大小：${size/1024} kb")
                .setPositiveButton("开始下载"){_,_->
                    val logFile = File(activity.filesDir.absolutePath+"/DlLog/"+filename)
                    if (logFile.exists()){
                        Snackbar.make(activity.findViewById(R.id.floatingActionButton),"无法添加重复任务",Snackbar.LENGTH_LONG).show()
                    }
                    else{
                        File(activity.filesDir.absolutePath+"/DlLog/"+filename).writeText(url)
                        val downloadTask = GlobalScope.download(url,filename,activity.filesDir.absolutePath+"/Download/")
                        downloadTask.state()
                            .onEach {
                                if (downloadTask.isSucceed()){
                                    installJar(activity,
                                        File(activity.filesDir.absolutePath+"/Download/"+filename)
                                    )
                                }
                            }
                            .launchIn(GlobalScope)
                        downloadTask.start()
                        Snackbar.make(activity.findViewById(R.id.floatingActionButton),"下载任务已添加",Snackbar.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("通过外部软件下载"){_,_-> otherOpen(activity,url) }
                .show()
        }
        else{
            MaterialAlertDialogBuilder(activity)
                .setMessage("$filename\n\n大小：${size / 1024} kb\n\n（内置下载器不允许下载此格式文件）")
                .setPositiveButton("通过外部软件下载"){_,_-> otherOpen(activity, url) }
                .show()
        }
        contentFormat(activity,null,null,null,null,null,null,false)
    }
}