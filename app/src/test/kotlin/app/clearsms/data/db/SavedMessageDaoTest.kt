package app.clearsms.data.db

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.clearsms.domain.model.Category
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedMessageDaoTest {
    private lateinit var db: ClearSmsDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ClearSmsDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `bookmarks retain save order and can be removed without deleting messages`() =
        runBlocking<Unit> {
            val messages = db.messageDao()
            val saved = db.savedMessageDao()
            messages.insert(message(1))
            messages.insert(message(2))

            assertThat(saved.save(1, 1000)).isEqualTo(1L)
            assertThat(saved.save(2, 2000)).isEqualTo(2L)
            assertThat(saved.save(1, 3000)).isEqualTo(-1L)
            assertThat(savedIds()).containsExactly(2L, 1L).inOrder()
            assertThat(saved.observeSavedIds(threadId = 9).first()).containsExactly(1L, 2L)

            assertThat(saved.remove(1)).isEqualTo(1)
            assertThat(savedIds()).containsExactly(2L)
            assertThat(messages.messageIdsInThread(threadId = 9)).containsExactly(1L, 2L)
        }

    @Test
    fun `trash hides a bookmark until restore and permanent delete removes it`() =
        runBlocking<Unit> {
            val messages = db.messageDao()
            val saved = db.savedMessageDao()
            messages.insert(message(1))
            saved.save(1, 1000)

            messages.stageDelete(listOf(1), 2000)
            assertThat(savedIds()).isEmpty()
            assertThat(saved.save(1, 3000)).isEqualTo(-1L)

            messages.restoreRow(id = 1, systemSmsId = 80)
            assertThat(savedIds()).containsExactly(1L)

            messages.deleteByIds(listOf(1))
            messages.insert(message(1))
            assertThat(savedIds()).isEmpty()
        }

    private suspend fun savedIds(): List<Long> {
        val page =
            db
                .savedMessageDao()
                .pagingSaved()
                .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
                as PagingSource.LoadResult.Page<Int, MessageEntity>
        return page.data.map { it.id }
    }

    private fun message(id: Long) =
        MessageEntity(
            id = id,
            threadId = 9,
            sender = "AX-TEST",
            normalizedSender = "TEST",
            body = "Receipt $id",
            timestamp = 1000,
            category = Category.IMPORTANT,
        )
}
