// SPDX-License-Identifier: MIT
/*
 * HistoryPeriod.kt - Calendar month, week and day selection for extended history
 *
 * Copyright (C) 2026 The Breathe Open Source Project
 * Copyright (C) 2026 sidharthify <wednisegit@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.sidharthify.breathe.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class HistoryPeriodMode(val label: String) {
    MONTH("Month"),
    WEEK("Week"),
    DAYS("Days"),
}

data class HistoryPeriod(val start: LocalDate, val end: LocalDate) {
    val token: String
        get() = "${start}_${end}"

    val interval: String
        get() {
            val days = ChronoUnit.DAYS.between(start, end) + 1
            return when {
                days <= 14 -> "1h"
                days <= 62 -> "4h"
                else -> "1d"
            }
        }

    fun label(mode: HistoryPeriodMode): String {
        if (mode == HistoryPeriodMode.MONTH) {
            return start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        }
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        if (start == end) {
            return start.format(formatter)
        }
        return "${start.format(formatter)} to ${end.format(formatter)}"
    }

    fun shifted(mode: HistoryPeriodMode, direction: Long): HistoryPeriod {
        if (mode == HistoryPeriodMode.MONTH) {
            return containing(mode, start.plusMonths(direction))
        }
        val length = ChronoUnit.DAYS.between(start, end) + 1
        return HistoryPeriod(start.plusDays(direction * length), end.plusDays(direction * length))
    }

    companion object {
        fun containing(mode: HistoryPeriodMode, date: LocalDate): HistoryPeriod =
            when (mode) {
                HistoryPeriodMode.MONTH ->
                    HistoryPeriod(date.withDayOfMonth(1), date.with(TemporalAdjusters.lastDayOfMonth()))
                HistoryPeriodMode.WEEK -> {
                    val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    HistoryPeriod(monday, monday.plusDays(6))
                }
                HistoryPeriodMode.DAYS -> HistoryPeriod(date, date)
            }
    }
}
