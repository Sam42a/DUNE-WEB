package Dune.web.tv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import Dune.web.tv.utils.PreferencesManager

class LaunchActivity : AppCompatActivity() {
    private lateinit var prefs: PreferencesManager
    private lateinit var urlInput: EditText
    private lateinit var connectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        prefs = PreferencesManager.getInstance(this)
        urlInput = findViewById(R.id.urlInput)
        connectButton = findViewById(R.id.connectButton)
        
        // Check if we have a saved URL
        prefs.serverUrl?.let { savedUrl ->
            // If we have a saved URL, show it in the input field
            urlInput.setText(savedUrl)
            // Auto-submit if the saved URL is valid
            if (savedUrl.isNotBlank()) {
                connectToServer()
                return
            }
        }

        // Handle keyboard's done/enter key
        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                connectToServer()
                true
            } else {
                false
            }
        }

        // Handle connect button click
        connectButton.setOnClickListener {
            connectToServer()
        }

        // Focus the input field when activity starts
        urlInput.requestFocus()
    }

    private fun connectToServer() {
        var url = urlInput.text.toString().trim()

        // Add http:// if no protocol is specified
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server address", Toast.LENGTH_SHORT).show()
            return
        }

        // Save the URL for future use
        prefs.serverUrl = url

        // Start MainActivity with the URL
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SERVER_URL", url)
        }
        startActivity(intent)
        finish() // Close the launch activity
    }

    // Handle hardware keyboard enter key for TV
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            connectToServer()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
