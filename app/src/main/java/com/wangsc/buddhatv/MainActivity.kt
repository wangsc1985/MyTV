package com.wangsc.buddhatv

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
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
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileFilter
import java.text.Collator
import java.text.DecimalFormat
import java.text.RuleBasedCollator
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// TODO: 2021/7/2 网络直播先检查网络能否连通。并且程序运行中随时监听网络变化广播。

class MainActivity : AppCompatActivity() {
    private var timer = Timer()
    private var task: TimerTask? = null
    private var playFileIndex: Int
    private var selectedFileIndex: Int
    private var mediaPosition: Int
    private var operateList: MutableList<Operate> = ArrayList()
    lateinit var adapter: ListAdapter

    private var dc: DataContext? = null
    val format = DecimalFormat("000")
    var viewMap: MutableMap<Int, View> = HashMap()

    data class MyUri(var path: String, var name: String)

    data class Operate(var name: String, var fileList: List<MyUri>)

    init {
        playFileIndex = 0
        selectedFileIndex = 0
        mediaPosition = 0

    }

    fun createOperateList() {
        var fileList1: MutableList<MyUri> = ArrayList()
        val operate1 = Operate("净土大经科注第四回", fileList1)
        for (ii in 1..578) {
            fileList1.add(MyUri("https://js1.amtb.cn/media/himp4/02/02-037/02-037-0${format.format(ii)}.mp4", "${operate1.name} ${format.format(ii)}"))//CCTV1高清
        }
        operateList.add(operate1)

        val fileList2: MutableList<MyUri> = ArrayList()
        val operate2 = Operate("净土大经科注第五回", fileList2)
        for (ii in 1..14) {
            fileList2.add(MyUri("https://hk2.amtb.de/redirect/media/himp4/02/02-047/02-047-0${format.format(ii)}.mp4", "${operate2.name} ${format.format(ii)}"))//CCTV1高清
        }
        operateList.add(operate2)


        val path = _Utils.searchPath().replace("2", "1")
            .replace("3", "1")
            .replace("4", "1")
            .replace("5", "1")
            .replace("6", "1")
            .replace("7", "1")
            .replace("8", "1")
            .replace("9", "1")
        if (File(path).exists()) {
            val fileList3: MutableList<MyUri> = ArrayList()
            loadAllFiles(fileList3, path, FileFilter { file -> file.extension == "mp4" })
            Collections.sort(fileList3, SortByName())
            if (fileList3.size > 0) {
                operateList.add(Operate("本地", fileList3))
            }
        }
    }

    inner class NetworkStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var mAction = intent?.getAction()
            if (mAction.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                var connManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                var info = connManager.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {

                } else {

                }
            }
        }
    }

    var receiver = NetworkStateReceiver()


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
                    val appName = resources.getString(R.string.app_name)
                    var html = html.replace("\r", "").replace("\n", "")
                    var matcher = Pattern.compile("(?<=${appName}【).{1,100}(?=】)").matcher(html)
                    if (matcher.find()) {
                        var title = matcher.group().trim()
                        dc?.editSetting(Setting.KEYS.media_title, title)
                        runOnUiThread {
                            tv_log.text = title
                        }
                    }
                })
            }.start()
        } catch (e: Throwable) {
            dc?.editSetting(Setting.KEYS.media_title, e.message ?: "空信息")
        }
    }

    fun log(log: String) {
        runOnUiThread {
            e(log)
//            tv_log.text = "${DateTime().toTimeString()} $log \n${tv_log.text}"
        }
    }

    var selectedOperateIndex = 0
    var operateIndex = 0
    fun refreshOperate() {
        e("selectedOperateIndex : $selectedOperateIndex")
        layout_operate.removeAllViews()
        for (ii in operateList.indices) {
            var view = View.inflate(this, R.layout.inflate_operate_btn, null)
            var name = view.findViewById<TextView>(R.id.tv_name)
            name.text = operateList[ii].name
            if (ii == selectedOperateIndex) {
                name.setBackgroundResource(R.color.select)
            } else {
                name.setBackgroundResource(R.color.black)
            }
            if (ii == operateIndex) {
                name.setTextColor(Color.RED)
            } else {
                name.setTextColor(Color.WHITE)
            }
            layout_operate.addView(view)
        }
    }

    //region 对话框


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
        lv_list.visibility = View.GONE
    }

    fun isOperateShow(): Boolean {
        return layout_operate.visibility == View.VISIBLE
    }

    fun showOperate() {
        runOnUiThread {
            layout_operate.visibility = View.VISIBLE
        }
    }

    fun hideOperate() {
        runOnUiThread {
            layout_operate.visibility = View.GONE
            selectedOperateIndex = operateIndex
            refreshOperate()
        }
    }

    fun showLog() {
        loadTitle()
        runOnUiThread {
            dc?.let {
                tv_log.setText(it.getSetting(Setting.KEYS.media_title, "一门深入 长时薰修").string)
            }
            tv_log.visibility = View.VISIBLE
        }
    }

    fun hideLog() {
        runOnUiThread {
            tv_log.visibility = View.GONE
        }
    }

    fun showLoading() {
        e("show loading")
        runOnUiThread {
            tv_loading.visibility = View.VISIBLE
            pb_loading.visibility = View.VISIBLE
        }
    }

    fun hideLoading() {
        runOnUiThread {
            tv_loading.visibility = View.GONE
            pb_loading.visibility = View.GONE
        }
    }
    //endregion

    fun selectedView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setBackgroundResource(R.color.select)
