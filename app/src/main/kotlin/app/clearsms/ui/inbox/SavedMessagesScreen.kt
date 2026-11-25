package app.clearsms.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.clearsms.R
import app.clearsms.mms.MmsSnippet
import app.clearsms.ui.common.RelativeTime
import app.clearsms.ui.common.rememberRelativeTimeStrings
import app.clearsms.ui.components.EmptyState
import app.clearsms.ui.components.MessageTextDirection
import app.clearsms.ui.components.PagedRowPlaceholder
import app.clearsms.ui.components.SenderAvatar
import app.clearsms.ui.components.TooltipIconButton
import app.clearsms.ui.components.awaitingFirstPage
import app.clearsms.ui.components.brandGlyphFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMessagesScreen(
    onBack: () -> Unit,
    onOpenMessage: (threadId: Long, messageId: Long) -> Unit,
    viewModel: SavedMessagesViewModel = hiltViewModel(),
) {
    val items = viewModel.messages.collectAsLazyPagingItems()
    val richAvatars by viewModel.richAvatars.collectAsStateWithLifecycle(initialValue = true)
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_messages_title)) },
                navigationIcon = {
                    TooltipIconButton(
                        label = stringResource(R.string.action_back),
                        onClick = onBack,
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    )
                },
            )
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        val refresh = items.loadState.refresh
        when {
            items.itemCount == 0 && refresh is LoadState.NotLoading -> {
                EmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = stringResource(R.string.saved_messages_empty),
                    subtitle = stringResource(R.string.saved_messages_empty_hint),
                    illustration = R.drawable.pip_favorite,
                    modifier = Modifier.padding(padding),
                )
            }

            items.itemCount == 0 && refresh is LoadState.Error -> {
                Box(content, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.saved_messages_load_failed))
                        TextButton(onClick = items::retry) { Text(stringResource(R.string.action_retry)) }
                    }
                }
            }

            listState.awaitingFirstPage(items) || items.itemCount == 0 -> {
                Box(content, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(state = listState, modifier = content) {
                    items(count = items.itemCount, key = items.itemKey { it.message.id }) { index ->
                        val item = items[index]
                        if (item == null) {
                            PagedRowPlaceholder()
                        } else {
                            SavedMessageRow(
                                item = item,
                                richAvatars = richAvatars,
                                onOpen = { onOpenMessage(item.message.threadId, item.message.id) },
                                onRemove = { viewModel.remove(item.message.id) },
                            )
                        }
                    }
                    if (items.loadState.append is LoadState.Error) {
                        item(key = "retry") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                TextButton(onClick = items::retry) { Text(stringResource(R.string.action_retry)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedMessageRow(
    item: SavedMessageItem,
    richAvatars: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val message = item.message
    ListItem(
        modifier = Modifier.clickable(onClick = onOpen),
        leadingContent = {
            SenderAvatar(
                name = item.display.name,
                richAvatars = richAvatars,
                photoUri = item.display.photoUri,
                isKnownSender = item.display.isKnownSender,
                glyph = brandGlyphFor(message.subCategory, item.display.name),
            )
        },
        headlineContent = {
            Text(item.display.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                Text(
                    text = MmsSnippet.overrideRes(message)?.let { stringResource(it) } ?: message.body,
                    style = MaterialTheme.typography.bodyMedium.merge(MessageTextDirection.style),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = RelativeTime.format(message.timestamp, rememberRelativeTimeStrings()),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        trailingContent = {
            TooltipIconButton(
                label = stringResource(R.string.action_remove_saved_message),
                onClick = onRemove,
                icon = Icons.Filled.Bookmark,
            )
        },
    )
}
