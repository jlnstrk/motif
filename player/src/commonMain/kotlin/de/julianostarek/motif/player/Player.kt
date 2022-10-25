package de.julianostarek.motif.player

import kotlinx.coroutines.flow.Flow

public sealed interface Player {
    public suspend fun resume()
    public suspend fun pause()
    public suspend fun stop()
    public suspend fun getPlayerState(): PlayerState
    public suspend fun playerState(): Flow<PlayerState>
    public suspend fun seekTo(position: Long)
    public suspend fun setRepeatMode(repeatMode: PlayerState.RepeatMode)
    public suspend fun setShuffleMode(shuffleMode: PlayerState.ShuffleMode)
}