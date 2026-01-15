package com.lygttpod.monitor.web

import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import com.lygttpod.monitor.MonitorHelper
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import androidx.annotation.Keep
import androidx.annotation.RequiresApi


/**
 * Created By Mahongyin
 * Date    2026/1/14 13:51
 *
 */
@Keep
class ProxyX5Client(private val client: WebViewClient? = null) : WebViewClient(){

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        client?.onReceivedSslError(view, handler, error) ?: handler?.proceed()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view?.evaluateJavascript(MonitorHelper.injectVConsole(), null)
        } else {
            view?.loadUrl(MonitorHelper.injectVConsole())
        }
        client?.onPageFinished(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        if (client != null) {
            return client.shouldInterceptRequest(view, request)
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        url: String?
    ): WebResourceResponse? {
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

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: com.tencent.smtt.export.external.interfaces.HttpAuthHandler?,
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
