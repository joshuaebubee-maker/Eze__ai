package com.example.command

import java.util.Locale
import java.util.regex.Pattern

object CommandParser {

    /**
     * Parses the user's raw query into a structured ParsedCommand.
     */
    fun parse(rawQuery: String): ParsedCommand {
        val trimmed = rawQuery.trim()
        val query = trimmed.lowercase(Locale.ROOT)

        // 1. Local Calculator / Unit Conversion (Fast offline evaluation)
        val calcResult = LocalCalculator.evaluate(trimmed)
        if (calcResult != null) {
            return ParsedCommand(
                actionType = ActionType.CALCULATOR,
                rawQuery = trimmed,
                directAnswer = calcResult
            )
        }

        // 2. Flashlight / Torch
        if (query.matches(Regex(".*(turn\\s+on\\s+(the\\s+)?(flashlight|torch)|(flashlight|torch)\\s+on).*"))) {
            return ParsedCommand(
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                rawQuery = trimmed,
                parameters = mapOf("state" to "on")
            )
        }
        if (query.matches(Regex(".*(turn\\s+off\\s+(the\\s+)?(flashlight|torch)|(flashlight|torch)\\s+off).*"))) {
            return ParsedCommand(
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                rawQuery = trimmed,
                parameters = mapOf("state" to "off")
            )
        }

        // 3. Alarms
        if (query.contains("show alarm") || query.contains("open alarm") || query == "alarms") {
            return ParsedCommand(
                actionType = ActionType.SHOW_ALARMS,
                rawQuery = trimmed
            )
        }

        val alarmPattern = Pattern.compile("(?:set\\s+(?:an?\\s+)?alarm(?:\\s+for)?|wake\\s+me(?:\\s+up)?(?:\\s+tomorrow)?(?:\\s+at)?)\\s+([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm)?")
        val alarmMatcher = alarmPattern.matcher(query)
        if (alarmMatcher.find()) {
            val hourStr = alarmMatcher.group(1) ?: "7"
            val minStr = alarmMatcher.group(2) ?: "00"
            val amPm = alarmMatcher.group(3)

            var hour = hourStr.toIntOrNull() ?: 7
            val min = minStr.toIntOrNull() ?: 0

            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0

            return ParsedCommand(
                actionType = ActionType.SET_ALARM,
                rawQuery = trimmed,
                parameters = mapOf(
                    "hour" to hour.toString(),
                    "minutes" to min.toString(),
                    "label" to "EZE Alarm"
                )
            )
        }

        // 4. Timers
        val timerPattern = Pattern.compile("(?:set|start|create)\\s+(?:a\\s+)?(?:timer\\s+(?:for\\s+)?)?([0-9]+)\\s*(minute|min|minutes|second|sec|seconds|hour|hours)\\s*(?:timer)?")
        val timerMatcher = timerPattern.matcher(query)
        if (timerMatcher.find()) {
            val amount = timerMatcher.group(1)?.toIntOrNull() ?: 5
            val unit = timerMatcher.group(2) ?: "minute"
            val totalSeconds = when {
                unit.startsWith("sec") -> amount
                unit.startsWith("hour") -> amount * 3600
                else -> amount * 60
            }
            return ParsedCommand(
                actionType = ActionType.SET_TIMER,
                rawQuery = trimmed,
                parameters = mapOf(
                    "seconds" to totalSeconds.toString(),
                    "label" to "EZE Timer"
                )
            )
        }

        // 5. Reminders
        val reminderPattern = Pattern.compile("remind\\s+me\\s+(?:at\\s+([0-9]{1,2}(?::[0-9]{2})?\\s*(?:am|pm)?)\\s+)?to\\s+(.+)")
        val reminderMatcher = reminderPattern.matcher(query)
        if (reminderMatcher.find()) {
            val time = reminderMatcher.group(1) ?: "soon"
            val task = reminderMatcher.group(2) ?: "Task"
            return ParsedCommand(
                actionType = ActionType.SET_REMINDER,
                rawQuery = trimmed,
                parameters = mapOf("time" to time, "task" to task)
            )
        }

        // 6. Phone Calls & Contacts
        if (query == "open dialer" || query == "open phone" || query == "dialer") {
            return ParsedCommand(ActionType.OPEN_DIALER, trimmed)
        }
        if (query.contains("open contacts") || query == "contacts") {
            return ParsedCommand(ActionType.OPEN_CONTACTS, trimmed)
        }
        if (query.contains("recent calls") || query.contains("call log")) {
            return ParsedCommand(ActionType.OPEN_CALL_LOG, trimmed)
        }

        val callPattern = Pattern.compile("(?:call|dial)\\s+([a-zA-Z0-9+ ]+)")
        val callMatcher = callPattern.matcher(query)
        if (callMatcher.find()) {
            val target = callMatcher.group(1)?.trim() ?: ""
            if (target.isNotEmpty() && !target.contains("police") && !target.contains("mom's phone number")) {
                return ParsedCommand(
                    actionType = ActionType.CALL_PHONE,
                    rawQuery = trimmed,
                    parameters = mapOf("target" to target)
                )
            }
        }

        // 7. SMS / Messaging
        val smsPattern = Pattern.compile("(?:send\\s+(?:an?\\s+)?sms|text|message)\\s+(?:to\\s+)?([a-zA-Z0-9+]+)?\\s*(?:saying|that|:)?\\s*(.*)")
        val smsMatcher = smsPattern.matcher(query)
        if (smsMatcher.find()) {
            val recipient = smsMatcher.group(1) ?: ""
            val message = smsMatcher.group(2) ?: ""
            return ParsedCommand(
                actionType = ActionType.COMPOSE_SMS,
                rawQuery = trimmed,
                parameters = mapOf("recipient" to recipient, "message" to message)
            )
        }

        // 8. System Settings
        if (query.contains("wifi setting") || query.contains("wi-fi setting") || query == "open wifi") {
            return ParsedCommand(ActionType.OPEN_WIFI_SETTINGS, trimmed)
        }
        if (query.contains("bluetooth setting") || query == "open bluetooth") {
            return ParsedCommand(ActionType.OPEN_BLUETOOTH_SETTINGS, trimmed)
        }
        if (query.contains("battery setting")) {
            return ParsedCommand(ActionType.OPEN_BATTERY_SETTINGS, trimmed)
        }
        if (query.contains("display setting") || query.contains("brightness setting")) {
            return ParsedCommand(ActionType.OPEN_DISPLAY_SETTINGS, trimmed)
        }
        if (query.contains("sound setting") || query.contains("volume setting")) {
            return ParsedCommand(ActionType.OPEN_SOUND_SETTINGS, trimmed)
        }
        if (query.contains("assistant setting") || query.contains("default assistant")) {
            return ParsedCommand(ActionType.OPEN_ASSISTANT_SETTINGS, trimmed)
        }
        if (query == "open settings" || query == "settings") {
            return ParsedCommand(ActionType.OPEN_SETTINGS, trimmed)
        }

        // 9. Volume Controls
        if (query.contains("volume up") || query.contains("increase volume")) {
            return ParsedCommand(ActionType.SET_VOLUME, trimmed, mapOf("direction" to "up"))
        }
        if (query.contains("volume down") || query.contains("decrease volume") || query.contains("lower volume")) {
            return ParsedCommand(ActionType.SET_VOLUME, trimmed, mapOf("direction" to "down"))
        }

        // 10. Media controls
        if (query.matches(Regex(".*(play\\s+music|resume\\s+music|pause\\s+music|stop\\s+music).*"))) {
            return ParsedCommand(ActionType.MEDIA_PLAY_PAUSE, trimmed)
        }
        if (query.contains("next song") || query.contains("next track") || query.contains("skip song")) {
            return ParsedCommand(ActionType.MEDIA_NEXT, trimmed)
        }
        if (query.contains("previous song") || query.contains("previous track")) {
            return ParsedCommand(ActionType.MEDIA_PREVIOUS, trimmed)
        }

        // 11. Navigation & Maps
        val navPattern = Pattern.compile("(?:navigate\\s+to|directions\\s+to|take\\s+me\\s+to|how\\s+to\\s+get\\s+to)\\s+(.+)")
        val navMatcher = navPattern.matcher(query)
        if (navMatcher.find()) {
            val destination = navMatcher.group(1)?.trim() ?: ""
            return ParsedCommand(
                actionType = ActionType.NAVIGATE_TO,
                rawQuery = trimmed,
                parameters = mapOf("destination" to destination)
            )
        }
        if (query == "open maps" || query == "maps") {
            return ParsedCommand(ActionType.OPEN_MAPS, trimmed)
        }

        // 12. App Launching
        val appPattern = Pattern.compile("(?:open|launch|start)\\s+([a-zA-Z0-9 ]+)")
        val appMatcher = appPattern.matcher(query)
        if (appMatcher.find()) {
            val appName = appMatcher.group(1)?.trim() ?: ""
            if (appName.isNotEmpty() && appName !in listOf("the flashlight", "flashlight", "torch", "settings", "wifi", "bluetooth", "maps", "dialer", "contacts", "camera")) {
                return ParsedCommand(
                    actionType = ActionType.OPEN_APP,
                    rawQuery = trimmed,
                    parameters = mapOf("appName" to appName)
                )
            }
        }

        // 13. Notifications
        if (query.contains("read my notification") || query.contains("read notification") || query.contains("what are my notification") || query.contains("show notification")) {
            return ParsedCommand(ActionType.READ_NOTIFICATIONS, trimmed)
        }

        // 14. Web search explicit
        val searchPattern = Pattern.compile("(?:search\\s+(?:for|google\\s+for|the\\s+web\\s+for)?)\\s+(.+)")
        val searchMatcher = searchPattern.matcher(query)
        if (searchMatcher.find() && (query.startsWith("search") || query.startsWith("google"))) {
            val searchTerms = searchMatcher.group(1)?.trim() ?: ""
            return ParsedCommand(
                actionType = ActionType.WEB_SEARCH,
                rawQuery = trimmed,
                parameters = mapOf("query" to searchTerms),
                requiresInternet = true
            )
        }

        // 15. Memory Commands
        val rememberPattern = Pattern.compile("(?:remember(?:\\s+that)?|don't\\s+forget(?:\\s+that)?)\\s+(.+)")
        val rememberMatcher = rememberPattern.matcher(query)
        if (rememberMatcher.find()) {
            val memoryFact = rememberMatcher.group(1)?.trim() ?: ""
            return ParsedCommand(
                actionType = ActionType.SAVE_MEMORY,
                rawQuery = trimmed,
                parameters = mapOf("fact" to memoryFact)
            )
        }
        if (query.contains("clear all memory") || query.contains("clear my memory") || query.contains("delete memory")) {
            return ParsedCommand(ActionType.CLEAR_MEMORY, trimmed)
        }

        // Default: Route to Groq AI / Knowledge query
        return ParsedCommand(
            actionType = ActionType.AI_QUERY,
            rawQuery = trimmed,
            requiresInternet = true
        )
    }
}
