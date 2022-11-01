package de.julianostarek.motif.profileedit

data class ProfileEdit(
    val displayName: String? = null,
    val username: String? = null,
    val biography: String? = null,
) {

    companion object {
        val NO_CHANGES = ProfileEdit()
    }
}