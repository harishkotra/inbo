package app.clearsms.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMessageDao {
    @Query(
        """
        SELECT m.* FROM saved_messages s
        JOIN messages m ON m.id = s.messageId
        WHERE m.deletedAt IS NULL
        ORDER BY s.savedAt DESC, s.messageId DESC
        """,
    )
    fun pagingSaved(): PagingSource<Int, MessageEntity>

    @Query(
        """
        SELECT s.messageId FROM saved_messages s
        JOIN messages m ON m.id = s.messageId
        WHERE m.threadId = :threadId AND m.deletedAt IS NULL
        """,
    )
    fun observeSavedIds(threadId: Long): Flow<List<Long>>

    @Query(
        """
        INSERT OR IGNORE INTO saved_messages (messageId, savedAt)
        SELECT id, :savedAt FROM messages WHERE id = :messageId AND deletedAt IS NULL
        """,
    )
    suspend fun save(
        messageId: Long,
        savedAt: Long,
    ): Long

    @Query("DELETE FROM saved_messages WHERE messageId = :messageId")
    suspend fun remove(messageId: Long): Int
}
