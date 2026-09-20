package com.autoskip

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAdService : AccessibilityService() {

    private val skipKeywords = listOf("跳过", "跳過", "skip", "Skip", "SKIP")

    private var screenWidth = 0
    private var screenHeight = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        val dm = resources.displayMetrics
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType !in listOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            )
        ) return

        val rootNode = rootInActiveWindow ?: return
        val rightRegion = Rect(
            (screenWidth * 0.5).toInt(),
            0,
            screenWidth,
            (screenHeight * 0.35).toInt()
        )

        findAndClickSkip(rootNode, rightRegion)
        rootNode.recycle()
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

    override fun onInterrupt() {}
}
