package com.famy.tree.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.famy.tree.R

class CrashActivity : ComponentActivity() {

    private var crashInfo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        crashInfo = intent.getStringExtra(EXTRA_CRASH_INFO) ?: "No crash information available"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.crash_background))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left + 24.dp,
                top = systemBars.top + 24.dp,
                right = systemBars.right + 24.dp,
                bottom = systemBars.bottom + 24.dp
            )
            insets
        }

        val titleText = TextView(this).apply {
            text = getString(R.string.crash_title)
            textSize = 20f
            setTextColor(getColor(R.color.crash_text))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dp
            }
        }

        val messageText = TextView(this).apply {
            text = getString(R.string.crash_message)
            textSize = 14f
            setTextColor(getColor(R.color.crash_text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dp
            }
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = 16.dp
            }
        }

        val logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.crash_log_background))
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
        }

        val logText = TextView(this).apply {
            text = crashInfo
            textSize = 11f
            setTextColor(getColor(R.color.crash_log_text))
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        logContainer.addView(logText)
        scrollView.addView(logContainer)

        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val copyButton = Button(this).apply {
            text = getString(R.string.crash_copy)
            textSize = 14f
            isAllCaps = false
            setBackgroundColor(getColor(R.color.crash_log_background))
            setTextColor(getColor(R.color.crash_button))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8.dp
            }
            setOnClickListener { copyToClipboard() }
        }

        val restartButton = Button(this).apply {
            text = getString(R.string.crash_restart)
            textSize = 14f
            isAllCaps = false
            setBackgroundColor(getColor(R.color.crash_button))
            setTextColor(getColor(R.color.crash_button_text))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { restartApp() }
        }

        buttonContainer.addView(copyButton)
        buttonContainer.addView(restartButton)

        rootLayout.addView(titleText)
        rootLayout.addView(messageText)
        rootLayout.addView(scrollView)
        rootLayout.addView(buttonContainer)

        setContentView(rootLayout)
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", crashInfo)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.crash_copied), Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            startActivity(intent)
        }
        finish()
        Runtime.getRuntime().exit(0)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_CRASH_INFO = "extra_crash_info"
    }
}
