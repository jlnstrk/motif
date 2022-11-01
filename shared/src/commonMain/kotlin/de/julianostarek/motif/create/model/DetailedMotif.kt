package de.julianostarek.motif.create.model

data class DetailedMotif(
    val id: Long,
    val spotifyTrackId: String,
    val offset: Int,
    val listened: Boolean,
    val creatorPhotoUrl: String?
)