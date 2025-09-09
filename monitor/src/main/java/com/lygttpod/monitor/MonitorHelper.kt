package com.lygttpod.monitor

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.android.local.service.core.ALSHelper
import com.android.local.service.core.data.ServiceConfig
import com.google.gson.Gson
import com.lygttpod.monitor.data.MonitorData
import com.lygttpod.monitor.data.SpValueInfo
import com.lygttpod.monitor.enum.SPValueType
import com.lygttpod.monitor.interceptor.MonitorInterceptor
import com.lygttpod.monitor.interceptor.MonitorMockInterceptor
import com.lygttpod.monitor.interceptor.MonitorMockResponseInterceptor
import com.lygttpod.monitor.interceptor.MonitorWeakNetworkInterceptor
import com.lygttpod.monitor.room.MonitorDao
import com.lygttpod.monitor.room.MonitorDatabase
import com.lygttpod.monitor.service.MonitorService
import com.lygttpod.monitor.streamhandler.OkHttpURLStreamHandler
import com.lygttpod.monitor.utils.MonitorProperties
import com.lygttpod.monitor.utils.OkhttpUtils
import com.lygttpod.monitor.utils.SPUtils
import com.lygttpod.monitor.utils.defaultContentTypes
import com.lygttpod.monitor.utils.lastUpdateDataId
import java.io.File
import java.net.URL
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.concurrent.thread


@SuppressLint("StaticFieldLeak")
object MonitorHelper {

    const val TAG = "MonitorHelper"

    var context: Context? = null
    var monitorDb: MonitorDatabase? = null

    private var port = 0

    //有从来ASM修改字节码对OKHTTP进行hook用的
    val hookInterceptors = listOf(
        MonitorMockInterceptor(),
        MonitorInterceptor(),
        MonitorWeakNetworkInterceptor(),
        MonitorMockResponseInterceptor()
    )

    var whiteContentTypes: String? = null
    var whiteHosts: String? = null
    var blackHosts: String? = null
    var isFilterIPAddressHost: Boolean = false

    var isOpenMonitor = true

    private var singleThreadExecutor: ExecutorService? = null

    private fun threadExecutor(action: () -> Unit) {
        if (singleThreadExecutor == null || singleThreadExecutor?.isShutdown == true) {
            singleThreadExecutor = Executors.newSingleThreadExecutor()
        }
        singleThreadExecutor?.execute(action)
    }

    /**
     * 配置参数 他是采用了 主项目assets目录下新建 monitor.properties 文件的方式
     */
    fun init(context: Context) {
        MonitorHelper.context = context
        thread {
            // 获取相关配置
            val propertiesData = MonitorProperties().paramsProperties()
            val dbName: String = propertiesData?.dbName ?: "monitor_room_db"
            val contentTypes = propertiesData?.whiteContentTypes
            whiteContentTypes = if (contentTypes.isNullOrBlank()) defaultContentTypes else contentTypes
            whiteHosts = propertiesData?.whiteHosts
            blackHosts = propertiesData?.blackHosts
            port = propertiesData?.port?.toInt() ?: 0
            isFilterIPAddressHost = propertiesData?.isFilterIPAddressHost ?: false
            initMonitorDataDao(context, dbName)
            initPCService(context, port)
            OkhttpUtils.initUserAgent(context)
        }
    }

