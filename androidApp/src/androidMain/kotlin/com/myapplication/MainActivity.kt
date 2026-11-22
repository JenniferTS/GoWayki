package com.myapplication

import GowaykiRuntime
import MainView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import installGowaykiRuntime
import pe.edu.tecsup.gowayki.data.auth.AndroidFirebaseAuthRepository
import pe.edu.tecsup.gowayki.data.citypack.openArequipaCityPack
import pe.edu.tecsup.gowayki.data.prefs.AndroidPreferencesRepository
import pe.edu.tecsup.gowayki.domain.models.PlaceResult
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import pe.edu.tecsup.gowayki.platform.GoogleSignInBridge
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val packRef = AtomicReference<CityPackRepository?>(null)
    private val networkRef = AtomicReference<PlannerNetwork?>(null)
    private lateinit var authRepo: AndroidFirebaseAuthRepository

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        authRepo.completeGoogleSignInAsync(
            data = result.data,
            onSuccess = { user -> GoogleSignInBridge.onSuccess?.invoke(user) },
            onError = { msg -> GoogleSignInBridge.onError?.invoke(msg) },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authRepo = AndroidFirebaseAuthRepository(
            activity = this,
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
        GoogleSignInBridge.launch = { googleLauncher.launch(authRepo.beginGoogleSignIn()) }
        GoogleSignInBridge.authRepository = authRepo

        val placeSearch = PlaceSearchRepository {
            packRef.get()?.searchPlaces(it)?.map { place -> place.toPlaceResult() } ?: emptyList()
        }
        val preferences: PreferencesRepository = AndroidPreferencesRepository(this)

        installGowaykiRuntime(
            GowaykiRuntime(
                deviceLanguage = resources.configuration.locales[0]?.language ?: "es",
                placeSearch = placeSearch,
                networkProvider = { networkRef.get() },
                preferences = preferences,
            ),
        )

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
}

private fun PlaceSearchRepository(block: (String) -> List<PlaceResult>): PlaceSearchRepository =
    object : PlaceSearchRepository {
        override fun search(query: String): List<PlaceResult> = block(query)
    }
