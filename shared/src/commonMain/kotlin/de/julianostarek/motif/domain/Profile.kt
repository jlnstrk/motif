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

sealed interface Profile {
    val displayName: String
    val id: String
    val photoUrl: String?
    val username: String

    data class Simple(
        override val displayName: String,
        override val photoUrl: String?,
        override val id: String,
        override val username: String,
    ) : Profile

    data class Detail(
        override val displayName: String,
        override val id: String,
        override val photoUrl: String?,
        override val username: String,
        val biography: String?,
        val follows: Boolean,
        val followersCount: Int,
        val followingCount: Int,
    ) : Profile
}