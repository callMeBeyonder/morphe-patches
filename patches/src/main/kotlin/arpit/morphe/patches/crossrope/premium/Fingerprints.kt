package arpit.morphe.patches.crossrope.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches the session class's `isPremium()` accessor.
 *
 * The obfuscated session/account store (`Le6/e2;`) keeps the account state in a
 * `SharedPreferences` file and exposes the paid flag as a zero-argument boolean
 * method that ORs `is_premium` with `just_bought_premium`:
 *
 * ```
 * getBoolean("is_premium", false) || getBoolean("just_bought_premium", false)
 * ```
 *
 * Both keys are R8 literals and survive name obfuscation, which makes this method
 * identifiable across builds. Roughly two dozen app classes (screens, ViewModels,
 * settings, workout preview) call it to decide whether paid content is playable,
 * so forcing it true is the single highest-leverage gate.
 */
object PremiumFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("is_premium", "just_bought_premium")
)
