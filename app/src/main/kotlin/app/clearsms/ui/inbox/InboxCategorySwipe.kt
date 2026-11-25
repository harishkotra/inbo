package app.clearsms.ui.inbox

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.clearsms.domain.model.InboxPill
import kotlin.math.abs

@Composable
internal fun Modifier.swipeInboxCategories(
    visible: List<InboxPill>,
    current: InboxPill?,
    onSelect: (InboxPill?) -> Unit,
): Modifier {
    val categories = remember(visible) { listOf(null) + visible }
    val index = categories.indexOf(current).coerceAtLeast(0)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val distance = with(density) { 72.dp.toPx() }
    val flingVelocity = with(density) { 550.dp.toPx() }
    var offset by remember(index) { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf(false) }
    val drag = rememberDraggableState { delta ->
        val proposed = offset + delta
        val next = index + categorySwipeStep(proposed, rtl)
        offset =
            if (next in categories.indices) {
                proposed.coerceIn(-distance * 2, distance * 2)
            } else {
                (offset + delta * 0.2f).coerceIn(-distance / 3, distance / 3)
            }
    }
    return clipToBounds()
        .draggable(
            state = drag,
            orientation = Orientation.Horizontal,
            startDragImmediately = settling,
            onDragStopped = { velocity ->
                val next = index + categorySwipeStep(offset, rtl)
                val fling = abs(velocity) >= flingVelocity && velocity * offset > 0
                if (offset != 0f && next in categories.indices && (abs(offset) >= distance || fling)) {
                    onSelect(categories[next])
                    offset = 0f
                } else {
                    settling = true
                    try {
                        drag.drag {
                            animate(
                                initialValue = offset,
                                targetValue = 0f,
                                initialVelocity = velocity.coerceIn(-flingVelocity, flingVelocity),
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            ) { value, _ -> offset = value }
                        }
                    } finally {
                        settling = false
                    }
                }
            },
        ).graphicsLayer { translationX = offset }
}

internal fun categorySwipeStep(offset: Float, rtl: Boolean): Int =
    if ((offset < 0) != rtl) 1 else -1
