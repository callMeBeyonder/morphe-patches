package arpit.morphe.patches.istreamflare.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import arpit.morphe.patches.istreamflare.shared.Constants.COMPATIBILITY_ISTREAMFLARE
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Unlocks the client-side subscription gates of iStreamFlare and removes the
 * ad placements bound to them.
 *
 * The app stores its entitlements as a digit string in the `subscription_type`
 * preference ('1' removes ads, '2' allows playing premium content, '3' allows
 * downloading premium content) and re-reads them through seven identical
 * parsers: one per consuming screen and one in the Application class. Every
 * parser is forced to read `"123"`, which sets its `removeAds`, `playPremium`
 * and `downloadPremium` booleans through the app's own digit logic. That covers
 * the premium play and download paywalls (`Buy_Premium_Dialog`) and every ad
 * placement, all of which are gated on those booleans (banners, interstitials
 * and the app-open ads of the AdMob/Wortise managers).
 *
 * The expired-subscription nag (`SubscriptionExpiredDialog`) that the server
 * triggers with the account status "nosub" is silenced by retargeting its two
 * comparison strings.
 *
 * Server-side limits are unaffected: account/device binding, the maintenance
 * and blocked-country screens, and content the backend refuses to deliver.
 */
@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks premium content playback and downloads, removes the " +
        "buy-premium paywalls, disables all ad placements, and hides the " +
        "expired-subscription nag. Server-side restrictions such as account " +
        "device binding remain.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ISTREAMFLARE)

    execute {
        //
        // Target 1 — force every entitlement parser to read "123".
        //
        // Inserting `const-string <reg>, "123"` right after the
        // `move-result-object` of `getString("subscription_type", null)`
        // overwrites the register that `String.valueOf` wraps. The digit loop
        // then sets removeAds ('1'), playPremium ('2') and downloadPremium
        // ('3') to true through the app's own logic, for any stored value —
        // including guests, whose preference was never written at all.
        //

        // The six named activity/fragment parsers. A count other than six means
        // the template changed and the patch needs re-verification.
        SubscriptionTypeParserFingerprint.matchAll(6..6).forEach { match ->
            val moveResultIndex = match.instructionMatches[2].index
            val register =
                match.method.getInstruction<OneRegisterInstruction>(moveResultIndex).registerA
            match.method.addInstructions(
                moveResultIndex + 1,
                "const-string v$register, \"123\"",
            )
        }

        // The seventh parser copy in the Application class, which gates the
        // app-open ad managers.
        AppOpenAdManagerOnCreateFingerprint.method.apply {
            val moveResultIndex = AppOpenAdManagerOnCreateFingerprint.instructionMatches[2].index
            val register = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA
            addInstructions(moveResultIndex + 1, "const-string v$register, \"123\"")
        }

        //
        // Target 2 — silence the expired-subscription nag.
        //
        // `MovieDetails.onCreate` and `WebSeriesDetails.onCreate` compare the
        // stored account status against "nosub" and show a non-dismissible
        // dialog when it matches. Rewriting the comparison constant (same
        // register, same instruction slot) makes the comparison always false,
        // which is exactly the path a subscribed account takes. Exactly two
        // matches are expected.
        //
        SubscriptionExpiredNagFingerprint.matchAll(2..2).forEach { match ->
            val constantIndex = match.instructionMatches[0].index
            val register =
                match.method.getInstruction<OneRegisterInstruction>(constantIndex).registerA
            match.method.replaceInstruction(
                constantIndex,
                "const-string v$register, \"subscribed\"",
            )
        }
    }
}
