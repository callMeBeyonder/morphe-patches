package arpit.morphe.patches.stronglifts.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_STRONGLIFTS = Compatibility(
        name = "StrongLifts",
        packageName = "com.stronglifts.app",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xD22E2E,
        targets = listOf(
            AppTarget(
                version = "4.4.1"
            )
        )
    )
}
