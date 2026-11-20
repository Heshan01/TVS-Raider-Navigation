package com.heshan.tvsraider

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.regex.Pattern

/**
 * Reads Google Maps ongoing navigation notification and forwards turn-by-turn directions to the bike.
 */
class MapsListener : NotificationListenerService() {
    companion object {
        const val MAPS: String = "com.google.android.apps.maps"

        @Volatile var lastRaw: String = "(no Google Maps notification yet)"
        @Volatile var lastParsed: String = ""
        @Volatile var current: TvsProtocol.Nav? = null
        @Volatile var road: String = ""
        @Volatile var eta: String = ""
        @Volatile var tripIntent: PendingIntent? = null

        private val ETA_PATTERN: Pattern = Pattern.compile("(\\d{1,2}[:.]\\d{2}\\s*(?:[AaPp]\\.?[Mm]\\.?)?)\\s*ETA")
        private var lastKey: String = ""
        private var lastSentAt: Long = 0
        private var lastIcon: String = ""
        private var navActive: Boolean = false

        fun mirrorOn(c: Context): Boolean = BikeService.prefs(c).getBoolean("mirrorNav", true)

        private fun shortHash(h: String): String = if (h.length > 12) "#" + Integer.toHexString(h.hashCode()) else h

        private fun str(c: CharSequence?): String = c?.toString()?.trim() ?: ""
    }

    private val h = Handler(Looper.getMainLooper())
    private var pending: TvsProtocol.Nav? = null
    private val flush = Runnable { flushPending() }

    override fun onListenerConnected() {
        BikeService.initLog(this)
        BikeService.log("Notification reader connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || MAPS != sbn.packageName) return
        val n = sbn.notification ?: return
        if ((n.flags and Notification.FLAG_ONGOING_EVENT) == 0) return
        if (n.contentIntent != null) tripIntent = n.contentIntent
        val e = n.extras
        val title = str(e.getCharSequence(Notification.EXTRA_TITLE))
        val text = str(e.getCharSequence(Notification.EXTRA_TEXT))
        var sub = str(e.getCharSequence(Notification.EXTRA_SUB_TEXT))
        val big = str(e.getCharSequence(Notification.EXTRA_BIG_TEXT))
        val info = str(e.getCharSequence(Notification.EXTRA_INFO_TEXT))
        val summary = str(e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        if (sub.isEmpty()) sub = if (info.isEmpty()) summary else info

        val icon = IconReader.read(this, n)
        IconReader.logIfNew(icon)
        val iconHash = icon?.hash ?: "none"

        val raw = "title=[$title] text=[$text] sub=[$sub] big=[$big]"
        if (raw != lastRaw || iconHash != lastIcon) {
            lastRaw = raw
            lastIcon = iconHash
            BikeService.log("MAPS $raw icon=" + shortHash(iconHash))
        }

        val nav = MapsParser.parse(title, text, sub, big, icon?.picto ?: -1) ?: return
        if (mirrorOn(this)) BikeService.autoStart(this, if (navActive) "maps" else "maps-start")
        navActive = true
        road = if (text.startsWith("toward ")) text.substring(7) else text
        val em = ETA_PATTERN.matcher(sub)
        eta = if (em.find()) em.group(1)?.trim() ?: "" else ""
        val key = nav.toString()
        if (key == lastKey) return

        pending = nav
        h.removeCallbacks(flush)
        val wait = 1000L - (System.currentTimeMillis() - lastSentAt)
        if (wait <= 0) flushPending() else h.postDelayed(flush, wait)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null || MAPS != sbn.packageName) return
        val n = sbn.notification ?: return
        if ((n.flags and Notification.FLAG_ONGOING_EVENT) == 0) return
        h.removeCallbacks(flush)
        pending = null
        lastKey = ""
        MapsParser.reset()
        IconReader.forgetLogged()
        navActive = false
        current = null
        tripIntent = null
        road = ""
        eta = ""
        BikeService.log("Google Maps navigation ended")
        BikeService.instance?.stopNav()
    }

    private fun flushPending() {
        val nav = pending
        pending = null
        if (nav == null) return
        val key = nav.toString()
        lastKey = key
        lastSentAt = System.currentTimeMillis()
        lastParsed = key
        current = nav
        val s = BikeService.instance
        if (!mirrorOn(this)) return
        if (s != null) s.sendNav(nav)
        else BikeService.log("Parsed $key (bike service not running)")
    }
}
