package de.julianostarek.motif.player

public interface Player {
    public suspend fun resume()
    public suspend fun pause()
    public suspend fun stop()
    public suspend fun playerState(): PlayerState
    public suspend fun seekTo(position: Long)
    public suspend fun setRepeatMode(repeatMode: PlayerState.RepeatMode)
    public suspend fun setShuffleMode(shuffleMode: PlayerState.ShuffleMode)
}