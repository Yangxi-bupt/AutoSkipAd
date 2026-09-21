package com.autoskip

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class SkipAdService : AccessibilityService() {

    private val skipKeywords = listOf("跳过", "跳過", "关闭", "close", "Close", "CLOSE")

    private var screenWidth = 0
    private var screenHeight = 0

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private var pollCount = 0

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val MAX_POLL_COUNT = 6
        private const val COOLDOWN_MS = 3000L
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "autoskip_channel"
    }

    private var lastTriggerTime = 0L
    private var lastTriggerPackage = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        val dm = resources.displayMetrics
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "AutoSkip Service", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoSkip Running")
            .setContentText("Monitoring for ad skip buttons")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val currentPackage = event.packageName?.toString() ?: return
        if (currentPackage == packageName) return

        val now = System.currentTimeMillis()
        if (currentPackage == lastTriggerPackage && now - lastTriggerTime < COOLDOWN_MS) return

        lastTriggerTime = now
        lastTriggerPackage = currentPackage
        startPolling()
    }

    private fun startPolling() {
        stopPolling()
        pollCount = 0
        pollRunnable = object : Runnable {
            override fun run() {
                if (pollCount >= MAX_POLL_COUNT) return
                pollCount++

                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val rightRegion = Rect(
                        (screenWidth * 0.5).toInt(),
                        0,
                        screenWidth,
                        screenHeight
                    )

                    val found = findAndClickSkip(rootNode, rightRegion)
                    rootNode.recycle()

                    if (found) {
                        stopPolling()
                        return
                    }
                }

                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.post(pollRunnable!!)
    }

    private fun stopPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        pollRunnable = null
    }

    private fun findAndClickSkip(node: AccessibilityNodeInfo, region: Rect): Boolean {
        val nodeRect = Rect()
        node.getBoundsInScreen(nodeRect)

        if (!Rect.intersects(nodeRect, region)) return false

        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val combined = "$text$contentDesc"

        val isSkip = skipKeywords.any { combined.contains(it, ignoreCase = true) }
        if (isSkip && node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        if (isSkip && !node.isClickable) {
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.recycle()
                    return true
                }
                val next = parent.parent
                parent.recycle()
                parent = next
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickSkip(child, region)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    override fun onInterrupt() {}
}
