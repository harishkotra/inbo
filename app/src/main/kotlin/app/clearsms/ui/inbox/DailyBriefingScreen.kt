package app.clearsms.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.clearsms.R
import app.clearsms.data.repository.FinanceRepository
import app.clearsms.data.repository.MessageRepository
import app.clearsms.ui.common.rememberDisplayLocale
import app.clearsms.ui.components.PipIllustration
import app.clearsms.ui.components.TooltipIconButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class DailyBriefing(
    val unreadConversations: Int,
    val remindersToday: Int,
)

@HiltViewModel
class DailyBriefingViewModel
    @Inject
    constructor(
        messageRepository: MessageRepository,
        financeRepository: FinanceRepository,
    ) : ViewModel() {
        private val zone = ZoneId.systemDefault()
        val date: LocalDate = LocalDate.now(zone)
        private val startOfToday = date.atStartOfDay(zone).toInstant().toEpochMilli()
        private val startOfTomorrow = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val briefing: StateFlow<DailyBriefing?> =
            combine(
                messageRepository.observeUnreadCounts(),
                financeRepository.observeUpcomingReminders(startOfToday),
            ) { counts, reminders ->
                DailyBriefing(
                    unreadConversations = counts.sumOf { it.count },
                    remindersToday = reminders.count { reminder -> reminder.dueDate?.let { it < startOfTomorrow } == true },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBriefingScreen(
    onBack: () -> Unit,
    viewModel: DailyBriefingViewModel = hiltViewModel(),
) {
    val briefing by viewModel.briefing.collectAsStateWithLifecycle()
    val locale = rememberDisplayLocale()
    val countFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val dateLabel =
        remember(viewModel.date, locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(viewModel.date)
        }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_briefing_title)) },
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PipIllustration(R.drawable.pip_reading, modifier = Modifier.size(112.dp))
            Text(dateLabel, style = MaterialTheme.typography.titleMedium)
            val summary = briefing
            if (summary == null) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp))
            } else {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.daily_briefing_unread)) },
                    trailingContent = { Text(countFormat.format(summary.unreadConversations)) },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.daily_briefing_due)) },
                    trailingContent = { Text(countFormat.format(summary.remindersToday)) },
                )
            }
        }
    }
}
