package de.julianostarek.motif.client.auth

class BackendAuthExpiredException : Exception("Access token expired: Must reconnect")