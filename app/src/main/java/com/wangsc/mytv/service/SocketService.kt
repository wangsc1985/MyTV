package com.wangsc.mytv.service

import android.app.ActivityManager
import android.app.Instrumentation
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.os.PowerManager
import android.view.KeyEvent
import com.wangsc.mytv.util._NotificationUtils
import com.wangsc.mytv.util._Utils
import com.wangsc.mytv.util._Utils.e
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.ServerSocket


class SocketService : Service() {

    private lateinit var serverSocket: ServerSocket
    private lateinit var wakeLock: PowerManager.WakeLock


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(0, _NotificationUtils.sendNotification(this))
            startSocket()

            wakeLock = _Utils.acquireWakeLock(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)!!
        } catch (e: Exception) {
            e(e.message)
        }

        return super.onStartCommand(intent, flags, startId)
    }
    override fun onBind(intent: Intent): IBinder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        _Utils.releaseWakeLock(applicationContext, wakeLock)
    }
    fun startSocket() {
        Thread {
            try {
                serverSocket = ServerSocket(8123);
                while (true) {
                    var socket = serverSocket.accept();
                    e("socket accept")
                    var dis = DataInputStream(socket.getInputStream())
                    e("socket get input stream")
                    val dd = dis.readInt()
                    e("read int ${dd}")
                    when (dd) {
                        0 -> {
                            e("获取播放状态")
                            val dos = DataOutputStream(socket.getOutputStream())
                            e("1")
                            dos.writeBoolean(_Utils.isScreenOn(applicationContext))
                            e("2")
                            dos.flush()
                            e("3")
                            dos.close()
                            e("4")
                        }
                        1 -> {
                            e("更改播放状态")
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
                            e("音量加")
                            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            am.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                        3 -> {
                            e("音量减")
                            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            am.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                        4 -> {
                            e("查看播经app是否在栈顶")
                            val manager = applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                            val currentClassName = manager.getRunningTasks(1)[0].topActivity!!.packageName
//                            e("current class name: ${currentClassName}")
                            val dos = DataOutputStream(socket.getOutputStream())
                            e("a")
                            dos.writeBoolean(currentClassName == "com.wangsc.mytv")
                            e("b")
                            dos.flush()
                            e("c")
                            dos.close()
                            e("d")
                        }
                        10 -> {
                            e("返回")
                            try {
                                Runtime.getRuntime().exec("input keyevent ${KeyEvent.KEYCODE_BACK}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        11 -> {
                            e("确定")
                            try {
                                Runtime.getRuntime().exec("input keyevent ${KeyEvent.KEYCODE_DPAD_CENTER}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        21 -> {
                            e("左")
                            try {
                                e("左")
//                                Runtime.getRuntime().exec("input keyevent ${KeyEvent.KEYCODE_DPAD_LEFT}")
                                Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);

                            } catch (e: Exception) {
                                e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx${e.message}")
                                e.printStackTrace()
                            }
                        }
                        22 -> {
                            e("右")
                            try {
                                e("右")
//                                Runtime.getRuntime().exec("input keyevent ${KeyEvent.KEYCODE_DPAD_RIGHT}")

                                Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
                            } catch (e: Exception) {
                                e("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx${e.message}")
                                e.printStackTrace()
                            }
                        }
                        23 -> {
                            e("上")
                            try {
                                Runtime.getRuntime().exec("input keyevent ${KeyEvent.KEYCODE_DPAD_UP}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        24 -> {
                            e("下")
                            try {
                                Runtime.getRuntime().exec("input keyevent ${KeyEvent.KEYCODE_DPAD_DOWN}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    dis.close()
                    socket.close()
                }
            }catch (e:EOFException){
                e.printStackTrace()
                e(e.message)
            }
            catch (e: Exception) {
                e(e.message)
            }
        }.start()
    }
    /**
     *   KeyEvent.KEYCODE_DPAD_UP; 上
     *   KeyEvent.KEYCODE_DPAD_DOWN; 下
     *   KeyEvent.KEYCODE_DPAD_LEFT;左
     *   KeyEvent.KEYCODE_DPAD_RIGHT;右
     *   KeyEvent.KEYCODE_DPAD_CENTER;确定键
     *   KeyEvent.KEYCODE_DPAD_RIGHT; 右
     *   KeyEvent.KEYCODE_XXX:数字键 (xx表示你按了数字几)
     *   KeyEvent.KEYCODE_BACK; 返回键
     *   KeyEvent.KEYCODE_HOME;房子键
     *   KeyEvent.KEYCODE_A: A-Z,26个字母
     *   KeyEvent.KEYCODE_MENU菜单键。
     */
}
