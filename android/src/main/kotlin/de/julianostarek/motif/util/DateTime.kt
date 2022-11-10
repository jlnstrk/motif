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
import kotlin.math.abs

fun Instant.formatRelative(): String {
    val now = Clock.System.now()
    val offset = now - this
    val formatter = RelativeDateTimeFormatter.getInstance()
    return when {
        offset.inWholeDays != 0L -> formatter.format(
            abs(offset.inWholeDays.toDouble()),
            if (offset.inWholeDays > 0) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.DAYS
        )

        offset.inWholeHours != 0L -> formatter.format(
            abs(offset.inWholeHours.toDouble()),
            if (offset.inWholeHours > 0) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.HOURS
        )

        offset.inWholeMinutes != 0L -> formatter.format(
            abs(offset.inWholeMinutes.toDouble()),
            if (offset.inWholeMinutes > 0) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.MINUTES
        )

        else -> formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
    }
}