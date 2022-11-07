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

package de.julianostarek.motif

import android.app.Application
import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingSource
import androidx.paging.PagingState
import coil.ImageLoader
import coil.ImageLoaderFactory

class MotifApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        androidStartKoin(this)
    }

    override fun newImageLoader(): ImageLoader {
        Pager()
        return ImageLoader.Builder(this)
            .build()
    }

    companion object : PagingSource<Int, String>() {
        override fun getRefreshKey(state: PagingState<Int, String>): Int? {
            TODO("Not yet implemented")
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            invalidate()
        }

    }
}