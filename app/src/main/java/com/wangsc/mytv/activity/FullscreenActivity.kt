package com.wangsc.mytv.activity

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.wangsc.mytv.R
import com.wangsc.mytv.util._Utils
import com.wangsc.mytv.util._Utils.e
import com.wangsc.mytv.model.DataContext
import com.wangsc.mytv.model.Material
import com.wangsc.mytv.model.Setting
import com.wangsc.mytv.service.SocketService
import com.wangsc.mytv.util._Session
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.text.DecimalFormat
import java.util.*

class FullscreenActivity : AppCompatActivity() {
    private var timer = Timer()
    private var task: TimerTask? = null
    private var index: Int
    private var netVideoNum: Int
    private var mediaPosition: Int
    private var mediaPath: String
    val mReceiver: BroadcastReceiver = ScreenReceiver()
    private lateinit var fileList: List<Material>
    private lateinit var serverSocket: ServerSocket

    var format = DecimalFormat("#%")

    private lateinit var dataContext: DataContext
    private lateinit var fileNames: Array<String?>
    private var isPlayLocal = true

    init {
        index = 0
        mediaPosition = 0
        mediaPath = ""
        netVideoNum = 0
    }

    //region 全屏处理
    val mHideHandler = Handler()
    private fun hideActionBar() {
        //软件自身的action bar
        val actionBar = supportActionBar
        actionBar?.hide()

        // 系统的bar和虚拟键盘
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, 0)
    }

    private val mHidePart2Runnable = Runnable {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
    private val mShowPart2Runnable = Runnable {
        val actionBar = supportActionBar
        actionBar?.show()
    }

    private val mHideRunnable = Runnable { hideActionBar() }
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }
    //endregion

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        e("on post create")
        hideActionBar()
    }

    override fun onRestart() {
        e("on restart")
        super.onRestart()
//        val apps = _Utils.getAppInfos(application)
//        apps.forEach {
//            e(it)
//        }
        // com.cibn.tv 优酷
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        e("on create")
        super.onCreate(savedInstanceState)
        dataContext = DataContext(this)

        try {
            setContentView(R.layout.activity_fullscreen)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            /**
             * 保持屏幕常亮，即使视频暂停播放
             */
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            isPlayLocal = dataContext.getSetting(Setting.KEYS.is_play_local, false).boolean

            if (isPlayLocal) {
                e("播放本地视频")
                playLocalVideo()
            } else {
                e("播放网络视频")
                playNetVideo()
            }

            videoView.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setItems(fileNames, object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            val file = fileList[which]
                            val filePath = file.FilePath
                            videoView.setVideoPath(filePath)
                            textView_title.text = file.Title
                            mediaPosition = 0
                            dataContext.editSetting(Setting.KEYS.media_path, filePath)
                            dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
                        }
                    }).show();
                true
            }

//            imageView_play.setOnClickListener {
//                mediaStart()
//            }

            //region 接受广播
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            registerReceiver(mReceiver, filter)
            //endregion

//            startService(Intent(this,SocketService::class.java))
            startSocket()
        } catch (e: Exception) {
            e("${e.message}")
        }
    }

    inner class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    e("屏幕开启")
