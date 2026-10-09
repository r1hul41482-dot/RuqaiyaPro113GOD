package com.ruqaiyapro.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {

    companion object {
        var isAutoReplyEnabled = true
        var autoReplyMessage = "Boss Rubel busy ache, pore reply dibe - Ruqaiya Lobby"
        var isBroadcastRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: ""
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        val rootNode = rootInActiveWindow ?: return

        if (isAutoReplyEnabled) {
            handleAutoReply(rootNode)
        }
    }

    private fun handleAutoReply(rootNode: AccessibilityNodeInfo) {
        // Find WhatsApp input field
        val inputNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (inputNodes.isNotEmpty()) {
            val inputField = inputNodes[0]
            if (inputField.text.isNullOrEmpty()) {
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        autoReplyMessage
                    )
                }
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                // Paced 300ms delay to press send button
                Handler(Looper.getMainLooper()).postDelayed({
                    val sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                    if (sendButtons.isNotEmpty()) {
                        sendButtons[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }, 300)
            }
        }
    }

    override fun onInterrupt() {}
}