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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.automatelinux.carShare.pricing.Company
import com.automatelinux.carShare.pricing.GoToCar
import com.automatelinux.carShare.pricing.PRESETS
import com.automatelinux.carShare.pricing.Preset
import com.automatelinux.carShare.pricing.Quote
import com.automatelinux.carShare.pricing.Rates
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

/** The current text style with tabular digits, so prices do not jitter while they animate. */
@Composable
private fun num(): TextStyle = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")

private val PRESET_ICONS: Map<String, ImageVector> = mapOf(
    "Quick hop" to Icons.Filled.Bolt,
    "To a meeting" to Icons.Filled.Work,
    "Pick-up and back" to Icons.Filled.Inventory2,
    "Supermarket" to Icons.Filled.ShoppingCart,
    "Errand" to Icons.Filled.Assignment,
    "Evening out" to Icons.Filled.Restaurant,
    "Out of town" to Icons.Filled.Landscape,
    "Jerusalem" to Icons.Filled.LocationCity,
    "Day up north" to Icons.Filled.Terrain,
    "Day trip" to Icons.Filled.WbSunny,
    "Weekend away" to Icons.Filled.Weekend,
)

@Composable
fun App(font: FontFamily) {
    AppTheme(font) {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            CompareScreen()
        }
    }
}

@Composable
private fun CompareScreen() {
    val start = PRESETS.indexOfFirst { it.name == "Errand" }
    var trip by remember { mutableStateOf(PRESETS[start].trip) }
    var preset by remember { mutableStateOf<String?>(PRESETS[start].name) }
    fun edit(t: Trip) { trip = t; preset = null }

    val quotes = quoteAll(trip)

    Column(
        Modifier
            .fillMaxSize()
            // Insets first, then scroll: content stops at the status bar instead of sliding under the clock.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar()
        Duel(quotes, trip, Modifier.padding(horizontal = 16.dp))
        Presets(preset) { p ->
            trip = p.trip.copy(goToCar = trip.goToCar, tripsPerMonth = trip.tripsPerMonth)
            preset = p.name
        }
        TripPanel(trip, ::edit, Modifier.padding(horizontal = 16.dp))
        SectionTitle("Every plan, cheapest first")
        AllPlans(quotes, Modifier.padding(horizontal = 16.dp))
        Footer()
    }
}

// ── Top ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Car Share", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(Teal); Spacer(Modifier.width(5.dp))
                Text("AutoTel", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("  vs  ", color = Faint, fontSize = 13.sp)
                Dot(Orange); Spacer(Modifier.width(5.dp))
                Text("GoTo", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("  ·  by the hour", color = Faint, fontSize = 13.sp)
            }
        }
    }
}

/**
 * The signature element: the two companies face off. Each side shows its cheaper plan;
 * the winner lights up in its colour, the loser dims, and a verdict says by how much.
 */