    private fun initPCService(context: Context, port: Int = 0) {
        ALSHelper.init(context)
        ALSHelper.startService(
            if (port > 0)
                ServiceConfig(MonitorService::class.java, port)
            else ServiceConfig(MonitorService::class.java)
        )
        // 不管是公配还是(优先)单配 获取正使用的端口
        MonitorHelper.port = ALSHelper.getServiceList().firstOrNull()?.port ?: 0
    }
    fun getPort(): Int {
        return port
    }
    private fun initMonitorDataDao(context: Context, dbName: String) {
        if (monitorDb == null) {
            monitorDb = Room
                .databaseBuilder(context.applicationContext, MonitorDatabase::class.java, dbName)
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    private fun getMonitorDataDao(): MonitorDao? {
        return monitorDb?.monitorDao()
    }

    fun insert(monitorData: MonitorData) {
        lastUpdateDataId = monitorData.id
        getMonitorDataDao()?.insert(monitorData)
    }

    fun insertAsync(map: Map<String, Any>?) {
        if (map == null || map.isEmpty()) return
        threadExecutor {
            try {
                val monitor = Gson().fromJson(Gson().toJson(map), MonitorData::class.java)
                insert(monitor)
            } catch (e: Exception) {
                Log.d(TAG, "insertAsync--${e.message}")
            }
        }
    }

    fun update(monitorData: MonitorData) {
        getMonitorDataDao()?.update(monitorData)
    }

    fun deleteAll() {
        lastUpdateDataId = 0L
        getMonitorDataDao()?.deleteAll()
    }

    fun getMonitorDataListForAndroid(
        limit: Int = 50,
        offset: Int = 0
    ): LiveData<MutableList<MonitorData>>? {
        return getMonitorDataDao()?.queryByOffsetForAndroid(limit, offset)
    }

    fun getMonitorDataList(limit: Int = 50, offset: Int = 0): MutableList<MonitorData> {
        return getMonitorDataDao()?.queryByOffset(limit, offset) ?: mutableListOf()
    }

    fun getMonitorDataByLastIdForAndroid(lastUpdateDataId: Long): LiveData<MutableList<MonitorData>>? {
        return getMonitorDataDao()?.queryByLastIdForAndroid(lastUpdateDataId)
    }

    fun getMonitorDataByLastId(lastUpdateDataId: Long): MutableList<MonitorData> {
        return getMonitorDataDao()?.queryByLastId(lastUpdateDataId) ?: mutableListOf()
    }

    fun getSharedPrefsFilesData(): HashMap<String, HashMap<String, SpValueInfo?>> {
        val ctx = context ?: return hashMapOf()
        val map = hashMapOf<String, HashMap<String, SpValueInfo?>>()
        val targetFile = File("${ctx.cacheDir.parentFile?.absolutePath}/shared_prefs")
        if (!targetFile.exists()) return hashMapOf()
        if (targetFile.isDirectory) {
            targetFile.listFiles()?.forEach { spFile ->
                Log.d(TAG, "getSharedPrefsFiles: " + spFile.name)
                val fileName = spFile.name
                if (!fileName.isNullOrBlank()) {
                    val name =
                        if (fileName.endsWith(".xml")) fileName.split(".xml")[0] else fileName
                    val value = getSpFile(name)
                    map[name] = value
                }
            }
        }
        return map
    }

    fun getSpFile(name: String?): HashMap<String, SpValueInfo?> {
        if (name.isNullOrBlank()) return hashMapOf()
        val map = hashMapOf<String, SpValueInfo?>()
        context?.getSharedPreferences(name, Context.MODE_PRIVATE)?.all?.entries?.forEach {
            val valueType = when (it.value) {
                is Int -> SPValueType.Int
                is Double -> SPValueType.Double
                is Float -> SPValueType.Float
                is Long -> SPValueType.Long
                is Boolean -> SPValueType.Boolean
                is String -> SPValueType.String
                else -> SPValueType.String
            }
            val key = it.key
            if (!key.isNullOrBlank()) {
                map[key] = SpValueInfo(it.value, valueType)
            }
        }
        return map
    }

    fun updateSpValue(fileName: String, key: String, value: Any?) {
        SPUtils.saveValue(context ?: return, fileName, key, value)
    }

    /**
     * 用进程pid当端口号。进程id 32位整数（4 字节） 通常范围通常从 1-32768（系统相关）由操作系统分配和管理
     * 端口范围 2字节 系统端口(0-1023)、注册端口(1024-49151)和动态端口(49152-65535)
     */
    fun getMyPid(): String {
        val myPid = android.os.Process.myPid().toString()
        if (myPid.isEmpty()) {
            // 需要权限: <uses-permission android:name="android.permission.GET_TASKS" />
            val activityManager =
                context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val processes = activityManager?.runningAppProcesses
            if (processes != null) {
                for (processInfo in processes) {
                    val processName = processInfo.processName
                    val pid = processInfo.pid
                    if (context?.packageName == processName) {
                        Log.d(TAG, "Process Name: $processName, PID: $pid")
                        return "${pid}"
                    }
                }
            }
        }
        Log.d(TAG, "Process PID: $myPid")
        if (TextUtils.isEmpty(myPid)){
            return "0"
        }
        return myPid
    }
    /************************************ WebView  ******************************************/
    /**
     * 做一些过滤
     */
    private fun shouldIntercept(webResourceRequest: WebResourceRequest?): Boolean {
        if (webResourceRequest == null) {
            return false
        }
        //Log.d("shouldIntercept", "url: ${webResourceRequest.url?.toString()}")
        val url = webResourceRequest.url ?: return false
        //非http协议不拦截
        if ("https" != url.scheme && "http" != url.scheme) {
            return false
        }
        //(webResourceRequest.hasGesture()) // 用户主动触发的请求
//        if (!webResourceRequest.isForMainFrame) {
//            //主框架：指整个网页的主要内容框架，即页面的根文档
//            //子框架：包括 iframe、图片、CSS、JavaScript 等子资源
//            return false
//        }
//        //只拦截GET请求, [POST/PUT/OPTIONS 需要body]
        if ("GET".equals(webResourceRequest.method, true)) {
            webResourceRequest.requestHeaders.entries.forEach({
                Log.d(TAG, "WebRequest key: ${it.key} value: ${it.value}")
            })
            val accept = webResourceRequest.requestHeaders["Accept"]
            val whiteContentTypes = this.whiteContentTypes
            if (whiteContentTypes.isNullOrBlank() ||
                (!accept.isNullOrEmpty() && whiteContentTypes.split(",")
                    .any { accept.contains(it, true) })
            ) {
                return true
            }
            val lowerUrl = url.path?.lowercase()
            val index = lowerUrl?.lastIndexOf("/")
            if (index != null && index > 0) {
                val extName = lowerUrl.substring(index + 1)
                //Log.d("shouldIntercept", "extName: $extName  $url")
                if (extName.isNotBlank()) {
                    val isResource = extensions.any { extName.contains(it) }
                    //Log.d("shouldIntercept", "isResource: $isResource")
                    if (!isResource) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (shouldIntercept(request)) {
            return OkhttpUtils.getResponseByOkHttp(request)
        }
        return null
    }

    //no log txt json xml
    private val extensions = arrayOf(
        ".css", ".js", ".jpg", ".jpeg", ".image", ".png", ".gif", ".ico", ".icon", ".svg",
        ".woff2", ".ttf", ".eot", ".pdf", ".zip", ".mp4", ".mp3", ".avi", ".mov", ".woff",
        ".webp", ".webm", ".awebp", ".md", ".ttc", ".otf", ".jsp", ".php", ".html", ".htm",
        ".xhtml", ".shtml", ".asp", ".aspx", ".wav", ".ogg", ".wmv",".flv",".m3u8",
        ".mkv", ".webm", ".ts", ".doc", ".docx",".xls", ".xlsx", ".ppt", ".pptx",
        ".rar", ".7z", ".tar", ".gz", ".exe", ".dll", ".so", ".apk"
    )
    fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        //Log.d("shouldIntercept", "url: $url")
        //Patterns.WEB_URL.matcher(url).matches() || URLUtil.isValidUrl(url)
        if (url.isNullOrBlank() || !url.startsWith("http", true)) {
            return null
        }
        val lowerUrl = url.lowercase().toUri().path
        val index = lowerUrl?.lastIndexOf("/")
        if (index != null && index > 0) {
            val extName = lowerUrl.substring(index + 1)
            //Log.d("shouldIntercept", "extName: $extName  $url")
            if (extName.isNotBlank()) {
                val isResource = extensions.any { extName.contains(it) }
                //Log.d("shouldIntercept", "isResource: $isResource")
                if (isResource) {
                    return null
                }
            }
        }
        return OkhttpUtils.getResponseByOkHttp(url)
    }

    fun injectVConsole() :String{
        val console = "https://unpkg.com/vconsole@3.14.6/dist/vconsole.min.js"
        val jsFun =
        "javascript:(function(){ " +
                "if (typeof window.vConsole !== 'undefined' && vConsole instanceof Object) { " +
                " console.log('vConsole已添加');" +
                "} else {" +
                "console.log('vConsole去添加');" +
                "if(document.head && !document.getElementById('v_console')) {" +
                "var injectScript = document.createElement('script');" +
                "injectScript.src='"+console+"';" +
                "injectScript.id='v_console';" +
                "injectScript.type='text/javascript';" +
                "injectScript.onload=function() {" +
                "let vConsole = new VConsole();" +
                "console.log('vConsole实例化成功'); " +
                "};" +
                "document.head.appendChild(injectScript);" +
                "} " +
                "};" +
                "})();"
        return jsFun
    }

    /************************************  URLStreamHandler  ***********************************************/
    //通过动态代理替换 URLStreamHandlerFactory，拦截 URL.openConnection()
    //仅适用于未设置过 URLStreamHandlerFactory 的应用（多数应用未设置）
    fun httpStreamHandler() {
        //注册自定义工厂
        URL.setURLStreamHandlerFactory(object : URLStreamHandlerFactory {
            override fun createURLStreamHandler(protocol: String?): URLStreamHandler? {
                if ("http" == protocol || "https" == protocol) {
                    //URLStreamHandler返回包装后的 HttpsURLConnection，用于拦截数据
                    return OkHttpURLStreamHandler()
                }
                return null // 其他协议使用默认实现
            }
        })
    }

    // HttpsURLConnection全局忽略SSL证书
    fun handleSSLHandShake() {
        try {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate?>?,
                    authType: String?
                ) {
                }

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate?>?,
                    authType: String?
                ) {
                }

                override fun getAcceptedIssuers(): Array<out X509Certificate?>? {
                    return arrayOfNulls<X509Certificate>(0)
                }
            }

            val sc: SSLContext = SSLContext.getInstance("TLS")
            // trustAllCerts信任所有的证书
            sc.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
        } catch (ignored: Exception) {
            Log.e(TAG, "handleSSLHandShake: " + ignored.message)
        }
    }
}