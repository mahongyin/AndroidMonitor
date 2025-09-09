package com.android.monitor.demo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.monitor.demo.databinding.ActivityMainBinding
import okhttp3.*
import okio.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getServiceAddress()
        initData()
        binding.btnSendRequest.setOnClickListener {
            sendRequest("https://www.wanandroid.com/article/list/0/json")
        }
    }

    private fun initData() {
        sendRequest("https://www.wanandroid.com/banner/json")
        spFileCommit()
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
            val monitorUrl = "$it:${9527}/index"
            binding.tvAddress.text = monitorUrl
        }
    }
    fun getPhoneWifiIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e("MonitorPCService", "get ip", e)
            return null
        }
        return null
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