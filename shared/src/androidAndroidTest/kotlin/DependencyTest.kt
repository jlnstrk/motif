import android.app.Instrumentation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import de.julianostarek.motif.AndroidModule
import de.julianostarek.motif.SharedModule
import de.julianostarek.motif.androidStartKoin
import de.julianostarek.motif.client.BackendModule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.ksp.generated.module


@RunWith(AndroidJUnit4::class)
class DependencyTest : KoinTest {

    @Test
    fun verifyKoinApp() {
        koinApplication {
            modules(
                AndroidModule().module,
                SharedModule().module,
                BackendModule().module,
                module {
                    single { InstrumentationRegistry.getInstrumentation().context }
                }
            )
            checkModules()
        }
        assert(true)
    }
}