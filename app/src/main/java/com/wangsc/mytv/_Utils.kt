package com.wangsc.mytv

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import com.wangsc.mytv.activity.FullscreenActivity
import com.wangsc.mytv.model.Material
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


object _Utils {
//    var mWakeLock: WakeLock? = null

    @SuppressLint("InvalidWakeLockTag")
    fun wakeScreen(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        var mWakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "bright"
        )
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        } else {
            mWakeLock.release();
        }
//        mWakeLock.acquire()
    }
    fun closeScreen(context: Context) {
        var mCompName = ComponentName(context, YNAdminReceiver::class.java)
        val mDevicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!mDevicePolicyManager.isAdminActive(mCompName)) { //这一句一定要有...
            val intent = Intent()
            intent.action = DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCompName)
            context.startActivity(intent)
        } else {
            mDevicePolicyManager.lockNow()
        }
    }

    fun e(log: Any){
        Log.e("wangsc", log.toString())
    }
    /**
     * 获取本地所有的视频
     *
     * @return list
     */
    fun getAllLocalVideos(context: Context): List<Material> {
        var totalUploadCount = 0
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val where = (MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Video.Media.MIME_TYPE + "=? or "  + MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=? or " + MediaStore.Video.Media.MIME_TYPE + "=?")
        val whereArgs = arrayOf("video/mp4", "video/rmvb", "video/flv", "video/mkv", "video/mpg")
        val list: MutableList<Material> = ArrayList()
        val cursor: Cursor = context.getContentResolver().query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, where, whereArgs,
            MediaStore.Video.Media.DISPLAY_NAME + " ASC "
        )?: return list

        try {
            while (cursor.moveToNext()) {
                val size: Long = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) // 大小
                if (size > 10 * 1024 * 1024) { //<600M
                    val materialBean = Material()
                    val path: String = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) // 路径
                    val duration: Long = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)).toLong() // 时长
                    materialBean.Title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
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