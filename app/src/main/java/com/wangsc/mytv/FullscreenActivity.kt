package com.wangsc.mytv

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.wangsc.mytv._Utils.e


class FullscreenActivity : AppCompatActivity() {
    private val hideHandler = Handler()
    private var index:Int=0

    private val showPart2Runnable = Runnable {
        supportActionBar?.show()
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_fullscreen)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            val fileList = _Utils.getAllLocalVideos(this)
            fileList.forEach {
                e(it.Title)
            }

            isFullscreen = true
            val videoView = findViewById(R.id.videoView) as VideoView
//            val path: String = Environment.getExternalStorageDirectory().getPath().toString() + "/0/myphone/1.MP4"
            videoView.setVideoPath(fileList[index++].FilePath)
            val mediaController = MediaController(this)
            videoView.setMediaController(mediaController)
            videoView.setOnCompletionListener {
                videoView.setVideoPath(fileList[index++].FilePath)
                videoView.start()
            }
            videoView.requestFocus()
            videoView.start()

        } catch (e: Exception) {
            e("xxxxxxxxxxxxxxxxxxxxxxxxxxx${e.message}")
        }

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        supportActionBar?.hide()
        isFullscreen = false

        hideHandler.removeCallbacks(showPart2Runnable)
    }

    private fun show() {
        isFullscreen = true

        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        private const val UI_ANIMATION_DELAY = 300
    }
}