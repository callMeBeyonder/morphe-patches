package arpit.morphe.patches.cricfy.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Locates the app's ad manager class. In 7.3 the class is `Lm7;` (R8 full mode short
 * name, changes between app versions, so it is never referenced directly).
 *
 * The constructor takes the `Application`, the `SharedPreferences` that stores the
 * downloaded ad configuration and the kotlinx.serialization JSON parser.
 *
 * Verified against `analysis/cricfy-tv/smali/classes/m7.smali`:
 * - `public constructor <init>(Landroid/app/Application;Landroid/content/SharedPreferences;Lqi1;)V`
 *   at line 19 (`.registers 6`) — the R8-renamed parser class is declared as an
 *   obfuscated object type (`"L"`).
 * - Instruction order of the filters: `const-string "ass"` (line 81) then
 *   `Lcom/unity3d/ads/UnityAds;->initialize` (line 157) — the app's only call site.
 */
object AdsManagerClassFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "Landroid/app/Application;",
        "Landroid/content/SharedPreferences;",
        "L",
    ),
    filters = listOf(
        // SharedPreferences key holding the Base64 encoded ad config.
        string("ass"),
        // Unity Ads bootstrap, gated on the downloaded config.
        methodCall(
            definingClass = "Lcom/unity3d/ads/UnityAds;",
            name = "initialize",
        ),
    ),
)

/**
 * Matches the only scheduler that loads a Unity full-screen ad:
 * `public final d(Landroid/app/Activity;)V` (line 1963, `.registers 30`) in the ad
 * manager class. It is called when the player screen, a channel or the floating
 * player opens.
 *
 * Verified instruction order of the filters (`m7.smali`):
 * `const-string "UmV3YXJkZWRfQW5kcm9pZA=="` (line 2805, Base64 of the
 * `Rewarded_Android` placement) then `UnityAds;->load(String, IUnityAdsLoadListener)`
 * (line 2830). Without `load` nothing can `show`.
 */
object UnityAdScheduleFingerprint : Fingerprint(
    classFingerprint = AdsManagerClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
    filters = listOf(
        // Base64 literal of the ad placement name.
        string("UmV3YXJkZWRfQW5kcm9pZA=="),
        methodCall(
            definingClass = "Lcom/unity3d/ads/UnityAds;",
            name = "load",
        ),
    ),
)

/**
 * Matches the single boolean that decides whether the sponsor banner is shown:
 * `public final b()Z` (line 972, `.registers 4`) in the ad manager class — the only
 * `()Z` method of the class.
 *
 * Verified instruction order of the filters (`m7.smali`):
 * - line 980: `iget-object v1, v0, Ls6;->c:Lag3;` — the SponsorBanner config field.
 * - line 984: `iget-boolean v1, v1, Lag3;->a:Z` — `SponsorBanner.enable`.
 * - line 1007: `const/4 v0, 0x4` — sponsor list index 4 (banner entry).
 * - line 1026: `iget-boolean p0, p0, Lxf3;->a:Z` — `Sponsor.enabled` of that entry.
 *
 * `Lag3`/`xf3` are R8 short names and may change between app versions; a renamed
 * class makes the fingerprint fail loudly instead of matching the wrong method.
 */
object SponsorBannerFingerprint : Fingerprint(
    classFingerprint = AdsManagerClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Lag3;"),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = "Lag3;"),
        literal(4),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, definingClass = "Lxf3;"),
    ),
)

/**
 * Matches the sponsor gate that runs before a channel is opened:
 * `public final g(Lcom/android/vending/activities/MainActivity;Ljava/lang/String;Li41;)V`
 * (line 3238, `.registers 17`) in the ad manager class. The gate picks the first
 * enabled sponsor entry of indices 1..3: index 1 shows the locked channel dialog,
 * index 2 opens a countdown WebActivity and index 3 opens an in-app page or the
 * external browser (the reported browser-redirect ad).
 *
 * Verified instruction order of the filters (`m7.smali`):
 * - line 3423: `invoke-virtual {p0, p2, v6, v9}, Lm7;->f(Ljava/lang/String;II)Z` —
 *   the frequency gate, matched by signature so the R8 name is irrelevant.
 * - line 3467: `UnlockDilaogBinding;->inflate` — the locked channel dialog (typo in
 *   the databinding class name is the app's own and is stable).
 * - line 3479: `const v4, 0x7f140078` — `string/channel_locked` of that dialog.
 */
object SponsorGateFingerprint : Fingerprint(
    classFingerprint = AdsManagerClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Lcom/android/vending/activities/MainActivity;",
        "Ljava/lang/String;",
        "L",
    ),
    filters = listOf(
        methodCall(
            definingClass = "this",
            parameters = listOf("Ljava/lang/String;", "I", "I"),
            returnType = "Z",
        ),
        methodCall(
            definingClass = "Lcom/cricfytv/sports/databinding/UnlockDilaogBinding;",
            name = "inflate",
        ),
        literal(0x7f140078),
    ),
)

/**
 * Matches `public final onCreate(Landroid/os/Bundle;)V` (line 3769, `.registers 27`)
 * of the main activity. The method contains the self-hosted full-screen ad
 * (`CustomAdFull` dialog) that is shown every `sponsors[0].adNumber` opens, as well
 * as the Telegram promo dialog (`w7`/`DialogTgJoinBinding`) that runs before it.
 *
 * Verified instruction order of the filters (`MainActivity.smali`):
 * - line 5486/5502: `"dialog_alert"` / `"start_alert"` — the promo dialog prefs.
 * - line 5839: `const-string "fullTries"` — the ad open-count preference read.
 * - line 5922: `CustomAdFullBinding;->inflate` — the ad dialog inflation.
 */
object CustomAdFullFingerprint : Fingerprint(
    // Suffix match, survives package/class renames.
    definingClass = "/activities/MainActivity;",
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        string("dialog_alert"),
        string("start_alert"),
        string("fullTries"),
        methodCall(
            definingClass = "Lcom/cricfytv/sports/databinding/CustomAdFullBinding;",
            name = "inflate",
        ),
    ),
)
