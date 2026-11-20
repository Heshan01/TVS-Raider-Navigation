package com.heshan.tvsraider

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Keeps Bluetooth LE link to TVS SmartXonnect / Raider cluster and writes navigation frames.
 */
@SuppressLint("MissingPermission")
class BikeService : Service() {
    companion object {
        // TVS cluster BLE service ("TVSMVGSASBENTORQ" in ASCII)
        val SERVICE: UUID = UUID.fromString("5456534d-5647-5341-5342-454e544f5251")
        val WRITE_CHAR: UUID = UUID.fromString("00005352-0000-1000-8000-00805f9b34fb")
        val NOTIFY_CHAR: UUID = UUID.fromString("00005354-0000-1000-8000-00805f9b34fb")
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val ACTION_CONNECT = "connect"
        const val ACTION_DISCONNECT = "disconnect"

        @Volatile var instance: BikeService? = null
        @Volatile var status: String = "Not connected"
        private val LOG = ArrayList<String>()

        private const val MEM_LINES = 5000
        const val KEEP_DAYS = 7
        private const val DAY_FILE = "dd-MM-yyyy"
        private val DAY_NAME = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})\\.txt")
        private val LOG_IO = Executors.newSingleThreadExecutor()
        private var logDir: File? = null
        @Volatile private var toFile = false
        private var lastDay = ""
        private var logOut: Writer? = null
        private var openDay: String? = null

        @Volatile var speedKmh: Int = 0
        @Volatile var odoTenths: Int = 0
        @Volatile var telemetryAt: Long = 0
        @Volatile var currentRide: RideTracker.Ride? = null

        private var lastAutoStart: Long = 0

        fun log(s: String) {
            val now = Date()
            val day = SimpleDateFormat("yyyy-MM-dd EEE", Locale.ROOT).format(now)
            val fileDay = SimpleDateFormat(DAY_FILE, Locale.ROOT).format(now)
            val line = SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(now) + " " + s
            Log.i("TvsRaider", line)
            synchronized(LOG) {
                if (day != lastDay) {
                    lastDay = day
                    addLine("---- $day ----", fileDay)
                }
                addLine(line, fileDay)
            }
        }

        private fun addLine(line: String, fileDay: String) {
            LOG.add(line)
            while (LOG.size > MEM_LINES) LOG.removeAt(0)
            if (toFile) LOG_IO.execute { writeLine(fileDay, line) }
        }

        private fun today(): String = SimpleDateFormat(DAY_FILE, Locale.ROOT).format(Date())
        private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())

        fun initLog(c: Context) {
            val early: List<String>
            val files = c.applicationContext.filesDir
            synchronized(LOG) {
                if (logDir != null) return
                logDir = File(files, "logs")
                early = ArrayList(LOG)
                toFile = saveLogsWanted(c)
            }
            LOG_IO.execute {
                File(files, "raider.log").delete()
                File(files, "raider-prev.log").delete()
                if (!toFile) return@execute
                val d = today()
                writeLine(d, "==== TVS Raider Navigations started ${stamp()} ====")
                for (l in early) writeLine(d, l)
            }
        }

        fun saveLogsWanted(c: Context): Boolean {
            val p = prefs(c)
            return p.getBoolean("devOptions", true) && p.getBoolean("saveLogs", true)
        }

        fun setSaveToFile(on: Boolean) {
            synchronized(LOG) {
                if (logDir == null || toFile == on) return
                toFile = on
            }
            val d = today()
            LOG_IO.execute {
                writeLine(d, "==== Log saving ${if (on) "on " else "off "}${stamp()} ====")
                if (!on) closeWriter()
            }
        }

        private fun writeLine(day: String, line: String) {
            try {
                if (logOut == null || day != openDay) {
                    closeWriter()
                    val dir = logDir
                    if (dir != null && !dir.isDirectory && !dir.mkdirs()) return
                    logOut = BufferedWriter(
                        OutputStreamWriter(
                            FileOutputStream(File(logDir, "$day.txt"), true),
                            StandardCharsets.UTF_8
                        )
                    )
                    openDay = day
                    prune()
                }
                logOut?.write(line)
                logOut?.write("\n")
                logOut?.flush()
            } catch (e: IOException) {
                Log.w("TvsRaider", "log write: $e")
                closeWriter()
            }
        }

        private fun closeWriter() {
            try { logOut?.close() } catch (ignored: IOException) {}
            logOut = null
            openDay = null
        }

        private fun dayFiles(): List<File> {
            val out = ArrayList<File>()
            val all = logDir?.listFiles()
            if (all != null) {
                for (f in all) {
                    if (DAY_NAME.matcher(f.name).matches()) out.add(f)
                }
            }
            Collections.sort(out) { a, b -> sortKey(a).compareTo(sortKey(b)) }
            return out
        }

        private fun sortKey(f: File): String {
            val m = DAY_NAME.matcher(f.name)
            return if (m.matches()) m.group(3).orEmpty() + m.group(2).orEmpty() + m.group(1).orEmpty() else f.name
        }

        private fun prune() {
            val days = dayFiles()
            for (i in 0 until days.size - KEEP_DAYS) {
                if (days[i].delete()) Log.i("TvsRaider", "old log deleted: ${days[i].name}")
            }
        }

        fun logFiles(): List<File> {
            return try {
                LOG_IO.submit<List<File>> { dayFiles() }.get(10, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.w("TvsRaider", "log files: $e")
                ArrayList()
            }
        }

        fun logLines(maxLines: Int): List<String> {
            synchronized(LOG) {
                return ArrayList(LOG.subList(Math.max(0, LOG.size - maxLines), LOG.size))
            }
        }

        fun logText(maxLines: Int): String {
            synchronized(LOG) {
                val b = java.lang.StringBuilder()
                val stop = Math.max(0, LOG.size - maxLines)
                for (i in LOG.size - 1 downTo stop) b.append(LOG[i]).append('\n')
                return b.toString()
            }
        }

        fun liveLog(): String {
            synchronized(LOG) {
                val b = java.lang.StringBuilder()
                for (s in LOG) b.append(s).append('\n')
                return b.toString()
            }
        }

        fun clearScreen() {
            synchronized(LOG) {
                LOG.clear()
                lastDay = ""
            }
        }

        fun clearLog() {
            synchronized(LOG) {
                LOG.clear()
                lastDay = ""
                if (logDir != null) {
                    LOG_IO.execute {
                        closeWriter()
                        for (f in dayFiles()) f.delete()
                    }
                }
            }
            log("Log deleted")
        }

        fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("tvs_raider", MODE_PRIVATE)

        fun autoStart(c: Context, why: String) {
            val p = prefs(c)
            if (p.getString("mac", "").isNullOrEmpty()) return
            if ("maps-start" != why && !p.getBoolean("autoConnect", false)) return
            val s = instance
            if (s != null && s.wantConnected) return
            val now = System.currentTimeMillis()
            if (now - lastAutoStart < 10_000) return
            lastAutoStart = now
            log("Auto-connect ($why)")
            try {
                c.startForegroundService(Intent(c, BikeService::class.java).setAction(ACTION_CONNECT))
            } catch (e: Exception) {
                log("Auto-connect blocked by Android: $e")
            }
        }

        fun decodeTelemetry(v: ByteArray): Boolean {
            if (v.size < 20 || (v[0].toInt() and 0xFF) != 0x5A || (v[1].toInt() and 0xFF) != 0x10) return false
            var sum = 0
            for (i in 0 until 18) sum += v[i].toInt() and 0xFF
            if ((255 - sum % 256) != (v[18].toInt() and 0xFF)) return false
            speedKmh = v[2].toInt() and 0xFF
            odoTenths = ((v[3].toInt() and 0xFF) shl 16) or ((v[4].toInt() and 0xFF) shl 8) or (v[5].toInt() and 0xFF)
            telemetryAt = System.currentTimeMillis()
            return true
        }

        fun props(p: Int): String {
            val b = java.lang.StringBuilder()
            if ((p and BluetoothGattCharacteristic.PROPERTY_READ) != 0) b.append("READ ")
            if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) b.append("WRITE ")
            if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) b.append("WRITE_NO_RESP ")
            if ((p and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) b.append("NOTIFY ")
            if ((p and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) b.append("INDICATE ")
            return b.toString().trim()
        }
    }

    private val h = Handler(Looper.getMainLooper())
    private val ops = ArrayDeque<Runnable>()
    private var busy = false
    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null
    private var wantConnected = false
    private var ready = false

    private val KEEPALIVE_MS: Long = 700
    private var lastTextSent: String? = null
    private var lastNav: TvsProtocol.Nav? = null
    private var lastSendAt: Long = 0
    private val lastFromBike = HashMap<String, String>()

    private lateinit var rides: RideTracker
    private var rideSavedAt: Long = 0
    private var speedArmed = true
    private var failCount = 0
    private var bgConnect = false
    private var lastStatusAt: Long = 0
    private var mtuWatchdog: Runnable? = null

    private val rideTick = object : Runnable {
        override fun run() {
            rides.tick(System.currentTimeMillis())
            h.postDelayed(this, 10_000)
        }
    }

    private val watchdog = Runnable {
        log("GATT op timed out, continuing")
        opDone()
    }

    private val keepAlive = object : Runnable {
        override fun run() {
            if (!ready) return
            val now = System.currentTimeMillis()
            val nav = lastNav
            if (nav != null && !busy && ops.isEmpty() && now - lastSendAt >= KEEPALIVE_MS) {
                writeNav(nav, false)
            }
            if (now - lastStatusAt >= 2000L && ops.size < 4) {
                val first = lastStatusAt == 0L
                lastStatusAt = now
                enqueueWrite(TvsProtocol.phoneStatusFrame(phoneBatteryBars(), Calendar.getInstance()), first)
            }
            h.postDelayed(this, 100)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        initLog(this)
        rides = RideTracker(object : RideTracker.Listener {
            override fun finished(r: RideTracker.Ride) {
                currentRide = null
                r.avgKmL = Math.max(0, BikeFrames.avgKmL)
                RideStore.add(this@BikeService, r)
                log("RIDE finished: " + RideTracker.summary(r))
                notifyRide(r)
            }

            override fun changed(r: RideTracker.Ride?) {
                currentRide = r
                val now = System.currentTimeMillis()
                if (r == null) {
                    RideStore.saveActive(this@BikeService, null)
                    rideSavedAt = 0
                } else if (now - rideSavedAt > 30_000) {
                    RideStore.saveActive(this@BikeService, r)
                    rideSavedAt = now
                }
            }
        })

        val saved = RideStore.active(this)
        if (saved != null) {
            rides.resume(saved, saved.end)
            currentRide = saved
            rides.tick(System.currentTimeMillis())
        }
        h.postDelayed(rideTick, 10_000)
    }

    private fun checkAlerts() {
        val p = prefs(this)
        val now = System.currentTimeMillis()
        if (p.getBoolean("speedAlert", false) && now - telemetryAt < 3000) {
            val limit = p.getInt("speedLimit", 80)
            val kmh = speedKmh
            if (kmh < limit - 5) {
                speedArmed = true
            } else if (speedArmed && kmh >= limit) {
                speedArmed = false
                log("ALERT speed $kmh km/h (limit $limit)")
                vibrate()
            }
        }
        val bars = BikeFrames.fuelBars
        if (p.getBoolean("fuelAlert", true) && bars >= 0) {
            val level = p.getInt("fuelLevel", 1)
            val warned = p.getBoolean("fuelWarned", false)
            if (bars > level && warned) {
                p.edit().putBoolean("fuelWarned", false).apply()
            } else if (bars <= level && !warned) {
                p.edit().putBoolean("fuelWarned", true).apply()
                log("ALERT low fuel: $bars of 5 bars (warn at $level)")
                notifyLowFuel(bars)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val pattern = longArrayOf(0, 450, 150, 450, 150, 450)
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        if (vib == null || !vib.hasVibrator()) return
        val fx = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= 33) {
            vib.vibrate(fx, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            vib.vibrate(fx)
        }
    }

    private fun notifyLowFuel(bars: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("alerts", "Low fuel", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            4242,
            Notification.Builder(this, "alerts")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Low fuel")
                .setContentText(
                    if (bars == 0) "The fuel gauge is empty. Fill up soon."
                    else "Fuel is down to $bars of 5 bars. Time to fill up."
                )
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun notifyRide(r: RideTracker.Ride) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("rides", "Ride summary", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val pi = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            (r.start / 1000).toInt(),
            Notification.Builder(this, "rides")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentTitle("Ride finished")
                .setContentText(RideTracker.summary(r))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        val a = intent?.action ?: ACTION_CONNECT
        if (ACTION_DISCONNECT == a) {
            prefs(this).edit().putBoolean("autoConnect", false).apply()
            disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            prefs(this).edit().putBoolean("autoConnect", true).apply()
            wantConnected = true
            failCount = 0
            if (gatt != null && bgConnect && !ready) closeGatt()
            connect()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        h.removeCallbacks(rideTick)
        RideStore.saveActive(this, rides.current())
        currentRide = null
        wantConnected = false
        closeGatt()
        instance = null
        setStatus("Not connected")
        super.onDestroy()
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("bike", "Bike connection", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "bike")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("TVS Raider")
            .setContentText("Sending Google Maps directions to the bike")
            .setContentIntent(pi)
            .setOngoing(true)
            .addAction(disconnectAction())
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, n)
        }
    }

    private fun disconnectAction(): Notification.Action {
        val stop = PendingIntent.getService(
            this,
            3,
            Intent(this, BikeService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Disconnect",
            stop
        ).build()
    }

    private fun setStatus(s: String) {
        val repeat = s == status && s == "Bluetooth is off"
        status = s
        if (!repeat) log("STATUS $s")
    }

    private fun connect() {
        if (gatt != null) return
        val ad = getSystemService(BluetoothManager::class.java)?.adapter
        if (ad == null || !ad.isEnabled) {
            setStatus("Bluetooth is off")
            retryLater()
            return
        }
        val mac = prefs(this).getString("mac", "") ?: ""
        if (mac.isEmpty()) {
            wantConnected = false
            setStatus("No bike chosen. Tap Find bike")
            return
        }
        val dev: BluetoothDevice = try {
            ad.getRemoteDevice(mac)
        } catch (e: IllegalArgumentException) {
            setStatus("Bad bike address: $mac")
            return
        }
        bgConnect = failCount >= 3
        setStatus(if (bgConnect) "Waiting for bike (ignition on?) $mac" else "Connecting to $mac ...")
        gatt = dev.connectGatt(this, bgConnect, cb, BluetoothDevice.TRANSPORT_LE)
    }

    private fun retryLater() {
        if (wantConnected) h.postDelayed({ connect() }, 4000)
    }

    private fun disconnect() {
        wantConnected = false
        h.removeCallbacksAndMessages(null)
        closeGatt()
        setStatus("Disconnected")
    }

    private fun closeGatt() {
        ready = false
        lastTextSent = null
        lastStatusAt = 0
        mtuWatchdog?.let { h.removeCallbacks(it) }
        mtuWatchdog = null
        h.removeCallbacks(keepAlive)
        lastFromBike.clear()
        ops.clear()
        busy = false
        writeChar = null
        notifyChar = null
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (ignored: Exception) {}
        gatt = null
    }

    fun isReady(): Boolean = ready
    fun isActive(): Boolean = ready || wantConnected

    private val cb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, st: Int, newState: Int) {
            h.post {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    setStatus("Connected, setting up ...")
                    mtuWatchdog?.let { h.removeCallbacks(it) }
                    val watchdog = Runnable {
                        log("MTU callback timed out (~2s), discovering services anyway")
                        g.discoverServices()
                    }
                    mtuWatchdog = watchdog
                    h.postDelayed(watchdog, 2000)
                    g.requestMtu(137)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mtuWatchdog?.let { h.removeCallbacks(it) }
                    mtuWatchdog = null
                    if (ready) failCount = 3 else failCount++
                    setStatus("Disconnected (code $st)${if (wantConnected) ", retrying" else ""}")
                    closeGatt()
                    retryLater()
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, st: Int) {
            h.post {
                mtuWatchdog?.let { h.removeCallbacks(it) }
                mtuWatchdog = null
                log("MTU $mtu (status $st)")
                g.discoverServices()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, st: Int) {
            h.post { setupCharacteristics(g) }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, st: Int) {
            h.post {
                if (st != BluetoothGatt.GATT_SUCCESS) log("Write failed, status $st")
                opDone()
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, st: Int) {
            h.post {
                log("Notifications on (status $st)")
                opDone()
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, v: ByteArray) {
            fromBike(v)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) fromBike(c.value)
        }
    }

    private fun fromBike(v: ByteArray?) {
        if (v == null) return
        if (decodeTelemetry(v)) {
            val kmh = speedKmh
            val odo = odoTenths
            val t = telemetryAt
            h.post { rides.sample(kmh, odo, t) }
        }
        val hex = TvsProtocol.hex(v)
        BikeFrames.update(v)
        h.post { checkAlerts() }
        val meaning = BikeFrames.describe(v)
        h.post {
            val type = if (hex.length >= 5) hex.substring(0, 5) else hex
            val navRestart = type == "5A 9D"
            if (navRestart) lastTextSent = null
            if (!navRestart && hex == lastFromBike[type]) return@post
            lastFromBike[type] = hex
            log("FROM BIKE $hex${if (navRestart) "   <- bike (re)started navigation mode" else ""}")
            if (meaning != null) log(meaning)
        }
    }

    private fun setupCharacteristics(g: BluetoothGatt) {
        for (s in g.services) {
            log("Service ${s.uuid}")
            for (c in s.characteristics) {
                log("  char ${c.uuid} props=${props(c.properties)}")
            }
        }
        var s = g.getService(SERVICE)
        if (s == null) {
            for (cand in g.services) {
                if (cand.uuid.toString().endsWith("-0000-1000-8000-00805f9b34fb")
                    && cand.uuid.toString().startsWith("000018")
                ) continue
                for (c in cand.characteristics) {
                    if ((c.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                        s = cand
                        break
                    }
                }
                if (s != null) break
            }
        }
        if (s == null) {
            setStatus("TVS service not found on this device")
            return
        }
        log("Using service ${s.uuid}")
        writeChar = s.getCharacteristic(WRITE_CHAR)
        notifyChar = s.getCharacteristic(NOTIFY_CHAR)
        for (c in s.characteristics) {
            val p = c.properties
            val canWrite = (p and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
            val canNotify = (p and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
            if (canWrite && writeChar == null) writeChar = c
            if (canNotify && notifyChar == null) notifyChar = c
        }
        if (writeChar == null) {
            setStatus("No writable characteristic in TVS service")
            return
        }
        log("Using write char ${writeChar?.uuid}")
        val nc = notifyChar
        if (nc != null) {
            log("Using notify char ${nc.uuid}")
            enqueue {
                g.setCharacteristicNotification(nc, true)
                val d = nc.getDescriptor(CCCD)
                if (d == null) {
                    opDone()
                    return@enqueue
                }
                val value = if ((nc.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                writeDescriptor(g, d, value)
            }
        }
        ready = true
        failCount = 0
        setStatus("READY - connected to bike")
        h.removeCallbacks(keepAlive)
        h.postDelayed(keepAlive, KEEPALIVE_MS)
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptor(g: BluetoothGatt, d: BluetoothGattDescriptor, value: ByteArray) {
        if (Build.VERSION.SDK_INT >= 33) {
            g.writeDescriptor(d, value)
        } else {
            d.value = value
            g.writeDescriptor(d)
        }
    }

    private fun enqueue(op: Runnable) {
        ops.add(op)
        if (!busy) next()
    }

    private fun next() {
        val op = ops.poll()
        if (op == null) {
            busy = false
            return
        }
        busy = true
        h.postDelayed(watchdog, 1500)
        try {
            op.run()
        } catch (e: Exception) {
            log("GATT op error: $e")
            opDone()
        }
    }

    private fun opDone() {
        h.removeCallbacks(watchdog)
        busy = false
        next()
    }

    private fun enqueueDelay(ms: Long) {
        enqueue { h.postDelayed({ opDone() }, ms) }
    }

    @Suppress("DEPRECATION")
    private fun enqueueWrite(data: ByteArray, logHex: Boolean) {
        enqueue {
            val c = writeChar
            val currentGatt = gatt
            if (currentGatt == null || c == null) {
                opDone()
                return@enqueue
            }
            val type = if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            if (logHex) log("TO BIKE ${TvsProtocol.hex(data)}")
            val ok = if (Build.VERSION.SDK_INT >= 33) {
                currentGatt.writeCharacteristic(c, data, type) == 0
            } else {
                c.writeType = type
                c.value = data
                currentGatt.writeCharacteristic(c)
            }
            if (!ok) {
                log("Write rejected by Android")
                opDone()
            }
        }
    }

    fun sendNav(nav: TvsProtocol.Nav) {
        h.post {
            lastNav = nav
            if (!ready) {
                log("Not sent (bike not ready): $nav")
                return@post
            }
            log("SEND $nav")
            writeNav(nav, true)
        }
    }

    fun stopNav() {
        h.post {
            lastNav = null
            lastSendAt = 0
        }
    }

    private fun phoneBatteryBars(): Int {
        return try {
            val bm = getSystemService(android.os.BatteryManager::class.java)
            val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (pct < 0) 5 else Math.max(0, Math.min(5, Math.round(pct / 20f)))
        } catch (e: Exception) {
            5
        }
    }

    private fun writeNav(nav: TvsProtocol.Nav, logHex: Boolean) {
        val now = System.currentTimeMillis()
        if (lastSendAt > 0 && now - lastSendAt > 6000) {
            log("GAP ${(now - lastSendAt) / 1000} s without sending (phone paused app?)")
        }
        lastSendAt = now
        val text = nav.text ?: ""
        val sendText = text != lastTextSent
        val control = TvsProtocol.controlFrame(nav)
        val textFrame = TvsProtocol.textFrame(nav)

        enqueueWrite(control, logHex)
        if (sendText) {
            enqueueDelay(100)
            enqueueWrite(textFrame, logHex)
            lastTextSent = text
        }
    }
}
