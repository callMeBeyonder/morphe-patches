package arpit.morphe.patches.stronglifts.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import arpit.morphe.patches.stronglifts.shared.Constants.COMPATIBILITY_STRONGLIFTS
import arpit.morphe.util.requireRootMount

@Suppress("unused")
val strongLiftsUnlockProPatch = bytecodePatch(
    name = "Unlock Pro (Root)",
    description = "Unlocks all Pro and PowerPack features. Requires root: the patched " +
        "build must be mounted over the genuine Play-signed install — a normal " +
        "sideload will not run.",
    default = true
) {
    compatibleWith(COMPATIBILITY_STRONGLIFTS)

    // Mirrors MacroFactor: only offered for root + MOUNT installs.
    availability(requireRootMount)

    // Deliberately no PairIP dependency. This patch targets root + mount deployments,
    // where the genuine Play-signed APK stays installed underneath and the PairIP VM runs
    // untouched (it must keep running to materialize relocated strings). Sideloading a
    // re-signed build is structurally non-viable for this app — the detached, sideload-only
    // PairIP bypasses live in ../pairip/PairIpBypassPatch.kt and must not be re-attached
    // here.

    execute {
        // Primary gates. Every premium UI surface (programs, progress graphs, timers,
        // exercise editing, history, warmup screens) funnels through these two methods.
        FeatureRepositoryHasProSubscriptionFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        FeatureRepositoryHasPowerPackFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        // Secondary belt-and-suspenders. PurchaseCoordinator stores the RevenueCat
        // entitlement state into shared prefs through these methods on every launch,
        // so forcing them keeps the stored flags consistent with the forced gates
        // (and with the FeatureRepository log output).
        RevenueCatHasProSubscriptionFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )

        RevenueCatHasPowerPackFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}
