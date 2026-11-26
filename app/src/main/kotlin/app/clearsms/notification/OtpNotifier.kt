// Modified for Inbo from Clear SMS. See LICENSE and NOTICE.

package app.clearsms.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TtsSpan
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.clearsms.ConversationDeepLink
import app.clearsms.R
import app.clearsms.data.db.MessageEntity
import app.clearsms.domain.model.NotificationAction
import app.clearsms.domain.model.OtpDisplaySize
import app.clearsms.domain.model.StartDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Heads-up notification for a received OTP.
 *
 * Custom views size the code independently of the sender line. Android
 * strips font-size spans from the fallback title, so that title uses bold
 * digits and a speech span for surfaces that render their own template.
 * Copy is always available in the system actions. Other actions honor
 * [NotificationActionPlanner.forOtp].
 *
 * LOCKSCREEN: the OTP digits are the title, so the notification is
 * [NotificationCompat.VISIBILITY_PRIVATE] with a digit-free public version
 * ("New OTP from <sender>"). This is the default with no setting - leaking
 * codes to anyone who can see the locked screen defeats the point of an OTP,
 * so private-by-default is the safer choice.
 */
@Singleton
class OtpNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val senderResolver: NotificationSenderResolver,
        private val iconFactory: SenderIconFactory,
        private val sectionGate: NotificationSectionGate,
        private val mutedSenderGate: MutedSenderGate,
    ) {
        suspend fun notify(
            message: MessageEntity,
            otp: String,
            displaySize: OtpDisplaySize,
            selected: Set<NotificationAction> = MessageNotifier.DEFAULT_SELECTED,
        ) {
            // An OTP is a MESSAGE (OTP is an inbox category), so its
            // notification follows the Inbox flag - the operator's "any
            // incoming message notification" includes OTPs.
            if (!sectionGate.allows(StartDestination.INBOX)) return
            // A muted sender's OTP is quiet too - "mute means quiet" is the
            // predictable contract; the code stays visible and copyable
            // in-app (MutedSenderGate argues the trade-off).
            if (!mutedSenderGate.allows(message.sender)) return
            Channels.ensureCreated(context)
            try {
                NotificationManagerCompat
                    .from(context)
                    .notify(notificationId(message.id), build(message, otp, displaySize, selected))
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted; onboarding asks for it.
            }
        }

        /** Builds the notification; internal so tests can inspect it without posting. */
        internal fun build(
            message: MessageEntity,
            otp: String,
            displaySize: OtpDisplaySize,
            selected: Set<NotificationAction>,
        ): Notification {
            val title = buildTitle(otp)
            // Same resolution chain as the UI (contact → directory → brand → raw).
            val resolved = senderResolver.resolve(message.sender)
            val senderName = resolved.name
            // Sender identity (logo/photo/tile) carries no digits, so it is
            // safe on the lockscreen public version too.
            val largeIcon = iconFactory.largeIconFor(resolved)
            val publicVersion =
                NotificationCompat
                    .Builder(context, Channels.OTP)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(largeIcon)
                    // Digit-free on purpose: no OTP on the lockscreen.
                    .setContentTitle(context.getString(R.string.otp_public_title, senderName))
                    .build()
            val builder =
                NotificationCompat
                    .Builder(context, Channels.OTP)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(largeIcon)
                    // Title/text stay set for accessibility services and
                    // surfaces that ignore custom views (e.g. wearables).
                    .setContentTitle(title)
                    .setContentText(context.getString(R.string.otp_from, senderName))
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(codeView(R.layout.notification_otp, title, senderName, displaySize))
                    .setCustomHeadsUpContentView(codeView(R.layout.notification_otp, title, senderName, displaySize))
                    .setCustomBigContentView(
                        codeView(R.layout.notification_otp_big, title, senderName, displaySize)
                            .apply { setTextViewText(R.id.otp_body, message.body) },
                    ).setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicVersion)
                    // Tap opens the conversation at the OTP message - the same
                    // convention every other notifier follows (GitHub issue #8
                    // family: a notification whose tap does nothing reads as
                    // broken even when its actions work).
                    .setContentIntent(conversationIntent(message))
                    .setAutoCancel(true)
            for (action in NotificationActionPlanner.forOtp(selected)) {
                when (action) {
                    NotificationAction.COPY_OTP -> {
                        builder.addAction(
                            R.drawable.ic_notification_copy,
                            context.getString(R.string.action_copy),
                            otpAction(OtpActionReceiver.ACTION_COPY, message, otp),
                        )
                    }

                    NotificationAction.SHARE_OTP -> {
                        builder.addAction(
                            0,
                            context.getString(R.string.action_share),
                            otpAction(OtpActionReceiver.ACTION_SHARE, message, otp),
                        )
                    }

                    NotificationAction.DELETE -> {
                        builder.addAction(
                            0,
                            context.getString(R.string.action_delete),
                            otpAction(OtpActionReceiver.ACTION_DELETE, message, otp),
                        )
                    }

                    NotificationAction.MARK_READ -> {
                        MessageActionFactory
                            .build(context, message, notificationId(message.id), listOf(NotificationAction.MARK_READ))
                            .forEach(builder::addAction)
                    }

                    // Planner never emits REPLY, CALL or generic SHARE for OTP
                    // notifications (an OTP is a code to copy, not a person to
                    // answer or phone; OTP has its own SHARE_OTP action above).
                    NotificationAction.REPLY -> {
                        Unit
                    }

                    NotificationAction.CALL -> {
                        Unit
                    }

                    NotificationAction.SHARE -> {
                        Unit
                    }
                }
            }
            return builder.build()
        }

        /** Inflates one of the custom layouts with the code at the chosen size and the sender line. */
        private fun codeView(
            @LayoutRes layout: Int,
            code: CharSequence,
            senderName: String,
            displaySize: OtpDisplaySize,
        ): RemoteViews =
            RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.otp_code, code)
                setTextViewTextSize(R.id.otp_code, TypedValue.COMPLEX_UNIT_SP, notificationFontSp(displaySize).toFloat())
                setTextViewText(R.id.otp_sender, context.getString(R.string.otp_from, senderName))
            }

        fun cancel(messageId: Long) {
            NotificationManagerCompat.from(context).cancel(notificationId(messageId))
        }

        private fun otpAction(
            action: String,
            message: MessageEntity,
            otp: String,
        ): PendingIntent {
            val intent =
                Intent(context, OtpActionReceiver::class.java)
                    .setAction(action)
                    .putExtra(OtpActionReceiver.EXTRA_MESSAGE_ID, message.id)
                    .putExtra(OtpActionReceiver.EXTRA_OTP, otp)
            val requestOffset =
                when (action) {
                    OtpActionReceiver.ACTION_COPY -> 0
                    OtpActionReceiver.ACTION_SHARE -> 1
                    else -> 2
                }
            return PendingIntent.getBroadcast(
                context,
                ((message.id % 100_000) * 4 + requestOffset).toInt(),
                intent,
                NotificationIntents.flags(),
            )
        }

        private fun notificationId(messageId: Long) = NotificationIds.otp(messageId)

        /**
         * Deep link into the conversation, scrolled to the OTP message -
         * mirrors [TransactionNotifier.contentIntent] (explicit component so
         * no other app claiming the scheme can intercept it).
         */
        private fun conversationIntent(message: MessageEntity): PendingIntent {
            val intent =
                ConversationDeepLink
                    .intent(context, message.threadId, message.id)
                    .putExtra(MessageNotifier.EXTRA_THREAD_ID, message.threadId)
                    .putExtra(MessageActionReceiver.EXTRA_MESSAGE_ID, message.id)
            return PendingIntent.getActivity(
                context,
                notificationId(message.id),
                intent,
                NotificationIntents.flags(),
            )
        }

        companion object {
            internal fun buildTitle(otp: String): CharSequence =
                SpannableString(otp).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(TtsSpan.DigitsBuilder(otp).build(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

            /** Code size in sp inside the custom notification views. */
            internal fun notificationFontSp(displaySize: OtpDisplaySize): Int =
                when (displaySize) {
                    OtpDisplaySize.OPTION_1 -> 16
                    OtpDisplaySize.OPTION_2 -> 24
                    OtpDisplaySize.OPTION_3 -> 26
                    OtpDisplaySize.OPTION_4 -> 28
                    OtpDisplaySize.OPTION_5 -> 30
                }
        }
    }
