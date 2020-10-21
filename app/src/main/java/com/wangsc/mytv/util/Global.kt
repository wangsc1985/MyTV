package com.wangsc.mytv.util

import android.text.TextUtils
import java.text.SimpleDateFormat
import java.util.*

fun String.Companion.concat(vararg args:Any):String{
    var sb=StringBuffer()
    for(str in args){
        sb.append(str.toString())
    }
    return sb.toString()
}

/**
 * 根据timeInMillis格式化时间，默认格式"yyyy-MM-dd HH:mm:ss"。
 * @param timeInMillis
 * @param strPattern
 * @return
 */
fun String.Companion.formatUTC(timeInMillis: Long, strPattern: String?): String {
    var strPattern: String? = strPattern
    if (TextUtils.isEmpty(strPattern)) {
        strPattern = "yyyy-MM-dd HH:mm:ss"
    }
    var sdf = SimpleDateFormat(strPattern, Locale.CHINA)
    sdf.applyPattern(strPattern)
    return if (sdf == null) "NULL" else sdf.format(timeInMillis)
}

fun String.formatToCardNumber(): String {
    var num: String = ""
    var index: Int = this.length - 1
    for (i in 0 until this.length) {
        index = this.length - 1 - i
        if (i % 4 == 0 && i != 0) {
            num = " " + num
        }
        num = this.get(index).toString() + num
    }
    return num
}
