package arpit.morphe.patches.runna.premium

import app.morphe.patcher.Fingerprint

/**
 * Matches `EntitlementInfosMapperKt.map(EntitlementInfos)` of RevenueCat's
 * `hybridcommon` module — the conversion step between the native `CustomerInfo`
 * object and the map handed to the React Native bridge.
 *
 * Runna is a React Native app, so its JavaScript bundle never touches the native
 * `EntitlementInfos` object directly: `CustomerInfoMapperKt.map(customerInfo)`
 * builds the bridge payload and delegates the entitlement subtree to this method,
 * whose result is exposed to JS as `customerInfo.entitlements`. Returning a
 * synthetic map here is enough for every `entitlements.active.premium` and
 * `entitlements.all.premium` check performed by the bundle.
 *
 * The RevenueCat SDK keeps its class and method names in release builds (only the
 * host app is minified), so this fingerprint is name-stable across app versions.
 */
object EntitlementInfosMapperFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/hybridcommon/mappers/EntitlementInfosMapperKt;",
    name = "map",
    parameters = listOf("Lcom/revenuecat/purchases/EntitlementInfos;"),
    returnType = "Ljava/util/Map;",
)
