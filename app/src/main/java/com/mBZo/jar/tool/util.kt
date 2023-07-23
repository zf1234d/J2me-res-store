package com.mBZo.jar.tool

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mBZo.jar.BuildConfig
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10
import kotlin.math.pow

//跳转浏览器
fun otherOpen(activity: Activity, url:String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        ContextCompat.startActivity(activity, intent, null)
    } catch (e: Exception) {
        if (isDestroy(activity).not()){
            activity.runOnUiThread {
                Toast.makeText(activity,"未找到浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

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
fun downloadFile(url: String, filePath: String, listener: DownloadProgressListener) {

    val client = OkHttpClient.Builder()
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(
                    ProgressResponseBody(
                        originalResponse.body,
                        listener
                    )
                )
                .build()
        }
        .build()

    val request = Request.Builder()
        .url(url)
        .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) throw RuntimeException("Unexpected code $response")

    val inputStream = response.body.byteStream()
    val outputStream = FileOutputStream(File(filePath))

    val buffer = ByteArray(1024)
    var read: Int
    while (inputStream.read(buffer).also { read = it } != -1) {
        outputStream.write(buffer, 0, read)
        listener.onProgress(response.body.contentLength(), outputStream.channel.size())
    }

    outputStream.flush()
    outputStream.close()
    inputStream.close()
    listener.onSucceed()
}

interface DownloadProgressListener {
    fun onProgress(totalBytes: Long, downloadedBytes: Long)
    fun onSucceed()
}

class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: DownloadProgressListener
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0L
                progressListener.onProgress(responseBody.contentLength(), totalBytesRead)
                return bytesRead
            }
        }
    }
}

fun Long.formatSize(): String {
    if (this <= 0) {
        return "0B"
    }
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.1f %s", this / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

data class ArchiveItem(
    val name: String,
    val nameFC: String,
    val from: String,
    val path: String
)

interface OkHttpListener{
    fun onSucceed(result: String)
}