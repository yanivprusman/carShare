package com.automatelinux.carShare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.carShare.pricing.Company
import com.automatelinux.carShare.pricing.GoToCar
import com.automatelinux.carShare.pricing.PRESETS
import com.automatelinux.carShare.pricing.Quote
import com.automatelinux.carShare.pricing.Rates
import com.automatelinux.carShare.pricing.Trip
import com.automatelinux.carShare.pricing.quoteAll
import com.automatelinux.carShare.ui.theme.AppTheme
import com.automatelinux.carShare.ui.theme.Ink
import com.automatelinux.carShare.ui.theme.Muted
import com.automatelinux.carShare.ui.theme.Page
import com.automatelinux.carShare.ui.theme.Teal
import com.automatelinux.carShare.ui.theme.TealSoft
import com.automatelinux.carShare.ui.theme.Orange
import com.automatelinux.carShare.ui.theme.OrangeSoft
import com.automatelinux.carShare.ui.theme.Track
import kotlin.math.abs
import kotlin.math.roundToInt

private val DURATIONS = listOf(
    10, 15, 20, 25, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360, 480, 600, 720,
    1440, 2160, 2880, 4320,
)

@Composable
fun App() {
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            CompareScreen()
        }
    }
}

@Composable
private fun CompareScreen() {
    var trip by remember { mutableStateOf(PRESETS[4].trip) }
    var presetName by remember { mutableStateOf<String?>(PRESETS[4].name) }
    fun edit(t: Trip) { trip = t; presetName = null }

    val quotes = quoteAll(trip)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header()
        Verdict(quotes)
        Presets(presetName) { p -> trip = p.trip.copy(goToCar = trip.goToCar, tripsPerMonth = trip.tripsPerMonth); presetName = p.name }
        SectionTitle("Your trip")
        TripCard(trip, ::edit)
        SectionTitle("What it costs")
        Results(quotes)
        Footer()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
            .padding(20.dp),
    ) {
        Text("AutoTel or GoTo?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Set up a trip and see which hourly car costs less.",
            color = Color(0xFFCBD5E1), fontSize = 14.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Badge("AutoTel", "Per minute · leave it anywhere in TLV", Teal, Modifier.weight(1f))
            Badge("GoTo", "Per hour + km · return where you took it", Orange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Badge(name: String, text: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(text, color = Color(0xFFE2E8F0), fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun Verdict(quotes: List<Quote>) {
    val priced = quotes.filterIsInstance<Quote.Price>().sortedBy { it.total }
    val best = priced.first()
    val bestOther = priced.firstOrNull { it.plan.company != best.plan.company }
    val color = companyColor(best.plan.company)
    val close = bestOther != null && abs(bestOther.total - best.total) / bestOther.total < 0.08

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(18.dp)
            .animateContentSize(),
    ) {
        Text("CHEAPEST FOR THIS TRIP", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(best.plan.label, color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text(shekel(best.total), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(6.dp))
        val line = when {
            bestOther == null -> "Only AutoTel can do this trip: GoTo cars must go back where you took them."
            close -> "Close call: ${bestOther.plan.label} costs ${shekel(bestOther.total)}, only ${shekel(bestOther.total - best.total)} more."
            else -> "${shekel(bestOther.total - best.total)} less than ${bestOther.plan.label} (${shekel(bestOther.total)})."
        }
        Text(line, color = Ink.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun Presets(selected: String?, onPick: (com.automatelinux.carShare.pricing.Preset) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRESETS.forEach { p ->
            FilterChip(
                selected = p.name == selected,
                onClick = { onPick(p) },
                label = { Text(p.name, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White,
                    selectedContainerColor = Ink,
                    selectedLabelColor = Color.White,
                ),
                modifier = Modifier.testTag("preset-${p.name.lowercase().replace(' ', '-')}"),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

@Composable
private fun TripCard(trip: Trip, onChange: (Trip) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Total time
        val idx = DURATIONS.indexOfFirst { it >= trip.totalMinutes }.let { if (it < 0) DURATIONS.lastIndex else it }
        ValueRow("How long you have the car", duration(trip.totalMinutes))
        Slider(
            value = idx.toFloat(),
            onValueChange = { v ->
                val total = DURATIONS[v.roundToInt()]
                onChange(trip.copy(totalMinutes = total, drivingMinutes = minOf(trip.drivingMinutes, total)))
            },
            valueRange = 0f..DURATIONS.lastIndex.toFloat(),
            steps = DURATIONS.size - 2,
            colors = sliderColors(),
            modifier = Modifier.testTag("total-time"),
        )

        // Driving time — only matters for AutoTel's per-minute price inside Tel Aviv
        AnimatedVisibility(!trip.leavesTelAviv) {
            Column {
                ValueRow(
                    "Of that, driving",
                    "${duration(trip.drivingMinutes)}  ·  parked ${duration(trip.parkedMinutes)}",
                )
                Slider(
                    value = trip.drivingMinutes.toFloat(),
                    onValueChange = { v -> onChange(trip.copy(drivingMinutes = ((v / 5).roundToInt() * 5).coerceIn(0, trip.totalMinutes))) },
                    valueRange = 0f..trip.totalMinutes.toFloat().coerceAtLeast(5f),
                    colors = sliderColors(),
                    modifier = Modifier.testTag("driving-time"),
                )
            }
        }

        // Distance
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Distance", color = Muted, fontSize = 13.sp)
                Text("${trip.km} km", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Stepper("km", onMinus = { onChange(trip.copy(km = (trip.km - 1).coerceAtLeast(0))) },
                onPlus = { onChange(trip.copy(km = (trip.km + 1).coerceAtMost(600))) })
        }
        Slider(
            value = trip.km.toFloat(),
            onValueChange = { v -> onChange(trip.copy(km = v.roundToInt())) },
            valueRange = 0f..600f,
            colors = sliderColors(),
            modifier = Modifier.testTag("distance"),
        )

        Divider()
        ToggleRow("One way", "Leave the car where you end up", trip.oneWay, "one-way") { onChange(trip.copy(oneWay = it)) }
        ToggleRow("Leaves Tel Aviv", "AutoTel switches to its long-trip rate", trip.leavesTelAviv, "leaves-tel-aviv") { onChange(trip.copy(leavesTelAviv = it)) }
        ToggleRow("Weekend", "Thursday night to Saturday night (GoTo +₪4/hour)", trip.weekend, "weekend") { onChange(trip.copy(weekend = it)) }
        Divider()

        Text("GoTo car size", color = Muted, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoToCar.entries.forEach { c ->
                FilterChip(
                    selected = trip.goToCar == c,
                    onClick = { onChange(trip.copy(goToCar = c)) },
                    label = { Text("${c.label} · ₪${c.driveHour.toInt()}/h") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangeSoft,
                        selectedLabelColor = Color(0xFF9A3412),
                    ),
                    modifier = Modifier.testTag("goto-car-${c.name.lowercase()}"),
                )
            }
        }
        Divider()

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Monthly fee", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (trip.tripsPerMonth == 0) "Left out of the price"
                    else "Split over ${trip.tripsPerMonth} trip${if (trip.tripsPerMonth > 1) "s" else ""} a month",
                    color = Muted, fontSize = 12.5.sp,
                )
            }
            Stepper("trips per month",
                onMinus = { onChange(trip.copy(tripsPerMonth = (trip.tripsPerMonth - 1).coerceAtLeast(0))) },
                onPlus = { onChange(trip.copy(tripsPerMonth = (trip.tripsPerMonth + 1).coerceAtMost(60))) })
        }
    }
}

@Composable
private fun Results(quotes: List<Quote>) {
    val priced = quotes.filterIsInstance<Quote.Price>()
    val max = priced.maxOf { it.total }.coerceAtLeast(1.0)
    val cheapest = priced.minOf { it.total }
    val ordered = priced.sortedBy { it.total } + quotes.filterIsInstance<Quote.NotPossible>()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ordered.forEach { q -> ResultCard(q, max, q is Quote.Price && q.total == cheapest) }
    }
}

@Composable
private fun ResultCard(q: Quote, max: Double, isBest: Boolean) {
    var open by remember(q.plan) { mutableStateOf(false) }
    val color = companyColor(q.plan.company)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(enabled = q is Quote.Price) { open = !open }
            .testTag("result-${q.plan.name.lowercase()}")
            .padding(16.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(q.plan.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (isBest) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Cheapest", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (q.plan.company == Company.AUTOTEL) Color(0xFF115E59) else Color(0xFF9A3412),
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (q.plan.company == Company.AUTOTEL) TealSoft else OrangeSoft)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            when (q) {
                is Quote.Price -> {
                    Text(shekel(q.total), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Muted)
                }
                is Quote.NotPossible -> Text("—", color = Muted, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        when (q) {
            is Quote.Price -> {
                val frac by animateFloatAsState((q.total / max).toFloat().coerceIn(0.03f, 1f))
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Track)) {
                    Box(
                        Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(7.dp))
                            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))),
                    )
                }
                AnimatedVisibility(open) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(q.plan.note, color = Muted, fontSize = 12.5.sp)
                        q.lines.forEach { (label, amount) ->
                            Row {
                                Text(label, color = Ink, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                                Text(shekel(amount, decimals = true), color = Ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            is Quote.NotPossible -> Text(q.reason, color = Muted, fontSize = 13.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun Footer() {
    Text(
        "Fuel and insurance are included in both. Out of Tel Aviv, AutoTel charges its long-trip rate: " +
            "₪99 for 3 hours, then ₪20 an hour, plus ₪1.2/km. GoTo bills whole hours, at least one, " +
            "and a 24-hour block never costs more than its day rate. Prices: ${Rates.AUTOTEL_SOURCE}; " +
            "${Rates.GOTO_SOURCE}. The app shows the price that applies when you book.",
        color = Muted, fontSize = 11.5.sp, lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

// ── small pieces ──────────────────────────────────────────────────────────

@Composable
private fun ValueRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Muted, fontSize = 12.5.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Ink),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun Stepper(what: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.testTag("$what-minus".replace(' ', '-'))) {
            Icon(Icons.Filled.Remove, "Fewer $what")
        }
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.testTag("$what-plus".replace(' ', '-'))) {
            Icon(Icons.Filled.Add, "More $what")
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Track))
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Ink, activeTrackColor = Ink, inactiveTrackColor = Track,
    activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
)

private fun companyColor(c: Company) = if (c == Company.AUTOTEL) Teal else Orange

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
