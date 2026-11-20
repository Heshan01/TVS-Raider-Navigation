package com.heshan.tvsraider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONException

/**
 * Ride history database helper for rides.db.
 */
class RideDb private constructor(private val app: Context) : SQLiteOpenHelper(app, "rides.db", null, VERSION) {
    companion object {
        const val T = "rides"
        private const val VERSION = 1
        const val OLD_KEY = "rides"

        @Volatile private var inst: RideDb? = null

        @Synchronized
        fun get(c: Context): SQLiteDatabase {
            if (inst == null) inst = RideDb(c.applicationContext)
            val helper = inst!!
            val db = helper.writableDatabase
            if (helper.imported) {
                helper.imported = false
                BikeService.prefs(c).edit().remove(OLD_KEY).apply()
            }
            return db
        }

        fun values(r: RideTracker.Ride): ContentValues {
            val v = ContentValues()
            v.put("start_ms", r.start)
            v.put("end_ms", r.end)
            v.put("odo_start", r.odoStart)
            v.put("odo_end", r.odoEnd)
            v.put("max_kmh", r.maxKmh)
            v.put("moving_ms", r.movingMs)
            v.put("speed_km", r.speedKm)
            v.put("km", r.km())
            v.put("avg_kml", Math.max(0, r.avgKmL))
            return v
        }
    }

    private var imported = false

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $T (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "start_ms INTEGER NOT NULL," +
                    "end_ms INTEGER NOT NULL," +
                    "odo_start INTEGER NOT NULL," +
                    "odo_end INTEGER NOT NULL," +
                    "max_kmh INTEGER NOT NULL," +
                    "moving_ms INTEGER NOT NULL," +
                    "speed_km REAL NOT NULL," +
                    "km REAL NOT NULL," +
                    "avg_kml INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE INDEX rides_start ON $T (start_ms)")
        importOld(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, from: Int, to: Int) {
        // Upgrade migrations
    }

    private fun importOld(db: SQLiteDatabase) {
        val p = BikeService.prefs(app)
        val json = p.getString(OLD_KEY, null) ?: return
        var n = 0
        try {
            val a = JSONArray(json)
            for (i in 0 until a.length()) {
                db.insert(T, null, values(RideStore.fromJson(a.getJSONObject(i))))
                n++
            }
        } catch (e: JSONException) {
            BikeService.log("Old ride history unreadable, not copied: $e")
        }
        imported = true
        BikeService.log("RIDE history moved to database ($n rides)")
    }
}
