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

package de.julianostarek.motif.ui.detail

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import de.julianostarek.motif.detail.MotifDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidMotifDetailViewModel(motifId: Int) : ViewModel() {
    val shared: MotifDetailViewModel = MotifDetailViewModel(motifId)

    val _themeColor: MutableStateFlow<Color?> = MutableStateFlow(null)
    val themeColor: StateFlow<Color?> get() = _themeColor

    fun submitDrawableForThemeColor(drawable: Drawable) = viewModelScope.launch {
        _themeColor.value = withContext(Dispatchers.Default) {
            val bitmap = drawable.toBitmap()
            val rgbInt = Palette.Builder(bitmap)
                .generate().darkMutedSwatch?.rgb
            rgbInt?.let(::Color)
        }
    }
}