package com.wangsc.buddhatv

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.TextView
import com.wangsc.buddhatv.callback.HttpCallback
import com.wangsc.buddhatv.model.DataContext
import com.wangsc.buddhatv.model.DateTime
import com.wangsc.buddhatv.model.Setting
import com.wangsc.buddhatv.util._OkHttpUtil
import com.wangsc.buddhatv.util._Utils
import com.wangsc.buddhatv.util._Utils.e
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.io.File
import java.text.Collator
import java.text.RuleBasedCollator
import java.util.*
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private var timer = Timer()
    private var task: TimerTask? = null
    private var playingFileIndex: Int
    private var selectedFileIndex: Int
    private var mediaPosition: Int
    private var mediaPath: String
    private lateinit var fileList: Array<File>
    lateinit var adapter: ListAdapter

    private lateinit var dc: DataContext
    private lateinit var fileNames: Array<String?>

    init {
        playingFileIndex = 0
        selectedFileIndex = 0
        mediaPosition = 0
        mediaPath = ""
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
    //endregion

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        e("on post create")
        hideActionBar()
    }

    private fun loadTitle() {
        Thread {
            try {
                _OkHttpUtil.getRequest(resources.getString(R.string.version_url), HttpCallback { html ->
                    var html = html.replace("\r", "").replace("\n", "")
                    var matcher = Pattern.compile("(?<=佛陀讲堂)*(?=</p>)").matcher(html)
                    matcher.find()
                    var title = matcher.group().trim()
                    dc.editSetting(Setting.KEYS.media_title, title)
                })
            } catch (e: Exception) {
            }
        }.start()
    }

    fun log(log: String) {
        runOnUiThread {
            e(log)
//            tv_log.text = "${DateTime().toTimeString()} $log \n${tv_log.text}"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        e("on create")
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_fullscreen)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            /**
             * 保持屏幕常亮，即使视频暂停播放
             */
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            dc = DataContext(this)
            loadTitle()

            lv_list.visibility = View.GONE

//            lv_list.setOnItemClickListener { parent, view, position, id ->
//                if (position != playingFileIndex) {
//                    val file = fileList[position]
//                    var filePath = file.absolutePath
//                    e("$filePath")
//                    videoView.setVideoPath(filePath)
//                    videoView.start()
//                    videoView.requestFocus()
////                    tv_title.text = file.name
//                    mediaPosition = 0
//                    dc.editSetting(Setting.KEYS.media_path, filePath)
//                    dc.editSetting(Setting.KEYS.media_position, mediaPosition)
//                }
//            }


            playLocalVideo()
            adapter = ListAdapter()
            lv_list.adapter = adapter
            lv_list.setSelection(selectedFileIndex)
            hideLog()

            videoView.setOnPreparedListener {
                try {
                    e("video view has prepared... ${mediaPosition / 1000}秒")
                    log("视频加载完毕，当前位置： ${mediaPosition / 1000}秒")
//                    tv_progress.text = format.format(mediaPosition.toDouble() * 100 / videoView.duration)
                    setProgress()
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
                log("当前视频播放完毕！")
                mediaNext()
            }

            videoView.setOnErrorListener(object : MediaPlayer.OnErrorListener {
                override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
                    e("不能播放当前视频，播放下一个视频。错误信息：what: $what  extra: $extra")
                    log("不能播放当前视频，播放下一个视频。错误信息：what: $what  extra: $extra")
                    return true
                }
            })
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    internal class SortByName : Comparator<File> {
        // java提供的对照器
        private var collator: RuleBasedCollator

        /**
         * 默认构造器是按中文字母表进行排序
         * */
        constructor() {
            collator = Collator.getInstance(Locale.CHINA) as RuleBasedCollator
        }

        /**
         * 可以通过传入Locale值实现按不同语言进行排序
         * */
        constructor(locale: Locale) {
            collator = Collator.getInstance(locale) as RuleBasedCollator
        }

        override fun compare(o1: File, o2: File): Int {
            val s1 = collator.getCollationKey(o1.name)
            val s2 = collator.getCollationKey(o2.name)
            return collator.compare(
                s1.getSourceString(),
                s2.getSourceString()
            )
        }
    }

    fun playLocalVideo() {
        try {
            mediaPosition = dc.getSetting(Setting.KEYS.media_position, 0).int
            mediaPath = dc.getSetting(Setting.KEYS.media_path, "").string
            log("路径：${mediaPath} , 位置：${mediaPosition / 1000}秒")

            val path = _Utils.searchPath().replace("2", "1")
                .replace("3", "1")
                .replace("4", "1")
                .replace("5", "1")
                .replace("6", "1")
                .replace("7", "1")
                .replace("8", "1")
                .replace("9", "1")
            log(path)
            val dir = File(path)
            val list = dir.listFiles({ file -> file.extension == "mp4" }).toList()
            Collections.sort(list, SortByName())
            fileList = list.toTypedArray()

            fileNames = arrayOfNulls(fileList.size)
            for (i in fileList.indices) {
                fileNames[i] = fileList[i].name
                if (fileList[i].absolutePath == mediaPath) {
                    playingFileIndex = i
                    selectedFileIndex = playingFileIndex
                }
            }
            if (playingFileIndex == 0) {
                dc.editSetting(Setting.KEYS.media_path, fileList[playingFileIndex].absolutePath)
            }

            if (fileList.size <= 0)
                return

            val file = fileList[playingFileIndex]

            runOnUiThread {
                videoView.setVideoPath(file.absolutePath)
                tv_title.text = file.name
//                tv_progress.text = format.format(mediaPosition.toDouble() * 100 / videoView.duration)
                videoView.requestFocus()
            }
            startTimerTask()
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaNext() {
        try {
            log("下一个视频")
            playingFileIndex++
            selectedFileIndex = playingFileIndex
            if (playingFileIndex >= fileList.size)
                playingFileIndex = 0
            val filePath = fileList[playingFileIndex].absolutePath
            videoView.setVideoPath(filePath)
            tv_title.text = fileList[playingFileIndex].name
            mediaPosition = 0
            dc.editSetting(Setting.KEYS.media_path, filePath)
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaPrv() {
        try {
            log("上一个视频")
            playingFileIndex--
            selectedFileIndex = playingFileIndex
            if (playingFileIndex <= -1)
                playingFileIndex = fileList.size - 1
            val filePath = fileList[playingFileIndex].absolutePath
            videoView.setVideoPath(filePath)
            tv_title.text = fileList[playingFileIndex].name
            mediaPosition = 0
            dc.editSetting(Setting.KEYS.media_path, filePath)
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaForward() {
        try {
            log("快进")
            mediaPosition = videoView.currentPosition + videoView.duration / 100
            videoView.seekTo(mediaPosition)
//            tv_progress.text = format.format(mediaPosition.toDouble() * 100 / videoView.duration)
            setProgress()
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaRewind() {
        try {
            log("快退")
            mediaPosition = videoView.currentPosition - videoView.duration / 100
            videoView.seekTo(mediaPosition)
//            tv_progress.text = format.format(mediaPosition.toDouble() * 100 / videoView.duration)
            setProgress()
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaStart() {
        try {
            log("视频开始")
            if (!videoView.isPlaying)
                videoView.start()
            startTimerTask()
//            tv_title.setTextColor(Color.WHITE)
//            tv_progress.setTextColor(Color.WHITE)
            hideLog()
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }


    fun mediaPause() {
        log("视频暂停")
        videoView.pause()
        mediaPosition = videoView.currentPosition
        dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        stopTimerTask()
        showLog()
        e("当前播放位置：${mediaPosition / 1000}秒")
    }

    fun startTimerTask() {
        e("启动timerTask , timer task=$task , ${if (task == null) "系统重新构造task对象" else ""}")
        if (task == null) {
            task = object : TimerTask() {
                override fun run() {
                    try {
                        if (videoView.isPlaying) {
                            mediaPosition = videoView.currentPosition
                            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
                        }

                        runOnUiThread {
                            hideActionBar()
//                            tv_progress.text = format.format(mediaPosition.toDouble() * 100 / videoView.duration)
                            setProgress()
                            tv_time.text = DateTime().toShortTimeString()
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            timer.schedule(task, 0, 1000)
        }
    }

    fun setProgress() {
//        tv_progress.text = "${durationToTimeString(mediaPosition)}/${durationToTimeString(videoView.duration)}"
//        val duration =videoView.duration-mediaPosition
//        tv_progress.text = "${if(duration>0) durationToTimeString(duration) else ""}"
        pb_progress.max = videoView.duration
        pb_progress.progress = mediaPosition
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

    override fun onDestroy() {
        e("on destory")
        stopTimer()
        super.onDestroy()
    }

    inner class ListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return fileList.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0L
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var convertView = convertView
            try {
                convertView = View.inflate(this@MainActivity, R.layout.inflate_list_item, null)
                val buddha = fileList[position]
                val name = convertView.findViewById<TextView>(R.id.tv_name)
                name.text = buddha.name
                if (position == playingFileIndex) {
                    name.setTextColor(Color.RED)
                } else {
                    name.setTextColor(Color.WHITE)
                }
                if (position == selectedFileIndex) {
                    name.setBackgroundResource(R.color.select)
                } else {
                    name.setBackgroundResource(R.color.black)
                }
                log(buddha.name)
            } catch (e: Exception) {
            }
            return convertView
        }

    }

    fun isListShow(): Boolean {
        return lv_list.visibility == View.VISIBLE
    }

    fun showList() {
        lv_list.visibility = View.VISIBLE
    }

    fun hideList() {
        lv_list.visibility = View.GONE
        selectedFileIndex = playingFileIndex
        adapter.notifyDataSetChanged()
    }

    fun showLog() {
        tv_log.text = dc.getSetting(Setting.KEYS.media_title, "一门深入 长时薰修").string
        tv_log.visibility = View.VISIBLE
//        imageView.visibility = View.VISIBLE
    }

    fun hideLog() {
        tv_log.visibility = View.GONE
//        imageView.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                e("数字键0")
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                e("中间键")
                if (isListShow()) {
                    playingFileIndex = selectedFileIndex
                    val filePath = fileList[playingFileIndex].absolutePath
                    if (filePath != dc.getSetting(Setting.KEYS.media_path, filePath).string) {
                        videoView.setVideoPath(filePath)
                        tv_title.text = fileList[playingFileIndex].name
                        mediaPosition = 0
                        dc.editSetting(Setting.KEYS.media_path, filePath)
                        dc.editSetting(Setting.KEYS.media_position, mediaPosition)
                    }
                    hideList()
                } else {
                    if (videoView.isPlaying) {
                        mediaPause()
                    } else {
                        mediaStart()
                    }
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                e("下方向键")
                if (isListShow()) {

                    selectedFileIndex++
                    if (selectedFileIndex >= fileList.size)
                        selectedFileIndex = 0

                    lv_list.setSelection(selectedFileIndex)
                    adapter.notifyDataSetChanged()
                } else {
                    showList()
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                e("上方向键")
                if (isListShow()) {
                    selectedFileIndex--
                    if (selectedFileIndex <= -1)
                        selectedFileIndex = fileList.size - 1

                    lv_list.setSelection(selectedFileIndex)
                    adapter.notifyDataSetChanged()
                } else {
                    showList()
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                e("左方向键")
                if (isListShow()) {
                    hideList()
                } else {
                    mediaRewind()
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                e("右方向键")
                if (isListShow()) {
                    hideList()
                } else {
                    mediaForward()
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isListShow()) {
                    hideList()
                    return true
                }
            }

            KeyEvent.KEYCODE_HOME -> {

            }


            KeyEvent.KEYCODE_MENU -> {
                if (isListShow()) {
                    hideList()
                } else {
                    showList()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @Throws(Exception::class)
    fun durationToTimeString(duration: Int): String {
        val second = duration % 60000 / 1000
        val miniteT = duration / 60000
        val minite = miniteT % 60
        val hour = miniteT / 60
        return "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
    }
}