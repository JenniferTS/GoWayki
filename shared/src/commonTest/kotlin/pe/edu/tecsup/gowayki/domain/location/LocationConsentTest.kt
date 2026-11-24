package pe.edu.tecsup.gowayki.domain.location

import kotlin.test.Test
import kotlin.test.assertEquals
import pe.edu.tecsup.gowayki.domain.legal.LegalLinks

/** Port de `flutter/test/onboarding_test.dart`: el permiso nunca se pide sin acción explícita ni sin aviso. */
class LocationConsentTest {
    @Test
    fun passiveUseNeverRequestsPermission() {
        assertEquals(LocationAccessStep.Skip, LocationConsent.next(false, userInitiated = false, disclosureAccepted = false))
        assertEquals(LocationAccessStep.Skip, LocationConsent.next(false, userInitiated = false, disclosureAccepted = true))
    }

    @Test
    fun firstExplicitRequestShowsTheDisclosureBeforeTheSystemPrompt() {
        assertEquals(LocationAccessStep.ShowDisclosure, LocationConsent.next(false, userInitiated = true, disclosureAccepted = false))
    }

    @Test
    fun acceptedDisclosureAllowsTheSystemPromptOnExplicitAction() {
        assertEquals(LocationAccessStep.RequestSystem, LocationConsent.next(false, userInitiated = true, disclosureAccepted = true))
    }

    @Test
    fun grantedPermissionIsUsedWithoutAnyPrompt() {
        assertEquals(LocationAccessStep.Granted, LocationConsent.next(true, userInitiated = false, disclosureAccepted = false))
        assertEquals(LocationAccessStep.Granted, LocationConsent.next(true, userInitiated = true, disclosureAccepted = false))
    }

    @Test
    fun legalLinksAcceptOnlyWebUrls() {
        assertEquals(null, LegalLinks.parse(""))
        assertEquals(null, LegalLinks.parse("ftp://x.test/a"))
        assertEquals(null, LegalLinks.parse("https://"))
        assertEquals("https://gowayki.test/privacidad", LegalLinks.parse(" https://gowayki.test/privacidad "))
    }
}
