package com.automatelinux.carShare.pricing

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Published rates, all VAT included.
// AutoTel: autotel.co.il/pricing-hebrew ("נכון ל1.9.2024").
// GoTo:    gotoglobal.com/en/pricing-list (read 2026-11-23) + car-sharing FAQ
//          (1-hour minimum, round trip only).
object Rates {
    const val AUTOTEL_SOURCE = "AutoTel price list, 1 Sep 2024"
    const val GOTO_SOURCE = "GoTo price list, Nov 2026"

    // AutoTel long-trip package (used when the trip leaves Tel Aviv).
    const val AT_LONG_FIRST_HOURS = 3
    const val AT_LONG_FIRST_PRICE = 99.0
    const val AT_LONG_EXTRA_HOUR = 20.0
    const val AT_LONG_PER_KM = 1.2

    // GoTo per-km, every car except the big van.
    const val GOTO_KM_FIRST = 50
    const val GOTO_KM_FIRST_RATE = 2.3
    const val GOTO_KM_AFTER_RATE = 1.3
    const val GOTO_VAN_KM_RATE = 2.3

    // GoTo weekend surcharge (Thu 23:00 – Sat 23:00).
    const val GOTO_WEEKEND_HOUR = 4.0
    const val GOTO_WEEKEND_DAY = 40.0
}

enum class Company { AUTOTEL, GOTO }

enum class Plan(
    val company: Company,
    val label: String,
    val monthlyFee: Double,
    val note: String,
    /** How the face-off names the plan under the company name. */
    val short: String,
) {
    AT_HIGH(Company.AUTOTEL, "AutoTel ₪40", 40.0, "High Gear: ₪1.5/min driving, ₪0.75/min parked", "on the ₪40 plan"),
    AT_LOW(Company.AUTOTEL, "AutoTel ₪10", 10.0, "Low Gear: ₪2/min driving, ₪1/min parked", "on the ₪10 plan"),
    GO_DRIVE(Company.GOTO, "GoTo DRIVE", 50.0, "Lower hourly rate, ₪50 a month", "on the DRIVE plan"),
    GO_LITE(Company.GOTO, "GoTo LITE", 20.0, "Higher hourly rate, ₪20 a month", "on the LITE plan"),
}

enum class GoToCar(
    val label: String,
    val driveHour: Double, val driveDay: Double,
    val liteHour: Double, val liteDay: Double,
    val isVan: Boolean,
) {
    MINI("Small", 16.0, 140.0, 20.0, 170.0, false),
    FAMILY("Family", 21.0, 185.0, 25.0, 215.0, false),
    VAN("Van", 34.0, 270.0, 38.0, 300.0, false),
    BIG_VAN("Big van", 50.0, 500.0, 54.0, 530.0, true),
}

data class Trip(
    val totalMinutes: Int,
    val drivingMinutes: Int,
    val km: Int,
    val oneWay: Boolean,
    val leavesTelAviv: Boolean,
    val weekend: Boolean,
    val goToCar: GoToCar,
    /** 0 = leave monthly fees out; otherwise the fee is split over this many trips. */
    val tripsPerMonth: Int,
) {
    val parkedMinutes: Int get() = max(0, totalMinutes - drivingMinutes)
}

sealed interface Quote {
    val plan: Plan
    data class Price(override val plan: Plan, val total: Double, val lines: List<Pair<String, Double>>) : Quote
    data class NotPossible(override val plan: Plan, val reason: String) : Quote
}

fun quoteAll(trip: Trip): List<Quote> = Plan.entries.map { quote(it, trip) }

fun quote(plan: Plan, trip: Trip): Quote = when (plan.company) {
    Company.AUTOTEL -> autoTel(plan, trip)
    Company.GOTO -> goTo(plan, trip)
}

private fun feeLine(plan: Plan, trip: Trip): List<Pair<String, Double>> =
    if (trip.tripsPerMonth > 0)
        listOf("Monthly fee ÷ ${trip.tripsPerMonth} trips" to plan.monthlyFee / trip.tripsPerMonth)
    else emptyList()

