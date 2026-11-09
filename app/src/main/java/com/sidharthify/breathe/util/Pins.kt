// SPDX-License-Identifier: MIT
/*
 * Pins.kt - Pinned zone ids and the order the user put them in
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

import android.content.SharedPreferences
import androidx.core.content.edit

// the set in pinned_ids is what counts (the widgets read it too), pinned_order
// only remembers the order. pins from before reordering existed have no spot
// in it yet, so they go on the end alphabetically
fun loadPinnedOrder(prefs: SharedPreferences): List<String> {
    val pinnedSet = prefs.getStringSet("pinned_ids", emptySet()) ?: emptySet()
    val savedOrder =
        prefs
            .getString("pinned_order", null)
            ?.split(",")
            ?.filter { it in pinnedSet }
            ?.distinct()
            ?: emptyList()
    val unordered = pinnedSet.filter { it !in savedOrder }.sorted()
    return savedOrder + unordered
}

fun savePinnedOrder(
    prefs: SharedPreferences,
    order: List<String>,
) {
    prefs.edit {
        putStringSet("pinned_ids", order.toSet())
        putString("pinned_order", order.joinToString(","))
    }
}
