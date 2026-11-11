package arpit.morphe.patches.runna.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import arpit.morphe.patches.runna.shared.Constants.COMPATIBILITY_RUNNA

/** Entitlement identifier configured in Runna's RevenueCat dashboard. */
private const val PREMIUM_ENTITLEMENT = "premium"

/** Store SKU reported as the source of the active entitlement. */
private const val PREMIUM_PRODUCT = "runna_annual_2w"

/**
 * Unlocks Runna Premium.
 *
 * Runna is React Native and gates its paid surfaces on a RevenueCat entitlement
 * called `premium`. The Hermes bundle only ever sees
 * `customerInfo.entitlements.active.premium` / `.all.premium`, which is produced
 * by RevenueCat's `hybridcommon` bridge — not by Runna's own code. Replacing
 * `EntitlementInfosMapperKt.map()` with a synthetic result makes the JS layer see
 * an active `premium` entitlement, so the premium UI, plan setup and the
 * `isPremium` selectors unlock.
 *
 * Everything happens on-device: RevenueCat caches entitlement data locally and the
 * bridge is fed from that cache, so no request to the RevenueCat API is involved.
 * Runna's own backend still reports `userProfile.subscriptionStatus`, and
 * plan content the backend only ships to paying accounts stays server-gated.
 *
 * Deliberately not patched: `CustomerInfo.getActiveSubscriptions()`. Runna's
 * subscription management screen reads it; spoofing it would misreport the
 * account and can break the "restore purchases" flow without unlocking anything
 * extra.
 */
@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks the premium surfaces driven by RevenueCat by reporting an " +
        "active `premium` entitlement to the app's JavaScript layer. Server-delivered " +
        "plan content still requires a real subscription.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_RUNNA)

    execute {
        val mapper = EntitlementInfosMapperFingerprint.method

        // The injected body reuses the parameter register (`p0`) as scratch space and
        // needs v0-v3 (including a wide v2/v3 pair), so five registers total. A
        // one-parameter mapper from R8 has seven; fail loudly instead of emitting
        // invalid smali if a future build ever shrinks it.
        val registerCount = mapper.implementation?.registerCount ?: 0
        check(registerCount >= 5) {
            "EntitlementInfosMapperKt.map() has $registerCount registers; the patch " +
                "needs at least 5 to build the entitlement map."
        }

        mapper.addInstructions(
            0,
            """
                new-instance p0, Ljava/util/HashMap;
                invoke-direct {p0}, Ljava/util/HashMap;-><init>()V

                const-string v0, "identifier"
                const-string v1, "$PREMIUM_ENTITLEMENT"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "isActive"
                const/4 v1, 0x1
                invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v1
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "willRenew"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "periodType"
                const-string v1, "NORMAL"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "latestPurchaseDate"
                const-string v1, "2026-01-01T00:00:00.000Z"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "originalPurchaseDate"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "latestPurchaseDateMillis"
                const-wide v2, 0x19b76daa800L
                invoke-static {v2, v3}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
                move-result-object v1
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "originalPurchaseDateMillis"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "expirationDate"
                const-string v1, "expirationDateMillis"
                const/4 v2, 0x0
                invoke-interface {p0, v0, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                invoke-interface {p0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "store"
                const-string v1, "PLAY_STORE"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "productIdentifier"
                const-string v1, "$PREMIUM_PRODUCT"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "productPlanIdentifier"
                const-string v1, "unsubscribeDetectedAt"
                invoke-interface {p0, v0, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                invoke-interface {p0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "unsubscribeDetectedAtMillis"
                const-string v1, "billingIssueDetectedAt"
                invoke-interface {p0, v0, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                invoke-interface {p0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "billingIssueDetectedAtMillis"
                const/4 v1, 0x0
                invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v1
                const-string v2, "isSandbox"
                invoke-interface {p0, v2, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                const/4 v2, 0x0
                invoke-interface {p0, v0, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "ownershipType"
                const-string v1, "PURCHASED"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "verification"
                const-string v1, "VERIFIED"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                new-instance v1, Ljava/util/HashMap;
                invoke-direct {v1}, Ljava/util/HashMap;-><init>()V

                const-string v0, "$PREMIUM_ENTITLEMENT"
                invoke-interface {v1, v0, p0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                new-instance p0, Ljava/util/HashMap;
                invoke-direct {p0}, Ljava/util/HashMap;-><init>()V

                const-string v0, "active"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "all"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                const-string v0, "verification"
                const-string v1, "VERIFIED"
                invoke-interface {p0, v0, v1}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

                return-object p0
            """,
        )
    }
}
