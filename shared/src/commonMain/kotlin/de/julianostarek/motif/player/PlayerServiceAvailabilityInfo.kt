package de.julianostarek.motif.player

fun interface PlayerServiceAvailabilityInfo {
    fun availableServices(): List<ServiceStatus>

    data class ServiceStatus(
        val service: PlayerService,
        val isInstalled: Boolean,
        val isLinked: Boolean
    )
}