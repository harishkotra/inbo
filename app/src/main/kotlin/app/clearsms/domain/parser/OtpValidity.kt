package app.clearsms.domain.parser

/** Reads an explicitly stated relative validity window from an OTP message. */
object OtpValidity {
    private val durationPattern =
        Regex(
            """\b(?:valid\s+for|expires?\s+in|will\s+expire\s+in)\s+(\d{1,3})\s*-?\s*""" +
                """(seconds?|secs?|minutes?|mins?|hours?|hrs?)\b""",
            RegexOption.IGNORE_CASE,
        )

    /** Uses the sent time when available. A missing or ambiguous window has no deadline. */
    fun expiresAt(
        body: String,
        receivedAt: Long,
        sentAt: Long?,
    ): Long? {
        val match = durationPattern.findAll(body).singleOrNull() ?: return null
        val amount = match.groupValues[1].toLong().takeIf { it > 0 } ?: return null
        val unit = match.groupValues[2].lowercase()
        val millisPerUnit =
            when (unit.first()) {
                's' -> 1_000L
                'm' -> 60_000L
                else -> 3_600_000L
            }
        val start = sentAt?.takeIf { it in 1..receivedAt } ?: receivedAt
        return start + amount * millisPerUnit
    }
}
