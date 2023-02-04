package com.mBZo.jar

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.microsoft.appcenter.AppCenter
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //设置顶栏菜单
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.home_toolbar_menu)
        toolbar.setOnMenuItemClickListener {
            val loading: ProgressBar? = activity?.findViewById(R.id.progressBar2)
            when(it.itemId){
                //下载管理
                R.id.toolbar_download -> {
                    val intent = Intent(view.context,DownloadActivity::class.java)
                    startActivity(intent)
                }
                //捐赠页
                R.id.toolbar_thanks -> {
                    val toolbarDialog = MaterialAlertDialogBuilder(view.context)
                        .setTitle("支持和鼓励")
                        .setOnDismissListener { loading?.visibility = View.INVISIBLE }
                        .setMessage("正在获取……")
                        .setPositiveButton("确认"){_,_ -> }
                        .show()
                    loading?.visibility = View.VISIBLE
                    Thread{
                        try {
                            val client = OkHttpClient()
                            val request = Request.Builder()
                                .url("$netWorkRoot/jarlist/thanks_donate.long")
                                .build()
                            val response = client.newCall(request).execute()
                            activity?.runOnUiThread {
                                loading?.visibility = View.INVISIBLE
                                toolbarDialog.setTitle("感谢这些朋友的捐赠支持")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    toolbarDialog.setMessage(Html.fromHtml(response.body.string().replace("\n","<br>"),Html.FROM_HTML_MODE_LEGACY))
                                } else {
                                    @Suppress("DEPRECATION")
                                    toolbarDialog.setMessage(Html.fromHtml(response.body.string().replace("\n","<br>")))
                                }
                            }
                        } catch (e: Exception) {
                            //请求错误
                            activity?.runOnUiThread {
                                loading?.visibility = View.INVISIBLE
                                toolbarDialog.setTitle("错误")
                                toolbarDialog.setMessage("网络连接失败")
                            }
                        }
                    }.start()
                }
                //AppCenter
                R.id.toolbar_app_center -> {
                    MaterialAlertDialogBuilder(view.context)
                        .setTitle("AppCenterSDK")
                        .setIcon(R.drawable.ic_app_center)
                        .setMessage("SDK版本\n${AppCenter.getSdkVersion()}\n\n" +
                                "运行状态\n${AppCenter.isEnabled().get()}\n\n" +
                                "设备识别码\n${AppCenter.getInstallId().get()}\n\n"+
                                "使用AppCenter匿名收集崩溃日志")
                        .show()
                }
                //更新日志
                R.id.toolbar_updateLog -> {
                    loading?.visibility = View.VISIBLE
                    val toolbarDialog = MaterialAlertDialogBuilder(view.context)
                        .setTitle("更新日志")
                        .setOnDismissListener { loading?.visibility = View.INVISIBLE }
                        .setMessage("正在获取……")
                        .setPositiveButton("确认"){_,_ -> }
                        .show()
                    Thread{
                        try {
                            val client = OkHttpClient()
                            val request = Request.Builder()
                                .url("$netWorkRoot/jarlist/update.log")
                                .build()
                            val response = client.newCall(request).execute()
                            val updateLogRaw = response.body.string()
                            var updateLogDecode = ""
                            activity?.runOnUiThread {
                                loading?.visibility = View.INVISIBLE
                                for (index in updateLogRaw.substringAfter("更新日志").split("\n")) {
                                    if (index.contains(Regex("^### "))) {
                                        updateLogDecode += "<h3>${index.substringAfter("###")}</h3>"
                                        continue
                                    }
                                    if (index.contains(Regex("^\\* "))) {
                                        updateLogDecode += "<li>${index.substringAfter("*")}</li>"
                                        continue
                                    }
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    toolbarDialog.setMessage(Html.fromHtml(updateLogDecode,Html.FROM_HTML_MODE_LEGACY))
                                }
                                else{
                                    @Suppress("DEPRECATION")
                                    toolbarDialog.setMessage(Html.fromHtml(updateLogDecode))
                                }
                            }
                        } catch (e: Exception) {
                            //请求错误
                            activity?.runOnUiThread {
                                loading?.visibility = View.INVISIBLE
                                toolbarDialog.setTitle("错误")
                                toolbarDialog.setMessage("网络连接失败")
                            }
                        }
                    }.start()
                }
                //关于
                R.id.toolbar_about -> {
                    val dialog = MaterialAlertDialogBuilder(view.context)
                        .setView(R.layout.dialog_about)
                        .show()
                    val dialogAboutVersion: MaterialTextView? = dialog.findViewById(R.id.dialog_about_version)
                    val dialogAboutAdd: MaterialTextView? = dialog.findViewById(R.id.dialog_about_add)
                    val versionText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    val addText = "前往 <a href='https://github.com/zf1234d/J2me-res-store'>Github</a> 查看源代码<br>通过 <a href='https://support.qq.com/product/346579'>腾讯兔小巢</a> 反馈问题<br><br><a href='https://www.coolapk.com/u/2436868'>酷安@没空的人Zero</a><br><a href='https://space.bilibili.com/57862935'>哔哩哔哩@没空的人</a>"
                    dialogAboutVersion?.text = versionText
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        dialogAboutAdd?.text = Html.fromHtml(addText,Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        @Suppress("DEPRECATION")
                        dialogAboutAdd?.text = Html.fromHtml(addText)
                    }
                    dialogAboutAdd?.movementMethod = LinkMovementMethod.getInstance()
                }
                else -> Toast.makeText(view.context,"啥情况啊???", Toast.LENGTH_SHORT).show()
            }
            true
        }


        //获取软件版本
        val versioncard: TextView = view.findViewById(R.id.state3)
        val versionText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        versioncard.text = versionText
        //绑定按钮1
        val btn1: MaterialCardView = view.findViewById(R.id.btn1)
        btn1.setOnClickListener {
            val loadInfo: TextView = view.findViewById(R.id.state2)
            //同步库存
            when (loadInfo.text) {
                getString(R.string.findNewArchive),getString(R.string.findNewArchiveUnexpectedStop) -> {
                    syncArchive(activity,getString(R.string.findNewArchive), R.drawable.ic_baseline_update_24)
                }
                getString(R.string.findNewApp) -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.addCategory(Intent.CATEGORY_BROWSABLE)
                        intent.data = Uri.parse(otaUrl.invoke())
                        startActivity(intent,null)
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(view.context,getString(R.string.notFindBrowser), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                getString(R.string.allReady) -> {
                    MaterialAlertDialogBuilder(view.context)
                        .setMessage("版本\n${BuildConfig.VERSION_CODE} (云端${cloudVer.invoke()})\n\n库存\n${File("${view.context.filesDir.absolutePath}/mBZo/java/list/0.list").readText()}\n\n通道\n${BuildConfig.BUILD_TYPE}\n\n系统\n${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT})\n\n目标\n${view.context.applicationInfo.targetSdkVersion}")
                        .setPositiveButton("更改库存"){_,_ -> syncArchive(activity,getString(R.string.allReady), R.drawable.ic_baseline_check_24)}
                        .show()
                }
                else -> { }
            }
        }



    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}