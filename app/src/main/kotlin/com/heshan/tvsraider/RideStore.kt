package com.heshan.tvsraider

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

/**
 * Ride store for querying rides.db and saving the active ride to SharedPreferences.
 */
object RideStore {
    private const val KEY_ACTIVE = "rideActive"
    private const val COLS = "start_ms, end_ms, odo_start, odo_end, max_kmh, moving_ms, speed_km, avg_kml"

    fun recent(c: Context, limit: Int): List<RideTracker.Ride> {
        return query(c, "SELECT $COLS FROM ${RideDb.T} ORDER BY start_ms DESC LIMIT $limit")
    }

    fun between(c: Context, from: Long, to: Long): List<RideTracker.Ride> {
        return query(c, "SELECT $COLS FROM ${RideDb.T} WHERE start_ms >= $from AND start_ms < $to ORDER BY start_ms")
    }

    fun firstStart(c: Context): Long {
        val cursor = RideDb.get(c).rawQuery("SELECT MIN(start_ms) FROM ${RideDb.T}", null)
        cursor.use {
            return if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else 0L
        }
    }

    fun lastOdo(c: Context): Int {
        val cursor = RideDb.get(c).rawQuery("SELECT odo_end FROM ${RideDb.T} ORDER BY start_ms DESC LIMIT 1", null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun query(c: Context, sql: String): List<RideTracker.Ride> {
        val out = ArrayList<RideTracker.Ride>()
        val cursor = RideDb.get(c).rawQuery(sql, null)
        cursor.use { k ->
            while (k.moveToNext()) {
                val r = RideTracker.Ride()
                r.start = k.getLong(0)
                r.end = k.getLong(1)
                r.odoStart = k.getInt(2)
                r.odoEnd = k.getInt(3)
                r.maxKmh = k.getInt(4)
                r.movingMs = k.getLong(5)
                r.speedKm = k.getDouble(6)
                r.avgKmL = k.getInt(7)
                out.add(r)
            }
        }
        return out
    }

    fun add(c: Context, r: RideTracker.Ride) {
        RideDb.get(c).insert(RideDb.T, null, RideDb.values(r))
        BikeService.prefs(c).edit().remove(KEY_ACTIVE).apply()
    }

    fun clear(c: Context) {
        val n = RideDb.get(c).delete(RideDb.T, null, null)
        BikeService.log("RIDE history cleared ($n rides deleted)")
    }

    fun saveActive(c: Context, r: RideTracker.Ride?) {
        if (r == null) {
            BikeService.prefs(c).edit().remove(KEY_ACTIVE).apply()
            return
        }
        BikeService.prefs(c).edit().putString(KEY_ACTIVE, toJson(r).toString()).apply()
    }

    fun active(c: Context): RideTracker.Ride? {
        val s = BikeService.prefs(c).getString(KEY_ACTIVE, null) ?: return null
        return try {
            fromJson(JSONObject(s))
        } catch (e: JSONException) {
            null
        }
    }

    fun finishStale(c: Context) {
        val r = active(c) ?: return
        if (System.currentTimeMillis() - r.end < RideTracker.END_SILENCE_MS) return
        if (r.km() >= RideTracker.MIN_KM) add(c, r) else saveActive(c, null)
    }

    private fun toJson(r: RideTracker.Ride): JSONObject {
        val o = JSONObject()
        try {
            o.put("start", r.start)
                .put("end", r.end)
                .put("odoStart", r.odoStart)
                .put("odoEnd", r.odoEnd)
                .put("max", r.maxKmh)
                .put("moving", r.movingMs)
                .put("speedKm", r.speedKm)
        } catch (ignored: JSONException) {}
        return o
    }

    fun fromJson(o: JSONObject): RideTracker.Ride {
        val r = RideTracker.Ride()
        r.start = o.optLong("start")
        r.end = o.optLong("end")
        r.odoStart = o.optInt("odoStart")
        r.odoEnd = o.optInt("odoEnd")
        r.maxKmh = o.optInt("max")
        r.movingMs = o.optLong("moving")
        r.speedKm = o.optDouble("speedKm", 0.0)
        return r
    }
}
