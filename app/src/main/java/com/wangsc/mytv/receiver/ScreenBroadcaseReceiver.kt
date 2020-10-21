package com.wangsc.mytv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wangsc.mytv.service.SocketService

class ScreenBroadcaseReceiver : BroadcastReceiver() {
    private fun e(msg: Any) {
        Log.e("wangsc", msg.toString())
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> try {
                val now = System.currentTimeMillis()
                if (now - preDateTime >= 10000) {
                    try {
                        e("ACTION_USER_PRESENT")
                        context.startService(Intent(context,SocketService::class.java))
                    } finally {
                        preDateTime = now
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private var preDateTime: Long = 0
    }
}