package com.mBZo.jar.tool

import android.app.Activity
import android.widget.ImageView
import com.bumptech.glide.Glide


fun isDestroy(mActivity: Activity?): Boolean {
    return mActivity == null || mActivity.isFinishing || mActivity.isDestroyed
}

fun imageLoad(view: ImageView, img: Any){
    if (isDestroy(view.context as Activity).not()){
        Glide.with(view.context).load(img).into(view)
    }
}