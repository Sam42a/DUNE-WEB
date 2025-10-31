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
import android.util.Log
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

        if (serverUrl.isNullOrEmpty()) {
            serverUrl = prefs.serverUrl
        }

        if (serverUrl.isNullOrEmpty()) {
            startActivity(Intent(this, LaunchActivity::class.java))
            finish()
            return
        }

        if (prefs.serverUrl != serverUrl) {
            prefs.serverUrl = serverUrl
        }

        setupWebView()
        loadJellyfinWeb(serverUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
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

            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        // Enable focus for D-pad navigation
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isError = false
                progressBar.visibility = View.VISIBLE
                Log.d("WebView", "Page started loading: $url")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                Log.d("WebView", "Page finished loading: $url")

                injectTVStyles()
                view.requestFocus()

                view.postDelayed({
                    view.evaluateJavascript("""
                        (function() {
                            const focusable = document.querySelectorAll(
                                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"]), [role="button"]'
                            );
                            if (focusable.length > 0) {
                                focusable[0].focus();
                                focusable[0].scrollIntoView({behavior: 'smooth', block: 'center'});
                            } else {
                                document.body.tabIndex = 0;
                                document.body.focus();
                            }
                            document.documentElement.classList.add('dune-focus-enabled');
                            return true;
                        })();
                    """.trimIndent(), null)
                }, 300)
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
    }

    private fun loadJellyfinWeb(serverUrl: String) {
        val url = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            "https://$serverUrl"
        } else {
            serverUrl
        }
        webView.loadUrl(url)
    }

    private fun injectTVStyles() {
        val css = """
            /* Base focus style - smaller and thinner */
            *:focus {
                outline: 2px solid rgba(255, 255, 255, 0.5) !important;
                outline-offset: 2px !important;
                box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.2) !important;
                border-radius: 10% !important;
            }
            
            /* Image container focus - prioritize the image itself */
            a:has(img):focus, 
            div:has(img):focus,
            [role="button"]:has(img):focus {
                outline: 2px solid rgba(255, 255, 255, 0.5) !important;
                outline-offset: 1px !important;
                box-shadow: 0 0 10px rgba(255, 255, 255, 0.3) !important;
                border-radius: 10% !important;
            }
            
            /* Make sure image containers expand to full image size */
            a:has(img), 
            div:has(img),
            [role="button"]:has(img) {
                display: block !important;
                position: relative !important;
            }
            
            /* Ensure images themselves don't get separate focus */
            img {
                pointer-events: none !important;
                outline: none !important;
            }
            
            /* Hide text focus when parent image container is focused */
            a:has(img):focus *:not(img),
            div:has(img):focus *:not(img),
            [role="button"]:has(img):focus *:not(img) {
                outline: none !important;
            }
            
            /* Scale effect for image containers - more subtle */
            a:has(img):focus, 
            div:has(img):focus,
            [role="button"]:has(img):focus {
                transform: scale(1.02) !important;
                z-index: 999 !important;
                transition: transform 0.15s ease, box-shadow 0.15s ease !important;
            }
            
            /* Regular button/link focus */
            button, a, [role="button"], [tabindex]:not([tabindex="-1"]) {
                min-height: 48px !important;
                min-width: 48px !important;
                padding: 12px !important;
                cursor: pointer !important;
            }
            
            button:focus, a:focus:not(:has(img)), [role="button"]:focus:not(:has(img)) {
                transform: scale(1.02) !important;
                z-index: 999 !important;
                transition: transform 0.15s ease !important;
            }
            
            html {
                scroll-behavior: smooth !important;
            }
            
            body {
                -webkit-user-select: none !important;
                user-select: none !important;
            }
        """.trimIndent()

        val js = """
            (function() {
                var existingStyle = document.getElementById('dune-focus-style');
                if (existingStyle) existingStyle.remove();
                
                var style = document.createElement('style');
                style.id = 'dune-focus-style';
                style.textContent = `$css`;
                (document.head || document.documentElement).appendChild(style);
                
                // Enhanced keyboard navigation with image priority
                document.addEventListener('keydown', function(e) {
                    const focused = document.activeElement;
                    
                    // Handle Enter/OK button
                    if (e.keyCode === 13 || e.keyCode === 23) { // Enter or DPAD_CENTER
                        if (focused && focused !== document.body) {
                            e.preventDefault();
                            focused.click();
                            return;
                        }
                    }
                    
                    // Enhanced scrolling for focused elements, especially images
                    if ([37, 38, 39, 40].includes(e.keyCode)) { // Arrow keys
                        setTimeout(() => {
                            const newFocused = document.activeElement;
                            if (newFocused && newFocused !== document.body) {
                                // Check if focused element contains an image
                                const hasImage = newFocused.querySelector('img') !== null;
                                newFocused.scrollIntoView({
                                    behavior: 'smooth',
                                    block: hasImage ? 'center' : 'nearest',
                                    inline: 'center'
                                });
                            }
                        }, 50);
                    }
                });
                
                // Make sure all interactive elements are focusable
                // Prioritize image containers
                function ensureFocusable() {
                    // First, make all image containers focusable
                    const imageContainers = document.querySelectorAll('a:has(img), div:has(img), [role="button"]:has(img)');
                    imageContainers.forEach(el => {
                        if (!el.hasAttribute('tabindex')) {
                            el.setAttribute('tabindex', '0');
                        }
                        // Make images inside non-focusable
                        const imgs = el.querySelectorAll('img');
                        imgs.forEach(img => {
                            img.setAttribute('tabindex', '-1');
                        });
                    });
                    
                    // Then handle other interactive elements
                    const elements = document.querySelectorAll('button, a[href]:not(:has(img)), [role="button"]:not(:has(img)), [onclick]');
                    elements.forEach(el => {
                        if (!el.hasAttribute('tabindex')) {
                            el.setAttribute('tabindex', '0');
                        }
                    });
                }
                
                ensureFocusable();
                
                // Re-apply when DOM changes
                const observer = new MutationObserver(ensureFocusable);
                observer.observe(document.body, { 
                    childList: true, 
                    subtree: true 
                });
                
                // Prevent images from stealing focus
                document.addEventListener('focus', function(e) {
                    if (e.target.tagName === 'IMG') {
                        e.preventDefault();
                        const parent = e.target.closest('a, div, [role="button"]');
                        if (parent) {
                            parent.focus();
                        }
                    }
                }, true);
            })();
        """.trimIndent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(js, null)
        } else {
            webView.loadUrl("javascript:$js")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("KeyEvent", "Key down: $keyCode")

        // Only handle BACK button specially
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        // Handle Center/Enter to click focused element
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            webView.evaluateJavascript("""
                (function() {
                    const active = document.activeElement;
                    if (active && active !== document.body) {
                        active.click();
                        return true;
                    }
                    return false;
                })();
            """.trimIndent(), null)
            return true
        }

        // Let WebView handle D-pad navigation natively
        return super.onKeyDown(keyCode, event)
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