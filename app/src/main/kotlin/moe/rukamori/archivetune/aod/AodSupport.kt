/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.aod

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

object AodSupport {
    private const val AOD_AVAILABLE_RESOURCE = "config_dozeAlwaysOnDisplayAvailable"

    @SuppressLint("DiscouragedApi")
    fun isSupported(context: Context): Boolean {
        val resourceId =
            context.resources.getIdentifier(
                AOD_AVAILABLE_RESOURCE,
                "bool",
                "android",
            )
        if (resourceId == 0) return false

        return try {
            context.resources.getBoolean(resourceId)
        } catch (_: Resources.NotFoundException) {
            false
        }
    }
}