private fun autoTel(plan: Plan, trip: Trip): Quote {
    val lines = if (trip.leavesTelAviv) {
        val hours = ceil(trip.totalMinutes / 60.0).toInt()
        val extraHours = max(0, hours - Rates.AT_LONG_FIRST_HOURS)
        buildList {
            add("Long trip, first ${Rates.AT_LONG_FIRST_HOURS} hours" to Rates.AT_LONG_FIRST_PRICE)
            if (extraHours > 0) add("$extraHours extra hours × ₪20" to extraHours * Rates.AT_LONG_EXTRA_HOUR)
            if (trip.km > 0) add("${trip.km} km × ₪1.2" to trip.km * Rates.AT_LONG_PER_KM)
        }
    } else {
        val (drive, park) = if (plan == Plan.AT_HIGH) 1.5 to 0.75 else 2.0 to 1.0
        buildList {
            add("${trip.drivingMinutes} min driving × ₪${fmtRate(drive)}" to trip.drivingMinutes * drive)
            if (trip.parkedMinutes > 0)
                add("${trip.parkedMinutes} min parked × ₪${fmtRate(park)}" to trip.parkedMinutes * park)
        }
    } + feeLine(plan, trip)
    return Quote.Price(plan, lines.sumOf { it.second }, lines)
}

private fun goTo(plan: Plan, trip: Trip): Quote {
    if (trip.oneWay) return Quote.NotPossible(plan, "GoTo cars go back where you took them")
    val car = trip.goToCar
    val hourRate = if (plan == Plan.GO_DRIVE) car.driveHour else car.liteHour
    val dayRate = if (plan == Plan.GO_DRIVE) car.driveDay else car.liteDay

    // At least one hour, billed in whole hours; each 24-hour block is capped at the day rate.
    val hours = max(1, ceil(trip.totalMinutes / 60.0).toInt())
    val fullDays = hours / 24
    val restHours = hours % 24
    val restByHour = restHours * hourRate
    val restIsDay = restHours > 0 && restByHour >= dayRate
    val days = fullDays + if (restIsDay) 1 else 0
    val billedHours = if (restIsDay) 0 else restHours

    val lines = buildList {
        if (days > 0) add("$days day${if (days > 1) "s" else ""} × ₪${dayRate.toInt()}" to days * dayRate)
        if (billedHours > 0) add("$billedHours hour${if (billedHours > 1) "s" else ""} × ₪${hourRate.toInt()}" to billedHours * hourRate)
        if (trip.weekend) {
            val surcharge = days * Rates.GOTO_WEEKEND_DAY + billedHours * Rates.GOTO_WEEKEND_HOUR
            add("Weekend surcharge" to surcharge)
        }
        if (trip.km > 0) {
            if (car.isVan) {
                add("${trip.km} km × ₪2.3" to trip.km * Rates.GOTO_VAN_KM_RATE)
            } else {
                val first = min(trip.km, Rates.GOTO_KM_FIRST)
                val after = trip.km - first
                add("$first km × ₪2.3" to first * Rates.GOTO_KM_FIRST_RATE)
                if (after > 0) add("$after km × ₪1.3" to after * Rates.GOTO_KM_AFTER_RATE)
            }
        }
    } + feeLine(plan, trip)
    return Quote.Price(plan, lines.sumOf { it.second }, lines)
}

private fun fmtRate(v: Double): String =
    if (v == v.toInt().toDouble()) v.toInt().toString() else v.toString()

data class Preset(val name: String, val trip: Trip)

private fun t(total: Int, drive: Int, km: Int, oneWay: Boolean = false, out: Boolean = false, weekend: Boolean = false) =
    Trip(total, drive, km, oneWay, out, weekend, GoToCar.MINI, 0)

val PRESETS = listOf(
    Preset("Quick hop", t(10, 10, 3, oneWay = true)),
    Preset("To a meeting", t(25, 25, 8, oneWay = true)),
    Preset("Pick-up and back", t(30, 20, 6)),
    Preset("Supermarket", t(60, 20, 8)),
    Preset("Errand", t(60, 30, 20)),
    Preset("Evening out", t(180, 40, 25)),
    Preset("Out of town", t(180, 120, 60, out = true)),
    Preset("Jerusalem", t(300, 150, 130, out = true)),
    Preset("Day up north", t(720, 300, 300, out = true)),
    Preset("Day trip", t(1440, 180, 100, out = true)),
    Preset("Weekend away", t(2880, 300, 200, out = true, weekend = true)),
)
