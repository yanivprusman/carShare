package com.automatelinux.carShare.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.carShare.ui.theme.Faint
import com.automatelinux.carShare.ui.theme.Ink
import com.automatelinux.carShare.ui.theme.Line
import com.automatelinux.carShare.ui.theme.Muted
import com.automatelinux.carShare.ui.theme.Page
import com.automatelinux.carShare.ui.theme.Raised

/**
 * A slider drawn from scratch: a thick track, a filled part, and a white thumb.
 * [position] and [onPosition] are 0..1; the caller snaps to its own steps and the
 * phone ticks (haptic) each time the snapped value changes.
 * [rest] optionally paints the unfilled part (used to show "parked" time).
 */
@Composable
fun TrackSlider(
    position: Float,
    onPosition: (Float) -> Unit,
    fill: Brush,
    tag: String,
    rest: Color = Line,
    modifier: Modifier = Modifier,
) {
    val cb by rememberUpdatedState(onPosition)
    val shown by animateFloatAsState(position.coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessMediumLow))
    Canvas(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .testTag(tag)
            .pointerInput(Unit) {
                val r = 13.dp.toPx()
                detectTapGestures { cb(((it.x - r) / (size.width - 2 * r)).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                val r = 13.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    cb(((change.position.x - r) / (size.width - 2 * r)).coerceIn(0f, 1f))
                }
            },
    ) {
        val r = 13.dp.toPx()
        val h = 10.dp.toPx()
        val y = size.height / 2
        val usable = size.width - 2 * r
        val x = r + usable * shown
        drawRoundRect(rest, Offset(r, y - h / 2), Size(usable, h), CornerRadius(h / 2))
        drawRoundRect(fill, Offset(r, y - h / 2), Size((x - r).coerceAtLeast(h), h), CornerRadius(h / 2))
        drawCircle(Color.Black.copy(alpha = 0.35f), r + 1.5f, Offset(x, y + 2.5f))
        drawCircle(Color.White, r, Offset(x, y))
        drawCircle(Page, r * 0.32f, Offset(x, y))
    }
}

/** On/off control, drawn to match the app instead of Material's default. */
@Composable
fun Toggle(checked: Boolean, accent: Color, tag: String, onChange: (Boolean) -> Unit) {
    val knob by animateDpAsState(if (checked) 22.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
    val track by animateColorAsState(if (checked) accent else Raised, tween(180))
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, if (checked) Color.Transparent else Line, CircleShape)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .testTag(tag)
            .padding(3.dp),
    ) {
        Box(
            Modifier.offset(x = knob).size(24.dp).shadow(3.dp, CircleShape).clip(CircleShape)
                .background(if (checked) Page else Muted),
        )
    }
}

/** Segmented choice with a sliding highlight. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    sub: (T) -> String,
    accent: Color,
    tag: (T) -> String,
    onSelect: (T) -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Raised).padding(4.dp),
    ) {
        val w = maxWidth / options.size
        val idx = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(w * idx, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
        Box(
            Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f)).border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { o ->
                val on = o == selected
                Box(
                    Modifier.width(w).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(o) }
                        .testTag(tag(o)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label(o), color = if (on) Ink else Muted, fontSize = 13.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium)
                        Text(sub(o), color = if (on) accent else Faint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/** Round −/+ button. */
@Composable
fun RoundButton(icon: ImageVector, description: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Raised).border(1.dp, Line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (enabled) Ink else Faint, modifier = Modifier.size(20.dp))
    }
}
