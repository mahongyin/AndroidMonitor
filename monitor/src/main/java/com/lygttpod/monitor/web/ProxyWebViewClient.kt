package com.lygttpod.monitor.web

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.lygttpod.monitor.MonitorHelper


/**
 * Created By Mahongyin
 * Date    2025/9/11 11:02
 *
 */
@Keep
class ProxyWebViewClient(private val client: WebViewClient? = null) : WebViewClient() {

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        client?.onReceivedSslError(view, handler, error) ?: handler?.proceed()//TODO 忽略了证书错误
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (client != null) {
            return client.shouldOverrideUrlLoading(view, url)
        }
        return super.shouldOverrideUrlLoading(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (client != null) {
            return client.shouldOverrideUrlLoading(view, request)
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        client?.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view?.evaluateJavascript(MonitorHelper.injectVConsole(), null)
        } else {
            view?.loadUrl(MonitorHelper.injectVConsole())
        }
        client?.onPageFinished(view, url)
    }

    //在 API >= 21 的设备上优先被调用 后台线程调用，严禁在此方法中直接进行UI操作
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        // 可以拦截请求，返回自定义的response，但不推荐
//        val response = MonitorHelper.shouldInterceptRequest(view, request)
//        if (response != null) {
//            return response
//        }
        if (client != null) {
            return client.shouldInterceptRequest(view, request)
        }
        return super.shouldInterceptRequest(view, request)
    }

    //在 API < 21 的设备上被调用 后台线程调用，严禁在此方法中直接进行UI操作
    override fun shouldInterceptRequest(
        view: WebView?,
        url: String?
    ): WebResourceResponse? {
        // 可以拦截请求，返回自定义的response，但不推荐
//        val response = MonitorHelper.shouldInterceptRequest(view, url)
//        if (response != null) {
//            return response
//        }
        if (client != null) {
            return client.shouldInterceptRequest(view, url)
        }
        return super.shouldInterceptRequest(view, url)
    }

    override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
        if (client != null) {
            return client.shouldOverrideKeyEvent(view, event)
        }
        return super.shouldOverrideKeyEvent(view, event)
    }

    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        client?.onScaleChanged(view, oldScale, newScale)
    }

    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        client?.onReceivedError(view, errorCode, description, failingUrl)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        client?.onReceivedError(view, request, error)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        client?.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onFormResubmission(
        view: WebView?,
        dontResend: Message?,
        resend: Message?
    ) {
        client?.onFormResubmission(view, dontResend, resend)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        client?.onLoadResource(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPageCommitVisible(view: WebView?, url: String?) {
        client?.onPageCommitVisible(view, url)
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        client?.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        client?.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        client?.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedLoginRequest(
        view: WebView?,
        realm: String?,
        account: String?,
        args: String?
    ) {
        client?.onReceivedLoginRequest(view, realm, account, args)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        if (client != null) {
            return client.onRenderProcessGone(view, detail)
        }
        return super.onRenderProcessGone(view, detail)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponse?
    ) {
        client?.onSafeBrowsingHit(view, request, threatType, callback)
    }

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        client?.onUnhandledKeyEvent(view, event)
    }

    override fun onTooManyRedirects(
        view: WebView?,
        cancelMsg: Message?,
        continueMsg: Message?
    ) {
        client?.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    override fun equals(other: Any?): Boolean {
        if (client != null) {
            return client.equals(other)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        if (client != null) {
            return client.hashCode()
        }
        return super.hashCode()
    }

    override fun toString(): String {
        if (client != null) {
            return client.toString()
        }
        return super.toString()
    }

}