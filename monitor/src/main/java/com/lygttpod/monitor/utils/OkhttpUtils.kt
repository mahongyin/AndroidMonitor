package com.lygttpod.monitor.utils

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import androidx.core.net.toUri
import okhttp3.Authenticator
import okhttp3.Cookie
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * Created By Mahongyin
 * Date    2025/8/17 12:36
 *
 */
object OkhttpUtils {
    const val webUserAgent1 = "Mozilla/5.0 (Phone; OpenHarmony 6.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36  ArkWeb/6.0.0.39 Mobile HuaweiBrowser/5.1.7.304"
    const val webUserAgent2 = "Mozilla/5.0 (Phone; OpenHarmony 5.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36  ArkWeb/4.1.6.1 Mobile HuaweiBrowser/5.1.5.330"
    const val webUserAgent3 = "Mozilla/5.0 (Linux; Android 12; HarmonyOS; LIO-AN00; HMSCore 6.15.0.322) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.196 HuaweiBrowser/16.0.7.301 Mobile Safari/537.36"
    const val webUserAgent4 = "Mozilla/5.0 (Linux; Android 12; LIO-AN00 Build/HUAWEILIO-AN00; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/114.0.5735.196 Mobile Safari/537.36"
    const val webUserAgent = "Mozilla/5.0 (Linux; U; Android 12; zh-cn; M2002J9E Build/SKQ1.220303.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36 XiaoMi/MiuiBrowser/20.0.20728"
    const val webUserAgent5 = "Mozilla/5.0 (Linux; U; Android 4.4.4; zh-cn; M351 Build/KTU84P) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
    const val webUserAgent6 = "Mozilla/5.0 (Linux; Android 4.2.1; M040 Build/JOP40D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.59 Mobile Safari/537.36"
    const val webUserAgent7 = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12F70 Safari/600.1.4"
    const val webUserAgent8 = "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0_4 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) CriOS/31.0.1650.18 Mobile/11B554a Safari/8536.25"

