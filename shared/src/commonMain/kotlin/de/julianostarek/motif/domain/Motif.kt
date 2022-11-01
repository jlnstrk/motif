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

package de.julianostarek.motif.domain

import kotlinx.datetime.Instant

sealed interface Motif {
    val id: Int
    val isrc: String
    val offset: Int
    val listened: Boolean
    val createdAt: Instant
    val creator: Profile

    data class Simple(
        override val id: Int,
        override val isrc: String,
        override val offset: Int,
        override val listened: Boolean,
        override val createdAt: Instant,
        override val creator: Profile
    ) : Motif

    data class Detail(
        override val id: Int,
        override val isrc: String,
        override val offset: Int,
        override val listened: Boolean,
        override val createdAt: Instant,
        override val creator: Profile,
        val listenersCount: Int,
        val listeners: List<Profile>,
        val liked: Boolean,
        val likesCount: Int,
        val likedBy: List<Profile>,
        val commentsCount: Int,
        val comments: List<Comment>
    ) : Motif
}