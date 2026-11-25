// Modified for Inbo from Clear SMS. See LICENSE and NOTICE.

package app.clearsms.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.clearsms.data.prefs.MessageCleanup
import app.clearsms.data.prefs.SettingsRepository
import app.clearsms.data.repository.MessageRepository
import app.clearsms.domain.model.Category
import app.clearsms.domain.model.OtpAutoDeletePolicy
import app.clearsms.sms.DefaultSmsAppHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Applies the user's message retention policies to the local index and SMS provider.
 * Keeps the existing worker identity so upgrades reuse the periodic schedule.
 */
@HiltWorker
class OtpAutoDeleteWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val messageRepository: MessageRepository,
        private val settingsRepository: SettingsRepository,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            if (!DefaultSmsAppHelper.isDefaultSmsApp(applicationContext)) return Result.success()

            val now = System.currentTimeMillis()
            val organizer = settingsRepository.organizerPreferences.first()
            val policies =
                mapOf(
                    Category.OTP to settingsRepository.otpAutoDeletePolicy.first(),
                    Category.PROMOTIONAL to organizer.cleanup(MessageCleanup.PROMOTIONS),
                    null to organizer.cleanup(MessageCleanup.BLOCKED),
                )
            val cutoffs = policies.mapNotNull { (category, policy) -> cutoffFor(policy, now)?.let { category to it } }.toMap()
            messageRepository.deleteOlderMessages(cutoffs)
            return Result.success()
        }

        companion object {
            const val WORK_NAME = "otp_auto_delete"

            /**
             * Timestamp before which OTP messages should be deleted, or null
             * when the policy is [OtpAutoDeletePolicy.NEVER].
             */
            fun cutoffFor(
                policy: OtpAutoDeletePolicy,
                nowMs: Long,
            ): Long? =
                when (policy) {
                    OtpAutoDeletePolicy.NEVER -> null
                    OtpAutoDeletePolicy.HOURS_24 -> nowMs - TimeUnit.HOURS.toMillis(24)
                    OtpAutoDeletePolicy.DAYS_3 -> nowMs - TimeUnit.DAYS.toMillis(3)
                    OtpAutoDeletePolicy.DAYS_7 -> nowMs - TimeUnit.DAYS.toMillis(7)
                    OtpAutoDeletePolicy.MONTH_1 -> nowMs - TimeUnit.DAYS.toMillis(30)
                    OtpAutoDeletePolicy.MONTHS_3 -> nowMs - TimeUnit.DAYS.toMillis(90)
                }
        }
    }
