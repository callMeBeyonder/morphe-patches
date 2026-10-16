package arpit.morphe.patches.stronglifts.pairip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/*
 * =============================================================================
 * DETACHED PAIRIP BYPASSES — SIDELOAD-ONLY LEGACY, NOT PART OF ANY PUBLIC PATCH
 * =============================================================================
 *
 * StrongLifts 4.4.1 ships with Google Play PairIP protection. The public
 * "Unlock Pro (Root)" patch intentionally does NOT depend on anything in this file.
 *
 * (a) Sideload is structurally non-viable for this app.
 *     PairIP relocates string literals into static fields of ~217 obfuscated "holder"
 *     classes that have no DEX write-sites; the PairIP VM materializes them at runtime.
 *     Decryption requires the Play-provisioned keystore key
 *     ("pairip_encryption_wrapping_key_<versionCode>"), which only exists for the
 *     genuine Play-signed install. A re-signed build leaves those fields null, and the
 *     first null-intolerant consumer kills the process during ContentProvider init,
 *     before Application.onCreate. Full analysis:
 *     analysis/stronglifts/notes/deploy-failure.md.
 *
 * (b) Under the supported root + mount deployment the PairIP VM WORKS and must keep
 *     running. Flow: install the original from Play → launch once (keystore key
 *     provisioning) → mount the patched APK over it via `morphe-cli patch --mount -i`.
 *     The installed package record keeps the original signature and key, so PairIP's
 *     signature/license checks pass as-is and must not be patched. Crucially,
 *     [pairIpVmNoOpPatch]'s `VMRunner.invoke → null` no-op MUST NOT be applied either:
 *     it would stop the VM from materializing the relocated strings and re-break the
 *     app.
 *
 * (c) Re-attach selectively only if mount testing reveals a concrete failure.
 *     If (and only if) a tamper or license verdict shows up under mount, a future patch
 *     may depend on [pairIpBypassPatch] (signature, license and key-import gates).
 *     [pairIpVmNoOpPatch] stays detached for mount deployments, permanently.
 */

// Signature / license / key-import neutralization. Only meaningful for a re-signed
// build; kept as the selective re-attach candidate for mount-failure debugging.
val pairIpBypassPatch = bytecodePatch {
    execute {
        // Tamper check throws SignatureTamperedException after re-signing.
        SignatureCheckVerifyIntegrityFingerprint.method.addInstructions(0, "return-void")

        // Play license verdict would start the PairIP paywall and schedule an app shutdown.
        LicenseClientCheckLicenseFingerprint.method.addInstructions(0, "return-void")

        // Take the "original component" branch so the app is not replaced by the
        // Play Store key import placeholders.
        KeyImportIsEncryptionKeyPresentFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
    }
}

// SIDELOAD-ONLY — NEVER apply under root + mount.
//
// No-ops every VM invocation. On a re-signed sideload (without the Play-provisioned
// keystore key) this was the only way to stop the uncaught IllegalStateException from
// crashing the process, but it also stops the VM from materializing relocated string
// literals. Under mount the VM is functional and required, so this patch must remain
// unattached.
val pairIpVmNoOpPatch = bytecodePatch {
    execute {
        VmRunnerInvokeFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """
        )
    }
}
