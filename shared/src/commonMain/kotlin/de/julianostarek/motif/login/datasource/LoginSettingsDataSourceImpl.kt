package de.julianostarek.motif.login.datasource

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.set
import de.julianostarek.motif.client.auth.BackendAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@OptIn(ExperimentalSettingsApi::class, ExperimentalCoroutinesApi::class)
@Single
class LoginSettingsDataSourceImpl(
    @Named("LoginSettings")
    private val loginSettings: ObservableSettings,
) : LoginSettingsDataSource {
    override val authChanged: Flow<BackendAuth?> = loginSettings.serializableFlow(SETTINGS_KEY_AUTH)

    override suspend fun getAuth(): BackendAuth? {
        return withContext<BackendAuth?>(Dispatchers.Default) {
            loginSettings.getStringOrNull(SETTINGS_KEY_AUTH)?.let {
                Json.decodeFromString(it)
            }
        }?.also {
            println("Reading authentication: ${it.appToken.accessToken}")
        }
    }

    override suspend fun persistAuth(auth: BackendAuth?) {
        println("Persist authentication: ${auth?.appToken?.accessToken}")
        withContext(Dispatchers.Default) {
            loginSettings[SETTINGS_KEY_AUTH] = auth?.let { value -> Json.encodeToString(value) }
        }
    }

    companion object {
        private const val SETTINGS_KEY_AUTH = "auth"
    }

    private inline fun <reified T> ObservableSettings.serializableFlow(key: String): Flow<T?> = getStringOrNullFlow(key)
        .map { it?.let { string -> withContext(Dispatchers.Default) { Json.decodeFromString(string) } } }
}