package com.wangsc.buddhatv.util

import android.util.Log
import com.wangsc.buddhatv.callback.HttpCallback
import com.wangsc.buddhatv.model.PostArgument
import com.wangsc.buddhatv.model.SSLSocketFactoryCompat
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLSocketFactory


/**
 * @Description
 * @ClassName OkHttpClientUtil
 * @Author
 * @Copyright
 */
object _OkHttpUtil {

    @JvmField
    var client: OkHttpClient
    init {
//        client = getOkHttpClient()
        client = OkHttpClient()
    }

    @Throws(Throwable::class)
    fun getOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        try {
            val factory: SSLSocketFactory = SSLSocketFactoryCompat()
            builder.sslSocketFactory(factory)
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return builder.build()
    }

    @JvmStatic
    fun postRequestByJson(url: String?, args: List<PostArgument>, callback: HttpCallback) {
        try {
            //创建OkHttpClient对象。
            val client = client
            //创建表单请求体
            val JSON = MediaType.parse("application/json; charset=utf-8")
            val json = JSONObject()
            for (arg in args) {
                json.put(arg.name, arg.value)
            }
            val requestBody: RequestBody = RequestBody.create(JSON, json.toString())
            val request = Request.Builder().url(url!!)
                    .post(requestBody) //传递请求体
                    .build()

            //new call
            val call = client!!.newCall(request)
            //请求加入调度
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        //回调的方法执行在子线程。
                        val htmlStr = response.body()!!.string()
                        callback.excute(htmlStr)
                    }
                }
            })
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun postRequest(url: String?, args: List<PostArgument>, callback: HttpCallback) {
        //创建OkHttpClient对象。
        val client = client
        //创建表单请求体
        val formBody = FormBody.Builder()
        //创建Request 对象。
        for (arg in args) {
            Log.e("wangsc", "name : " + arg.name + " , value : " + arg.value)
            formBody.add(arg.name, arg.value)
        }
        val request = Request.Builder().url(url!!)
                .post(formBody.build()) //传递请求体
                .build()

        //new call
        val call = client!!.newCall(request)
        //请求加入调度
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //回调的方法执行在子线程。
                    val htmlStr = response.body()!!.string()
                    callback.excute(htmlStr)
                }
            }
        })
    }

    @JvmStatic
    @Throws(Throwable::class)
    fun getRequest(url: String?, callback: HttpCallback) {
        //创建okHttpClient对象
        val mOkHttpClient = client

        //创建一个Request
        val request = Request.Builder()
            .url(url!!)
            .build()
        //new call
        val call = mOkHttpClient!!.newCall(request)
        //请求加入调度
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.excute("xxxxxxxxxxx  ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //回调的方法执行在子线程。
                    val htmlStr = response.body()!!.string()
                    callback.excute(htmlStr)
                }else{
                    callback.excute("xxxxxxxxxxx  response is not Successful......")
                }
            }
        })
    }
}