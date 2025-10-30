package Dune.web.tv

import android.app.Application
import android.webkit.WebView

class Duneapp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable WebView debugging (always enabled for now)
        WebView.setWebContentsDebuggingEnabled(true)
    }
}
