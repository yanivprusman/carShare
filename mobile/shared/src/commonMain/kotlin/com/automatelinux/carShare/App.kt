package com.automatelinux.carShare

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.carShare.pricing.Company
import com.automatelinux.carShare.pricing.GoToCar
import com.automatelinux.carShare.pricing.Quote
import com.automatelinux.carShare.pricing.Trip
import com.automatelinux.carShare.pricing.quoteAll
import com.automatelinux.carShare.ui.RoundButton
import com.automatelinux.carShare.ui.Segmented
import com.automatelinux.carShare.ui.Toggle
import com.automatelinux.carShare.ui.TrackSlider
import com.automatelinux.carShare.ui.theme.AppTheme
import com.automatelinux.carShare.ui.theme.Card
import com.automatelinux.carShare.ui.theme.Faint
import com.automatelinux.carShare.ui.theme.Ink
import com.automatelinux.carShare.ui.theme.Line
import com.automatelinux.carShare.ui.theme.Muted
import com.automatelinux.carShare.ui.theme.Orange
import com.automatelinux.carShare.ui.theme.OrangeDeep
import com.automatelinux.carShare.ui.theme.Page
import com.automatelinux.carShare.ui.theme.Raised
import com.automatelinux.carShare.ui.theme.Teal
import com.automatelinux.carShare.ui.theme.TealDeep
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val DURATIONS = listOf(
    10, 15, 20, 25, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360, 480, 600, 720,
    1440, 2160, 2880, 4320,
)
private const val MAX_KM = 600

/** The one question about the route. The three answers cover every case the two price lists tell apart. */
private enum class Where(val label: String) {
    IN_TOWN("In town"), ONE_WAY("One way"), OUT_OF_TOWN("Out of town");

    companion object {
        fun of(t: Trip) = when {
            t.leavesTelAviv -> OUT_OF_TOWN
            t.oneWay -> ONE_WAY
            else -> IN_TOWN
        }
    }
}

/** The current text style with tabular digits, so prices do not jitter while they animate. */
@Composable
private fun num(): TextStyle = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")

@Composable
fun App(font: FontFamily) {
    AppTheme(font) {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            CompareScreen()
        }
    }
}

/** One page: the answer, then the three things that decide it. The rarely-changed rest folds away. */
@Composable
private fun CompareScreen() {
    var trip by remember {
        mutableStateOf(Trip(totalMinutes = 60, km = 20, oneWay = false, leavesTelAviv = false, weekend = false, goToCar = GoToCar.MINI, tripsPerMonth = 0))
    }
    val haptic = LocalHapticFeedback.current
    fun change(t: Trip) {
        if (t != trip) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        trip = t
    }

    Column(
        Modifier
            .fillMaxSize()
            // Insets first, then scroll: content stops at the status bar instead of sliding under the clock.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Duel(quoteAll(trip))
        TripPanel(trip, ::change)
        More(trip, ::change)
    }
}

// ── The answer ────────────────────────────────────────────────────────────

/**
 * The two companies face off. Each side shows its cheaper plan; the winner lights up in its
 * colour, the loser dims, and one line says by how much. Tapping a side opens its sums.
 */
@Composable
private fun Duel(quotes: List<Quote>) {
    fun bestOf(c: Company): Quote =
        quotes.filter { it.plan.company == c }.filterIsInstance<Quote.Price>().minByOrNull { it.total }
            ?: quotes.first { it.plan.company == c }

    val at = bestOf(Company.AUTOTEL) as Quote.Price
    val go = bestOf(Company.GOTO) as? Quote.Price
    val close = go != null && abs(go.total - at.total) / maxOf(go.total, at.total) < 0.08
    val winner = when {
        go == null -> Company.AUTOTEL
        close -> null
        go.total < at.total -> Company.GOTO
        else -> Company.AUTOTEL
    }
    var open by remember { mutableStateOf<Company?>(null) }
    val shown = when (open) {
        Company.AUTOTEL -> at
        Company.GOTO -> go
        null -> null
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(26.dp)).padding(10.dp).animateContentSize(),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Side("AutoTel", at, Teal, TealDeep, winner, Company.AUTOTEL, open == Company.AUTOTEL, Modifier.weight(1f)) {
                open = if (open == Company.AUTOTEL) null else Company.AUTOTEL
            }
            Side("GoTo", go, Orange, OrangeDeep, winner, Company.GOTO, open == Company.GOTO, Modifier.weight(1f)) {
                open = if (open == Company.GOTO) null else Company.GOTO
            }
        }
        val verdict = when {
            go == null -> "Only AutoTel does one way"
            winner == null -> "Too close to call"
            winner == Company.GOTO -> "GoTo saves you ${shekel(at.total - go.total)}"
            else -> "AutoTel saves you ${shekel(go.total - at.total)}"
        }
        AnimatedContent(verdict, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "verdict") { v ->
            Text(
                v, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp).testTag("verdict"),
            )
        }
        if (shown != null) Receipt(shown)
    }
}

