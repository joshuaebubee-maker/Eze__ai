package com.example.command

enum class ActionCategory {
    SYSTEM,
    COMMUNICATION,
    CLOCK,
    MEDIA,
    NAVIGATION,
    CALCULATION,
    UTILITY,
    AI_CONVERSATION
}

enum class ActionType(val category: ActionCategory, val displayName: String) {
    // Clock
    SET_ALARM(ActionCategory.CLOCK, "Set Alarm"),
    SHOW_ALARMS(ActionCategory.CLOCK, "Show Alarms"),
    SET_TIMER(ActionCategory.CLOCK, "Set Timer"),
    SET_REMINDER(ActionCategory.CLOCK, "Set Reminder"),

    // Phone / Communication
    OPEN_DIALER(ActionCategory.COMMUNICATION, "Open Dialer"),
    CALL_PHONE(ActionCategory.COMMUNICATION, "Make Call"),
    OPEN_CONTACTS(ActionCategory.COMMUNICATION, "Open Contacts"),
    OPEN_CALL_LOG(ActionCategory.COMMUNICATION, "Open Call Log"),
    COMPOSE_SMS(ActionCategory.COMMUNICATION, "Send SMS"),

    // System
    TOGGLE_FLASHLIGHT(ActionCategory.SYSTEM, "Flashlight"),
    SET_VOLUME(ActionCategory.SYSTEM, "Volume Control"),
    OPEN_SETTINGS(ActionCategory.SYSTEM, "System Settings"),
    OPEN_WIFI_SETTINGS(ActionCategory.SYSTEM, "Wi-Fi Settings"),
    OPEN_BLUETOOTH_SETTINGS(ActionCategory.SYSTEM, "Bluetooth Settings"),
    OPEN_BATTERY_SETTINGS(ActionCategory.SYSTEM, "Battery Settings"),
    OPEN_DISPLAY_SETTINGS(ActionCategory.SYSTEM, "Display Settings"),
    OPEN_SOUND_SETTINGS(ActionCategory.SYSTEM, "Sound Settings"),
    OPEN_ASSISTANT_SETTINGS(ActionCategory.SYSTEM, "Default Assistant Settings"),

    // Apps
    OPEN_APP(ActionCategory.UTILITY, "Open App"),

    // Media
    MEDIA_PLAY_PAUSE(ActionCategory.MEDIA, "Play / Pause Media"),
    MEDIA_NEXT(ActionCategory.MEDIA, "Next Track"),
    MEDIA_PREVIOUS(ActionCategory.MEDIA, "Previous Track"),

    // Navigation & Web
    OPEN_MAPS(ActionCategory.NAVIGATION, "Open Maps"),
    NAVIGATE_TO(ActionCategory.NAVIGATION, "Navigate"),
    WEB_SEARCH(ActionCategory.UTILITY, "Web Search"),

    // Calculator
    CALCULATOR(ActionCategory.CALCULATION, "Calculator"),

    // Notifications
    READ_NOTIFICATIONS(ActionCategory.UTILITY, "Read Notifications"),

    // Memory
    SAVE_MEMORY(ActionCategory.UTILITY, "Save Memory"),
    CLEAR_MEMORY(ActionCategory.UTILITY, "Clear Memory"),

    // AI / General
    AI_QUERY(ActionCategory.AI_CONVERSATION, "AI Query"),
    UNKNOWN(ActionCategory.AI_CONVERSATION, "Unknown")
}

data class ParsedCommand(
    val actionType: ActionType,
    val rawQuery: String,
    val parameters: Map<String, String> = emptyMap(),
    val directAnswer: String? = null,
    val requiresInternet: Boolean = false,
    val requiresPermission: String? = null
)

sealed class ActionResult {
    data class Success(val message: String, val details: String? = null) : ActionResult()
    data class PermissionNeeded(val permission: String, val rationale: String) : ActionResult()
    data class SecurityRestriction(val message: String, val alternativeAction: (() -> Unit)? = null) : ActionResult()
    data class Failed(val reason: String) : ActionResult()
}
