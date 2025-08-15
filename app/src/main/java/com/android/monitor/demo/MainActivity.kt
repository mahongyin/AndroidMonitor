package com.android.monitor.demo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.monitor.demo.databinding.ActivityMainBinding
import com.lygttpod.monitor.MonitorHelper
import com.lygttpod.monitor.utils.getPhoneWifiIpAddress
import okhttp3.*
import okio.IOException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private var client = OkHttpClient()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getServiceAddress()

        // 手动加入拦截器
//        val builder = client.newBuilder()
//        builder.interceptors().addAll(MonitorHelper.hookInterceptors)
//        client = builder.build()

        initData()
        binding.btnSendRequest.setOnClickListener {
            sendRequest("https://www.wanandroid.com/article/list/0/json")
        }
    }

    private fun initData() {
        sendRequest("https://www.wanandroid.com/banner/json")
        spFileCommit()
        thread(true){
            getRequest("https://www.wanandroid.com/article/list/0/json")
        }
    }
    private fun getRequest(requestUrl: String) {
        var isSuccess = false
        var message: String

        var inputStream: InputStream? = null
        var baos: ByteArrayOutputStream? = null
        try {
            val url = URL(requestUrl)
            val connection = url.openConnection() as HttpURLConnection
            // 设定请求的方法为"POST"，默认是GET
            connection.setRequestMethod("GET")
            connection.setConnectTimeout(50000)
            connection.setReadTimeout(50000)
            // User-Agent  IE9的标识
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0;"
            )
            connection.setRequestProperty("Accept-Language", "zh-CN")
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Charset", "UTF-8")
            //当我们要获取我们请求的http地址访问的数据时就是使用connection.getInputStream().read()方式时我们就需要setDoInput(true)，
            //根据api文档我们可知doInput默认就是为true。我们可以不用手动设置了，如果不需要读取输入流的话那就setDoInput(false)。
            //当我们要采用非get请求给一个http网络地址传参 就是使用connection.getOutputStream().write() 方法时我们就需要setDoOutput(true), 默认是false
            // 设置是否从httpUrlConnection读入，默认情况下是true;
            connection.setDoInput(true)
            // 设置是否向httpUrlConnection输出，如果是post请求，参数要放在http正文内，因此需要设为true, 默认是false;
            //connection.setDoOutput(true);//Android  4.0 GET时候 用这句会变成POST  报错java.io.FileNotFoundException
            connection.setUseCaches(false)
            connection.connect() //
            val contentLength = connection.getContentLength()
            if (connection.getResponseCode() == 200) {
                inputStream = connection.getInputStream() //会隐式调用connect()
                baos = ByteArrayOutputStream()
                var readLen: Int
                val bytes = ByteArray(1024)
                while ((inputStream.read(bytes).also { readLen = it }) != -1) {
                    baos.write(bytes, 0, readLen)
                }
                val result = baos.toString()
                Log.i("请求", " result:" + result)

                message = result
                isSuccess = true
            } else {
                message = "请求失败 code:" + connection.getResponseCode()
            }
        } catch (e: MalformedURLException) {
            message = "${e.message}"
            e.printStackTrace()
        } catch (e: java.io.IOException) {
            message = "${e.message}"
            e.printStackTrace()
        } finally {
            try {
                if (baos != null) {
                    baos.close()
                }
                if (inputStream != null) {
                    inputStream.close()
                }
            } catch (e: java.io.IOException) {
                message = "${e.message}"
                e.printStackTrace()
            }
        }
        if (isSuccess) {
            Log.i("请求s",message)
        } else {
            Log.e("请求e",message)
        }
    }
    private fun sendRequest(url: String) {
        val request = Request.Builder().url(url).build();
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun getServiceAddress() {
        getPhoneWifiIpAddress()?.let {
            val monitorUrl = "$it:${MonitorHelper.port}/index"
            binding.tvAddress.text = monitorUrl
        }
    }

    private fun spFileCommit() {
        getSharedPreferences("spFileName111", Context.MODE_PRIVATE).edit().also {
            it.putString("testString", "我是字符串")
            it.putInt("testInt", 111)
            it.putBoolean("testBoolean", true)
            it.putFloat("testFloat", 222f)
            it.putLong("testLong", System.currentTimeMillis())
        }.apply()
        getSharedPreferences("spFileName222", Context.MODE_PRIVATE).edit().also {
            it.putString("keyString", "我是字符串")
            it.putInt("keyInt", 111)
            it.putBoolean("keyBoolean", true)
            it.putFloat("keyFloat", 222f)
            it.putLong("keyLong", System.currentTimeMillis())
        }.apply()
    }
}