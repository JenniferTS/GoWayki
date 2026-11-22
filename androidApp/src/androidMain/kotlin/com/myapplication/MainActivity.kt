package com.myapplication

import MainView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import installGowaykiRuntime
import GowaykiRuntime
import pe.edu.tecsup.gowayki.data.citypack.openArequipaCityPack
import pe.edu.tecsup.gowayki.data.prefs.AndroidPreferencesRepository
import pe.edu.tecsup.gowayki.domain.models.PlaceResult
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val packRef = AtomicReference<CityPackRepository?>(null)
    private val networkRef = AtomicReference<PlannerNetwork?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
