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
 * Matches the two `SubscriptionExpiredDialog` gates, in
 * `Lmk/neptun/neptunapp/MovieDetails;->onCreate` (smali line 5934) and
 * `Lmk/neptun/neptunapp/WebSeriesDetails;->onCreate` (smali line 5568):
 *
 * ```smali
 * const-string vX, "nosub"
 * invoke-virtual {vX, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
 * move-result p1                 <-- patch point
 * if-eqz p1, :cond_5e
 * ```
 *
 * The stored value is the server-provided `subscriptionStatus`, and the dialog
 * (non-dismissible) is shown when it equals `"nosub"`. Exactly two such
 * literals exist in the whole app (verified by search over all 10 smali trees).
 *
 * The patch forces the `equals` RESULT to false instead of rewriting the
 * `"nosub"` literal to some other value. A literal swap only works while the
 * replacement string happens to be one the backend never sends: 25.0.0 uses
 * `"active"`/`"nosub"`, so `"subscribed"` would be inert today, but any future
 * status vocabulary that adopted it would make the nag appear for legitimate
 * subscribers. Forcing the result is vocabulary-independent.
 */
object SubscriptionExpiredNagFingerprint : Fingerprint(
    filters = listOf(
        string("nosub"),
        methodCall(
            definingClass = "Ljava/lang/String;",
            name = "equals",
            parameters = listOf("Ljava/lang/Object;"),
            returnType = "Z",
        ),
        opcode(Opcode.MOVE_RESULT, InstructionLocation.MatchAfterImmediately()),
    ),
)

/*
 * Per-item premium flags.
 *
 * The screen parsers above only reach gates whose boolean lives on the screen or
 * adapter. Three list adapters instead read the per-item `Play_Premium` flag the
 * API returns for every row (live TV channels and episodes) plus the per-episode
 * `downloadPremium` flag, so those paywalls are unaffected by the parser patch:
 *
 * - `EpisodeListAdepter` -> `EpisodeList.isPlay_Premium()` (play) and
 *   `EpisodeList.isDownloadPremium()` (download)
 * - `LiveTvChannelListAdepter` -> `LiveTvChannelList.isPlay_Premium()`
 * - `LiveTvAllListAdepter` -> `LiveTvAllList.isPlay_Premium()`
 *
 * All four are plain field getters (`iget-boolean` + `return`), so the patch
 * forces each to return true. Verified for 25.0.0 in smali/classes9.
 */

/** `Lmk/neptun/neptunapp/list/EpisodeList;->isPlay_Premium()Z` (line 331). */
object EpisodeIsPlayPremiumFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/list/EpisodeList;",
    name = "isPlay_Premium",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

/** `Lmk/neptun/neptunapp/list/EpisodeList;->isDownloadPremium()Z` (line 322). */
object EpisodeIsDownloadPremiumFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/list/EpisodeList;",
    name = "isDownloadPremium",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

/** `Lmk/neptun/neptunapp/list/LiveTvAllList;->isPlay_Premium()Z` (line 177). */
object LiveTvAllIsPlayPremiumFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/list/LiveTvAllList;",
    name = "isPlay_Premium",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)

/** `Lmk/neptun/neptunapp/list/LiveTvChannelList;->isPlay_Premium()Z` (line 177). */
object LiveTvChannelIsPlayPremiumFingerprint : Fingerprint(
    definingClass = "Lmk/neptun/neptunapp/list/LiveTvChannelList;",
    name = "isPlay_Premium",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
)
