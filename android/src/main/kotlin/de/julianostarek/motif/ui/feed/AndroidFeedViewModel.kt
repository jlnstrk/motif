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

package de.julianostarek.motif.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import de.julianostarek.motif.domain.ProfileWithMotifs
import de.julianostarek.motif.feed.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AndroidFeedViewModel : ViewModel() {
    val shared: FeedViewModel = FeedViewModel()

    val profiles: Flow<PagingData<FeedItem>> = shared.state
        .filterIsInstance<FeedState.Data>().map { it.profiles }
        .map {
            it.map { FeedItem.Item(it) }
                .insertSeparators { a, b ->
                    val aRecentness = a?.data?.recentness()
                    val bRecentness = b?.data?.recentness()
                    when {
                        bRecentness != null && (aRecentness == null || aRecentness != bRecentness) -> FeedItem.Header(bRecentness)
                        else -> null
                    }
                }
        }
        .cachedIn(viewModelScope)
}

fun ProfileWithMotifs.recentness(): Recentness {
    val offset = (Clock.System.now() - motifs.first().createdAt)
    return when {
        offset.inWholeDays < 1 -> Recentness.TODAY
        offset.inWholeDays < 2 -> Recentness.YESTERDAY
        offset.inWholeDays < 7 -> Recentness.LAST_WEEK
        else -> Recentness.OLDER
    }
}

sealed class FeedItem {
    data class Header(val recentness: Recentness) : FeedItem()
    data class Item(val data: ProfileWithMotifs) : FeedItem()
}