@Composable
private fun Side(
    name: String, quote: Quote.Price?, accent: Color, deep: Color,
    winner: Company?, me: Company, open: Boolean, modifier: Modifier, onTap: () -> Unit,
) {
    val won = winner == me
    val lost = winner != null && !won
    val bg by animateColorAsState(
        when { won -> deep.copy(alpha = 0.32f); lost -> Raised.copy(alpha = 0.5f); else -> Raised }, tween(300),
    )
    val border by animateColorAsState(if (won) accent else Color.Transparent, tween(300))
    val alpha by animateFloatAsState(if (lost) 0.55f else 1f, tween(300))
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(220))
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(bg)
            .border(1.5.dp, border, RoundedCornerShape(20.dp))
            .clickable(enabled = quote != null, onClick = onTap)
            .testTag("side-${name.lowercase()}")
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(accent.copy(alpha = alpha)))
            Spacer(Modifier.width(6.dp))
            Text(name, color = Ink.copy(alpha = alpha), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (quote != null) {
                Icon(
                    Icons.Filled.ExpandMore, if (open) "Hide the sums" else "Show the sums",
                    tint = Muted.copy(alpha = alpha), modifier = Modifier.size(20.dp).rotate(turn),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (quote != null) {
            val price by animateFloatAsState(quote.total.toFloat(), tween(450))
            Text(shekel(price.toDouble()), color = Ink.copy(alpha = alpha), fontSize = 36.sp, fontWeight = FontWeight.Bold, style = num())
            Text(quote.plan.short, color = Muted.copy(alpha = alpha), fontSize = 12.5.sp)
        } else {
            Text("—", color = Faint, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Round trips only", color = Muted, fontSize = 12.5.sp)
        }
    }
}

/** The sums behind one price, set like a till receipt. */
@Composable
private fun Receipt(q: Quote.Price) {
    Column(
        Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Page)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        q.lines.forEach { (label, amount) ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(label, color = Muted, fontSize = 13.5.sp)
                Canvas(Modifier.weight(1f).padding(horizontal = 6.dp).height(10.dp)) {
                    val y = size.height - 2f
                    drawLine(
                        Faint.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f)),
                    )
                }
                Text(shekel(amount, decimals = true), color = Ink, fontSize = 13.5.sp, style = num())
            }
        }
    }
}

// ── The trip ──────────────────────────────────────────────────────────────

@Composable
private fun TripPanel(trip: Trip, change: (Trip) -> Unit) {
    val ink = Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color.White))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(26.dp)).padding(18.dp),
    ) {
        Readout("Time", duration(trip.totalMinutes))
        val idx = DURATIONS.indexOfFirst { it >= trip.totalMinutes }.let { if (it < 0) DURATIONS.lastIndex else it }
        TrackSlider(
            position = idx / DURATIONS.lastIndex.toFloat(),
            onPosition = { f -> change(trip.copy(totalMinutes = DURATIONS[(f * DURATIONS.lastIndex).roundToInt()])) },
            fill = ink, tag = "total-time",
        )

        Spacer(Modifier.height(6.dp))
        Readout("Distance", "${trip.km} km")
        // Square-root scale, so a few km are as easy to set as a few hundred.
        TrackSlider(
            position = sqrt(trip.km / MAX_KM.toFloat()),
            onPosition = { f -> change(trip.copy(km = (f * f * MAX_KM).roundToInt())) },
            fill = ink, tag = "distance",
        )

        Spacer(Modifier.height(10.dp))
        Segmented(
            options = Where.entries.toList(),
            selected = Where.of(trip),
            label = { it.label },
            accent = Teal,
            tag = { "where-${it.name.lowercase().replace('_', '-')}" },
            onSelect = { change(trip.copy(oneWay = it == Where.ONE_WAY, leavesTelAviv = it == Where.OUT_OF_TOWN)) },
        )

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { change(trip.copy(weekend = !trip.weekend)) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Weekend", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Toggle(trip.weekend, Orange, "weekend") { change(trip.copy(weekend = it)) }
        }
    }
}

@Composable
private fun Readout(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, style = num())
    }
}

// ── Set once ──────────────────────────────────────────────────────────────

/** Which GoTo car, and whether the monthly fee counts: things about you, not about this trip. */
@Composable
private fun More(trip: Trip, change: (Trip) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(220))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(26.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.testTag("more").padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${trip.goToCar.label} car  ·  " + if (trip.tripsPerMonth == 0) "no monthly fee" else "${trip.tripsPerMonth} trips a month",
                color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ExpandMore, if (open) "Hide" else "Change", tint = Muted, modifier = Modifier.rotate(turn))
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Segmented(
                    options = GoToCar.entries.toList(),
                    selected = trip.goToCar,
                    label = { it.label },
                    sub = { "₪${it.driveHour.toInt()}/h" },
                    accent = Orange,
                    tag = { "goto-car-${it.name.lowercase()}" },
                    onSelect = { change(trip.copy(goToCar = it)) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trips a month", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    RoundButton(Icons.Filled.Remove, "Fewer trips a month", "trips-minus", enabled = trip.tripsPerMonth > 0) {
                        change(trip.copy(tripsPerMonth = trip.tripsPerMonth - 1))
                    }
                    Text(
                        "${trip.tripsPerMonth}", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, style = num(),
                        textAlign = TextAlign.Center, modifier = Modifier.width(40.dp),
                    )
                    RoundButton(Icons.Filled.Add, "More trips a month", "trips-plus", enabled = trip.tripsPerMonth < 60) {
                        change(trip.copy(tripsPerMonth = trip.tripsPerMonth + 1))
                    }
                }
            }
        }
    }
}

// ── small pieces ──────────────────────────────────────────────────────────

private fun duration(min: Int): String = when {
    min < 60 -> "$min min"
    min % 1440 == 0 -> "${min / 1440} day${if (min >= 2880) "s" else ""}"
    min % 60 == 0 -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60} min"
}

private fun shekel(v: Double, decimals: Boolean = false): String =
    if (decimals && v != v.roundToInt().toDouble()) {
        val cents = (v * 100).roundToInt()
        "₪${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
    } else "₪${v.roundToInt()}"
