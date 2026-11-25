package app.clearsms.ui.components

import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.clearsms.R
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun OtpExpiryLabel(expiresAt: Long) {
    val context = LocalContext.current
    var expired by remember(expiresAt) { mutableStateOf(System.currentTimeMillis() >= expiresAt) }
    LaunchedEffect(expiresAt) {
        delay((expiresAt - System.currentTimeMillis()).coerceAtLeast(0))
        expired = true
    }
    val label =
        if (expired) {
            stringResource(R.string.otp_validity_expired)
        } else {
            val expiry = DateFormat.getTimeFormat(context).format(Date(expiresAt))
            stringResource(R.string.otp_validity_expires_at, expiry)
        }
    Text(label, style = MaterialTheme.typography.labelSmall)
}
