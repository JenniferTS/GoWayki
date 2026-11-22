package pe.edu.tecsup.gowayki.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Genera Baseline Profiles en un dispositivo/emulador conectado.
 *
 * ```
 * ./gradlew :baselineprofile:generateBaselineProfile
 * ```
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndHome() = rule.collect(
        packageName = "pe.edu.tecsup.gowayki_lab",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.textContains("GoWayki")), 15_000)
        // Onboarding skip if present
        device.findObject(By.text("Omitir"))?.click()
            ?: device.findObject(By.text("Skip"))?.click()
        device.waitForIdle()
        device.findObject(By.textContains("dónde"))?.click()
            ?: device.findObject(By.textContains("Where"))?.click()
        device.waitForIdle()
    }
}
