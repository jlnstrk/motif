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

package de.julianostarek.motif.feed

private fun printGrid(grid: Array<Int?>, rows: Int) {
    for (y in 0 until rows) {
        if (y % 2 == 0) {
            print("  ")
        }
        for (x in 0 until (grid.size / rows)) {
            val entry = grid[y * (grid.size / rows) + x]
            print("${entry?.toString()?.padStart(3, '0') ?: "---"} ")
        }
        println()
    }
}

inline fun <reified T> Collection<T>.toSpiralHexagon(): SquareGrid<T?> {
    var capacity = 1
    var factor = 1
    while (capacity < size) {
        capacity += factor * 6
        ++factor
    }
    val nominalSize = factor * 2 - 1
    val gridSize = nominalSize + (nominalSize + 1) % 2
    return toSpiralHexagon(gridSize, arrayOfNulls(gridSize * gridSize))
}

/**
 * Transforms a (pre-sorted) collection into a spiraled hexagon grid representation.
 * In the view, even rows should be offset by (1/2 item width).
 *
 * Input:
 * 001, 002, ...
 *
 * Returns:
 *    --- 056 057 058 059 060 --- ---
 *  --- 055 033 034 035 036 037 ---
 *    054 032 016 017 018 019 038 ---
 *  053 031 015 005 006 007 020 039
 *    030 014 004 000 001 008 021 040
 *  051 029 013 003 002 009 022 041
 *    050 028 012 011 010 023 042 ---
 *  --- 049 027 026 025 024 043 ---
 *    --- 048 047 046 045 044 --- ---
 */
fun <T> Collection<T>.toSpiralHexagon(gridSize: Int, grid: Array<T?>): SquareGrid<T?> {
    fun has(x: Int, y: Int): Boolean {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) return false
        return grid.getOrNull(y * gridSize + x) != null
    }

    fun mark(x: Int, y: Int, with: T) {
        grid[y * gridSize + x] = with
    }

    var curX = gridSize / 2 - 1
    var curY = gridSize / 2

    for (item in this) {
        val left = has(curX - 1, curY)
        val topLeft = has(curX - 1, curY - 1)
        val top = has(curX, curY - 1)
        val topRight = has(curX + 1, curY - 1)
        val right = has(curX + 1, curY)
        val bottomRight = has(curX + 1, curY + 1)
        val bottom = has(curX, curY + 1)
        val bottomLeft = has(curX - 1, curY + 1)
        val even = curY % 2 == 0
        when {
            // center (initial)
            !left && !top && !right && !bottom -> {
                curX++
            }
            // right edge, > vertical center
            left && (even == bottom) && (even && !bottomRight || !even) && curY < gridSize / 2 -> {
                if (even) ++curX
                ++curY
            }
            // right edge, <= vertical center
            left && curY >= gridSize / 2 -> {
                if (!even) --curX
                ++curY
            }
            // bottom edge
            !left && (right || (!even xor topRight)) && (even && top || !even && topLeft) -> {
                --curX
            }
            // left edge, < vertical center
            right && (even xor top) && curY > gridSize / 2 -> {
                if (!even) --curX
                --curY
            }
            // left edge, >= vertical center
            right && curY <= gridSize / 2 -> {
                if (even) ++curX
                --curY
            }
            // top edge
            !right && (left || (even xor bottomLeft)) && (even && bottomRight || !even && bottom) -> {
                ++curX
            }
        }
        mark(curX, curY, item)
    }
    return SquareGrid(
        grid = grid.toList(),
        size = gridSize
    )
}


data class SquareGrid<T>(val grid: List<T?>, val size: Int)