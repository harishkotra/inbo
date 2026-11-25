package app.clearsms.data.prefs

import app.clearsms.domain.model.OtpAutoDeletePolicy

enum class OrganizerToggle(val default: Boolean) {
    SWIPE_CATEGORIES(false),
    MESSAGE_NOTIFICATIONS(true),
    PROMOTION_NOTIFICATIONS(false),
    OTP_NOTIFICATIONS(true),
    REMINDER_NOTIFICATIONS(true),
    TRAIN_NOTIFICATIONS(true),
}

enum class MessageCleanup {
    PROMOTIONS,
    BLOCKED,
}

data class OrganizerPreferences(
    val toggles: Map<OrganizerToggle, Boolean> = emptyMap(),
    val cleanupPolicies: Map<MessageCleanup, OtpAutoDeletePolicy> = emptyMap(),
) {
    fun enabled(toggle: OrganizerToggle): Boolean = toggles[toggle] ?: toggle.default

    fun cleanup(kind: MessageCleanup): OtpAutoDeletePolicy = cleanupPolicies[kind] ?: OtpAutoDeletePolicy.NEVER
}
