package app.clearsms.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** A bookmark pointing to the original message, with no copy of its contents. */
@Entity(
    tableName = "saved_messages",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SavedMessageEntity(
    @PrimaryKey val messageId: Long,
    val savedAt: Long,
)
