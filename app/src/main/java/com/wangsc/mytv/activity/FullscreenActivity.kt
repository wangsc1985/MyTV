package com.wangsc.mytv.activity

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.wangsc.mytv.R
import com.wangsc.mytv._Utils
import com.wangsc.mytv._Utils.e
import com.wangsc.mytv.model.DataContext
import com.wangsc.mytv.model.Material
import com.wangsc.mytv.model.Setting
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.text.DecimalFormat
import java.util.*

class FullscreenActivity : AppCompatActivity() {
    private var index: Int
    private var mediaPosition: Int
    private var mediaPath: String
    val mReceiver: BroadcastReceiver = ScreenReceiver()
    private lateinit var fileList: List<Material>
    private lateinit var timer: Timer
    private lateinit var serverSocket: ServerSocket

    var format = DecimalFormat("#%")

    private lateinit var dataContext: DataContext

    init {
        index = 0
        mediaPosition = 0
        mediaPath = ""
    }


    //region 全屏处理
    val mHideHandler = Handler()
    private fun hide() {
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

    private val mHideRunnable = Runnable { hide() }
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }
    //endregion

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        e("on window focus changed : $hasFocus")
        hide()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        e("on post create")
        hide()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataContext = DataContext(this)

        try {
            setContentView(R.layout.activity_fullscreen)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            /**
             * 保持屏幕常亮，即使视频暂停播放
             */
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            mediaPosition = dataContext.getSetting(Setting.KEYS.media_position, 0).int
            mediaPath = dataContext.getSetting(Setting.KEYS.media_path, "").string

            fileList = _Utils.getAllLocalVideos(this)
            var fileNames = arrayOfNulls<String>(fileList.size)
            fileList.forEach {
                fileNames[it.FileId] = it.Title
                if (it.FilePath == mediaPath) {
                    index = it.FileId
                }
            }


            if (fileList.size <= 0)
                return

            val file = fileList[index]
            videoView.setVideoPath(file.FilePath)
            textView_title.text = file.Title
//            val mediaController = MediaController(this)
//            videoView.setMediaController(mediaController)

            videoView.setOnPreparedListener {
                textView_time.text = format.format(mediaPosition.toDouble() / videoView.duration)
                videoView.seekTo(mediaPosition)
                videoView.start()
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

            videoView.requestFocus()

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
                serverSocket = ServerSocket(8000);
                while (true) {
                    var socket = serverSocket.accept();
                    var dis = DataInputStream(socket.getInputStream())
                    val dd = dis.readInt()
                    when (dd) {
                        0 -> {
                            val dos = DataOutputStream(socket.getOutputStream())
                            dos.writeBoolean(videoView.isPlaying)
                            dos.flush()
                            dos.close()
                        }
                        1 -> {
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
                            // 音量加
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
                            // 音量减
                            runOnUiThread {
                                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                am.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        }
                        4 -> {
                            // 前进
                            runOnUiThread {
                                mediaForward()
                            }
                        }
                        5 -> {
                            // 后退
                            runOnUiThread {
                                mediaRewind()
                            }
                        }
                        6 -> {
                            // 下一个
                            runOnUiThread {
                                mediaNext()
                            }
                        }
                        7 -> {
                            // 上一个
                            runOnUiThread {
                                mediaPrev()
                            }
                        }
                    }
                    dis.close()
                    socket.close()
                }
            } catch (e: Exception) {
                e(e.message.toString())
            }
        }.start()
    }

    fun mediaForward() {
        mediaPosition = videoView.currentPosition + 30000
        videoView.seekTo(mediaPosition)
        textView_time.text = format.format(mediaPosition.toDouble() / videoView.duration)
        dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
    }

    fun mediaRewind() {
        mediaPosition = videoView.currentPosition - 30000
        videoView.seekTo(mediaPosition)
        textView_time.text = format.format(mediaPosition.toDouble() / videoView.duration)
        dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
    }

    fun mediaPrev() {
        index--
        if (index < 0)
            index = fileList.size - 1
        val filePath = fileList[index].FilePath
        videoView.setVideoPath(filePath)
        textView_title.text = fileList[index].Title
        mediaPosition = 0
        dataContext.editSetting(Setting.KEYS.media_path, filePath)
        dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
    }

    fun mediaNext() {
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

    fun mediaStart() {
        videoView.start()
        startTimer()
        textView_title.setTextColor(Color.WHITE)
        textView_time.setTextColor(Color.WHITE)
//        imageView_play.visibility=View.INVISIBLE
    }

    fun mediaPause() {
        videoView.pause()
        mediaPosition = videoView.currentPosition
        dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
        timer.cancel()
        textView_title.setTextColor(Color.RED)
        textView_time.setTextColor(Color.RED)
        e("当前播放位置：$mediaPosition")
    }

    override fun onResume() {
        e("on resume")
        mediaStart()
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
        super.onDestroy()
    }

    fun startTimer() {
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                try {
                    mediaPosition = videoView.currentPosition
                    dataContext.editSetting(Setting.KEYS.media_position, mediaPosition)
                    hide()

                    runOnUiThread {
                        textView_time.text =
                            format.format(mediaPosition.toDouble() / videoView.duration)
                    }
                } catch (e: Exception) {
                }
            }
        }, 10000, 10000)
    }

}