//                    mediaStart()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    e("屏幕关闭")
//                    mediaPause()
                }
            }
        }
    }

    fun startSocket() {
        Thread {
            try {
                serverSocket = ServerSocket(_Session.ActivitySocketPort);
                while (true) {
                    var socket = serverSocket.accept();
                    var dis = DataInputStream(socket.getInputStream())
                    val dd = dis.readInt()
                    when (dd) {
                        //region 已有替代
                        10 -> {
                            // TODO: 2020/10/26 runOnUiHandler块内访问属性变量，不能读到正确的值
                            if (isPlayLocal) {
                                playNetVideo()
                            } else {
                                playLocalVideo()
                            }
                        }
                        0 -> {
                            e("返回播放状态")
                            val dos = DataOutputStream(socket.getOutputStream())
                            dos.writeBoolean(videoView.isPlaying)
                            dos.flush()
                            dos.close()
                        }
                        1 -> {
                            e("暂停或播放视频，并返回播放状态")
                            val dos = DataOutputStream(socket.getOutputStream())
                            dos.writeBoolean(!videoView.isPlaying)
                            dos.flush()
                            dos.close()
                            // 暂停
                            runOnUiThread {
                                if (videoView.isPlaying)

                                    _Utils.closeScreen(this)
//                                    mediaPause()
                                else
                                    _Utils.wakeScreen(this)
//                                    mediaStart()
                            }

                        }
                        2 -> {
                            e("音量加")
                            runOnUiThread {
                                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                am.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        }
                        3 -> {
                            e("音量减")
                            runOnUiThread {
                                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                am.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        }
                        //endregion
                        4 -> {
                            e("前进")
                            runOnUiThread {
                                mediaForward()
                            }
                        }
                        5 -> {
                            e("后退")
                            runOnUiThread {
                                mediaRewind()
                            }
                        }
                        6 -> {
                            e("下一个")
                            runOnUiThread {
                                mediaNext()
                            }
                        }
                        7 -> {
                            e("上一个")
                            runOnUiThread {
                                mediaPrev()
                            }
                        }
                    }
                    dis.close()
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e("scoket : ${e.message}")
                _Utils.addLog2File("err","activity socket运行异常",e.message)
            }
        }.start()
    }

    fun playNetVideo() {
        playLocalVideo()
//        try {
//            isPlayLocal = false
//            dataContext.editSetting(Setting.KEYS.is_play_local, isPlayLocal)
//            netVideoNum = dataContext.getSetting(Setting.KEYS.net_video_num, "0").int
//            if (netVideoNum >= uris.size) {
//                netVideoNum = 0
//            }
//            e("uri : ${uris[netVideoNum]}")
//            runOnUiThread {
//                videoView.setVideoURI(Uri.parse(uris[netVideoNum]))
//                textView_title.text = ""
//                textView_time.text = ""
////            val mediaController = MediaController(this)
////            videoView.setMediaController(mediaController)
//
//                videoView.setOnPreparedListener {
//                    try {
//                        e("video view has prepared...")
//                        if (!videoView.isPlaying) {
//                            e("视频不在播放状态，启动播放。")
//                            videoView.start()
//                        }
//                    } catch (e: Exception) {
//                        e(e.message)
//                    }
//                }
//                videoView.setOnErrorListener(object : MediaPlayer.OnErrorListener {
//                    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
////                        Toast.makeText(this@FullscreenActivity,"不能播放当前直播源，正在准备播放下一个视频",Toast.LENGTH_LONG).show()
//                        playLocalVideo()
//                        return true
//                    }
//                })
//                videoView.requestFocus()
//            }
////            stopTimerTask()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            e(e.message)
//        }
    }

    fun playLocalVideo() {
        try {
            isPlayLocal = true
            dataContext.editSetting(Setting.KEYS.is_play_local, isPlayLocal)
            mediaPosition = dataContext.getSetting(Setting.KEYS.media_position, 0).int
            mediaPath = dataContext.getSetting(Setting.KEYS.media_path, "").string
            e("路径：${mediaPath} , 位置：${mediaPosition / 1000}秒")

            fileList = _Utils.getAllLocalVideos(this)
            fileNames = arrayOfNulls<String>(fileList.size)
            fileList.forEach {
                //                e("title : ${it.Title} , size : ${it.FileSize/1024/1024}M")
                fileNames[it.FileId] = it.Title
                if (it.FilePath == mediaPath) {
                    index = it.FileId
                }
            }


            if (fileList.size <= 0)
                return

            val file = fileList[index]

            var mediaPosition = mediaPosition
            runOnUiThread {
                videoView.setVideoPath(file.FilePath)
                textView_title.text = file.Title
//            val mediaController = MediaController(this)
//            videoView.setMediaController(mediaController)

                videoView.setOnPreparedListener {
                    try {
                        e("video view has prepared... ${mediaPosition / 1000}秒")
                        textView_time.text =
                            format.format(mediaPosition.toDouble() / videoView.duration)
                        videoView.seekTo(mediaPosition)
                        if (!videoView.isPlaying) {
                            e("视频不在播放状态，启动播放。")
                            videoView.start()
                        }
                    } catch (e: Exception) {
                        e(e.message)
                    }
                }
                videoView.setOnCompletionListener {
                    index++
                    if (index >= fileList.size)
                        index = 0
                    val filePath = fileList[index].FilePath
                    videoView.setVideoPath(filePath)
                    textView_title.text = fileList[index].Title
                    mediaPosition = 0
                    dataContext.editSetting(Setting.KEYS.media_path, filePath)
                    dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
                }

                videoView.setOnErrorListener(object : MediaPlayer.OnErrorListener {
                    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
                        Toast.makeText(
                            this@FullscreenActivity,
                            "不能播放当前视频，正在准备播放下一个视频",
                            Toast.LENGTH_LONG
                        ).show()
                        mediaNext()
                        return true
                    }
                })
                videoView.requestFocus()
            }
            startTimerTask()
        } catch (e: Exception) {
            e.printStackTrace()
            e(e.message)
        }
    }

    fun mediaForward() {
        try {
            e("is play local : $isPlayLocal")
            if (isPlayLocal) {
                mediaPosition = videoView.currentPosition + videoView.duration / 100
                videoView.seekTo(mediaPosition)
                textView_time.text = format.format(mediaPosition.toDouble() / videoView.duration)
                dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
            } else {
                netVideoNum++
                if (netVideoNum >= uris.size) {
                    netVideoNum = 0
                }
                e("uri : ${uris[netVideoNum]}")
                dataContext.editSetting(Setting.KEYS.net_video_num, netVideoNum)
                videoView.setVideoURI(Uri.parse(uris[netVideoNum]))
            }
        } catch (e: Exception) {
            e(e.message)
        }
    }

    fun mediaRewind() {
        try {
            e("is play local : $isPlayLocal")
            if (isPlayLocal) {
                mediaPosition = videoView.currentPosition - videoView.duration / 100
                videoView.seekTo(mediaPosition)
                textView_time.text = format.format(mediaPosition.toDouble() / videoView.duration)
                dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
            } else {
                netVideoNum--
                if (netVideoNum < 0) {
                    netVideoNum = uris.size - 1
                }
                e("uri : ${uris[netVideoNum]}")
                dataContext.editSetting(Setting.KEYS.net_video_num, netVideoNum)
                videoView.setVideoURI(Uri.parse(uris[netVideoNum]))
            }
        } catch (e: Exception) {
            e(e.message)
        }
    }

    fun mediaPrev() {
        try {
            index--
            if (index < 0)
                index = fileList.size - 1
            val filePath = fileList[index].FilePath
            videoView.setVideoPath(filePath)
            textView_title.text = fileList[index].Title
            mediaPosition = 0
            dataContext.editSetting(Setting.KEYS.media_path, filePath)
            dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            e(e.message)
        }
    }

    fun mediaNext() {
        try {
            index++
            if (index >= fileList.size)
                index = 0
            val filePath = fileList[index].FilePath
            videoView.setVideoPath(filePath)
            textView_title.text = fileList[index].Title
            mediaPosition = 0
            dataContext.editSetting(Setting.KEYS.media_path, filePath)
            dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            e(e.message)
        }
    }

    fun mediaStart() {
        try {
            if (!videoView.isPlaying)
                videoView.start()
            startTimerTask()
            textView_title.setTextColor(Color.WHITE)
            textView_time.setTextColor(Color.WHITE)
        } catch (e: Exception) {
            e(e.message)
        }
    }

    fun mediaPause() {
        videoView.pause()
        mediaPosition = videoView.currentPosition
        dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
        stopTimerTask()
//        task?.cancel()
        textView_title.setTextColor(Color.RED)
        textView_time.setTextColor(Color.RED)
        e("当前播放位置：${mediaPosition / 1000}秒")
    }

    fun startTimerTask() {
        e("启动timerTask , timer task=$task , ${if (task == null) "系统重新构造task对象" else ""}")
        if (task == null) {
            task = object : TimerTask() {
                override fun run() {
                    try {
//                        e("南无阿弥陀佛：$isPlayLocal")
                        if (!isPlayLocal) {
                            runOnUiThread {
                                textView_time.text = ""
                                textView_title.text = ""
                                hideActionBar()
                            }
                            return
                        }
                        mediaPosition = videoView.currentPosition
                        dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)

                        runOnUiThread {
                            hideActionBar()
                            textView_time.text =
                                format.format(mediaPosition.toDouble() / videoView.duration)
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            timer.schedule(task, 0, 3000)
        }
    }

    fun stopTimerTask() {
        task?.cancel()
        task = null
    }

    fun stopTimer() {
        task?.cancel()
        task = null
        timer.cancel()
    }

    companion object {
        val uris = ArrayList<String>()

        init {
            uris.add("http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8")//CCTV1高清
            uris.add("http://ivi.bupt.edu.cn/hls/cctv3hd.m3u8")//CCTV3高清
            uris.add("http://ivi.bupt.edu.cn/hls/cctv5phd.m3u8")//CCTV5+高清
            uris.add("http://ivi.bupt.edu.cn/hls/cctv6hd.m3u8")//CCTV6高清
        }
    }

    override fun onResume() {
        e("on resume")
        mediaStart()
        val socketServiceIsRun = _Utils.isRunService(this, SocketService::class.java.name)
        val portCanUse = _Utils.isPortAvailable(_Session.ServiceSocketPort)
        e("port can use? $portCanUse")
        if (portCanUse) {
            e("重新启动ServiceSocket")
            val intent = Intent(this, SocketService::class.java)
            if (socketServiceIsRun) {
                e("重新启动socket服务")
                stopService(intent)
            }
            startService(Intent(this, SocketService::class.java))
        }
        super.onResume()
    }

    override fun onPause() {
        e("on pause")
        mediaPause()
        super.onPause()
    }

    override fun onStop() {
        e("on stop")
        super.onStop()
    }

    override fun onDestroy() {
        e("on destory")
        serverSocket.close()
        unregisterReceiver(mReceiver)
        stopTimer()
        super.onDestroy()
    }
}