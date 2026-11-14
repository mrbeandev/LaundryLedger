package dev.mrbean.laundryledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import dev.mrbean.laundryledger.data.ClothType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ClothIcon(type: ClothType, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(type.icon),
        contentDescription = type.label,
        modifier = modifier.size(size),
    )
}

@Composable
fun ClothBubble(type: ClothType, size: Dp, bg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) { ClothIcon(type, size * 0.72f) }
}

/** Number that rolls up/down when it changes. */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            } else {
                (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
            }
        },
        label = "rolling",
    ) { Text(it.toString(), style = style, color = color) }
}

/** Springy press-scale feedback for any clickable using the given interaction source. */
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.93f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

@Composable
fun rememberSource() = remember { MutableInteractionSource() }

private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

fun Long.toLocal() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

fun friendlyDay(millis: Long): String {
    val d = millis.toLocal().toLocalDate()
    val today = LocalDate.now()
    return when (ChronoUnit.DAYS.between(d, today)) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> dayFmt.format(d)
    }
}

fun friendlyDateTime(millis: Long): String = "${friendlyDay(millis)}, ${timeFmt.format(millis.toLocal())}"

fun daysAgo(millis: Long): String {
    val n = ChronoUnit.DAYS.between(millis.toLocal().toLocalDate(), LocalDate.now())
    return when (n) {
        0L -> "Given today"
        1L -> "Given yesterday"
        else -> "Given $n days ago"
    }
}

fun pieces(n: Int) = if (n == 1) "1 piece" else "$n pieces"
