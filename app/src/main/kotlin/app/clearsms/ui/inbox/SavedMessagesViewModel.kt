package app.clearsms.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import app.clearsms.data.db.MessageEntity
import app.clearsms.data.db.SavedMessageDao
import app.clearsms.data.prefs.SettingsRepository
import app.clearsms.di.IoDispatcher
import app.clearsms.domain.categorizer.SenderIdLookup
import app.clearsms.sms.ContactsSource
import app.clearsms.ui.components.SenderDisplay
import app.clearsms.ui.components.resolveSenderDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class SavedMessageItem(
    val message: MessageEntity,
    val display: SenderDisplay,
)

@HiltViewModel
class SavedMessagesViewModel
    @Inject
    constructor(
        private val savedMessageDao: SavedMessageDao,
        contactsSource: ContactsSource,
        senderIdLookup: SenderIdLookup,
        settings: SettingsRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val displayCache = ConcurrentHashMap<String, SenderDisplay>()

        val richAvatars = settings.showRichAvatars

        val messages =
            Pager(PagingConfig(pageSize = 30), pagingSourceFactory = savedMessageDao::pagingSaved)
                .flow
                .map { page ->
                    page.map { message ->
                        val display =
                            displayCache.getOrPut(message.sender) {
                                resolveSenderDisplay(
                                    sender = message.sender,
                                    contactLookup = contactsSource::lookup,
                                    directoryLookup = { senderIdLookup.lookup(it)?.name },
                                )
                            }
                        SavedMessageItem(message, display)
                    }
                }.flowOn(ioDispatcher)
                .cachedIn(viewModelScope)

        fun remove(messageId: Long) {
            viewModelScope.launch(ioDispatcher) {
                savedMessageDao.remove(messageId)
            }
        }
    }
