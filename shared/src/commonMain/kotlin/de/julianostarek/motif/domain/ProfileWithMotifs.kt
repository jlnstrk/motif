package de.julianostarek.motif.domain

data class ProfileWithMotifs(
    val profile: Profile,
    val motifs: List<Motif>
) {
    val anyUnlistened: Boolean get() = motifs.firstOrNull()?.listened == false
}