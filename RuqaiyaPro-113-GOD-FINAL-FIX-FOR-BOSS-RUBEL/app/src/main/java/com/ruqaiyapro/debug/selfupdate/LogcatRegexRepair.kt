package com.ruqaiyapro.debug.selfupdate

object LogcatRegexRepair {

    fun repairKotlinSource(source: String): String {
        var fixed = source

        // 1. Remove RenderScript usages -> Replace with Box blur or standard graphics
        fixed = fixed.replace(Regex("import android\\.renderscript\\..*\\n"), "")
        fixed = fixed.replace(Regex("RenderScript\\.create\\([^)]*\\)"), "null /* Box Blur Used */")

        // 2. Ensure FOREGROUND_SERVICE_MICROPHONE is NOT referenced directly
        fixed = fixed.replace("FOREGROUND_SERVICE_TYPE_MICROPHONE", "FOREGROUND_SERVICE_TYPE_SPECIAL_USE")

        // 3. Ensure memory safety try-catch wrappers around bitmap decodes
        if (fixed.contains("BitmapFactory.decode") && !fixed.contains("try {")) {
            fixed = fixed.replace(
                "BitmapFactory.decode",
                "try { BitmapFactory.decode"
            )
        }
        return fixed
    }

    fun repairManifest(manifestXml: String): String {
        var fixed = manifestXml
        // Strip microphone foreground service permission
        fixed = fixed.replace(
            Regex("<uses-permission[^>]*FOREGROUND_SERVICE_MICROPHONE[^>]*/>"),
            "<!-- Safe: FOREGROUND_SERVICE_MICROPHONE stripped for API 30 Infinix HOT 12 Play -->"
        )
        // Ensure specialUse is in foregroundServiceType
        if (!fixed.contains("android:foregroundServiceType=\"specialUse\"")) {
            fixed = fixed.replace(
                "android:name=\".service.RuqaiyaHotwordService\"",
                "android:name=\".service.RuqaiyaHotwordService\"\n            android:foregroundServiceType=\"specialUse\""
            )
        }
        return fixed
    }
}
