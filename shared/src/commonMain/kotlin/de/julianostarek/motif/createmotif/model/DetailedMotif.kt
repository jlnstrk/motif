package de.julianostarek.motif.createmotif.model

data class DetailedMotif(
    val id: Long,
    val spotifyTrackId: String,
    val offset: Int,
    val listened: Boolean,
    val creatorPhotoUrl: String?
)