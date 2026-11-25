package app.clearsms.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Decorative artwork. The surrounding text and controls describe the state. */
@Composable
internal fun PipIllustration(
    @DrawableRes drawable: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
