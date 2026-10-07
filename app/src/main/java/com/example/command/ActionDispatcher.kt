package com.example.command

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.service.AlarmReceiver
import com.example.service.EzeNotificationListener
import java.util.Calendar

class ActionDispatcher(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun execute(command: ParsedCommand): ActionResult {
        return when (command.actionType) {
            ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight(command.parameters["state"] == "on")
            ActionType.SET_ALARM -> setAlarm(command.parameters)
            ActionType.SHOW_ALARMS -> showAlarms()
            ActionType.SET_TIMER -> setTimer(command.parameters)
            ActionType.SET_REMINDER -> setReminder(command.parameters)
            ActionType.OPEN_DIALER -> openDialer()
            ActionType.CALL_PHONE -> callPhone(command.parameters["target"] ?: "")
            ActionType.OPEN_CONTACTS -> openContacts()
            ActionType.OPEN_CALL_LOG -> openCallLog()
            ActionType.COMPOSE_SMS -> composeSms(command.parameters["recipient"] ?: "", command.parameters["message"] ?: "")
            ActionType.OPEN_APP -> openApp(command.parameters["appName"] ?: "")
            ActionType.SET_VOLUME -> adjustVolume(command.parameters["direction"] == "up")
            ActionType.MEDIA_PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ActionType.MEDIA_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            ActionType.MEDIA_PREVIOUS -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ActionType.OPEN_SETTINGS -> openSettings(Settings.ACTION_SETTINGS, "System Settings")
            ActionType.OPEN_WIFI_SETTINGS -> openSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
            ActionType.OPEN_BLUETOOTH_SETTINGS -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
            ActionType.OPEN_BATTERY_SETTINGS -> openSettings(Intent.ACTION_POWER_USAGE_SUMMARY, "Battery Settings")
            ActionType.OPEN_DISPLAY_SETTINGS -> openSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
            ActionType.OPEN_SOUND_SETTINGS -> openSettings(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
            ActionType.OPEN_ASSISTANT_SETTINGS -> openAssistantSettings()
            ActionType.OPEN_MAPS -> openMaps()
            ActionType.NAVIGATE_TO -> navigateTo(command.parameters["destination"] ?: "")
            ActionType.WEB_SEARCH -> webSearch(command.parameters["query"] ?: "")
            ActionType.READ_NOTIFICATIONS -> readNotifications()
            ActionType.CALCULATOR -> ActionResult.Success(command.directAnswer ?: "Calculation complete.")
            ActionType.SAVE_MEMORY -> ActionResult.Success("I have recorded that in your local memory.")
            ActionType.CLEAR_MEMORY -> ActionResult.Success("All local memories have been cleared.")
            ActionType.AI_QUERY, ActionType.UNKNOWN -> ActionResult.Failed("Routed to conversational AI.")
        }
    }

    private fun toggleFlashlight(turnOn: Boolean): ActionResult {
        if (cameraManager == null) return ActionResult.Failed("Camera service unavailable on this device.")
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                ActionResult.Failed("No camera with a flashlight found on this device.")
            } else {
                cameraManager.setTorchMode(cameraId, turnOn)
                val status = if (turnOn) "turned on" else "turned off"
                ActionResult.Success("Flashlight has been $status.")
            }
        } catch (e: CameraAccessException) {
            ActionResult.Failed("Could not access flashlight: ${e.localizedMessage}")
        } catch (e: Exception) {
            ActionResult.Failed("Flashlight error: ${e.localizedMessage}")
        }
    }

    private fun setAlarm(params: Map<String, String>): ActionResult {
        val hour = params["hour"]?.toIntOrNull() ?: return ActionResult.Failed("Missing alarm hour.")
        val minutes = params["minutes"]?.toIntOrNull() ?: 0
        val label = params["label"] ?: "EZE Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val amPmHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                val amPm = if (hour >= 12) "PM" else "AM"
                val minStr = if (minutes < 10) "0$minutes" else "$minutes"
                ActionResult.Success("Alarm set for $amPmHour:$minStr $amPm.")
            } else {
                ActionResult.Failed("No alarm clock app available on this device.")
            }
        } catch (e: Exception) {
            ActionResult.Failed("Failed to set alarm: ${e.localizedMessage}")
        }
    }

    private fun showAlarms(): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success("Opening your alarms.")
            } else {
                ActionResult.Failed("No clock app available.")
            }
        } catch (e: Exception) {
            ActionResult.Failed("Failed to open alarms: ${e.localizedMessage}")
        }
    }

    private fun setTimer(params: Map<String, String>): ActionResult {
        val seconds = params["seconds"]?.toIntOrNull() ?: 300
        val label = params["label"] ?: "EZE Timer"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val mins = seconds / 60
                val secs = seconds % 60
                val durationText = if (mins > 0 && secs > 0) "$mins min $secs sec"
                else if (mins > 0) "$mins minutes"
                else "$secs seconds"
                ActionResult.Success("Timer started for $durationText.")
            } else {
                ActionResult.Failed("No timer app available on this device.")
            }
        } catch (e: Exception) {
            ActionResult.Failed("Failed to start timer: ${e.localizedMessage}")
        }
    }

    private fun setReminder(params: Map<String, String>): ActionResult {
        val task = params["task"] ?: "Reminder"
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return ActionResult.Failed("Alarm service unavailable.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return ActionResult.SecurityRestriction(
                    "Exact alarm permission is required for reminders. Please allow EZE in Settings."
                ) {
                    context.startActivity(intent)
                }
            }
        }

        val triggerTime = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30) // Default 30 min reminder if relative
        }.timeInMillis

        val reminderIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(AlarmReceiver.EXTRA_TITLE, "EZE Reminder")
            putExtra(AlarmReceiver.EXTRA_MESSAGE, task)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        return ActionResult.Success("Reminder scheduled: \"$task\".")
    }

    private fun openDialer(): ActionResult {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening Phone Dialer.")
        } catch (e: Exception) {
            ActionResult.Failed("Could not open dialer: ${e.localizedMessage}")
        }
    }

    private fun callPhone(target: String): ActionResult {
        val cleanNumber = target.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isNotEmpty()) {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return try {
                    context.startActivity(callIntent)
                    ActionResult.Success("Calling $cleanNumber.")
                } catch (e: Exception) {
                    ActionResult.Failed("Call failed: ${e.localizedMessage}")
                }
            } else {
                // Graceful fallback: Open dialer with prefilled number without breaking security
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                return ActionResult.Success("Opened dialer for $cleanNumber (Direct call permission not granted).")
            }
        } else {
            // Target is a name like "John"
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ActionResult.Success("Opened Contacts to find \"$target\".")
        }
    }

    private fun openContacts(): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening Contacts.")
        } catch (e: Exception) {
            ActionResult.Failed("Could not open contacts: ${e.localizedMessage}")
        }
    }

    private fun openCallLog(): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, CallLog.Calls.CONTENT_URI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening Call Log.")
        } catch (e: Exception) {
            ActionResult.Failed("Could not open call log: ${e.localizedMessage}")
        }
    }

    private fun composeSms(recipient: String, message: String): ActionResult {
        val uri = if (recipient.isNotBlank()) Uri.parse("smsto:$recipient") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            if (message.isNotBlank()) putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val targetText = if (recipient.isNotBlank()) " to $recipient" else ""
                ActionResult.Success("Drafted SMS$targetText.")
            } else {
                ActionResult.Failed("No SMS app installed on this device.")
            }
        } catch (e: Exception) {
            ActionResult.Failed("Failed to open SMS app: ${e.localizedMessage}")
        }
    }

    private fun openApp(appName: String): ActionResult {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
        val cleanTarget = appName.trim().lowercase()

        val matched = apps.firstOrNull {
            val label = it.loadLabel(pm).toString().lowercase()
            label == cleanTarget || label.contains(cleanTarget)
        }

        if (matched != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return ActionResult.Success("Opening ${matched.loadLabel(pm)}.")
            }
        }

        // Direct common fallbacks
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "camera" to "com.google.android.GoogleCamera"
        )
        val fallbackPkg = knownPackages[cleanTarget]
        if (fallbackPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(fallbackPkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return ActionResult.Success("Opening $appName.")
            }
        }

        return ActionResult.Failed("Could not find an app named \"$appName\" on this device.")
    }

    private fun adjustVolume(raise: Boolean): ActionResult {
        if (audioManager == null) return ActionResult.Failed("Audio manager unavailable.")
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        val dirText = if (raise) "increased" else "lowered"
        return ActionResult.Success("Media volume $dirText.")
    }

    private fun dispatchMediaKey(keyCode: Int): ActionResult {
        if (audioManager == null) return ActionResult.Failed("Audio manager unavailable.")
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
        val label = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> "Skipping to next track"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "Playing previous track"
            else -> "Toggling media playback"
        }
        return ActionResult.Success("$label.")
    }

    private fun openSettings(action: String, name: String): ActionResult {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening $name.")
        } catch (e: Exception) {
            ActionResult.Failed("Failed to open $name: ${e.localizedMessage}")
        }
    }

    private fun openAssistantSettings(): ActionResult {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening Default Assistant settings.")
        } catch (e: Exception) {
            // Fallback to manage default apps
            val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
                ActionResult.Success("Opening Default Apps settings.")
            } catch (ex: Exception) {
                ActionResult.Failed("Could not open Assistant settings.")
            }
        }
    }

    private fun openMaps(): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Opening Maps.")
        } catch (e: Exception) {
            ActionResult.Failed("Could not open maps: ${e.localizedMessage}")
        }
    }

    private fun navigateTo(destination: String): ActionResult {
        val enc = Uri.encode(destination)
        val uri = Uri.parse("google.navigation:q=$enc")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success("Starting navigation to $destination.")
            } else {
                // Fallback to web maps
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$enc")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult.Success("Opening directions to $destination in browser.")
            }
        } catch (e: Exception) {
            ActionResult.Failed("Navigation failed: ${e.localizedMessage}")
        }
    }

    private fun webSearch(query: String): ActionResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ActionResult.Success("Searching web for \"$query\".")
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult.Success("Searching web for \"$query\".")
        }
    }

    private fun readNotifications(): ActionResult {
        if (!EzeNotificationListener.isNotificationAccessEnabled(context)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return ActionResult.SecurityRestriction(
                "Notification access is not enabled. Please permit EZE in Notification Access settings."
            ) {
                context.startActivity(intent)
            }
        }

        val notifications = EzeNotificationListener.getRecentNotifications()
        if (notifications.isEmpty()) {
            return ActionResult.Success("You have no new notifications right now.")
        }

        val summary = buildString {
            append("You have ${notifications.size} recent notification(s):\n")
            notifications.take(4).forEachIndexed { i, notif ->
                append("${i + 1}. From ${notif.sender}: ${notif.text}\n")
            }
        }
        return ActionResult.Success(summary.trim())
    }
}
