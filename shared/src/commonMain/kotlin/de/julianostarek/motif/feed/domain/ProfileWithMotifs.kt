package de.julianostarek.motif.feed.domain

data class ProfileWithMotifs(
    val profile: Profile,
    val motifs: List<Motif>
) {
    val anyUnlistened: Boolean get() = motifs.firstOrNull()?.listened == false
}