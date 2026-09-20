package com.autoskip

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.tv_status)
        val btnEnable = findViewById<Button>(R.id.btn_enable)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        window.decorView.post {
            updateStatus(statusText)
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.tv_status)
        updateStatus(statusText)
    }

    private fun updateStatus(textView: TextView) {
        if (isServiceEnabled()) {
            textView.text = getString(R.string.status_on)
        } else {
            textView.text = getString(R.string.status_off)
        }
    }

    private fun isServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        am.enabledAccessibilityServiceList.forEach { info ->
            if (info.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }
}
