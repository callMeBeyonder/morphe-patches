package arpit.morphe.patches.stronglifts.premium

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Matches `FeatureRepository.hasProSubscription()`.
 *
 * The primary premium gate. Every premium UI surface funnels through this method
 * (17 app classes: screens, ViewModels and services). The log string is an R8 literal
 * and survives name obfuscation.
 */
object FeatureRepositoryHasProSubscriptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("hasProSubscription(): ")
)

/**
 * Matches `FeatureRepository.hasPowerPack()`.
 *
 * Returns `storedPro || storedPowerPack`, so PowerPack implies Pro level access.
 * All three log strings belong to this single method.
 */
object FeatureRepositoryHasPowerPackFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("hasPowerPack(): ", " (hasProSubscription=", ", storedPowerPack=")
)

/**
 * Matches `RevenueCatRepository.hasProSubscription(CustomerInfo)`.
 *
 * Secondary belt-and-suspenders gate: `PurchaseCoordinator` maps the RevenueCat
 * entitlement state into shared prefs through this method, so forcing it keeps the
 * stored flags consistent with the forced gates. Not `final` (interface implementation).
 */
object RevenueCatHasProSubscriptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf("Lcom/revenuecat/purchases/CustomerInfo;"),
    strings = listOf("SL Pro")
)

/**
 * Matches `RevenueCatRepository.hasPowerPack(CustomerInfo)`.
 *
 * Same secondary role as [RevenueCatHasProSubscriptionFingerprint], for the
 * "SL PowerPack" entitlement.
 */
object RevenueCatHasPowerPackFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf("Lcom/revenuecat/purchases/CustomerInfo;"),
    strings = listOf("SL PowerPack")
)
