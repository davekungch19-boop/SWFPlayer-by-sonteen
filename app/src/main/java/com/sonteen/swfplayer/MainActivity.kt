package com.sonteenswfplayer

import android.app.Activity
import android.os.Bundle
import android.webkit.*
import android.content.Intent
import android.net.Uri
import android.widget.*
import android.view.View

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val PICK = 10

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        web = findViewById(R.id.web)
        val pick = findViewById<Button>(R.id.pick)
        web.settings.javaScriptEnabled = true
        web.settings.allowFileAccess = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(v: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                return false
            }
        }
        pick.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/x-shockwave-flash"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, PICK)
        }
    }

    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        super.onActivityResult(r,c,d)
        if (r == PICK && c == RESULT_OK && d?.data != null) {
            val uri = d.data!!
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Toast.makeText(this, "เลือกไฟล์แล้ว: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
            web.loadUrl("file:///android_asset/player.html?swf=" + Uri.encode(uri.toString()))
        }
    }
}
