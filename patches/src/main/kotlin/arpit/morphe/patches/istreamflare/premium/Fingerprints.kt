package arpit.morphe.patches.istreamflare.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * The instruction shape shared by every entitlement parser of the app:
 *
 * ```smali
 * const-string vN, "subscription_type"
 * const/4 vM, 0x0
 * invoke-interface {vA, vN, vM}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 * move-result-object vB          <-- patch point
 * invoke-static {vB}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;
 * ```
 *
 * Verified for 25.0.0 in smali/classes9: the string is always the key of the
 * `SharedPreferences.getString` call, its result immediately lands in a register
 * and is wrapped by `String.valueOf`, and no other instruction sits between the
 * three. The payment-activity `"subscription_type"` strings use `put`/JSON
 * `get` calls instead and cannot match.
 */
private val subscriptionTypeReadFilters = listOf(
    string("subscription_type"),
    methodCall(
        definingClass = "Landroid/content/SharedPreferences;",
        name = "getString",
        parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
        returnType = "Ljava/lang/String;",
    ),
    opcode(Opcode.MOVE_RESULT_OBJECT, InstructionLocation.MatchAfterImmediately()),
)

/**
 * Matches the six identical `loadUserSubscriptionDetails()V` entitlement
 * parsers of 25.0.0 (all in smali/classes9):
 *
 * - `Lmk/neptun/neptunapp/MovieDetails;`
 * - `Lmk/neptun/neptunapp/WebSeriesDetails;`
 * - `Lmk/neptun/neptunapp/LiveTv;`
 * - `Lmk/neptun/neptunapp/LiveTvGenreDetailsActivity;`
 * - `Lmk/neptun/neptunapp/LiveTVSearch;`
 * - `Lmk/neptun/neptunapp/fragment/HomeFragment;`
 *
 * Each parser reads the digits of the stored `subscription_type` preference
 * ('1' -> removeAds, '2' -> playPremium, '3' -> downloadPremium, anything else
 * -> all three false) and writes the three booleans every ad placement and
 * premium play/download gate reads.
 *
 * The seventh copy of the same parser lives in the Application class
 * (`AppOpenAdManagerOnCreateFingerprint`) inside its `onCreate()` and needs its
 * own fingerprint because the method name differs.
 */
object SubscriptionTypeParserFingerprint : Fingerprint(
    name = "loadUserSubscriptionDetails",
    accessFlags = listOf(AccessFlags.PRIVATE),
    returnType = "V",
    parameters = emptyList(),
    filters = subscriptionTypeReadFilters,
)

/**
 * Matches the seventh entitlement parser copy, embedded in `onCreate()V` of the
 * app's Application class
 * `Lmk/neptun/neptunapp/utils/AppOpenAdManagerActivity;` (the class name is
 * misleading — it is registered as `android:name` of `<application>`, not as an
 * activity).
 *
 * Its three booleans gate the app-open ad managers (AdMob and Wortise), so this
 * copy must be forced along with the other six or app-open ads keep showing.
 *
 * Verified for 25.0.0 in smali/classes9: `.method public onCreate()V` with no
 * parameters (Application.onCreate) and the same filter shape as the named
 * parsers, with the `move-result-object v0` at line 258.
 */
object AppOpenAdManagerOnCreateFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/utils/AppOpenAdManagerActivity;",
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = emptyList(),
    filters = subscriptionTypeReadFilters,
)

/**
 * Matches the two `const-string "nosub"` comparisons that show the
 * non-dismissible `SubscriptionExpiredDialog`:
 *
 * - `Lmk/neptun/neptunapp/MovieDetails;->onCreate` (smali line 5934)
 * - `Lmk/neptun/neptunapp/WebSeriesDetails;->onCreate` (smali line 5568)
 *
 * Both compare `"nosub".equals(prefs.getString("subscriptionStatus", ""))` and
 * show the expired-subscription nag when the server flags the account. Exactly
 * two such strings exist in the whole app (verified by search over all 10 smali
 * trees).
 */
object SubscriptionExpiredNagFingerprint : Fingerprint(
    filters = listOf(string("nosub")),
)
