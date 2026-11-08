package arpit.morphe.patches.crossrope.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import arpit.morphe.patches.crossrope.shared.Constants.COMPATIBILITY_CROSSROPE

/**
 * Unlocks Crossrope Premium.
 *
 * Crossrope is a native Kotlin app whose account state lives in `SharedPreferences`.
 * The paid flag is exposed by one zero-argument boolean method on the session class,
 * which ORs the `is_premium` and `just_bought_premium` keys together. Every paid
 * surface (workout and video playback, programs, challenges, the plan screens and the
 * settings subscription row) reads that method, so forcing it true unlocks them all.
 *
 * Google Play Billing stays untouched: purchases, restore and the billing client are
 * not modified. The patch only changes what the *client* believes about the account,
 * so content the backend refuses to serve to free accounts — for example media URLs
 * issued per entitlement — remains gated and can still fail to load.
 */
@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Reports the account as premium by forcing the app's stored " +
        "`is_premium` check to true, which unlocks the paid workout, program and " +
        "challenge surfaces. Billing is untouched; media the server refuses to " +
        "serve to free accounts may still fail to load.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_CROSSROPE)

    execute {
        val flagged = PremiumFlagFingerprint.method

        // The injected body reuses `v0` as its return register, so the method must
        // have at least one local. R8 only emits locals in a method body, and this
        // accessor always reads two preference keys, so a missing local means the
        // fingerprint matched the wrong method — fail loudly instead of emitting
        // invalid smali.
        val registerCount = flagged.implementation?.registerCount ?: 0
        check(registerCount >= 1) {
            "isPremium() has $registerCount registers; the patch needs at least one " +
                "local to return true."
        }

        flagged.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
