package com.mBZo.jar.store

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mBZo.jar.R
import com.mBZo.jar.StoreActivity
import com.mBZo.jar.store.apidecode.lanzouApi
import com.mBZo.jar.tool.DownloadProgressListener
import com.mBZo.jar.tool.FileLazy
import com.mBZo.jar.tool.downloadFile
import com.mBZo.jar.tool.formatSize
import com.mBZo.jar.tool.installJar
import com.mBZo.jar.tool.isDestroy
import com.mBZo.jar.tool.otherOpen
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLDecoder

@OptIn(DelicateCoroutinesApi::class)
class WebViewListen2Download(activity: Activity, link: String){
    init {
        if (link.contains("52emu") && link.contains("xid=1")){
            //这是一个特殊情况，用于处理52emu中的高速下载，使其变得可用
            storeManage(activity,loading = true)
            Snackbar.make(activity.findViewById(R.id.storeDownloadManager),"检测到特殊链接，正在重处理", Snackbar.LENGTH_LONG).setAnchorView(activity.findViewById(R.id.storeDownloadManager)).show()
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
                            Snackbar.make(activity.findViewById(R.id.storeDownload),"未能修复52emu的高速下载", Snackbar.LENGTH_LONG)
                                .setAnchorView(activity.findViewById(R.id.storeDownloadManager))
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread {
                        Snackbar.make(activity.findViewById(R.id.storeDownload),"下载出错，请检查网络状况",
                            Snackbar.LENGTH_LONG).setAnchorView(activity.findViewById(R.id.storeDownloadManager)).show()
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
            if (downloader.not()) {
                //用户关闭内置下载
                otherOpen(activity,link)
            }
            else{
                //用户开启内置下载
                //创建webView窗口
                val webViewDialog = MaterialAlertDialogBuilder(activity)
                    .setView(R.layout.dialog_webview)
                    .show()
                //使用webView最终处理，确保url是最终下载链接
                val webView: WebView? = webViewDialog.findViewById(R.id.webview)
                webView?.loadUrl(link)
                val webClient = object : WebViewClient() {
                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url != null) {
                            view?.loadUrl(url)
                            webView?.visibility = View.GONE
                        }
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        webView?.visibility = View.VISIBLE
                    }
                }
                webView?.webViewClient= webClient
                @SuppressLint("SetJavaScriptEnabled")
                webView?.settings?.javaScriptEnabled = true
                webView?.settings?.domStorageEnabled = true
                webView?.settings?.cacheMode = WebSettings.LOAD_DEFAULT
                //监听webView下载
                webView?.setDownloadListener { url,_, contentDisposition,_, contentLength ->
                    //成功获得下载链接,关闭webView窗口并隐藏加载进度条
                    webViewDialog?.dismiss()
                    storeManage(activity,loading = false)
                    //传参，然后去下载
                    if (isDestroy(activity).not()){
                        if (contentDisposition==""){
                            val filename = URLUtil.guessFileName(url,null,null)
                            download(activity, url, filename, contentLength)
                        }
                        else{
                            val filename = URLDecoder.decode(Regex("\"$").replace(contentDisposition.substringAfter(Regex("filename\\s*=\\s*\"?").find(contentDisposition)!!.value),""),"utf-8")
                            download(activity, url, filename, contentLength)
                        }
                    }
                }
            }
        }
    }


    private fun download(
        activity: Activity,
        url: String,
        filename: String,
        size: Long
    )
    {
        if (filename.endsWith(".jar")){
            MaterialAlertDialogBuilder(activity)
                .setMessage("$filename\n\n大小：${size.formatSize()}")
                .setPositiveButton("开始下载"){_,_->
                    val logFile = File(activity.filesDir.absolutePath+"/DlLog/"+filename)
                    if (logFile.exists()){
                        Snackbar.make(activity.findViewById(R.id.storeDownload),"无法添加重复任务",Snackbar.LENGTH_LONG)
                            .setAnchorView(activity.findViewById(R.id.storeDownloadManager))
                            .show()
                    }
                    else{
                        FileLazy(logFile.absolutePath).writeNew(url)
                        Snackbar.make(activity.findViewById(R.id.storeDownload),"已添加,前往下载管理获取",Snackbar.LENGTH_LONG)
                            .setAnchorView(activity.findViewById(R.id.storeDownloadManager))
                            .show()
                    }
                }
                .setNegativeButton("通过外部软件下载"){_,_-> otherOpen(activity,url) }
                .show()
        }
        else{
            MaterialAlertDialogBuilder(activity)
                .setMessage("$filename\n\n大小：${size.formatSize()} kb\n\n（内置下载器不允许下载此格式文件）")
                .setPositiveButton("通过外部软件下载"){_,_-> otherOpen(activity, url) }
                .show()
        }
        storeManage(activity,loading = false)
    }
}