@Composable
private fun Duel(quotes: List<Quote>, trip: Trip, modifier: Modifier) {
    fun bestOf(c: Company): Quote =
        quotes.filter { it.plan.company == c }.filterIsInstance<Quote.Price>().minByOrNull { it.total }
            ?: quotes.first { it.plan.company == c }

    val at = bestOf(Company.AUTOTEL) as Quote.Price
    val go = bestOf(Company.GOTO)
    val goPrice = (go as? Quote.Price)?.total
    val close = goPrice != null && abs(goPrice - at.total) / maxOf(goPrice, at.total) < 0.08
    val winner = when {
        goPrice == null -> Company.AUTOTEL
        close -> null
        goPrice < at.total -> Company.GOTO
        else -> Company.AUTOTEL
    }

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(26.dp)).padding(10.dp),
    ) {
        Box {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Side("AutoTel", at.plan.short, at.total, null, Teal, TealDeep, winner, Company.AUTOTEL, Modifier.weight(1f))
                Side(
                    "GoTo", go.plan.short, goPrice,
                    (go as? Quote.NotPossible)?.let { "One-way trips aren't possible" },
                    Orange, OrangeDeep, winner, Company.GOTO, Modifier.weight(1f),
                )
            }
            Box(
                Modifier.align(Alignment.Center).size(38.dp).clip(CircleShape).background(Page)
                    .border(1.dp, Line, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("vs", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(12.dp))
        val verdict = when {
            goPrice == null -> "Only AutoTel can do this trip"
            winner == null -> "Too close to call: ${shekel(abs(goPrice - at.total))} apart"
            winner == Company.GOTO -> "GoTo saves you ${shekel(at.total - goPrice)}"
            else -> "AutoTel saves you ${shekel(goPrice - at.total)}"
        }
        AnimatedContent(verdict, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "verdict") { v ->
            Text(
                v, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            summary(trip), color = Faint, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun Side(
    name: String, plan: String, price: Double?, unavailable: String?,
    accent: Color, deep: Color, winner: Company?, me: Company, modifier: Modifier,
) {
    val won = winner == me
    val lost = winner != null && !won
    val bg by animateColorAsState(
        when { won -> deep.copy(alpha = 0.32f); lost -> Raised.copy(alpha = 0.5f); else -> Raised }, tween(300),
    )
    val border by animateColorAsState(if (won) accent else Color.Transparent, tween(300))
    val alpha by animateFloatAsState(if (lost) 0.55f else 1f, tween(300))
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(bg)
            .border(1.5.dp, border, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(accent.copy(alpha = alpha))
            Spacer(Modifier.width(6.dp))
            Text(name, color = Ink.copy(alpha = alpha), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(won, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "CHEAPER", color = Page, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(accent).padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (price != null) {
            AnimatedPrice(price, 36, Ink.copy(alpha = alpha))
            Text(plan, color = Muted.copy(alpha = alpha), fontSize = 12.5.sp)
        } else {
            Text("—", color = Faint, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(unavailable ?: "", color = Muted, fontSize = 12.5.sp, lineHeight = 16.sp)
        }
    }
}

// ── Presets ───────────────────────────────────────────────────────────────

@Composable
private fun Presets(selected: String?, onPick: (Preset) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    LaunchedEffect(selected) {
        val i = PRESETS.indexOfFirst { it.name == selected }
        if (i >= 0) state.animateScrollToItem((i - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(PRESETS) { _, p ->
            val on = p.name == selected
            val bg by animateColorAsState(if (on) Ink else Card, tween(180))
            val fg by animateColorAsState(if (on) Page else Ink, tween(180))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(bg)
                    .border(1.dp, if (on) Ink else Line, RoundedCornerShape(50))
                    .clickable { onPick(p) }
                    .testTag("preset-${p.name.lowercase().replace(' ', '-')}")
                    .padding(start = 12.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(PRESET_ICONS.getValue(p.name), null, tint = if (on) Page else Muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(p.name, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Trip controls ─────────────────────────────────────────────────────────

@Composable
private fun TripPanel(trip: Trip, onChange: (Trip) -> Unit, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    fun change(t: Trip) {
        if (t != trip) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onChange(t)
    }
    val ink = Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color.White))

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Card)
            .border(1.dp, Line, RoundedCornerShape(26.dp)).padding(horizontal = 18.dp, vertical = 18.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // How long
        Readout("How long you have the car", duration(trip.totalMinutes))
        val idx = DURATIONS.indexOfFirst { it >= trip.totalMinutes }.let { if (it < 0) DURATIONS.lastIndex else it }
        TrackSlider(
            position = idx / DURATIONS.lastIndex.toFloat(),
            onPosition = { f ->
                val total = DURATIONS[(f * DURATIONS.lastIndex).roundToInt()]
                change(trip.copy(totalMinutes = total, drivingMinutes = minOf(trip.drivingMinutes, total)))
            },
            fill = ink, tag = "total-time",
        )

        // Driving vs parked — only AutoTel's in-city per-minute price depends on it
        AnimatedVisibility(!trip.leavesTelAviv, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Driving", color = Muted, fontSize = 13.sp)
                        Text(duration(trip.drivingMinutes), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, style = num())
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Parked", color = Muted, fontSize = 13.sp)
                        Text(duration(trip.parkedMinutes), color = Muted, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, style = num())
                    }
                }
                TrackSlider(
                    position = if (trip.totalMinutes == 0) 0f else trip.drivingMinutes / trip.totalMinutes.toFloat(),
                    onPosition = { f ->
                        val step = if (trip.totalMinutes <= 30) 1 else 5
                        val m = ((f * trip.totalMinutes / step).roundToInt() * step).coerceIn(0, trip.totalMinutes)
                        change(trip.copy(drivingMinutes = m))
                    },
                    fill = ink, rest = Color(0xFF334155), tag = "driving-time",
                )
            }
        }

        // Distance — square-root scale so a few km are as easy to set as a few hundred
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Distance", color = Muted, fontSize = 13.sp)
                Text("${trip.km} km", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, style = num())
            }
            RoundButton(Icons.Filled.Remove, "Less distance", "km-minus", enabled = trip.km > 0) { change(trip.copy(km = (trip.km - 1).coerceAtLeast(0))) }
            Spacer(Modifier.width(10.dp))
            RoundButton(Icons.Filled.Add, "More distance", "km-plus", enabled = trip.km < MAX_KM) { change(trip.copy(km = (trip.km + 1).coerceAtMost(MAX_KM))) }
        }
        TrackSlider(
            position = sqrt(trip.km / MAX_KM.toFloat()),
            onPosition = { f -> change(trip.copy(km = (f * f * MAX_KM).roundToInt())) },
            fill = ink, tag = "distance",
        )

        Rule()
        Switch("One way", "Leave the car where you end up", trip.oneWay, Teal, "one-way") { change(trip.copy(oneWay = it)) }
        Switch("Leaves Tel Aviv", "AutoTel moves to its long-trip rate", trip.leavesTelAviv, Teal, "leaves-tel-aviv") { change(trip.copy(leavesTelAviv = it)) }
        Switch("Weekend", "Thu night – Sat night · GoTo +₪4 an hour", trip.weekend, Orange, "weekend") { change(trip.copy(weekend = it)) }
        Rule()

        Text("GoTo car", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        Segmented(
            options = GoToCar.entries.toList(),
            selected = trip.goToCar,
            label = { it.label },
            sub = { "₪${it.driveHour.toInt()}/h" },
            accent = Orange,
            tag = { "goto-car-${it.name.lowercase()}" },
            onSelect = { change(trip.copy(goToCar = it)) },
        )

        Rule()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Monthly fee", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (trip.tripsPerMonth == 0) "Not counted"
                    else "Shared over ${trip.tripsPerMonth} trip${if (trip.tripsPerMonth > 1) "s" else ""} a month",
                    color = Muted, fontSize = 12.5.sp,
                )
            }
            RoundButton(Icons.Filled.Remove, "Fewer trips a month", "trips-minus", enabled = trip.tripsPerMonth > 0) {
                change(trip.copy(tripsPerMonth = (trip.tripsPerMonth - 1).coerceAtLeast(0)))
            }
            Text(
                "${trip.tripsPerMonth}", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, style = num(),
                textAlign = TextAlign.Center, modifier = Modifier.width(36.dp),
            )
            RoundButton(Icons.Filled.Add, "More trips a month", "trips-plus", enabled = trip.tripsPerMonth < 60) {
                change(trip.copy(tripsPerMonth = (trip.tripsPerMonth + 1).coerceAtMost(60)))
            }
        }
    }
}

@Composable
private fun Readout(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, style = num())
    }
}

@Composable
private fun Switch(title: String, sub: String, checked: Boolean, accent: Color, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = Muted, fontSize = 12.5.sp)
        }
        Toggle(checked, accent, tag, onChange)
    }
}

// ── All plans ─────────────────────────────────────────────────────────────

@Composable
private fun AllPlans(quotes: List<Quote>, modifier: Modifier) {
    val priced = quotes.filterIsInstance<Quote.Price>()
    val max = priced.maxOf { it.total }.coerceAtLeast(1.0)
    val cheapest = priced.minOf { it.total }
    val ordered = priced.sortedBy { it.total } + quotes.filterIsInstance<Quote.NotPossible>()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ordered.forEach { q -> PlanCard(q, max, q is Quote.Price && q.total == cheapest) }
    }
}

