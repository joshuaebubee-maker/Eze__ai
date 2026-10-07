package com.example.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentLinkedQueue

data class NotificationItem(
    val id: String,
    val packageName: String,
    val sender: String,
    val text: String,
    val timestamp: Long
)

class EzeNotificationListener : NotificationListenerService() {

    companion object {
        private val recentNotifications = ConcurrentLinkedQueue<NotificationItem>()

        fun getRecentNotifications(): List<NotificationItem> {
            return recentNotifications.toList()
        }

        fun clearNotifications() {
            recentNotifications.clear()
        }

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val cn = ComponentName(context, EzeNotificationListener::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString("android.title") ?: extras.getCharSequence("android.title")?.toString() ?: "App"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (text.isNotBlank()) {
            val item = NotificationItem(
                id = "${sbn.id}_${sbn.postTime}",
                packageName = sbn.packageName,
                sender = title,
                text = text,
                timestamp = sbn.postTime
            )
            recentNotifications.add(item)
            // Limit in-memory buffer to 20 items to conserve memory
            while (recentNotifications.size > 20) {
                recentNotifications.poll()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
