package arpit.morphe.patches.istreamflare.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import arpit.morphe.patches.istreamflare.shared.Constants.COMPATIBILITY_ISTREAMFLARE

/**
 * Removes the startup checks that kill patched builds of iStreamFlare.
 *
 * 1. The installer/signature funnel. `Splash.loadData()` ->
 *    `verifyInstaller()` -> `validateSignature()` compares the app's signing
 *    certificate against a server-provided value (`VALIDATE_SIGNATURE`,
 *    `VERIFY_INSTALLER` via Firebase Remote Config and `get_config`) and the
 *    install source against the Play Store. Repackaged builds always fail the
 *    signature comparison once the operator enables it, and the app exits with
 *    "error: signature mismatch!". The method is redirected straight to
 *    `loadUserData()`, the continuation both passing checks take, so startup
 *    proceeds normally; the original body is left as dead code.
 *
 * 2. The restricted-app blocklist. `HelperUtils.getRestrictApp()` makes the app
 *    refuse to run while a blocklisted network-analysis tool (HttpCanary,
 *    SSLCapture, PacketCapture, WiCap, …) is installed on the device. The
 *    method now returns null — the "not restricted" outcome.
 *
 * Both checks are operator-configured and may currently be disabled; the patch
 * is inert in that case and only removes the risk of a future server-side
 * switch breaking patched installs.
 */
@Suppress("unused")
val bypassIntegrityChecksPatch = bytecodePatch(
    name = "Bypass integrity checks",
    description = "Disables the installer/signature validation that exits the " +
        "app on re-signed builds when the operator enables it, and the " +
        "restricted-app block that refuses to run while network-analysis " +
        "tools are installed.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ISTREAMFLARE)

    execute {
        //
        // Target 1 — skip the installer and signature checks entirely.
        //
        // `verifyInstaller()V` is private with the single `this` parameter
        // (`p0`); jumping straight to `loadUserData()V` (private, same class)
        // continues the normal startup flow. Every path of the original body
        // leads to either `finishAffinity()` or `validateSignature()`, so the
        // early return cannot skip anything else.
        //
        VerifyInstallerFingerprint.method.addInstructions(
            0,
            """
                invoke-direct {p0}, Lmk/neptun/neptunapp/Splash;->loadUserData()V
                return-void
            """,
        )

        //
        // Target 2 — report "no restricted app installed".
        //
        // `getRestrictApp()` is public with no parameters and
        // `.registers 6`, so v0 is a valid local; `Splash` only compares the
        // result against null.
        //
        RestrictAppCheckFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )
    }
}