@Composable
private fun PlanCard(q: Quote, max: Double, best: Boolean) {
    var open by remember(q.plan) { mutableStateOf(false) }
    val accent = if (q.plan.company == Company.AUTOTEL) Teal else Orange
    val deep = if (q.plan.company == Company.AUTOTEL) TealDeep else OrangeDeep
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(220))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card)
            .border(1.dp, if (best) accent.copy(alpha = 0.6f) else Line, RoundedCornerShape(20.dp))
            .clickable(enabled = q is Quote.Price) { open = !open }
            .testTag("plan-${q.plan.name.lowercase()}")
            .padding(16.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(accent)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(q.plan.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(q.plan.note, color = Faint, fontSize = 12.sp)
            }
            when (q) {
                is Quote.Price -> {
                    AnimatedPrice(q.total, 22, Ink)
                    Icon(Icons.Filled.ExpandMore, if (open) "Hide breakdown" else "Show breakdown", tint = Muted, modifier = Modifier.rotate(turn))
                }
                is Quote.NotPossible -> Text("—", color = Faint, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        when (q) {
            is Quote.Price -> {
                val frac by animateFloatAsState((q.total / max).toFloat().coerceIn(0.03f, 1f), tween(450))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Raised)) {
                    Box(
                        Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(deep, accent))),
                    )
                }
                AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Receipt(q)
                }
            }
            is Quote.NotPossible -> Text(q.reason, color = Muted, fontSize = 13.sp)
        }
    }
}

