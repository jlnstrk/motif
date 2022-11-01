package de.julianostarek.motif.domain

sealed class ProfileReference {
    abstract val id: String

    data class Id(override val id: String) : ProfileReference()

    data class Simple(val simple: Profile.Simple) : ProfileReference() {
        override val id: String
            get() = simple.id
    }
}