//            log("选择 ${name.text}")
        }
    }

    fun unSelectedView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setBackgroundResource(R.color.black)
//            log("XXX 选择 ${name.text}")
        }
    }

    fun playView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setTextColor(Color.RED)
//            log("${name.text} 播放")
        }
    }

    fun unPlayView(view: View?) {
        view?.let {
            val name = view.findViewById<TextView>(R.id.tv_name)
            name.setTextColor(Color.WHITE)
//            log("${name.text} 播放 XXX ")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        e("on create")
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            /**
             * 保持屏幕常亮，即使视频暂停播放
             */
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


            dc = DataContext(this)
            dc?.let { dc ->
                operateIndex = dc.getSetting(Setting.KEYS.operate_index, 0).int
            }

            loadTitle()
            createOperateList()
            refreshOperate()

            hideOperate()
            hideList()
            hideLoading()


//            videoView.setOnLongClickListener {
//                if (videoView.isPlaying) {
//                    mediaPause()
//                } else {
//                    mediaStart()
//                }
//                true
//            }
            lv_list.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                    log("iiiiiiiiiiiiiiiiiiiii列表项选中iiiiiiiiiiiiiiiiiiiii")
                    unSelectedView(viewMap[selectedFileIndex])
                    selectedFileIndex = position
                    selectedView(viewMap[selectedFileIndex])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    log("nothing selected...")
                }
            }


            lv_list.setOnItemClickListener { parent, view, position, id ->
//                log("iiiiiiiiiiiiiiiiiiiii【列表选项确认】iiiiiiiiiiiiiiiiiiiii")
                dc?.let { dc ->
                    unPlayView(viewMap[playFileIndex])
                    playFileIndex = selectedFileIndex
                    playView(viewMap[playFileIndex])


                    var filePathDB = ""
                    val mp = dc?.getMediaPosition(operateList[operateIndex].name)
                    if (mp != null) {
                        filePathDB = mp.filePath
                    }

                    val filePath = operateList[operateIndex].fileList[playFileIndex].path
                    if (filePath != filePathDB) {
                        showLoading()
                        videoView.setVideoPath(filePath)
                        tv_title.text = operateList[operateIndex].fileList[playFileIndex].name
                        mediaPosition = 0
                        dc.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[playFileIndex].path, mediaPosition)
                    }
                    hideList()
                }
            }

            startMediaPlayer()
            adapter = ListAdapter()
            lv_list.adapter = adapter
            lv_list.setSelection(selectedFileIndex)
            hideLog()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                videoView.setOnInfoListener { mp, what, extra ->
                    log("video view on info listener $what  $extra")
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                            e("开始缓冲...")
                            showLoading()

                        }
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                            e("缓冲完毕")
                            hideLoading()
                        }
                        else -> {

                        }
                    }
                    true
                }
            }
            videoView.setOnPreparedListener {
                try {
                    log("视频加载完毕，当前位置： ${mediaPosition / 1000}秒")
                    hideLoading()
                    errorCount = 0
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
                    log("不能播放当前视频，播放下一个视频。错误信息：what: $what  extra: $extra")
                    errorCount++
                    showLoading()
                    if (operateList[operateIndex].name == resources.getString(R.string.local_operate)) {
                        this@MainActivity.finish()
                    } else {
                        videoView.setVideoPath(operateList[operateIndex].fileList[playFileIndex].path)
                    }
                    return true
                }
            })

            var filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(receiver, filter)

        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    var errorCount = 0

    internal class SortByName : Comparator<MyUri> {
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

        override fun compare(o1: MyUri, o2: MyUri): Int {
            val s1 = collator.getCollationKey(o1.name)
            val s2 = collator.getCollationKey(o2.name)
            return collator.compare(
                s1.getSourceString(),
                s2.getSourceString()
            )
        }
    }

    fun loadAllFiles(list: MutableList<MyUri>, dirPath: String, filter: FileFilter) {
        val dir = File(dirPath)
        val dirs = dir.listFiles()
        val files = dir.listFiles { file -> file.extension == "mp4" }
        dirs.forEach { file ->
            if (file.isDirectory) {
                loadAllFiles(list, file.path, filter)
            }
        }
        var result: MutableList<MyUri> = ArrayList()
        files.forEach {
            result.add(MyUri(it.path, it.name))
        }
        list.addAll(result)
    }

    fun startMediaPlayer() {
        try {

            this.mediaPosition = 0
            playFileIndex = 0
            selectedFileIndex = playFileIndex

            dc?.let { dc ->
                val mediaPosition = dc.getMediaPosition(operateList[operateIndex].name)

                if (mediaPosition != null) {
                    var filePathDB = mediaPosition.filePath
                    var mediaNameDB = File(filePathDB).name
                    var positionDB = mediaPosition.position

                    var fileList = operateList[operateIndex].fileList
                    for (ii in fileList.indices) {
                        val fileName = File(fileList[ii].path).name
                        if (fileName == mediaNameDB) {
                            playFileIndex = ii
                            selectedFileIndex = playFileIndex
                            this.mediaPosition = positionDB
                        }
                    }
                } else {
                    dc.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[0].path, 0)
                }
            }


            if (operateList[operateIndex].fileList.size <= 0)
                return


            showLoading()
            runOnUiThread {
                e("地址：${operateList[operateIndex].fileList[playFileIndex].path}")
                videoView.setVideoPath(operateList[operateIndex].fileList[playFileIndex].path)
                tv_title.text = operateList[operateIndex].fileList[playFileIndex].name
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
            if (playFileIndex >= operateList[operateIndex].fileList.size)
                playFileIndex = 0

            unPlayView(viewMap[playFileIndex])
            unSelectedView(viewMap[selectedFileIndex])

            val filePath = operateList[operateIndex].fileList[playFileIndex].path
            showLoading()
            videoView.setVideoPath(filePath)
            tv_title.text = operateList[operateIndex].fileList[playFileIndex].name
            mediaPosition = 0
            dc?.editMediaPosition(operateList[operateIndex].name, filePath, mediaPosition)
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
                playFileIndex = operateList[operateIndex].fileList.size - 1

            unPlayView(viewMap[playFileIndex])
            unSelectedView(viewMap[selectedFileIndex])

            val filePath = operateList[operateIndex].fileList[playFileIndex].path
            showLoading()
            videoView.setVideoPath(filePath)
            tv_title.text = operateList[operateIndex].fileList[playFileIndex].name
            mediaPosition = 0
            dc?.editMediaPosition(operateList[operateIndex].name, filePath, mediaPosition)
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaForward() {
        try {
            mediaPosition = videoView.currentPosition + videoView.duration / 100
            videoView.seekTo(mediaPosition)
            setProgress()
            dc?.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[playFileIndex].path, mediaPosition)
            showLoading()
        } catch (e: Exception) {
            log(_Utils.getExceptionStr(e))
        }
    }

    fun mediaRewind() {
        try {
            mediaPosition = videoView.currentPosition - videoView.duration / 100
            videoView.seekTo(mediaPosition)
            setProgress()
            dc?.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[playFileIndex].path, mediaPosition)
            showLoading()
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
        dc?.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[playFileIndex].path, mediaPosition)
        stopTimerTask()
        showLog()
    }

    fun startTimerTask() {
        if (task == null) {
            task = object : TimerTask() {
                override fun run() {
                    try {
                        if (videoView.isPlaying && pb_loading.visibility != View.VISIBLE) {
                            mediaPosition = videoView.currentPosition
                            dc?.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[playFileIndex].path, mediaPosition)
                        }

                        runOnUiThread {
                            hideActionBar()
                            setProgress()
                            tv_time.text = DateTime().toShortTimeString()
                        }
                        if (tv_loading.visibility == View.VISIBLE) {
                            runOnUiThread {
                                tv_loading.text = _Utils.getNetSpeed()
                            }
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
        videoView.pause()
        stopTimer()
        unregisterReceiver(receiver);
        super.onDestroy()
    }

    inner class ListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return operateList[operateIndex].fileList.size
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0L
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var view = viewMap[position]
            val buddha = operateList[operateIndex].fileList[position]
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

        override fun notifyDataSetChanged() {
            viewMap.clear()
            super.notifyDataSetChanged()
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                e("--------------数字键0--------------")
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (isOperateShow()) {
                    e("选中操作")
                    operateIndex = selectedOperateIndex
                    dc?.editSetting(Setting.KEYS.operate_index, operateIndex)
                    refreshOperate()
                    adapter.notifyDataSetChanged()

                    mediaPosition = 0
                    playFileIndex = 0
                    selectedFileIndex = playFileIndex

                    val mp = dc?.getMediaPosition(operateList[operateIndex].name)
                    if (mp != null) {
                        var filePathDB = mp.filePath
                        var mediaNameDB = File(filePathDB).name
                        var positionDB = mp.position

                        var fileList = operateList[operateIndex].fileList
                        for (ii in fileList.indices) {
                            val fileName = File(fileList[ii].path).name
                            if (fileName == mediaNameDB) {
                                playFileIndex = ii
                                selectedFileIndex = playFileIndex
                                mediaPosition = positionDB
                            }
                        }
                    } else {
                        dc?.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[0].path, 0)
                    }
                    var filePath = operateList[operateIndex].fileList[playFileIndex].path
                    showLoading()
                    videoView.setVideoPath(filePath)

                    lv_list.setSelection(selectedFileIndex)
                    tv_title.text = operateList[operateIndex].fileList[playFileIndex].name

                    hideOperate()
                    return true
                }
                if (isListShow()) {
                    dc?.let { dc ->
                        log("--------------列表确认键--------------")
                        unPlayView(viewMap[playFileIndex])
                        playFileIndex = selectedFileIndex
                        playView(viewMap[playFileIndex])
                        hideList()


                        var filePathDB = ""
                        val mp = dc?.getMediaPosition(operateList[operateIndex].name)
                        if (mp != null) {
                            filePathDB = mp.filePath
                        }
                        val filePath = operateList[operateIndex].fileList[playFileIndex].path
                        if (filePath != filePathDB) {
                            showLoading()
                            videoView.setVideoPath(filePath)
                            tv_title.text = operateList[operateIndex].fileList[playFileIndex].name
                            mediaPosition = 0
                            dc.editMediaPosition(operateList[operateIndex].name, operateList[operateIndex].fileList[playFileIndex].path, mediaPosition)
                        }
                    }
                    return true
                }

                log("--------------播放暂停键--------------")
                if (videoView.isPlaying) {
                    mediaPause()
                } else {
                    mediaStart()
                }

            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isListShow()) {
//                    log("--------------上一个--------------")
                    unSelectedView(viewMap[selectedFileIndex])
                    selectedFileIndex--
                    if (selectedFileIndex <= -1)
                        selectedFileIndex = operateList[operateIndex].fileList.size - 1

                    lv_list.setSelection(selectedFileIndex)
                    return true
                }
                if (!isOperateShow()) {
                    showOperate()
                } else {
                    hideOperate()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isOperateShow()) {
                    hideOperate()
                    return true
                }
                if (isListShow()) {
//                    log("--------------下一个--------------")
                    unSelectedView(viewMap[selectedFileIndex])
                    selectedFileIndex++
                    if (selectedFileIndex >= operateList[operateIndex].fileList.size)
                        selectedFileIndex = 0

                    lv_list.setSelection(selectedFileIndex)
                    return true
                }
                showList()
                return true

            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
//                e("左方向键")
                if (isOperateShow()) {
                    selectedOperateIndex--
                    if (selectedOperateIndex <= -1)
                        selectedOperateIndex = operateList.size - 1
                    refreshOperate()
                    return true
                }
                if (isListShow()) {
                    hideList()
                    return true

                }
                mediaRewind()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
//                e("右方向键")
                if (isOperateShow()) {
                    selectedOperateIndex++
                    if (selectedOperateIndex >= operateList.size)
                        selectedOperateIndex = 0
                    refreshOperate()
                    return true
                }
                if (isListShow()) {
                    hideList()
                    return true
                }
                mediaForward()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                e("返回键")
                if (errorCount > 0) {
                    this.finish()
                }
                if (isOperateShow()) {
                    hideOperate()
                    return true
                }
                if (isListShow()) {
                    if (tv_log.text.toString().length > 100) {
                        tv_log.text = ""
                    } else {
                        hideList()
                    }
                    return true
                }
                if (!videoView.isPlaying) {
                    val now = System.currentTimeMillis()
                    if (pb_loading.visibility == View.VISIBLE) {
                        if (now - prvBackTime < 1000) {
                            this.finish()
                        }
                    } else if (tv_log.visibility == View.VISIBLE) {
                        e("3")
                        mediaStart()
                    }
                    prvBackTime = now
                    return true
                }
            }

            KeyEvent.KEYCODE_HOME -> {
            }


            KeyEvent.KEYCODE_MENU -> {
                if (isOperateShow()) {
                    hideOperate()
                } else {
                    showOperate()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    var prvBackTime = 0L

    @Throws(Exception::class)
    fun durationToTimeString(duration: Int): String {
        val second = duration % 60000 / 1000
        val miniteT = duration / 60000
        val minite = miniteT % 60
        val hour = miniteT / 60
        return "$hour:${if (minite < 10) "0" + minite else minite}:${if (second < 10) "0" + second else second}"
    }
}