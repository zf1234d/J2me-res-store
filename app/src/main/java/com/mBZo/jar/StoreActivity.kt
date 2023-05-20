package com.mBZo.jar

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.mBZo.jar.store.apidecode.*
import com.mBZo.jar.tool.isDestroy
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory


class StoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate).addOnViewCreatedListener(WindowInsetsHelper.LISTENER)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)
        //找组件
        val title = findViewById<TextView>(R.id.storeTitle)
        val copyFrom = findViewById<TextView>(R.id.storeFrom)
        val downloadManager = findViewById<ExtendedFloatingActionButton>(R.id.storeDownloadManager)
        downloadManager.setOnClickListener {
            val intent = Intent(this,DownloadActivity::class.java)
            startActivity(intent)
        }
        //读参数
        val name = intent.getStringExtra("name")
        val from = intent.getStringExtra("from")
        val path = intent.getStringExtra("path")
        //不知道干嘛
        title.text = name
        val copyFromText = "来源:$from"
        copyFrom.text = copyFromText
        //通过from判断解析方法吧，找不到对应from就返回不支持
        if (name != null && from != null && path != null) {//虽然做了防毒，但不这样写不能编译
            if (from.contains("没空云")){ apiDecodeBzyunCn(this,path,name) }//匹配规则，没空云（OneIndexApi）
            else if (from.contains("Joyin的jar游戏下载站")){ apiDecodeJoyin(this,path) }//匹配规则，Joyin (Lanzou)
            else if (from.contains("e简网")){ apiDecodeEjJava(this,path) }
            else if (from.contains("小众网")){ apiDecodeIniche(this,path) }
            else if (from.contains("52emu")){ apiDecode52emu(this,path) }//匹配规则，52emu (专属混合Lanzou规则)
            else if (from=="损坏") {//库存本身出意外都是在这里解决
                MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setMessage("当前资源损坏，截图向我反馈")
                    .setPositiveButton("退出"){ _,_ -> this.finish() }
                    .show()
            }
            else {//查了一圈也不认识
                MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setMessage("暂不支持解析该仓库内容，请等待更新")
                    .setPositiveButton("退出"){ _,_ -> this.finish() }
                    .show()
            }
        }
        else{//如果说，真的有毒的话，给一个提示
            MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setMessage("您触发了意料之外的情况，无法提供内容")
                .setPositiveButton("退出"){ _,_ -> this.finish() }
                .show()
        }
        //应该没啥要写了吧
    }
}


//跳转浏览器
fun otherOpen(activity: Activity,url:String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(activity,intent,null)
    } catch (e: Exception) {
        if (isDestroy(activity).not()){
            activity.runOnUiThread {
                Toast.makeText(activity,"未找到浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


