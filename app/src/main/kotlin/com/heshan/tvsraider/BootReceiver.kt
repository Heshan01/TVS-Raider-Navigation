package com.heshan.tvsraider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-connects to the bike after phone restart or app update if Ride Mode is enabled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent?) {
        val a = i?.action ?: return
        if (Intent.ACTION_BOOT_COMPLETED != a && Intent.ACTION_MY_PACKAGE_REPLACED != a) return
        BikeService.initLog(c)
        if (!BikeService.prefs(c).getBoolean("rideMode", true)) {
            BikeService.log(
                "Ride mode is off: not reconnecting after " +
                        if (Intent.ACTION_BOOT_COMPLETED == a) "phone restart" else "app update"
            )
            return
        }
        BikeService.log(
            "Auto-connect after " +
                    if (Intent.ACTION_BOOT_COMPLETED == a) "phone restart" else "app update"
        )
        BikeService.autoStart(c, "restart")
    }
}
