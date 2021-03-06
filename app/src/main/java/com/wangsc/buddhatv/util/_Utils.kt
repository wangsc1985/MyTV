package com.wangsc.buddhatv.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Environment
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import com.wangsc.buddhatv.MainActivity
import com.wangsc.buddhatv.model.DateTime
import java.io.*
import java.net.*
import java.util.*
import kotlin.collections.ArrayList


object _Utils {
    val ROOT_DIR = File(Environment.getExternalStorageDirectory().toString() + "/0/mytv")

    private var lastTotalRxBytes = 0L;
    private var lastTimeStamp = 0L;

    fun getNetSpeed():String {
        var cc = TrafficStats.getTotalRxBytes()
        if(cc==TrafficStats.UNSUPPORTED.toLong()){
            return ""
        }
        var nowTotalRxBytes = cc / 1024
        var nowTimeStamp = System.currentTimeMillis();
        var speed = (nowTotalRxBytes - lastTotalRxBytes)*1000 / (nowTimeStamp - lastTimeStamp)
        lastTimeStamp = nowTimeStamp
        lastTotalRxBytes = nowTotalRxBytes
        if(nowTimeStamp- lastTimeStamp>2000){
            return ""
        }
        return "$speed kb/s";
    }

    /**
     * 通过uid查询文件夹中的数据
     * @param localUid
     * @return
     */
    private fun getTotalBytesManual(uid:Int):Long {

        var dir = File("/proc/uid_stat")
        if(!dir.exists()){
            return 0L
        }
        var children = dir.list()
        var stringBuffer = StringBuffer()
        for (i in children.indices) {
            stringBuffer.append(children[i]);
            stringBuffer.append("   ");
        }
        if (!children.contains(uid.toString())) {
            return 0L;
        }
        var uidFileDir = File("/proc/uid_stat/"+uid.toString())
        var uidActualFileReceived = File(uidFileDir, "tcp_rcv")
        var textReceived = "0"
        try {
            var brReceived = BufferedReader(FileReader(uidActualFileReceived));
            var receivedLine=brReceived.readLine()
            if (receivedLine != null) {
                textReceived = receivedLine;
            }
        } catch (e:IOException ) {
        }
        return textReceived.toLong()
    }

