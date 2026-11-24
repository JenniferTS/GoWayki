package com.myapplication

import GowaykiRuntime
import MainView
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import androidx.lifecycle.lifecycleScope
import android.os.Looper
import installGowaykiRuntime
import pe.edu.tecsup.gowayki.data.account.FirebaseAccountDeletionRepository
import pe.edu.tecsup.gowayki.data.auth.AndroidFirebaseAuthRepository
import pe.edu.tecsup.gowayki.data.auth.GoogleSignInException
import pe.edu.tecsup.gowayki.data.map.OfflineMapInstaller
import pe.edu.tecsup.gowayki.data.citypack.openArequipaCityPack
import pe.edu.tecsup.gowayki.data.prefs.AndroidPreferencesRepository
import pe.edu.tecsup.gowayki.domain.auth.SignedInUser
import pe.edu.tecsup.gowayki.domain.models.PlaceResult
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import pe.edu.tecsup.gowayki.domain.legal.LegalLinks
import pe.edu.tecsup.gowayki.platform.AndroidPlatformHost
import pe.edu.tecsup.gowayki.platform.GoogleSignInBridge
import pe.edu.tecsup.gowayki.platform.installAndroidPlatform
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val packRef = AtomicReference<CityPackRepository?>(null)
    private val networkRef = AtomicReference<PlannerNetwork?>(null)
    private lateinit var authRepo: AndroidFirebaseAuthRepository
    private val mainHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        installAndroidPlatform(this)

        authRepo = AndroidFirebaseAuthRepository(
            activity = this,
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
        GoogleSignInBridge.launch = {
            lifecycleScope.launch {
                runCatching { authRepo.signInWithGoogle() }
                    .onSuccess { user -> postAuthSuccess(user) }
                    .onFailure { e ->
                        val cancelled = (e as? GoogleSignInException)?.cancelled == true
                        postAuthError(if (cancelled) "" else e.message ?: e.javaClass.simpleName)
                    }
            }
        }
        GoogleSignInBridge.signOut = {
            lifecycleScope.launch { authRepo.signOut() }
        }
        GoogleSignInBridge.authRepository = authRepo
        AndroidPlatformHost.accountDeletion = FirebaseAccountDeletionRepository(
            googleIdToken = { authRepo.requestGoogleIdToken() },
        )

        val placeSearch = PlaceSearchRepository {
            packRef.get()?.searchPlaces(it)?.map { place -> place.toPlaceResult() } ?: emptyList()
        }
        val preferences: PreferencesRepository = AndroidPreferencesRepository(this)
        val restored = authRepo.currentUser()?.takeIf { !it.isAnonymous }

        installGowaykiRuntime(
            GowaykiRuntime(
                deviceLanguage = resources.configuration.locales[0]?.language ?: "es",
                placeSearch = placeSearch,
                networkProvider = { networkRef.get() },
                cityPackProvider = { packRef.get() },
                preferences = preferences,
                initialSignedIn = restored,
                legalLinks = LegalLinks(
                    privacy = LegalLinks.parse(BuildConfig.PRIVACY_URL),
                    terms = LegalLinks.parse(BuildConfig.TERMS_URL),
                ),
            ),
        )

        thread(name = "offline-map") {
            runCatching { OfflineMapInstaller(applicationContext).installStyle(dark = false) }
        }
        thread(name = "city-pack") {
            runCatching {
                val pack = openArequipaCityPack()
                packRef.set(pack)
                networkRef.set(pack.plannerNetwork())
            }
        }

        setContent {
            MainView()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // uiMode está en configChanges: sin recrear la Activity, los íconos de las barras
        // del sistema tienen que seguir al modo claro/oscuro nuevo.
        enableEdgeToEdge()
    }

    private fun postAuthSuccess(user: SignedInUser) {
        mainHandler.post {
            GoogleSignInBridge.onSuccess?.invoke(user)
        }
    }

    private fun postAuthError(msg: String) {
        mainHandler.post {
            GoogleSignInBridge.onError?.invoke(msg)
        }
    }
}

private fun PlaceSearchRepository(block: (String) -> List<PlaceResult>): PlaceSearchRepository =
    object : PlaceSearchRepository {
        override fun search(query: String): List<PlaceResult> = block(query)
    }
