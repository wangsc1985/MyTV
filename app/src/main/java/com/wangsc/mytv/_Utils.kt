package com.wangsc.mytv

import android.R
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object _Utils {

    fun e(log:Any){
        Log.e("wangsc",log.toString())
    }
    /**
     * 获取本地所有的视频
     *
     * @return list
     */
    fun getAllLocalVideos(context: Context): List<Material> {
        var totalUploadCount: Long = 0
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        //全部图片
        val where = (MediaStore.Images.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=?")
        val whereArgs = arrayOf(
            "video/mp4", "video/3gp", "video/aiv", "video/rmvb", "video/vob", "video/flv",
            "video/mkv", "video/mov", "video/mpg"
        )
        val list: MutableList<Material> = ArrayList()
        val cursor: Cursor = context.getContentResolver().query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, where, whereArgs, MediaStore.Video.Media.DISPLAY_NAME + " ASC "
        )
            ?: return list
        try {
            while (cursor.moveToNext()) {
                val size: Long =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) // 大小
                if (size < 600 * 1024 * 1024) { //<600M
                    val materialBean = Material()
                    val path: String =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) // 路径
                    val duration: Long =
                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                            .toLong() // 时长
                    materialBean.Title =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
                    materialBean.Logo = path
                    materialBean.FilePath = path
                    materialBean.Checked = false
                    materialBean.FileType = 2
                    materialBean.FileId = totalUploadCount++
                    materialBean.UploadedSize = 0
                    materialBean.TimeStamps = System.currentTimeMillis().toString()
                    val format = SimpleDateFormat("HH:mm:ss")
                    format.setTimeZone(TimeZone.getTimeZone("GMT+0"))
                    val t: String = format.format(duration)
                    materialBean.Time = "时长：" + t
                    materialBean.FileSize = size
                    list.add(materialBean)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        return list
    }
}