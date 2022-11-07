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

package de.julianostarek.motif.graphql

import de.julianostarek.motif.client.ProfileMeUpdatedSubscription
import de.julianostarek.motif.domain.Profile

fun ProfileMeUpdatedSubscription.ProfileMe.toDetail(): Profile.Detail {
    return Profile.Detail(
        displayName = displayName,
        id = id,
        username = username,
        photoUrl = photoUrl,
        biography = biography,
        follows = false,
        followersCount = followersCount,
        followingCount = followingCount
    )
}