package com.wangsc.mytv.util

import android.annotation.SuppressLint
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import android.util.Log
import com.wangsc.mytv.model.Material
import com.wangsc.mytv.receiver.YNAdminReceiver
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
        if (!mDevicePolicyManager.isAdminActive(mCompName)) {
            e("重新设定权限")
            val intent = Intent()
//            intent.flags=Intent.FLAG_ACTIVITY_NEW_TASK
            intent.action = DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCompName)
            context.startActivity(intent)
        } else {
            mDevicePolicyManager.lockNow()
        }
    }
    fun isScreenOn(context: Context):Boolean{
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return manager.isScreenOn
    }

    /**
     * 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
     *
     *
     * PARTIAL_WAKE_LOCK :保持CPU 运转，屏幕和键盘灯是关闭的。
     * SCREEN_DIM_WAKE_LOCK ：保持CPU 运转，允许保持屏幕显示但有可能是灰的，关闭键盘灯
     * SCREEN_BRIGHT_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，关闭键盘灯
     * FULL_WAKE_LOCK ：保持CPU 运转，保持屏幕高亮显示，键盘灯也保持亮度
     *
     * @param context
     */
    fun acquireWakeLock(context: Context, PowerManager: Int): WakeLock? {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = pm.newWakeLock(PowerManager, context.javaClass.canonicalName)
            if (null != wakeLock) {
                wakeLock.acquire()
                Log.e("wangsc", "锁定唤醒锁: $wakeLock")
                return wakeLock
            }
        } catch (e: java.lang.Exception) {
        }
        return null
    }

    /**
     * 释放设备电源锁
     */
    fun releaseWakeLock(context: Context?, wakeLock: WakeLock?) {
        var wakeLock = wakeLock
        try {
            Log.e("wangsc", "解除唤醒锁: $wakeLock")
            if (null != wakeLock && wakeLock.isHeld) {
                wakeLock.release()
                wakeLock = null
            }
        } catch (e: java.lang.Exception) {
        }
    }

    fun e(log: Any){
        Log.e("wangsc", log.toString())
    }

    /**
     * 获取所有程序的包名信息
     *
     * @param application
     * @return
     */
    fun getAppInfos(application: Application): List<String> {
        val pm = application.packageManager
        val packgeInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES)
        val appInfos: MutableList<String> = java.util.ArrayList()
        /* 获取应用程序的名称，不是包名，而是清单文件中的labelname
            String str_name = packageInfo.applicationInfo.loadLabel(pm).toString();
            appInfo.setAppName(str_name);
         */
        for (packgeInfo in packgeInfos) {
            val packageName = packgeInfo.packageName
            appInfos.add(packageName)
        }
        return appInfos
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
        val where = "${MediaStore.Images.Media.MIME_TYPE}=? or ${MediaStore.Video.Media.MIME_TYPE}=? or ${MediaStore.Video.Media.MIME_TYPE}=? or ${MediaStore.Video.Media.MIME_TYPE}=? or ${MediaStore.Video.Media.MIME_TYPE}=?"
        val whereArgs = arrayOf("video/mp4", "video/rmvb", "video/flv", "video/mkv", "video/mpg")
        val list: MutableList<Material> = ArrayList()
        val cursor: Cursor = context.getContentResolver().query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, where, whereArgs,
            MediaStore.Video.Media.DISPLAY_NAME + " ASC "
        )?: return list

        try {
            while (cursor.moveToNext()) {
                val size: Long = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) // 大小
                if (size > 5 * 1024 * 1024) { //大于10M的文件
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