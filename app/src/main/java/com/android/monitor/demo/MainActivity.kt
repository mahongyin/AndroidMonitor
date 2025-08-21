package com.android.monitor.demo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.android.monitor.demo.databinding.ActivityMainBinding
import com.lygttpod.monitor.MonitorHelper
import com.lygttpod.monitor.utils.getPhoneWifiIpAddress
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.net.InetAddress
import java.util.Arrays
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private var client = OkHttpClient()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getServiceAddress()
        spFileCommit()


//        val builder = client.newBuilder()
        // 手动注入okhttp拦截器
        //builder.interceptors().addAll(MonitorHelper.hookInterceptors)
//        client = builder.build()

        initData()

        Executors.newCachedThreadPool().execute {
            val ips = Dns.SYSTEM.lookup("www.baidu.com") //InetAddress.getAllByName("www.baidu.com")
            Log.d("host:www.baidu.com", "ips: $ips")
        }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
            return
        }
        super.onBackPressed()
        finish()
    }

    private fun initData() {
        binding.tvResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        val callback = object : Test.HttpCallback {
            override fun onSuccess(result: String?) {
                runOnUiThread {
                    binding.tvResult.scrollY = 0
                    binding.tvResult.text = result
                }
            }

            override fun onFailure(errorMsg: String?) {
                runOnUiThread {
                    binding.tvResult.scrollY = 0
                    binding.tvResult.text = errorMsg ?: "请求失败"
                }
            }

        }
        binding.btnSendGet.setOnClickListener {
            Executors.newCachedThreadPool().execute {
                Test.getRequest("https://www.wanandroid.com/article/list/0/json", null, callback)
            }
        }
        binding.btnSendPost.setOnClickListener {
            Executors.newCachedThreadPool().execute {
                val map = hashMapOf("name" to "name", "token" to "123456789")
                Test.postRequest(
                    "https://www.wanandroid.com/lg/uncollect_originId/2333/json", map, callback
                )
            }
        }
        binding.btnSendOkhttp.setOnClickListener {
            sendRequest("https://www.wanandroid.com/banner/json")
        }
        webview()
        binding.webview.clearHistory()
        binding.webview.clearCache(true)
        binding.webview.clearFormData()
        binding.webview.clearMatches()
        binding.webview.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            displayZoomControls = false
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            blockNetworkImage = false
            setGeolocationEnabled(true)
            databaseEnabled = true
            setSupportZoom(false)
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // 1. 创建临时WebView用于捕获URL
                val result = view?.hitTestResult
                val url = result?.extra ?: "" // 获取点击链接的URL
                Log.d("shouldLoading", "_blank: $url")
                if (url.startsWith("http", true)) {
                    view?.loadUrl(url)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    if (packageManager.resolveActivity(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        ) != null
                    ) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "不支持", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        }
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    view?.evaluateJavascript(MonitorHelper.injectVConsole(), null)
                } else {
                    view?.loadUrl(MonitorHelper.injectVConsole())
                }
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()//忽略证书错误
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d("shouldLoading", "request: $url")
                if (url?.startsWith("http", true) == true) {
                    view?.loadUrl(url)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, url?.toUri())
                    if (packageManager.resolveActivity(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        ) != null
                    ) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "不支持", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return this.shouldOverrideUrlLoading(view, request?.url?.toString())
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                // 可以拦截请求，返回自定义的response，但不推荐
                val response = MonitorHelper.shouldInterceptRequest(view, request)
                if (response != null) {
                    return response
                }
                return super.shouldInterceptRequest(view, request)
            }

//            override fun shouldInterceptRequest(
//                view: WebView?,
//                url: String?
//            ): WebResourceResponse? {
//                val response = MonitorHelper.shouldInterceptRequest(view, url)
//                if (response != null){
//                    return response
//                }
//                return super.shouldInterceptRequest(view, url)
//            }
        }
        binding.webview.loadUrl("https://juejin.cn/")
    }

    private fun webview() {

    }

    private fun sendRequest(url: String) {
        val request = Request.Builder().url(url).build();
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.tvResult.scrollY = 0
                    binding.tvResult.text = e.message ?: "请求失败"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                runOnUiThread {
                    binding.tvResult.scrollY = 0
                    binding.tvResult.text = result
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