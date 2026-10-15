package arpit.morphe.patches.stronglifts.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Google Play PairIP protection runtime.
 *
 * The app wraps the real `Application` and `AppComponentFactory` with `com.pairip.*` classes
 * that verify the APK signature, check the Play license, and decrypt a native VM payload with
 * a Play-provisioned keystore key. Re-signing the APK trips all of them.
 *
 * These fingerprints feed the DETACHED, sideload-only bypasses in PairIpBypassPatch.kt.
 * Sideload of a re-signed build is structurally non-viable for this app (the relocated
 * string literals require the Play-provisioned keystore key), and under the supported
 * root + mount deployment the VM must keep running — see the note block there and
 * analysis/stronglifts/notes/deploy-failure.md.
 *
 * The `com.pairip.*` class names are versioned and may be relocated by a future build.
 * These fingerprints therefore anchor on string literals, native method declarations and
 * the instruction shape of each check instead of the class names alone.
 */

/**
 * Matches `SignatureCheck.verifyIntegrity(Context)`.
 *
 * Computes the APK signature digest and throws `SignatureTamperedException`
 * ("Apk signature is invalid.") when it does not match the stored Play values.
 * Sole caller is `com.pairip.application.Application.attachBaseContext` (no try/catch).
 */
object SignatureCheckVerifyIntegrityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Apk signature is invalid.", "Signature check ok")
)

/**
 * Matches `LicenseClient.checkLicense(Context)`.
 *
 * On a `NOT_LICENSED` verdict it starts the PairIP paywall activity and schedules an app
 * shutdown. Called from `Application.attachBaseContext` and `LicenseContentProvider`.
 */
object LicenseClientCheckLicenseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context.", "Skipping license check in isolated process.")
)

/**
 * Matches `KeyImportAppComponentFactory.isEncryptionKeyPresent()`.
 *
 * While it returns `false`, all five `instantiate*` methods return Play key import
 * placeholders instead of the real application components (the app starts as a no-op shell).
 *
 * `KeyImport.Factory` is the log tag of this class only — it separates this method from the
 * identically named method of `KeyImportPlaceholderBackupAgent`, which logs with
 * `KeyImport.BackupAgent` and must not be patched.
 */
object KeyImportIsEncryptionKeyPresentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        // The first instruction of this method reads the keystore manager (the placeholder
        // backup agent variant first reads its cached Boolean field instead).
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            name = "keyStoreManager",
            type = "Lcom/pairip/vmencryption/KeyStoreManager;",
            location = MatchFirst()
        ),
        methodCall(
            definingClass = "Lcom/pairip/vmencryption/KeyStoreManager;",
            name = "isEncryptionKeyPresent",
            parameters = listOf("I"),
            returnType = "Z"
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately())
    ),
    strings = listOf("KeyImport.Factory", "Failed to check if encryption key is present.")
)

/**
 * Matches `VMRunner.invoke(String, Object[])`.
 *
 * Reads an encrypted payload from `assets/`, decrypts it with the Play-provisioned keystore
 * key and passes it to the native `executeVM`. The key cannot exist on a re-signed sideload,
 * and the `IllegalStateException` from the decryptor is not caught, so every call site
 * (startup triggers and 22 virtualized library methods) would crash the process.
 *
 * SIDELOAD-ONLY anchor: no-oping this method also stops the VM from materializing the
 * relocated string literals. Never apply under root + mount, where the VM must keep running.
 */
object VmRunnerInvokeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/String;", "[Ljava/lang/Object;"),
    strings = listOf("Error while loading bytecode.", "Executing ")
)
