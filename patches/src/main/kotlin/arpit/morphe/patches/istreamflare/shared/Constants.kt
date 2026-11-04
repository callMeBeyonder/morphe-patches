package arpit.morphe.patches.istreamflare.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_ISTREAMFLARE = Compatibility(
        name = "iStreamFlare",
        packageName = "mk.neptun.neptunapp",
        // Sideloaded single APK from the official site (istreamflare.in);
        // no split container.
        apkFileType = ApkFileType.APK,
        // Magenta of the app's default theme color (AppConfig.primeryThemeColor
        // "#DF4674").
        appIconColor = 0xDF4674,
        targets = listOf(
            AppTarget(
                // versionName from AndroidManifest.xml; versionCode 25.
                version = "25.0.0"
            )
        )
    )
}
