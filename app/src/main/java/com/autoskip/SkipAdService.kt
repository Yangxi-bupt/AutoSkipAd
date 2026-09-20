package com.autoskip

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAdService : AccessibilityService() {

    private val skipKeywords = listOf("跳过", "跳過", "skip", "Skip", "SKIP")

    private var screenWidth = 0
    private var screenHeight = 0

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private var pollCount = 0

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val MAX_POLL_COUNT = 20
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val dm = resources.displayMetrics
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

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
