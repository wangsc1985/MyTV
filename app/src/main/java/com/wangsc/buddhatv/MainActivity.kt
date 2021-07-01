package com.wangsc.buddhatv

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
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
import java.io.FileFilter
import java.text.Collator
import java.text.RuleBasedCollator
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private var timer = Timer()
    private var task: TimerTask? = null
    private var playFileIndex: Int
    private var selectedFileIndex: Int
    private var mediaPosition: Int
    var selectedView: View?
    var playView: View?
    private var mediaPath: String
    private var fileList: MutableList<File> = ArrayList()
    lateinit var adapter: ListAdapter

    private lateinit var dc: DataContext
    private lateinit var fileNames: Array<String?>

    init {
        selectedView = null
        playView = null
        playFileIndex = 0
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

    fun loadTitle() {
        try {
            Thread {
                _OkHttpUtil.getRequest(resources.getString(R.string.version_url), HttpCallback { html ->
                    var html = html.replace("\r", "").replace("\n", "")
                    var matcher = Pattern.compile("(?<=佛陀讲堂【).{1,100}(?=】)").matcher(html)
                    matcher.find()
                    var title = matcher.group().trim()
                    dc.editSetting(Setting.KEYS.media_title, title)
                })
            }.start()
        } catch (e: Throwable) {
            dc.editSetting(Setting.KEYS.media_title, e.message ?: "空信息")
        }
    }

    fun log(log: String) {
        runOnUiThread {
            e(log)
//            tv_log.text = "${DateTime().toTimeString()} $log \n${tv_log.text}"
        }
    }

    fun showLog() {
        tv_log.setText(dc.getSetting(Setting.KEYS.media_title, "一门深入 长时薰修").string)
        tv_log.visibility = View.VISIBLE
    }

    fun hideLog() {
        tv_log.visibility = View.GONE
    }

    fun selectedView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setBackgroundResource(R.color.select)
            log("选择 ${name.text}")
        }
    }

    fun unSelectedView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setBackgroundResource(R.color.black)
            log("XXX 选择 ${name.text}")
        }
    }

    fun playView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setTextColor(Color.RED)
            log("${name.text} 播放")
        }
    }

    fun unPlayView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setTextColor(Color.WHITE)
            log("${name.text} 播放 XXX ")
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
//            loadTitle()

            hideList()

            lv_list.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    log("iiiiiiiiiiiiiiiiiiiii列表项选中iiiiiiiiiiiiiiiiiiiii")
                    unSelectedView(viewMap[selectedFileIndex])
                    selectedFileIndex = position
                    selectedView(viewMap[selectedFileIndex])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    log("nothing selected...")
                }
            }


            lv_list.setOnItemClickListener { parent, view, position, id ->
                log("iiiiiiiiiiiiiiiiiiiii【列表选项确认】iiiiiiiiiiiiiiiiiiiii")
                unPlayView(viewMap[playFileIndex])
                playFileIndex = selectedFileIndex
                playView(viewMap[playFileIndex])

                val filePath = fileList[playFileIndex].absolutePath
                if (filePath != dc.getSetting(Setting.KEYS.media_path, filePath).string) {
                    videoView.setVideoPath(filePath)
                    tv_title.text = fileList[playFileIndex].name
                    mediaPosition = 0
                    dc.editSetting(Setting.KEYS.media_path, filePath)
                    dc.editSetting(Setting.KEYS.media_position, mediaPosition)
                }
                hideList()
            }

            playLocalVideo()
            adapter = ListAdapter()
            lv_list.adapter = adapter
            lv_list.setSelection(selectedFileIndex)
            hideLog()

            videoView.setOnPreparedListener {
                try {
                    log("视频加载完毕，当前位置： ${mediaPosition / 1000}秒")
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
                    mediaPause()
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

    fun loadAllFiles( list:MutableList<File>,dirPath:String, filter: FileFilter){
        val dir = File(dirPath)
        val files = dir.listFiles()
        files.forEach { file->
            if(file.isDirectory){
                loadAllFiles(list,file.absolutePath,filter)
            }else{
                list.add(file)
            }
        }
    }

    fun playLocalVideo() {
        try {
            mediaPosition = dc.getSetting(Setting.KEYS.media_position, 0).int
            mediaPath = dc.getSetting(Setting.KEYS.media_path, "").string

            val path = _Utils.searchPath().replace("2", "1")
                .replace("3", "1")
                .replace("4", "1")
                .replace("5", "1")
                .replace("6", "1")
                .replace("7", "1")
                .replace("8", "1")
                .replace("9", "1")
            log("U盘路径：$path")
            log("播放路径：${mediaPath} , 位置：${mediaPosition / 1000}秒")
            fileList.clear()
            loadAllFiles(fileList,path, FileFilter { file -> file.extension == "mp4" })
            Collections.sort(fileList, SortByName())

            val mediaName = File(mediaPath).name
            fileNames = arrayOfNulls(fileList.size)
            for (iii in fileList.indices) {
                fileNames[iii] = fileList[iii].name
                if (fileList[iii].name == mediaName) {
                    playFileIndex = iii
                    selectedFileIndex = playFileIndex
                }
            }
            if (playFileIndex == 0) {
                dc.editSetting(Setting.KEYS.media_path, fileList[playFileIndex].absolutePath)
            }

            if (fileList.size <= 0)
                return

            val file = fileList[playFileIndex]

            runOnUiThread {
                videoView.setVideoPath(file.absolutePath)
                tv_title.text = file.name
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
            unPlayView(viewMap[playFileIndex])
            unSelectedView(viewMap[selectedFileIndex])

            playFileIndex++
            selectedFileIndex = playFileIndex
            if (playFileIndex >= fileList.size)
                playFileIndex = 0

            unPlayView(viewMap[playFileIndex])
            unSelectedView(viewMap[selectedFileIndex])

            val filePath = fileList[playFileIndex].absolutePath
            videoView.setVideoPath(filePath)
            tv_title.text = fileList[playFileIndex].name
            mediaPosition = 0
            dc.editSetting(Setting.KEYS.media_path, filePath)
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaPrv() {
        try {
            log("上一个视频")

            unPlayView(viewMap[playFileIndex])
            unSelectedView(viewMap[selectedFileIndex])

            playFileIndex--
            selectedFileIndex = playFileIndex
            if (playFileIndex <= -1)
                playFileIndex = fileList.size - 1

            unPlayView(viewMap[playFileIndex])
            unSelectedView(viewMap[selectedFileIndex])


            val filePath = fileList[playFileIndex].absolutePath
            videoView.setVideoPath(filePath)
            tv_title.text = fileList[playFileIndex].name
            mediaPosition = 0
            dc.editSetting(Setting.KEYS.media_path, filePath)
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaForward() {
        try {
            mediaPosition = videoView.currentPosition + videoView.duration / 100
            videoView.seekTo(mediaPosition)
            setProgress()
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaRewind() {
        try {
            mediaPosition = videoView.currentPosition - videoView.duration / 100
            videoView.seekTo(mediaPosition)
            setProgress()
            dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaStart() {
        try {
            if (!videoView.isPlaying)
                videoView.start()
            startTimerTask()
            hideLog()
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }


    fun mediaPause() {
        videoView.pause()
        mediaPosition = videoView.currentPosition
        dc.editSetting(Setting.KEYS.media_position, mediaPosition)
        stopTimerTask()
        showLog()
    }

    fun startTimerTask() {
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
            var view = viewMap[position]
            val buddha = fileList[position]
            try {
                if (view == null) {
                    log("加载 ${buddha.name}")
                    view = View.inflate(this@MainActivity, R.layout.inflate_list_item, null)
                    viewMap[position] = view
                }
                val name = view!!.findViewById<TextView>(R.id.tv_name)
                name.text = buddha.name
                if (position == playFileIndex) {
                    name.setTextColor(Color.RED)
                } else {
                    name.setTextColor(Color.WHITE)
                }
                if (position == selectedFileIndex) {
                    name.setBackgroundResource(R.color.select)
                } else {
                    name.setBackgroundResource(R.color.black)
                }
            } catch (e: Exception) {
                log(_Utils.getExceptionStr(e))
            }
            return view
        }
    }

    var viewMap: MutableMap<Int, View> = HashMap()

    fun isListShow(): Boolean {
        return lv_list.visibility == View.VISIBLE
    }

    fun showList() {
        lv_list.visibility = View.VISIBLE
        lv_list.itemsCanFocus = true
    }

    fun hideList() {
        unSelectedView(viewMap[selectedFileIndex])
        selectedFileIndex = playFileIndex
        selectedView(viewMap[selectedFileIndex])
        lv_list.setSelection(selectedFileIndex)

        // FIXME: 2021/6/30 列表关闭，setselection方法没有执行？那为什么刚启动，列表没有加载完毕，上下键还起作用的时候没事，一旦上下键不起作用，直接执行list的itemselected事件时就出问题。

        lv_list.visibility = View.GONE
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                e("--------------数字键0--------------")
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (isListShow()) {
                    log("--------------列表确认键--------------")
                    unPlayView(viewMap[playFileIndex])
                    playFileIndex = selectedFileIndex
                    playView(viewMap[playFileIndex])
                    hideList()

                    val filePath = fileList[playFileIndex].absolutePath
                    if (filePath != dc.getSetting(Setting.KEYS.media_path, filePath).string) {
                        videoView.setVideoPath(filePath)
                        tv_title.text = fileList[playFileIndex].name
                        mediaPosition = 0
                        dc.editSetting(Setting.KEYS.media_path, filePath)
                        dc.editSetting(Setting.KEYS.media_position, mediaPosition)
                    }
                } else {
                    log("--------------播放暂停键--------------")
                    if (videoView.isPlaying) {
                        mediaPause()
                    } else {
                        mediaStart()
                    }
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isListShow()) {
                    log("--------------上一个--------------")
                    unSelectedView(viewMap[selectedFileIndex])
                    selectedFileIndex--
                    if (selectedFileIndex <= -1)
                        selectedFileIndex = fileList.size - 1

                    lv_list.setSelection(selectedFileIndex)
                } else {
                    showList()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isListShow()) {
                    log("--------------下一个--------------")
                    unSelectedView(viewMap[selectedFileIndex])
                    selectedFileIndex++
                    if (selectedFileIndex >= fileList.size)
                        selectedFileIndex = 0

                    lv_list.setSelection(selectedFileIndex)
                } else {
                    showList()
                    return true
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
                    if (tv_log.text.toString().length > 100) {
                        tv_log.text = ""
                    } else {
                        hideList()
                    }
                    return true
                }
                if (!videoView.isPlaying) {
                    if (tv_log.text.toString().length > 100) {
                        tv_log.text = ""
                    } else {
                        mediaStart()
                    }
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