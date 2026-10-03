package com.sonteenswfplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.*
import java.io.ByteArrayInputStream
import java.io.InputStream

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var overlay: View
    private val PICK = 10
    private val HOST = "appassets.androidplatform.net"
    private var swfUri: Uri? = null
    private var counter = 0

    private fun notFound() = WebResourceResponse(
        "text/plain", "utf-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(ByteArray(0))
    )

    private fun respond(mime: String, s: InputStream): WebResourceResponse {
        val enc = if (mime.startsWith("text/") || mime == "application/json") "utf-8" else null
        return WebResourceResponse(mime, enc, 200, "OK", mapOf("Access-Control-Allow-Origin" to "*"), s)
    }

    private fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "js", "mjs" -> "text/javascript"
            "wasm" -> "application/wasm"
            "html" -> "text/html"
            "css" -> "text/css"
            "json" -> "application/json"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    private fun goImmersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goImmersive()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        goImmersive()
        web = findViewById(R.id.web)
        overlay = findViewById(R.id.overlay)
        val pick = findViewById<Button>(R.id.pick)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val u = request.url
                if (u.host != HOST) return null
                return try {
                    val path = u.path ?: "/"
                    if (path == "/swf") {
                        val uri = swfUri ?: return notFound()
                        val s = contentResolver.openInputStream(uri) ?: return notFound()
                        respond("application/x-shockwave-flash", s)
                    } else {
                        val name = path.trimStart('/')
                        respond(mimeFor(name), assets.open(name))
                    }
                } catch (e: Exception) {
                    notFound()
                }
            }
        }
        pick.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, PICK)
        }
    }

    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        super.onActivityResult(r, c, d)
        if (r == PICK && c == RESULT_OK && d?.data != null) {
            val uri = d.data!!
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            swfUri = uri
            overlay.visibility = View.GONE
            val swfUrl = "https://$HOST/swf?n=${++counter}"
            web.loadUrl("https://$HOST/player.html?swf=" + Uri.encode(swfUrl))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (overlay.visibility != View.VISIBLE) {
            web.loadUrl("about:blank")
            overlay.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}
