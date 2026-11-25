// Modified for Inbo from Clear SMS. See LICENSE and NOTICE.

package app.clearsms.domain.model

/** What a horizontal swipe on an inbox row does. */
enum class SwipeAction {
    NONE,
    TOGGLE_READ,
    DELETE,
    ARCHIVE,
    MARK_READ,
    SORT_SENDER,
}