/** The breakdown, set like a till receipt: dotted leaders, a rule, the total. */
@Composable
private fun Receipt(q: Quote.Price) {
    Column(
        Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Page)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        q.lines.forEach { (label, amount) ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(label, color = Muted, fontSize = 13.5.sp)
                Leader(Modifier.weight(1f).padding(horizontal = 6.dp))
                Text(shekel(amount, decimals = true), color = Ink, fontSize = 13.5.sp, style = num())
            }
        }
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(Line, Offset.Zero, Offset(size.width, 0f), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        }
        Row {
            Text("Total", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(shekel(q.total, decimals = true), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, style = num())
        }
    }
}

@Composable
private fun Leader(modifier: Modifier) {
    Canvas(modifier.height(10.dp)) {
        val y = size.height - 2f
        drawLine(Faint.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f)))
    }
}

@Composable
private fun Footer() {
    Text(
        "Fuel and insurance are included with both. Leaving Tel Aviv, AutoTel charges its long-trip rate: " +
            "₪99 for 3 hours, then ₪20 an hour, plus ₪1.2/km. GoTo bills whole hours, at least one, and " +
            "never more than its day rate for each 24 hours. ${Rates.AUTOTEL_SOURCE} · ${Rates.GOTO_SOURCE}. " +
            "The company's app shows the price that applies when you book.",
        color = Faint, fontSize = 11.5.sp, lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 22.dp),
    )
}

// ── small pieces ──────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 22.dp, top = 4.dp))
}

@Composable
private fun Rule() {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Line))
}

@Composable
private fun Dot(c: Color) {
    Box(Modifier.size(9.dp).clip(CircleShape).background(c))
}

@Composable
private fun AnimatedPrice(value: Double, size: Int, color: Color) {
    val shown by animateFloatAsState(value.toFloat(), tween(450))
    Text(shekel(shown.toDouble()), color = color, fontSize = size.sp, fontWeight = FontWeight.Bold, style = num())
}

private fun summary(t: Trip): String = buildList {
    add(duration(t.totalMinutes))
    add("${t.km} km")
    add(if (t.oneWay) "one way" else "round trip")
    add(if (t.leavesTelAviv) "out of Tel Aviv" else "in Tel Aviv")
    if (t.weekend) add("weekend")
}.joinToString("  ·  ")

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
