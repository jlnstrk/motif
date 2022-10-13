package de.julianostarek.motif.backend.auth

class BackendAuthExpiredException : Exception("Access token expired: Must reconnect")