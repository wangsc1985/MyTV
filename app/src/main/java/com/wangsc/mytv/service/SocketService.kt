package com.wangsc.mytv.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.os.PowerManager
import com.wangsc.mytv.util._Utils
import com.wangsc.mytv.util._Utils.e
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket


class SocketService : Service() {

    private lateinit var serverSocket: ServerSocket
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(0, null)
        startSocket()

        wakeLock = _Utils.acquireWakeLock(applicationContext,PowerManager.PARTIAL_WAKE_LOCK)!!

        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent): IBinder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        _Utils.releaseWakeLock(applicationContext,wakeLock)
    }
    fun startSocket() {
        Thread {
            try {
                serverSocket = ServerSocket(8123);
                while (true) {
                    var socket = serverSocket.accept();
                    var dis = DataInputStream(socket.getInputStream())
                    val dd = dis.readInt()
                    when (dd) {
                        0 -> {
                            val dos = DataOutputStream(socket.getOutputStream())
                            dos.writeBoolean(_Utils.isScreenOn(applicationContext))
                            dos.flush()
                            dos.close()
                        }
                        1 -> {
                            e("接收到屏幕信号")
                            val isOn = _Utils.isScreenOn(applicationContext)
                            val dos = DataOutputStream(socket.getOutputStream())
                            dos.writeBoolean(!isOn)
                            dos.flush()
                            dos.close()
                            if (isOn) {
                                _Utils.closeScreen(applicationContext)
                            } else {
                                _Utils.wakeScreen(applicationContext)
                            }
                        }
                        2 -> {
                            // 音量加
                            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            am.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                        3 -> {
                            // 音量减
                            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            am.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                        4 -> {
                            val manager = applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                            val currentClassName =
                                manager.getRunningTasks(1)[0].topActivity!!.packageName
                            val dos = DataOutputStream(socket.getOutputStream())
                            dos.writeBoolean(currentClassName == "com.wangsc.mytv")
                            dos.flush()
                            dos.close()
                        }
                        5->{

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
}