    /************************************* 创建 okhttpClient ***************************************/
    // 创建忽略证书的OkHttpClient
    fun createOkhttpClient(
        mProxy: Proxy? = null,
        proxySelector: ProxySelector? = null,
        proxyAuth: Authenticator? = null,
        httpDns: Dns? = null
    ): OkHttpClient {
        @SuppressLint("CustomX509TrustManager")
        val trustManager: X509TrustManager = object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(
                chain: Array<X509Certificate?>?,
                authType: String?
            ) {
            }

            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(
                chain: Array<X509Certificate?>?,
                authType: String?
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls<X509Certificate>(0)
            }
        }
        try {
            val sc = SSLContext.getInstance("TLS")
            // trustAllCerts信任所有的证书
            sc.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            val builder = OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .sslSocketFactory(sc.getSocketFactory(), trustManager)
                .hostnameVerifier(HostnameVerifier { hostname: String?, session: SSLSession? ->
//                    return@HostnameVerifier HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                    return@HostnameVerifier true
                })
                .followRedirects(true)  // 处理重定向默认为 true
                .followSslRedirects(true)  // 处理重定向默认为 true

            builder.cookieJar(object : okhttp3.CookieJar {
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val cookies = mutableListOf<Cookie>()
                    val cookieStr = CookieManager.getInstance().getCookie(url.toString())
                    if (TextUtils.isEmpty(cookieStr)) {
                        return cookies
                    }
                    cookieStr.split(";").forEach {
                        Cookie.parse(url, it.trim())?.apply {
                            cookies.add(this)
                        }
                    }
                    return cookies
                }

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookies.forEach {
                        CookieManager.getInstance()
                            .setCookie(url.toString(), "${it.name}=${it.value}")
                    }
                    CookieManager.getInstance().flush()
                }
            })
            if (httpDns != null) {
                builder.dns(httpDns)
            }
            if (mProxy != null) {
                builder.proxy(mProxy)
            }
            if (proxySelector != null) {
                builder.proxySelector(proxySelector)
            }
            //builder.authenticator()
            if (proxyAuth != null) {
                builder.authenticator(proxyAuth)
            }
            return builder.build()
        } catch (e: java.lang.Exception) {
            return OkHttpClient()
        }
    }

    private fun proxySelector(): ProxySelector {
        // 自定义代理选择器
        val proxySelector = object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy?>? {
                if (uri?.host?.contains("example.com") == true) {
                    // 为特定域名使用代理
                    return listOf(
                        Proxy(
                            Proxy.Type.HTTP,
                            InetSocketAddress("proxy.example.com", 8080)
                        )
                    )
                } else {
                    // 其他情况直连
                    return listOf(Proxy.NO_PROXY)
                }
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {

            }
        }
        return proxySelector
    }

    private fun httpProxy(): Proxy {
        return Proxy.NO_PROXY
//        return Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", 8888))
//        return Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 8888))
//        return Proxy(Proxy.Type.DIRECT, InetSocketAddress("127.0.0.1", 8888))
    }

    /**
     * `Authorization: Bearer` 和 `Authorization: Basic` 是两种不同的 HTTP 认证方式，主要区别如下：
     *
     * ## 认证方式差异
     *
     * 1. **Authorization: Basic**
     *    - 使用用户名和密码进行认证
     *    - 凭据格式：`username:password` 经过 Base64 编码
     *    - 编码方式：[base64(username:password)](file://okio\ByteString.java#L40-L41)
     *    - 每次请求都需要发送用户名和密码
     *
     * 2. **Authorization: Bearer**
     *    - 使用令牌（token）进行认证
     *    - 凭据格式：Bearer Token（通常是 JWT 或 OAuth token）
     *    - 令牌由认证服务器颁发，有有效期
     *    - 不包含用户凭据信息
     *
     * ## 安全性对比
     *
     * - **Basic 认证**：
     *   - 用户凭据在每次请求中传输
     *   - Base64 编码不是加密，容易被解码
     *   - 需要配合 HTTPS 使用
     *
     * - **Bearer 认证**：
     *   - 使用临时令牌，不暴露用户凭据
     *   - 令牌有过期时间，安全性更高
     *   - 支持令牌撤销机制
     *
     * ## 使用场景
     *
     * - **Basic**：适用于简单的 API 访问、内部系统调用
     * - **Bearer**：适用于 OAuth 2.0、JWT 认证、第三方应用授权等场景
     *
     * Bearer 认证是目前主流的 API 安全认证方式。
     */
    private fun proxyAuthor(userName: String, passWord: String): Authenticator {
        // 代理认证
        val proxyAuthenticator = object : Authenticator {
            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? {
                val credential = Credentials.basic(userName, passWord);
                return response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            }
        }
        return proxyAuthenticator
    }

    private fun proxyAuthor(token: String): Authenticator {
        // 代理认证
        val proxyAuthenticator = object : Authenticator {
            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? {
                val credential = "Bearer $token"
                return response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            }
        }
        return proxyAuthenticator
    }

    private fun author(token: String): Authenticator {
        // 代理认证
        val proxyAuthenticator = object : Authenticator {
            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? {
                val credential = "Bearer $token"
                return response.request.newBuilder()
                    .header("Authorization", credential)
                    .build();
            }
        }
        return proxyAuthenticator
    }

    private fun author(userName: String, passWord: String): Authenticator {
        // 代理认证
        val proxyAuthenticator = object : Authenticator {
            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? {
                val credential = Credentials.basic(userName, passWord);
                return response.request.newBuilder()
                    .header("Authorization", credential)
                    .build();
            }
        }
        return proxyAuthenticator
    }

    /******************************  WebResourceResponse   *************************************/

    private fun webResourceResponse(requestBuilder: Request.Builder): WebResourceResponse? {
        val okHttpClient = createOkhttpClient()
        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        val code = response.code
        if (code != 200) {
            return null
        }
        val body = response.body
        if (body != null) {
            val contentType = body.contentType()
            val encoding = contentType?.charset()
            val mediaType = contentType?.toString()
            var mimeType = "text/plain"
            if (!TextUtils.isEmpty(mediaType)) {
                val mediaTypeElements = mediaType?.split(";")
                if (!mediaTypeElements.isNullOrEmpty()) {
                    mimeType = mediaTypeElements[0]
                }
            }
            val responseHeaders = mutableMapOf<String, String>()
            for (header in response.headers) {
                responseHeaders[header.first] = header.second
            }
            var message = response.message
            if (message.isBlank()) {
                message = "OK"
            }
            val resourceResponse =
                WebResourceResponse(mimeType, encoding?.name(), body.byteStream())
            resourceResponse.responseHeaders = responseHeaders
            resourceResponse.setStatusCodeAndReasonPhrase(code, message)
            return resourceResponse
        }
        return null
    }
    private var userAgent : String = ""
    fun getUserAgent(context: Context?) {
        userAgent = WebSettings.getDefaultUserAgent(context)
        if (userAgent.isEmpty()){
            userAgent = System.getProperty("http.agent") ?: "Android"
        }
    }
    fun getResponseByOkHttp(url: String?): WebResourceResponse? {
        if (url.isNullOrBlank()) {
            return null
        }
        try {
            val requestBuilder = Request.Builder().url(url)
            requestBuilder.method("GET", null)
            if (userAgent.isEmpty()){
                userAgent = webUserAgent
            }
            requestBuilder.addHeader("User-Agent", userAgent)
            val uri = url.toUri()
            uri.host?.let {
                val referer = "${uri.scheme}://$it"
                requestBuilder.addHeader("Host", it)
                requestBuilder.addHeader("Origin", referer)
                requestBuilder.addHeader("Referer", referer)
            }
            return webResourceResponse(requestBuilder)
        } catch (e: Exception) {
        }
        return null
    }

    fun getResponseByOkHttp(webResourceRequest: WebResourceRequest?): WebResourceResponse? {
        if (webResourceRequest == null) {
            return null
        }
        try {
            val url = webResourceRequest.url.toString()
            val requestBuilder = Request.Builder().url(url)
            requestBuilder.method(webResourceRequest.method, null)
            val requestHeaders = webResourceRequest.requestHeaders
            if (!requestHeaders.isNullOrEmpty()) {
                requestHeaders.forEach {
                    requestBuilder.addHeader(it.key, it.value)
                }
            }
            return webResourceResponse(requestBuilder)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }
}