/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.julianostarek.motif.util

import android.icu.text.RelativeDateTimeFormatter
import kotlinx.datetime.*

fun Instant.formatRelative(): String {
    val now = Clock.System.now()
    val offset = now - this
    val formatter = RelativeDateTimeFormatter.getInstance()
    return when {
        offset.inWholeDays != 0L -> formatter.format(
            offset.inWholeDays.toDouble(),
            RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY
        )

        offset.inWholeHours != 0L -> formatter.format(
            offset.inWholeHours.toDouble(),
            RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY
        )

        offset.inWholeMinutes != 0L -> formatter.format(
            offset.inWholeMinutes.toDouble(),
            RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY
        )

        else -> formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
    }
}