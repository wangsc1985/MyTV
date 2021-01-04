package com.wangsc.mytv.util

import android.content.Context
import android.util.Log
import com.wangsc.mytv.callback.CloudCallback
import com.wangsc.mytv.callback.HttpCallback
import com.wangsc.mytv.model.DataContext
import com.wangsc.mytv.model.DateTime
import com.wangsc.mytv.model.PostArgument
import com.wangsc.mytv.model.Setting
import com.wangsc.mytv.util._OkHttpUtil.getRequest
import com.wangsc.mytv.util._OkHttpUtil.postRequestByJson
import org.json.JSONArray
import java.util.*
import java.util.concurrent.CountDownLatch

object _CloudUtils {

    private val env = "yipinshangdu-4wk7z"
    private val appid = "wxbdf065bdeba96196"
    private val secret = "d2834f10c0d81728e73a4fe4012c0a5d"

    @JvmStatic
    fun getToken(context: Context): String {
        val dc = DataContext(context)
        val setting = dc.getSetting("token_exprires")
        if (setting != null) {
            val exprires = setting.long
            if (System.currentTimeMillis() > exprires) {
                /**
                 * token过期
                 */
                e("本地token已过期，微软网站获取新的token。")
                return loadNewTokenFromHttp((context))
            } else {
                /**
                 * token仍有效
                 */
                e(dc.getSetting("token")!!.string)
                e("有效期：${DateTime(exprires).toLongDateTimeString()}")
                return dc.getSetting("token")!!.string
            }
        } else {
            e("本地不存在token信息，微软网站获取新的token。")
            return loadNewTokenFromHttp(context)
        }
    }

    private fun loadNewTokenFromHttp(context: Context): String {
        var token = ""
        // https://sahacloudmanager.azurewebsites.net/home/token/wxbdf065bdeba96196/d2834f10c0d81728e73a4fe4012c0a5d
        val a = System.currentTimeMillis()
        val latch = CountDownLatch(1)
        getRequest("https://sahacloudmanager.azurewebsites.net/home/token/${appid}/${secret}", HttpCallback { html ->
            try {
                e(html)
                val data = html.split(":")
                if (data.size == 2) {
                    token = data[0]
                    e(data[1].toDouble())
                    e(data[1].toDouble().toLong())
                    val exprires = data[1].toDouble().toLong()

                    // 将新获取的token及exprires存入本地数据库
                    val dc = DataContext(context)
                    dc.editSetting("token", token)
                    dc.editSetting("token_exprires", exprires)


                    val b = System.currentTimeMillis()
                    e("从微软获取到token：$token, 有效期：${DateTime(exprires).toLongDateTimeString()} 用时：${b - a}")
                }
            } catch (e: java.lang.Exception) {
                e(e.message!!)
            } finally {
                latch.countDown()
            }
        })
        latch.await()
        return token
    }


    @JvmStatic
    fun saveSetting(context: Context, pwd: String?, name: String?, value: Any, callback: CloudCallback?) {

        val accessToken = getToken(context)
        e("access token : $accessToken")
        val url = "https://api.weixin.qq.com/tcb/invokecloudfunction?access_token=$accessToken&env=$env&name=saveSetting"
        val args: MutableList<PostArgument> = ArrayList()
        args.add(PostArgument("pwd", pwd))
        args.add(PostArgument("name", name))
        args.add(PostArgument("value", value.toString()))
        postRequestByJson(url, args, HttpCallback { html ->
            try {
                e(html)
                val resp_data: Any = _JsonUtils.getValueByKey(html, "resp_data")
                if (_JsonUtils.isContainsKey(resp_data, "success")) {
                    val code = _JsonUtils.getValueByKey(resp_data.toString(), "code").toInt()
                    when (code) {
                        0 -> callback?.excute(0, "修改完毕")
                        1 -> callback?.excute(1, "添加成功")
                    }
                } else if (_JsonUtils.isContainsKey(resp_data, "msg")) {
                    callback?.excute(-1, "访问码错误")
                }
            } catch (e: Exception) {
                callback?.excute(-2, e.message)
            }
        })
    }

}