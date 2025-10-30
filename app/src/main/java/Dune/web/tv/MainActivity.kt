package Dune.web.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.webkit.WebViewFeature
import Dune.web.tv.databinding.ActivityMainBinding
import Dune.web.tv.utils.PreferencesManager
import android.annotation.SuppressLint
import androidx.annotation.RequiresApi
import androidx.webkit.WebSettingsCompat

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var isError = false

    private lateinit var prefs: PreferencesManager

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)
        webView = binding.mainWebview
        progressBar = binding.progressBar

        var serverUrl = intent.getStringExtra("SERVER_URL")
        
        // If no URL in intent, try to get it from preferences
        if (serverUrl.isNullOrEmpty()) {
            serverUrl = prefs.serverUrl
        }
        
        // If still no URL, go to launch screen
        if (serverUrl.isNullOrEmpty()) {
            startActivity(Intent(this, LaunchActivity::class.java))
            finish()
            return
        }
        
        // Save the URL if it's new or different
        if (prefs.serverUrl != serverUrl) {
            prefs.serverUrl = serverUrl
        }

        setupWebView()
        loadJellyfinWeb(serverUrl)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_AUTO)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isError = false
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                injectTVStyles()
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    handleError(error.errorCode, error.description.toString())
                }
            }

            @Deprecated("Deprecated in Android 6.0.1")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                handleError(errorCode, description)
            }
            
            private fun handleError(errorCode: Int, description: String) {
                if (isError) return
                isError = true
                showError("Error loading content (Code: $errorCode). Please check your connection.")
            }
        }

        webView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        webView.scrollBy(0, -100)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        webView.scrollBy(0, 100)
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (webView.canGoBack()) {
                            webView.goBack()
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }

    private fun loadJellyfinWeb(serverUrl: String) {
        // Ensure URL has proper protocol
        val url = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            "https://$serverUrl"
        } else {
            serverUrl
        }
        
        webView.loadUrl(url)
    }

    private fun injectTVStyles() {
        val css = """
            *:focus {
                outline: 2px solid #00b4ff !important;
                outline-offset: 2px;
            }
            button, a, [role="button"], [tabindex] {
                min-height: 48px;
                min-width: 48px;
            }
            body {
                -webkit-tap-highlight-color: transparent;
            }
        """.trimIndent()
        
        val js = """
            var style = document.createElement('style');
            style.type = 'text/css';
            style.appendChild(document.createTextNode('$css'));
            document.head.appendChild(style);
        """.trimIndent()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null)
        } else {
            webView.loadUrl("javascript:$js")
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
