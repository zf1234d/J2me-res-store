package com.mBZo.jar.tool

import android.app.Activity
import android.widget.ImageView
import coil.load



fun Activity.safe(): Activity? {
    return this
}
fun isDestroy(mActivity: Activity?): Boolean {
    return mActivity == null || mActivity.isFinishing || mActivity.isDestroyed
}

fun imageLoad(activity: Activity, view: ImageView?, img: Any){
    if (isDestroy(activity).not()){
        view?.load(img)
    }
}