    fun isNetworkConnected(context:Context ):Boolean {
        if (context != null) {
            var mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    //    var mWakeLock: WakeLock? = null
    fun openApp(context: Context, packageName: String) {
        try {
            val pm = context.packageManager
            val pi = context.packageManager.getPackageInfo(packageName, 0)
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            resolveIntent.setPackage(pi.packageName)
            val apps: List<ResolveInfo> = pm.queryIntentActivities(resolveIntent, 0)
            val ri = apps.iterator().next()
            if (ri != null) {
                val packageName = ri.activityInfo.packageName
                val className = ri.activityInfo.name
                val intent = Intent(Intent.ACTION_MAIN)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                val cn = ComponentName(packageName, className)
                intent.component = cn
                context.startActivity(intent)
            }
        } catch (e: Exception) {
        }
    }

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


    fun isScreenOn(context: Context): Boolean {
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return manager.isScreenOn
    }

    @Throws(java.lang.Exception::class)
    private fun bindPort(ip: String, port: Int) {
        //创建一个socket对象
        val s = Socket()
        //对指定端口进行绑定，如果绑定成功则未被占用
        s.bind(InetSocketAddress(ip, port))
        s.close()
    }

    fun getIp(): String {
        try {
            val enNetI = NetworkInterface.getNetworkInterfaces()
            while (enNetI.hasMoreElements()) {
                val netI = enNetI.nextElement()
                val enumIpAddr = netI.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }


    /**
     * 如果端口未被占用，返回true
     */
    fun isPortAvailable(context: Context, port: Int): Boolean {
        return try {
            //调用bindport函数对本机指定端口进行验证
            val ip = getIp()
            _CloudUtils.saveSetting(context, "0088", "tv_ip", ip, null)
            bindPort(ip, port)
            true
        } catch (e: java.lang.Exception) {
            false
        }
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
            if (null != wakeLock && wakeLock.isHeld) {
                wakeLock.release()
                wakeLock = null
            }
        } catch (e: java.lang.Exception) {
        }
    }

    fun e(log: Any?) {
        Log.e("wangsc", (log ?: "信息为空").toString())
    }

    /**
     * 判断服务是否在运行
     * @param context
     * @param serviceName
     * @return
     * 服务名称为全路径 例如com.ghost.WidgetUpdateService
     */
    fun isRunService(context: Context, serviceName: String): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceName == service.service.className) {
                return true
            }
        }
        return false
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

    fun searchPath():String {
        try {
            val filePath = "/proc/mounts"
            val file = File(filePath)
            val lineList: MutableList<String> = ArrayList()
            var inputStream: InputStream? = null
            try {
                inputStream = FileInputStream(file)
                if (inputStream != null) {
                    val inputStreamReader = InputStreamReader(inputStream, "GBK")
                    val bufferedReader = BufferedReader(inputStreamReader)
                    var line:String? = ""
                    while (bufferedReader.readLine().also({ line = it }) != null) {
                        line?.let {itl->
                            if (itl.contains("vfat")) {
                                lineList.add(itl)
                            }
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            val editPath = lineList[lineList.size - 1]
            e("edit path : ${editPath}")
            var start = editPath.indexOf("/storage")
            if(start<0){
                start = editPath.indexOf("/mnt")
            }
            val end = editPath.indexOf(" vfat")
            val path = editPath.substring(start, end)
            return path
        } catch (e: Exception) {
            return getExceptionStr(e)
        }
    }

    fun getExceptionStr(e: Exception): String {
        var msg = ""
        try {
            if (e.stackTrace.size == 0) return ""
            for (ste in e.stackTrace) {
                if (ste.className.contains(MainActivity::class.java.getPackage().name)) {
                    msg += """
                        类名：
                        ${ste.className}
                        方法名：
                        ${ste.methodName}
                        行号：${ste.lineNumber}
                        错误信息：
                        ${e.message}
                        
                        """.trimIndent()
                }
            }
        } catch (exception: Exception) {
        }
        return msg
    }

    fun GetUsbPath(): String {
        var strMountInfo = "";
        // 1.首先获得系统已加载的文件系统信息
        try {
            // 创建系统进程生成器对象
            var objProcessBuilder = ProcessBuilder();
            // 执行 mount -h 可以看到 mount : list mounted filesystems
            // 这条命令可以列出已加载的文件系统
            objProcessBuilder.command("mount"); // 新的操作系统程序和它的参数
            // 设置错误输出都将与标准输出合并
            objProcessBuilder.redirectErrorStream(true);
            // 基于当前系统进程生成器的状态开始一个新进程，并返回进程实例
            var objProcess = objProcessBuilder.start();
            // 阻塞线程至到本地操作系统程序执行结束，返回本地操作系统程序的返回值
            objProcess.waitFor();
            // 得到进程对象的输入流，它对于进程对象来说是已与本地操作系统程序的标准输出流(stdout)相连接的
            var objInputStream = objProcess.getInputStream();
            var buffer = ByteArray(1024)
            // 读取 mount 命令程序返回的信息文本
            while (-1 != objInputStream.read(buffer)) {
                strMountInfo = strMountInfo + String(buffer);
            }
            // 关闭进程对象的输入流
            objInputStream.close();
            // 终止进程并释放与其相关的任何流
            objProcess.destroy();
        } catch (e: Exception) {
            e.printStackTrace();
        }
        // 2.然后再在系统已加载的文件系统信息里查找 SD 卡路径
        // mount 返回的已加载的文件系统信息是以一行一个信息的形式体现的，
        // 所以先用换行符拆分字符串
        var lines = strMountInfo.split("\n");
        // 清空该字符串对象，下面将用它来装载真正有用的 SD 卡路径列表
        strMountInfo = "";
        for (i in lines.indices) {
            // 如果该行内有 /mnt/和 vfat 字符串，说明可能是内/外置 SD 卡的挂载路径
            if (-1 != lines[i].indexOf(" /mnt/") && // 前面要有空格，以防断章取义
                -1 != lines[i].indexOf(" vfat ")
            )  // 前后均有空格
            {
                // 再以空格分隔符拆分字符串
                var blocks = lines[i].split("\\s"); // \\s 为空格字符
                for (j in blocks.indices) {
                    // 如果字符串中含有/mnt/字符串，说明可能是我们要找的 SD 卡挂载路径
                    if (-1 != blocks[j].indexOf("/mnt/")) {
                        // 排除重复的路径
                        if (-1 == strMountInfo.indexOf(blocks[j])) {
                            // 用分号符(;)分隔 SD 卡路径列表，
                            strMountInfo += blocks[j]; //此处位一个插入一个U盘时的路径，如果U盘过多可能拼到一起。
                        }
                    }
                }
            }
        }
        return strMountInfo;
    }

    /**
     * 将日志记录到指定文件，文件名{filename}不用添加后缀。
     */
    fun addLog2File(filename: String, item: String, message: String?) {
        try {
            val logFile = File(ROOT_DIR, "${filename}.log")
            val writer = BufferedWriter(FileWriter(logFile, true))
            writer.write(DateTime().toLongDateTimeString())
            writer.newLine()
            writer.write(item)
            writer.newLine()
            if (message != null) {
                writer.write(message)
                writer.newLine()
            }
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}