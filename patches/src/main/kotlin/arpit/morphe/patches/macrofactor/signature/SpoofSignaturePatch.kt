package arpit.morphe.patches.macrofactor.signature

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.byteArrayOf
import app.morphe.util.getEndEntityCertificate
import java.security.MessageDigest

// SHA-1 of the original signing certificate (see analysis notes/recon.md), used as a
// fallback only when the certificate cannot be read from the input APK.
private const val FALLBACK_CERT_HASH = "44 40 CF FB DA 84 8C 6C 8B 9E 02 84 79 50 74 58 79 9D 51 2C"

/**
 * Internal helper patch (not user-visible).
 *
 * Spoofs the signing-certificate hash read by GMS/Firebase by making
 * [GetSignatureHashFingerprint] return the SHA-1 of the ORIGINAL signing certificate.
 * The 20 bytes are computed at patch time from the input APK's signing certificate
 * (fallback: the known hash of the genuine Play signature).
 *
 * This is what keeps Firebase App Check / Installations / Auth working when the
 * patched build is mounted over the genuine Play-signed install.
 */
val signatureHashSpoofPatch = bytecodePatch(
    default = false
) {
    execute {
        val certificate = runCatching {
            getEndEntityCertificate(packageMetadata.signingCertificates)
        }.getOrNull()

        val certHash = certificate
            ?.let { MessageDigest.getInstance("SHA-1").digest(it.encoded) }
            ?: byteArrayOf(FALLBACK_CERT_HASH)

        // .registers 3 => p0/p1 are the parameters, v0 is a free local register.
        // Returns before any original instruction runs.
        GetSignatureHashFingerprint.method.addInstructions(
            0,
            """
                const/16 v0, 0x14
                new-array v0, v0, [B
                fill-array-data v0, :original_cert_hash
                return-object v0

                :original_cert_hash
                .array-data 1
                    ${certHash.joinToString(separator = "\n") { "0x%02Xt".format(it.toInt() and 0xFF) }}
                .end array-data
            """
        )
    }
}
