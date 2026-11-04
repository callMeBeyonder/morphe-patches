package arpit.morphe.patches.istreamflare.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches `Lmk/neptun/neptunapp/Splash;->verifyInstaller()V`
 * (smali/classes9, smali line 3390, `.registers 5`).
 *
 * The single integrity funnel of the startup chain: `onCreate` ->
 * `loadData()` -> `verifyInstaller()`. When the operator enables
 * `VERIFY_INSTALLER` it exits the app unless it was installed from the Play
 * Store; either way it tail-calls `validateSignature()`, which exits the app on
 * any certificate that differs from the server-provided `RELEASE_SIGNATURE`
 * (Firebase Remote Config `VALIDATE_SIGNATURE` / `get_config`). Both checks are
 * fatal for a re-signed patched build.
 *
 * Pinned by the unique toast text of the installer check; `validateSignature()`
 * itself has no callers outside this method (verified for 25.0.0).
 */
object VerifyInstallerFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/Splash;",
    name = "verifyInstaller",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(string("App not Installed from Google play Store!")),
)

/**
 * Matches `Lmk/neptun/neptunapp/utils/HelperUtils;->getRestrictApp()Landroid/content/pm/ApplicationInfo;`
 * (smali/classes9, smali line 122, `.registers 6`).
 *
 * Enumerates every installed app (the APK requests `QUERY_ALL_PACKAGES`) and
 * returns the first entry from a blocklist of network-analysis and modification
 * tools — HttpCanary, SSLCapture, PacketCapture, pcap-remote, Package Sniffer,
 * WiFi Analyzer, ProxyMonitor, WiCap, Lucky Patcher's installer and one
 * obfuscated package. `Splash` reacts by showing a "Restricted App!" dialog and
 * exiting, which also blocks legitimate testing of patched builds on
 * devices that have any of these tools installed.
 *
 * Pinned by the unique blocklist package name. Returning null is the same
 * outcome as an unrestricted device, which `Splash` handles as "not
 * restricted".
 */
object RestrictAppCheckFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/utils/HelperUtils;",
    name = "getRestrictApp",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Landroid/content/pm/ApplicationInfo;",
    parameters = emptyList(),
    filters = listOf(string("com.guoshi.httpcanary")),
)
