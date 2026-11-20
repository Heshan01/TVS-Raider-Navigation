package com.heshan.tvsraider

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * TVS Raider Navigations - Main Activity.
 * Modern sports-styled UI with complete Bluetooth LE TVS Raider navigation & telemetry features.
 * Developed by Heshan.
 */
@SuppressLint("MissingPermission")
class MainActivity : Activity() {

    companion object {
        const val TAB_HOME = 0
        const val TAB_OVERVIEW = 1
        const val TAB_ANALYTICS = 2
        const val TAB_LOG = 3
        const val TAB_SETTINGS = 4

        const val BIKE_MODEL = "TVS Raider"
        const val DEVELOPER_NAME = "Heshan"
        const val APP_TITLE = "TVS Raider Navigations"

        val THEMES = arrayOf("dark", "light", "system", "auto")
        val PERIODS = arrayOf("Week", "Month", "Year", "All time")
        val FILTERS = arrayOf("All", "Bike", "Maps", "Status")

        const val SPEED_SEGS = 24
        const val SPEED_SCALE = 120
    }

    // Palette colors
    private var BG = 0
    private var BAR = 0
    private var BAR_LINE = 0
    private var HERO = 0
    private var HERO_LINE = 0
    private var TILE = 0
    private var TILE_LINE = 0
    private var PILL = 0
    private var PILL_LINE = 0
    private var SELECTED = 0
    private var LOG_BG = 0
    private var ACCENT = 0
    private var ON_ACCENT = 0
    private var TEXT = 0
    private var TEXT2 = 0
    private var SUB = 0
    private var MUTED = 0
    private var FAINT = 0
    private var EMPTY = 0
    private var OK = 0
    private var WARN = 0
    private var WARN_BG = 0
    private var ON_WARN = 0
    private var BAD = 0
    private var RED_TEXT = 0
    private var RED_BG = 0
    private var RED_LINE = 0
    private var PURPLE = 0
    private var BLUE = 0
    private var SCALE_HI = 0
    private var ARROW_DIM = 0
    private var RIPPLE = 0
    private var TOGGLE_OFF = 0
    private var KNOB_OFF = 0

    private var darkNow = true
    private var tab = TAB_HOME
    private val tabViews = ArrayList<View>()
    private val navItems = ArrayList<NavItem>()

    private val h = Handler(Looper.getMainLooper())
    private val fonts = HashMap<String, Typeface>()

    // UI elements: Home
    private lateinit var brandText: TextView
    private lateinit var pillDot: View
    private lateinit var pillText: TextView
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var setupCard: View
    private lateinit var setupNote: TextView
    private lateinit var arrowTile: FrameLayout
    private lateinit var arrow: ArrowView
    private lateinit var distView: TextView
    private lateinit var instrView: TextView
    private lateinit var roadView: TextView
    private lateinit var timeLeftView: TextView
    private lateinit var distLeftView: TextView
    private lateinit var etaView: TextView
    private val homeFuelSegs = arrayOfNulls<View>(5)
    private lateinit var homeFuelText: TextView
    private lateinit var homeMileage: TextView
    private lateinit var homeBattery: TextView
    private lateinit var speedView: TextView
    private lateinit var speedMax: TextView
    private val speedSegs = arrayOfNulls<View>(SPEED_SEGS)
    private lateinit var primaryBtn: TextView

    // UI elements: Overview
    private lateinit var liveDot: View
    private lateinit var liveText: TextView
    private val fuelSegs = arrayOfNulls<View>(5)
    private lateinit var fuelState: TextView
    private lateinit var fuelBig: TextView
    private lateinit var odoText: TextView
    private lateinit var tripBText: TextView
    private lateinit var mileageText: TextView
    private lateinit var batteryText: TextView
    private lateinit var weekText: TextView
    private lateinit var rideLine: TextView
    private lateinit var ridesBody: LinearLayout

    // UI elements: Analytics
    private var period = 0
    private val periodTabs = ArrayList<TextView>()
    private lateinit var anRecording: LinearLayout
    private lateinit var anHeroLabel: TextView
    private lateinit var anHeroKm: TextView
    private lateinit var anTrendIcon: LineIcon
    private lateinit var anTrend: TextView
    private lateinit var anOdo: TextView
    private lateinit var anRides: TextView
    private lateinit var anTime: TextView
    private lateinit var anAvg: TextView
    private lateinit var anTop: TextView
    private lateinit var anMileage: TextView
    private lateinit var anPerRide: TextView
    private lateinit var anBest: TextView
    private lateinit var anChart: BarChart

    // UI elements: Logs
    private var logFilter = 0
    private val filterChips = ArrayList<TextView>()
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private var lastLogKey = ""

    // UI elements: Settings
    private var bikeTitle: TextView? = null
    private var bikeAddr: TextView? = null
    private var bikeBtn: TextView? = null
    private var setupChevron: LineIcon? = null
    private var setupGroup: LinearLayout? = null
    private var chkPerms: CheckRow? = null
    private var chkMaps: CheckRow? = null
    private var chkBattery: CheckRow? = null

    private var scanning = false
    private var maxTodaySaved = 0
    private var anRefreshedAt: Long = 0
    private var autoScanned = false

    private fun applyPalette(dark: Boolean) {
        darkNow = dark
        if (dark) {
            BG = 0xFF060B14.toInt()
            BAR = 0xFF0D1524.toInt()
            BAR_LINE = 0xFF19253C.toInt()
            HERO = 0xFF111D33.toInt()
            HERO_LINE = 0xFF1D2F50.toInt()
            TILE = 0xFF0E1A2E.toInt()
            TILE_LINE = 0xFF1A2A47.toInt()
            PILL = 0xFF15223A.toInt()
            PILL_LINE = 0xFF24385E.toInt()
            SELECTED = 0xFF0D3344.toInt()
            LOG_BG = 0xFF04070D.toInt()
            ACCENT = 0xFF00E5FF.toInt()       // Vibrant neon cyan
            ON_ACCENT = 0xFF051722.toInt()
            TEXT = 0xFFF0F6FC.toInt()
            TEXT2 = 0xFFD3E0EA.toInt()
            SUB = 0xFFA0B3C6.toInt()
            MUTED = 0xFF768B9E.toInt()
            FAINT = 0xFF4E6274.toInt()
            EMPTY = 0xFF182336.toInt()
            OK = 0xFF00E676.toInt()          // Vibrant emerald
            WARN = 0xFFFFB300.toInt()        // High-vis amber
            WARN_BG = 0xFF2E2007.toInt()
            ON_WARN = 0xFF3D2500.toInt()
            BAD = 0xFFFF5252.toInt()         // Neon red
            RED_TEXT = 0xFFFF8A80.toInt()
            RED_BG = 0xFF2D1214.toInt()
            RED_LINE = 0xFF4D2226.toInt()
            PURPLE = 0xFFB388FF.toInt()
            BLUE = 0xFF40C4FF.toInt()
            SCALE_HI = 0xFFFFAB40.toInt()
            ARROW_DIM = 0x33FFFFFF
            RIPPLE = 0x22FFFFFF
            TOGGLE_OFF = 0xFF293B52.toInt()
            KNOB_OFF = 0xFF9FB2C4.toInt()
        } else {
            BG = 0xFFF0F4F8.toInt()
            BAR = 0xFFFFFFFF.toInt()
            BAR_LINE = 0xFFD8E2EC.toInt()
            HERO = 0xFFFFFFFF.toInt()
            HERO_LINE = 0xFFD0DCE7.toInt()
            TILE = 0xFFFFFFFF.toInt()
            TILE_LINE = 0xFFDBE5EF.toInt()
            PILL = 0xFFE2EAF2.toInt()
            PILL_LINE = 0xFFCCD9E5.toInt()
            SELECTED = 0xFFCCF4F8.toInt()
            LOG_BG = 0xFFFFFFFF.toInt()
            ACCENT = 0xFF00838F.toInt()
            ON_ACCENT = 0xFFFFFFFF.toInt()
            TEXT = 0xFF0F1A24.toInt()
            TEXT2 = 0xFF263747.toInt()
            SUB = 0xFF4B6072.toInt()
            MUTED = 0xFF657E94.toInt()
            FAINT = 0xFF8399AD.toInt()
            EMPTY = 0xFFDCE6EE.toInt()
            OK = 0xFF00897B.toInt()
            WARN = 0xFFE65100.toInt()
            WARN_BG = 0xFFFFF3E0.toInt()
            ON_WARN = 0xFFFFFFFF.toInt()
            BAD = 0xFFD32F2F.toInt()
            RED_TEXT = 0xFFC62828.toInt()
            RED_BG = 0xFFFFEBEE.toInt()
            RED_LINE = 0xFFFFCDD2.toInt()
            PURPLE = 0xFF7C4DFF.toInt()
            BLUE = 0xFF0288D1.toInt()
            SCALE_HI = 0xFFEF6C00.toInt()
            ARROW_DIM = 0x22000000
            RIPPLE = 0x18000000
            TOGGLE_OFF = 0xFFCFDCE6.toInt()
            KNOB_OFF = 0xFFFFFFFF.toInt()
        }
    }

    private fun wantDark(): Boolean {
        val t = BikeService.prefs(this).getString("theme", "dark")
        if ("light" == t) return false
        if ("system" == t) {
            return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        if ("auto" == t) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return hour < 6 || hour >= 18
        }
        return true
    }

    private fun font(file: String, weight: Int): Typeface {
        val key = "$file$weight"
        val cached = fonts[key]
        if (cached != null) return cached

        val loaded = try {
            Typeface.Builder(assets, "fonts/$file").setFontVariationSettings("'wght' $weight").build()
        } catch (e: Exception) {
            null
        }
        val result = loaded ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        fonts[key] = result
        return result
    }

    private fun display(w: Int): Typeface = font("SpaceGrotesk.ttf", w)
    private fun body(w: Int): Typeface = font("Inter.ttf", w)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        BikeService.initLog(this)
        applyPalette(wantDark())
        window.statusBarColor = BG
        window.navigationBarColor = BAR
        window.decorView.setBackgroundColor(BG)
        if (!darkNow && Build.VERSION.SDK_INT >= 26) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        val content = FrameLayout(this)
        outer.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        buildHome(tabPage(content, true))
        buildOverview(tabPage(content, true))
        buildAnalytics(tabPage(content, true))
        buildLog(tabPage(content, true))
        buildSettings(tabPage(content, true))

        buildNavBar(outer)
        setContentView(outer)

        showTab(b?.getInt("tab", TAB_HOME) ?: TAB_HOME)
        askPermissions()
        RideStore.finishStale(this)
    }

    override fun onResume() {
        super.onResume()
        if (wantDark() != darkNow) {
            recreate()
            return
        }
        h.post(refresh)
        h.post(speedTick)
        if (!autoScanned && BikeService.prefs(this).getString("mac", "").isNullOrEmpty()
            && (Build.VERSION.SDK_INT < 31 || checkSelfPermission("android.permission.BLUETOOTH_SCAN") == PackageManager.PERMISSION_GRANTED)
        ) {
            autoScanned = true
            findBike()
        }
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(refresh)
        h.removeCallbacks(speedTick)
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putInt("tab", tab)
    }

    private fun tabPage(content: FrameLayout, scroll: Boolean): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        val v: View = if (scroll) {
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                addView(root, ViewGroup.LayoutParams(-1, -2))
            }
        } else {
            root
        }
        content.addView(v, FrameLayout.LayoutParams(-1, -1))
        tabViews.add(v)
        return root
    }

    private fun showTab(t: Int) {
        tab = t
        for (i in tabViews.indices) {
            tabViews[i].visibility = if (i == t) View.VISIBLE else View.GONE
            navItems.getOrNull(i)?.set(i == t)
        }
        if (t == TAB_LOG) updateLog(true)
        if (t == TAB_OVERVIEW) {
            refreshRides()
            updateBike()
        }
        if (t == TAB_ANALYTICS) refreshAnalytics()
        if (t == TAB_SETTINGS) updateSettings()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (tab != TAB_HOME) showTab(TAB_HOME)
        else super.onBackPressed()
    }

    private fun buildNavBar(outer: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BAR)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val div = View(this).apply { setBackgroundColor(BAR_LINE) }
        outer.addView(div, LinearLayout.LayoutParams(-1, dp(1)))
        outer.addView(bar, LinearLayout.LayoutParams(-1, dp(64)))

        val defs = arrayOf(
            Pair("Home", LineIcon.HOME),
            Pair("Overview", LineIcon.OVERVIEW),
            Pair("Analytics", LineIcon.TREND_UP),
            Pair("Logs", LineIcon.LOG),
            Pair("Settings", LineIcon.SETTINGS)
        )
        for (i in defs.indices) {
            val item = NavItem(bar, defs[i].first, defs[i].second) { showTab(i) }
            navItems.add(item)
        }
    }

    private inner class NavItem(parent: LinearLayout, title: String, iconKind: Int, action: Runnable) {
        val box: LinearLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
            setBackground(ripple(round(0, 16, 0)))
            isClickable = true
            setOnClickListener { action.run() }
        }
        val icon: LineIcon = LineIcon(this@MainActivity, iconKind, MUTED)
        val text: TextView = text(title, 11, MUTED, body(500))

        init {
            box.addView(icon, LinearLayout.LayoutParams(dp(22), dp(22)))
            text.setPadding(0, dp(4), 0, 0)
            box.addView(text)
            parent.addView(box, LinearLayout.LayoutParams(0, -1, 1f))
        }

        fun set(on: Boolean) {
            val c = if (on) ACCENT else MUTED
            icon.setColor(c)
            text.setTextColor(c)
            text.typeface = body(if (on) 700 else 500)
        }
    }

    // =====================================================================================
    // TAB 0: HOME
    // =====================================================================================
    private fun buildHome(root: LinearLayout) {
        val top = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        brandText = text("TVS RAIDER", 15, ACCENT, display(700)).apply {
            letterSpacing = 0.28f
        }
        top.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))

        val pill = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(14), dp(6))
            background = round(PILL, 99, HERO_LINE)
        }
        pillDot = View(this)
        pill.addView(pillDot, LinearLayout.LayoutParams(dp(8), dp(8)))
        pillText = text("Not connected", 12, TEXT2, body(500)).apply {
            setPadding(dp(8), 0, 0, 0)
        }
        pill.addView(pillText)
        top.addView(pill)
        root.addView(top)

        statusSub = text("BLUETOOTH LE DASHBOARD", 11, MUTED, body(600)).apply {
            letterSpacing = 0.12f
            setPadding(0, dp(18), 0, dp(2))
        }
        root.addView(statusSub)
        statusTitle = text("Ready to ride", 26, TEXT, display(600))
        root.addView(statusTitle)

        // Setup Card (shows if permissions need attention)
        setupCard = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = ripple(round(WARN_BG, 18, 0))
            isClickable = true
            setOnClickListener { showTab(TAB_SETTINGS) }
        }
        val bang = text("!", 14, ON_WARN, body(700)).apply {
            gravity = Gravity.CENTER
            background = round(WARN, 99, 0)
        }
        (setupCard as LinearLayout).addView(bang, LinearLayout.LayoutParams(dp(26), dp(26)))
        val st = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            addView(text("Finish setup", 14, TEXT, body(600)))
        }
        setupNote = text("Complete device permissions", 12, MUTED, body(400))
        st.addView(setupNote)
        (setupCard as LinearLayout).addView(st, LinearLayout.LayoutParams(0, -2, 1f))
        (setupCard as LinearLayout).addView(LineIcon(this, LineIcon.CHEVRON, WARN), LinearLayout.LayoutParams(dp(18), dp(18)))
        root.addView(setupCard, marginTop(-1, -2, 14))

        // Navigation Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = ripple(round(HERO, 28, HERO_LINE))
            isClickable = true
            contentDescription = "Open Google Maps"
            setOnClickListener { openMaps() }
        }
        val row = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        arrowTile = FrameLayout(this).apply {
            val pad = dp(14)
            setPadding(pad, pad, pad, pad)
            background = round(PILL, 20, HERO_LINE)
        }
        arrow = ArrowView(this)
        arrowTile.addView(arrow, FrameLayout.LayoutParams(-1, -1))
        row.addView(arrowTile, LinearLayout.LayoutParams(dp(100), dp(100)))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, 0, 0)
        }
        distView = text("--", 48, TEXT, display(700)).apply {
            includeFontPadding = false
            letterSpacing = -0.02f
        }
        col.addView(distView)
        instrView = text("Start navigation in Google Maps", 16, TEXT, body(600)).apply {
            setPadding(0, dp(4), 0, 0)
        }
        col.addView(instrView)
        roadView = text("Directions will sync to your bike cluster", 13, SUB, body(400)).apply {
            maxLines = 2
        }
        col.addView(roadView)
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(row)

        val div = View(this).apply { setBackgroundColor(HERO_LINE) }
        card.addView(div, LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(0, dp(16), 0, dp(14)) })

        val stats = hbox()
        timeLeftView = stat(stats, "TIME LEFT", 18)
        distLeftView = stat(stats, "DISTANCE", 18)
        etaView = stat(stats, "ARRIVE", 18)
        card.addView(stats)
        root.addView(card, marginTop(-1, -2, 16))

        // Bike quick glance tiles
        val tiles = hbox()
        val fuel = tile(tiles, "FUEL", 0)
        val segs = hbox()
        for (i in 0 until 5) {
            val sView = View(this)
            homeFuelSegs[i] = sView
            val sl = LinearLayout.LayoutParams(0, dp(8), 1f)
            if (i < 4) sl.setMargins(0, 0, dp(3), 0)
            segs.addView(sView, sl)
        }
        fuel.addView(segs, marginTop(-1, -2, 10))
        homeFuelText = text("--", 15, TEXT, display(600)).apply {
            setPadding(0, dp(10), 0, 0)
        }
        fuel.addView(homeFuelText)
        homeMileage = tileValue(tile(tiles, "MILEAGE", dp(10)), "km/L avg")
        homeBattery = tileValue(tile(tiles, "BATTERY", dp(10)), "volts")
        for (i in 0 until tiles.childCount) {
            tiles.getChildAt(i).setOnClickListener { showTab(TAB_OVERVIEW) }
        }
        root.addView(tiles, marginTop(-1, -2, 14))

        // Speedometer section
        buildSpeed(root)

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        // Primary Actions
        val actions = hbox()
        primaryBtn = pillButton("Connect", true).apply {
            setOnClickListener {
                val s = BikeService.instance
                if (!bluetoothOn()) requestBluetooth { connect() }
                else if (s != null && s.isActive()) disconnect()
                else connect()
            }
        }
        actions.addView(primaryBtn, LinearLayout.LayoutParams(0, dp(52), 1f))

        val find = FrameLayout(this).apply {
            background = ripple(round(PILL, 99, PILL_LINE))
            isClickable = true
            contentDescription = "Find bike"
            addView(LineIcon(this@MainActivity, LineIcon.SEARCH, TEXT), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
            setOnClickListener { findBike() }
        }
        actions.addView(find, LinearLayout.LayoutParams(dp(52), dp(52)).apply { setMargins(dp(10), 0, 0, 0) })
        root.addView(actions, marginTop(-1, -2, 20))
    }

    private fun buildSpeed(root: LinearLayout) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = round(HERO, 28, HERO_LINE)
        }
        val head = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(LineIcon(this, LineIcon.OVERVIEW, ACCENT), LinearLayout.LayoutParams(dp(20), dp(20)))
        val l = caps("LIVE SPEEDOMETER")
        l.setPadding(dp(10), 0, 0, 0)
        head.addView(l, LinearLayout.LayoutParams(0, -2, 1f))
        speedMax = text("", 12, MUTED, body(400))
        head.addView(speedMax)
        c.addView(head)

        speedView = text("--", 42, TEXT, display(700)).apply {
            includeFontPadding = false
            letterSpacing = -0.02f
        }
        c.addView(speedView, marginTop(-2, -2, 10))

        val bar = hbox().apply { gravity = Gravity.BOTTOM }
        for (i in 0 until SPEED_SEGS) {
            val sv = View(this)
            speedSegs[i] = sv
            val hgt = dp(14 + Math.round(i * 16f / (SPEED_SEGS - 1)))
            val sl = LinearLayout.LayoutParams(0, hgt, 1f)
            if (i < SPEED_SEGS - 1) sl.setMargins(0, 0, dp(3), 0)
            bar.addView(sv, sl)
        }
        c.addView(bar, marginTop(-1, dp(30), 12))

        val scale = hbox()
        val marks = arrayOf("0", "30", "60", "90", "120")
        for (i in marks.indices) {
            val m = text(marks[i], 11, if (i == marks.size - 1) SCALE_HI else FAINT, display(500))
            m.gravity = if (i == 0) Gravity.START else if (i == marks.size - 1) Gravity.END else Gravity.CENTER_HORIZONTAL
            scale.addView(m, LinearLayout.LayoutParams(0, -2, 1f))
        }
        c.addView(scale, marginTop(-1, -2, 6))
        root.addView(c, marginTop(-1, -2, 14))
        updateSpeed()
    }

    private fun updateSpeed() {
        if (!::speedView.isInitialized) return
        val live = BikeService.instance != null && BikeService.instance!!.isReady()
                && (System.currentTimeMillis() - BikeService.telemetryAt < 3000)
        val kmh = if (live) BikeService.speedKmh else -1
        val p = BikeService.prefs(this)
        val warnAt = if (p.getBoolean("speedAlert", false)) p.getInt("speedLimit", 80) else 100
        val lit = if (kmh <= 0) 0 else Math.min(SPEED_SEGS, Math.round(kmh * SPEED_SEGS.toFloat() / SPEED_SCALE))

        for (i in 0 until SPEED_SEGS) {
            val fast = (i + 1) * SPEED_SCALE / SPEED_SEGS.toFloat() > warnAt
            val color = if (i < lit) (if (fast) WARN else ACCENT) else (if (fast) WARN_BG else EMPTY)
            speedSegs[i]?.background = round(color, 3, 0)
        }
        if (kmh < 0) {
            speedView.text = "--"
            speedView.setTextColor(MUTED)
        } else {
            val n = kmh.toString()
            val sp = SpannableString("$n km/h")
            sp.setSpan(AbsoluteSizeSpan(18, true), n.length, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(MUTED), n.length, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            speedView.text = sp
            speedView.setTextColor(if (kmh >= warnAt) WARN else TEXT)
        }
        val r = BikeService.currentRide
        val max = Math.max(maxTodaySaved, r?.maxKmh ?: 0)
        if (max <= 0) {
            speedMax.text = ""
        } else {
            val v = max.toString()
            val sp = SpannableString("Max today $v km/h")
            sp.setSpan(ForegroundColorSpan(TEXT), 10, 10 + v.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            speedMax.text = sp
        }
    }

    private val speedTick = object : Runnable {
        override fun run() {
            if (tab == TAB_HOME) updateSpeed()
            h.postDelayed(this, 300)
        }
    }

    // =====================================================================================
    // TAB 1: OVERVIEW
    // =====================================================================================
    private fun buildOverview(root: LinearLayout) {
        val head = hbox().apply { gravity = Gravity.BOTTOM }
        head.addView(pageTitle(BIKE_MODEL, "Overview"), LinearLayout.LayoutParams(0, -2, 1f))
        val live = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        liveDot = View(this)
        live.addView(liveDot, LinearLayout.LayoutParams(dp(7), dp(7)))
        liveText = text("", 12, SUB, body(400)).apply { setPadding(dp(6), 0, 0, 0) }
        live.addView(liveText)
        head.addView(live)
        root.addView(head)

        // Fuel display card
        val fc = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = round(HERO, 28, HERO_LINE)
        }
        val fh = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        fh.addView(LineIcon(this, LineIcon.FUEL, ACCENT), LinearLayout.LayoutParams(dp(20), dp(20)))
        val fl = caps("FUEL LEVEL")
        fl.setPadding(dp(10), 0, 0, 0)
        fh.addView(fl, LinearLayout.LayoutParams(0, -2, 1f))
        fuelState = text("", 13, ACCENT, body(600))
        fh.addView(fuelState)
        fc.addView(fh)

        val bars = hbox().apply { gravity = Gravity.BOTTOM }
        for (i in 0 until 5) {
            val fv = View(this)
            fuelSegs[i] = fv
            val bl = LinearLayout.LayoutParams(0, dp(28 + 9 * i), 1f)
            if (i < 4) bl.setMargins(0, 0, dp(6), 0)
            bars.addView(fv, bl)
        }
        fc.addView(bars, LinearLayout.LayoutParams(-1, dp(64)).apply { setMargins(0, dp(16), 0, dp(16)) })

        val fb = hbox().apply { gravity = Gravity.BOTTOM }
        fuelBig = text("--", 34, TEXT, display(700)).apply { includeFontPadding = false }
        fb.addView(fuelBig, LinearLayout.LayoutParams(0, -2, 1f))
        fc.addView(fb)
        root.addView(fc, marginTop(-1, -2, 14))

        val r1 = hbox()
        odoText = bigTile(r1, "ODOMETER", "km", 0)
        tripBText = bigTile(r1, "TRIP B", "km", dp(10))
        root.addView(r1, marginTop(-1, -2, 10))

        val r2 = hbox()
        mileageText = bigTile(r2, "AVG MILEAGE", "km/L", 0)
        batteryText = bigTile(r2, "BATTERY", "volts", dp(10))
        root.addView(r2, marginTop(-1, -2, 10))

        val rh = hbox().apply { gravity = Gravity.BOTTOM }
        rh.addView(text("Rides History", 18, TEXT, display(600)), LinearLayout.LayoutParams(0, -2, 1f))
        weekText = text("", 12, MUTED, body(400)).apply { setPadding(0, 0, 0, dp(2)) }
        rh.addView(weekText)
        root.addView(rh, marginTop(-1, -2, 20))

        rideLine = text("", 13, ACCENT, body(600)).apply { visibility = View.GONE }
        root.addView(rideLine, marginTop(-1, -2, 6))

        ridesBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ridesBody)
        updateBike()
    }

    private fun bigTile(row: LinearLayout, label: String, unit: String, leftGap: Int): TextView {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = round(TILE, 22, TILE_LINE)
            addView(caps(label))
        }
        val v = text("--", 24, TEXT, display(600)).apply {
            includeFontPadding = false
            setPadding(0, dp(12), 0, 0)
        }
        t.addView(v)
        val u = text(unit, 12, MUTED, body(400)).apply { setPadding(0, dp(8), 0, 0) }
        t.addView(u)
        val lp = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(leftGap, 0, 0, 0) }
        row.addView(t, lp)
        return v
    }

    private fun updateBike() {
        if (!::fuelBig.isInitialized) return
        val live = BikeService.instance != null && BikeService.instance!!.isReady()
                && (System.currentTimeMillis() - BikeFrames.updatedAt < 5000)
        liveDot.background = round(if (live) OK else FAINT, 99, 0)
        liveText.text = if (live) "Live telemetry" else "Ignition off / Standby"

        val bars = BikeFrames.fuelBars
        paintFuel(fuelSegs, bars)
        paintFuel(homeFuelSegs, bars)

        val fuelStr = if (bars < 0) "--" else if (bars == 0) "Empty" else "$bars of 5 bars"
        fuelBig.text = fuelStr
        fuelState.text = if (bars in 0..1) "Low Fuel" else if (bars >= 4) "Full Tank" else ""
        homeFuelText.text = fuelStr

        val odo = BikeService.odoTenths
        val odoStr = if (odo > 0) BikeFrames.tenthsText(odo) else "--"
        odoText.text = odoStr

        val tb = BikeFrames.tripBTenths
        tripBText.text = if (tb >= 0) BikeFrames.tenthsText(tb) else "--"

        val ml = BikeFrames.avgKmL
        val mlStr = if (ml > 0) ml.toString() else "--"
        mileageText.text = mlStr
        homeMileage.text = mlStr

        val bv = BikeFrames.batteryTenths
        val bvStr = if (bv > 0) BikeFrames.tenthsText(bv) else "--"
        batteryText.text = bvStr
        homeBattery.text = bvStr
    }

    private fun paintFuel(segs: Array<View?>, bars: Int) {
        val p = BikeService.prefs(this)
        val warnAt = if (p.getBoolean("fuelAlert", true)) p.getInt("fuelLevel", 1) else 1
        for (i in segs.indices) {
            val sv = segs[i] ?: continue
            val on = bars > i
            val warn = bars <= warnAt
            val color = if (on) (if (warn) WARN else ACCENT) else EMPTY
            sv.background = round(color, 3, 0)
        }
    }

    private fun refreshRides() {
        if (!::ridesBody.isInitialized) return
        ridesBody.removeAllViews()
        val all = RideStore.recent(this, 15)
        var todayKm = 0.0
        var maxToday = 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        for (r in all) {
            if (r.start >= todayStart) {
                todayKm += r.km()
                maxToday = Math.max(maxToday, r.maxKmh)
            }
            ridesBody.addView(rideRow(r), marginTop(-1, -2, 8))
        }
        maxTodaySaved = maxToday
        weekText.text = String.format(Locale.ROOT, "Today: %.1f km", todayKm)
    }

    private fun rideRow(r: RideTracker.Ride): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = round(TILE, 18, TILE_LINE)
        }
        val top = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        val dateStr = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.ROOT).format(Date(r.start))
        top.addView(text(dateStr, 13, SUB, body(500)), LinearLayout.LayoutParams(0, -2, 1f))
        val kmStr = String.format(Locale.ROOT, "%.1f km", r.km())
        top.addView(text(kmStr, 15, TEXT, display(600)))
        row.addView(top)

        val sub = String.format(
            Locale.ROOT,
            "%s  ·  avg %.0f km/h  ·  max %d km/h",
            RideTracker.duration(r.durationMs()),
            r.avgKmh(),
            r.maxKmh
        )
        row.addView(text(sub, 12, MUTED, body(400)).apply { setPadding(0, dp(4), 0, 0) })
        return row
    }

    // =====================================================================================
    // TAB 2: ANALYTICS
    // =====================================================================================
    private fun buildAnalytics(root: LinearLayout) {
        val head = hbox().apply { gravity = Gravity.BOTTOM }
        head.addView(pageTitle(null, "Analytics"), LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(head)

        val seg = hbox().apply {
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = round(PILL, 99, PILL_LINE)
        }
        for (i in PERIODS.indices) {
            val p = i
            val t = text(PERIODS[i], 13, SUB, body(500)).apply {
                gravity = Gravity.CENTER
                isClickable = true
                setOnClickListener {
                    period = p
                    paintPeriods()
                    refreshAnalytics()
                }
            }
            seg.addView(t, LinearLayout.LayoutParams(0, dp(38), 1f))
            periodTabs.add(t)
        }
        root.addView(seg, marginTop(-1, -2, 16))
        paintPeriods()

        anRecording = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            val dot = View(this@MainActivity).apply { background = round(BAD, 99, 0) }
            addView(dot, LinearLayout.LayoutParams(dp(8), dp(8)))
            val rec = text("Recording current ride", 13, TEXT2, body(500)).apply { setPadding(dp(8), 0, 0, 0) }
            addView(rec)
            visibility = View.GONE
        }
        root.addView(anRecording, marginTop(-1, -2, 12))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = round(HERO, 28, HERO_LINE)
        }
        anHeroLabel = caps("DISTANCE THIS WEEK")
        hero.addView(anHeroLabel)
        anHeroKm = text("--", 52, TEXT, display(700)).apply {
            includeFontPadding = false
            letterSpacing = -0.03f
        }
        hero.addView(anHeroKm, marginTop(-2, -2, 10))

        val trend = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        anTrendIcon = LineIcon(this, LineIcon.TREND_UP, OK)
        trend.addView(anTrendIcon, LinearLayout.LayoutParams(dp(18), dp(18)))
        anTrend = text("", 13, OK, body(600)).apply { setPadding(dp(8), 0, 0, 0) }
        trend.addView(anTrend)
        hero.addView(trend, marginTop(-1, -2, 12))

        val line = View(this).apply { setBackgroundColor(HERO_LINE) }
        hero.addView(line, marginTop(-1, dp(1), 14))

        val odo = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        odo.addView(text("Odometer", 13, SUB, body(400)), LinearLayout.LayoutParams(0, -2, 1f))
        anOdo = text("--", 15, TEXT, display(600))
        odo.addView(anOdo)
        hero.addView(odo, marginTop(-1, -2, 12))
        root.addView(hero, marginTop(-1, -2, 14))

        val r1 = hbox()
        anRides = statTile(r1, "RIDES", 0)
        anTime = statTile(r1, "RIDING TIME", dp(10))
        root.addView(r1, marginTop(-1, -2, 10))

        val r2 = hbox()
        anAvg = statTile(r2, "AVG SPEED", 0)
        anTop = statTile(r2, "TOP SPEED", dp(10))
        root.addView(r2, marginTop(-1, -2, 10))

        val r3 = hbox()
        anMileage = statTile(r3, "MILEAGE", 0)
        anPerRide = statTile(r3, "PER RIDE", dp(10))
        root.addView(r3, marginTop(-1, -2, 10))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(16), dp(14))
            background = round(HERO, 28, HERO_LINE)
        }
        val ch = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        ch.addView(text("Distance History", 18, TEXT, display(600)), LinearLayout.LayoutParams(0, -2, 1f))
        anBest = text("", 12, MUTED, body(400))
        ch.addView(anBest)
        card.addView(ch)

        anChart = BarChart()
        card.addView(anChart, marginTop(-1, dp(180), 12))
        root.addView(card, marginTop(-1, -2, 14))
    }

    private fun statTile(row: LinearLayout, label: String, leftGap: Int): TextView {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = round(TILE, 20, TILE_LINE)
            addView(caps(label))
        }
        val v = text("--", 20, TEXT, display(600)).apply {
            includeFontPadding = false
            setPadding(0, dp(10), 0, 0)
        }
        t.addView(v)
        val lp = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(leftGap, 0, 0, 0) }
        row.addView(t, lp)
        return v
    }

    private fun paintPeriods() {
        for (i in periodTabs.indices) {
            val on = i == period
            val t = periodTabs[i]
            t.background = if (on) round(HERO, 99, HERO_LINE) else null
            t.setTextColor(if (on) ACCENT else SUB)
            t.typeface = body(if (on) 600 else 500)
        }
    }

    private fun refreshAnalytics() {
        if (!::anHeroKm.isInitialized) return
        anRefreshedAt = System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        val from: Long
        val prevFrom: Long
        val prevTo: Long
        var chartBins = 7
        val binLabels = ArrayList<String>()

        when (period) {
            0 -> { // Week
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                from = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                prevFrom = cal.timeInMillis
                prevTo = from
                chartBins = 7
                val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                binLabels.addAll(dayNames)
                anHeroLabel.text = "DISTANCE THIS WEEK"
            }
            1 -> { // Month
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                from = cal.timeInMillis
                cal.add(Calendar.MONTH, -1)
                prevFrom = cal.timeInMillis
                prevTo = from
                chartBins = 4
                binLabels.addAll(listOf("W1", "W2", "W3", "W4"))
                anHeroLabel.text = "DISTANCE THIS MONTH"
            }
            2 -> { // Year
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                from = cal.timeInMillis
                cal.add(Calendar.YEAR, -1)
                prevFrom = cal.timeInMillis
                prevTo = from
                chartBins = 12
                val mNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                binLabels.addAll(mNames)
                anHeroLabel.text = "DISTANCE THIS YEAR"
            }
            else -> { // All time
                from = 0L
                prevFrom = 0L
                prevTo = 0L
                chartBins = 6
                binLabels.addAll(listOf("1", "2", "3", "4", "5", "6"))
                anHeroLabel.text = "TOTAL DISTANCE ALL TIME"
            }
        }

        val rides = RideStore.between(this, from, now)
        val prevRides = if (prevFrom > 0) RideStore.between(this, prevFrom, prevTo) else emptyList()

        var totalKm = 0.0
        var totalTime = 0L
        var totalMoving = 0L
        var maxKmh = 0
        var mileageSum = 0
        var mileageCount = 0

        for (r in rides) {
            totalKm += r.km()
            totalTime += r.durationMs()
            totalMoving += r.movingMs
            maxKmh = Math.max(maxKmh, r.maxKmh)
            if (r.avgKmL > 0) {
                mileageSum += r.avgKmL
                mileageCount++
            }
        }

        anHeroKm.text = String.format(Locale.ROOT, "%.1f km", totalKm)
        val lastOdo = RideStore.lastOdo(this)
        anOdo.text = if (lastOdo > 0) BikeFrames.tenthsText(lastOdo) + " km" else "--"

        if (prevFrom > 0) {
            var prevKm = 0.0
            for (r in prevRides) prevKm += r.km()
            if (prevKm > 0) {
                val diff = (totalKm - prevKm) / prevKm * 100.0
                val up = diff >= 0
                anTrendIcon.setKind(if (up) LineIcon.TREND_UP else LineIcon.TREND_DOWN)
                anTrendIcon.setColor(if (up) OK else WARN)
                anTrend.setTextColor(if (up) OK else WARN)
                anTrend.text = String.format(Locale.ROOT, "%+.1f%% vs previous period", diff)
            } else {
                anTrend.text = "First period with recorded rides"
            }
        } else {
            anTrend.text = "${rides.size} total finished rides"
        }

        anRides.text = rides.size.toString()
        anTime.text = RideTracker.duration(totalTime)
        val avgKmh = if (totalMoving > 10_000) totalKm / (totalMoving / 3_600_000.0) else 0.0
        anAvg.text = String.format(Locale.ROOT, "%.0f km/h", avgKmh)
        anTop.text = if (maxKmh > 0) "$maxKmh km/h" else "--"
        anMileage.text = if (mileageCount > 0) "${mileageSum / mileageCount} km/L" else "--"
        anPerRide.text = if (rides.isNotEmpty()) String.format(Locale.ROOT, "%.1f km", totalKm / rides.size) else "--"

        // Populate Bar Chart
        val chartValues = FloatArray(chartBins)
        if (rides.isNotEmpty() && chartBins > 0) {
            val step = Math.max(1L, (now - from) / chartBins)
            for (r in rides) {
                val bIdx = Math.min(chartBins - 1, Math.max(0, ((r.start - from) / step).toInt()))
                chartValues[bIdx] += r.km().toFloat()
            }
        }
        anChart.set(chartValues, binLabels.toTypedArray())
    }

    private inner class BarChart : View(this@MainActivity) {
        private var values = FloatArray(0)
        private var labels = arrayOf<String>()
        private val pBar = Paint(Paint.ANTI_ALIAS_FLAG)
        private val pText = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        fun set(v: FloatArray, l: Array<String>) {
            values = v
            labels = l
            invalidate()
        }

        override fun onDraw(cv: Canvas) {
            if (values.isEmpty()) return
            val w = width.toFloat()
            val hgt = height.toFloat()
            val bottomPad = dp(24).toFloat()
            val plotH = hgt - bottomPad
            var maxV = 1f
            for (v in values) maxV = Math.max(maxV, v)

            val n = values.size
            val barW = (w / n) * 0.55f
            val slotW = w / n

            pText.textSize = dp(11).toFloat()
            pText.typeface = display(500)
            pText.color = MUTED
            pText.textAlign = Paint.Align.CENTER

            for (i in 0 until n) {
                val cx = slotW * i + slotW / 2f
                val bh = (values[i] / maxV) * (plotH - dp(10))
                val top = plotH - bh

                pBar.color = if (values[i] > 0) ACCENT else EMPTY
                rect.set(cx - barW / 2f, top, cx + barW / 2f, plotH)
                cv.drawRoundRect(rect, dp(4).toFloat(), dp(4).toFloat(), pBar)

                if (i < labels.size) {
                    cv.drawText(labels[i], cx, hgt - dp(6), pText)
                }
            }
        }
    }

    // =====================================================================================
    // TAB 3: LOGS
    // =====================================================================================
    private fun buildLog(root: LinearLayout) {
        val head = hbox().apply { gravity = Gravity.BOTTOM }
        head.addView(pageTitle(null, "Logs"), LinearLayout.LayoutParams(0, -2, 1f))
        val clear = text("Clear", 13, TEXT, body(600)).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
            background = ripple(round(PILL, 99, PILL_LINE))
            isClickable = true
            setOnClickListener {
                BikeService.clearScreen()
                updateLog(true)
            }
        }
        head.addView(clear, LinearLayout.LayoutParams(-2, dp(34)))
        root.addView(head)

        val note = text("Live stream from current session. Daily files preserved under Settings › Developer options.", 12, MUTED, body(400))
        root.addView(note, marginTop(-1, -2, 8))

        val chips = hbox()
        for (i in FILTERS.indices) {
            val f = i
            val c = text(FILTERS[i], 13, SUB, body(500)).apply {
                gravity = Gravity.CENTER
                setPadding(dp(14), 0, dp(14), 0)
                isClickable = true
                setOnClickListener {
                    logFilter = f
                    paintChips()
                    updateLog(true)
                }
            }
            val lp = LinearLayout.LayoutParams(-2, dp(32))
            if (i > 0) lp.setMargins(dp(8), 0, 0, 0)
            chips.addView(c, lp)
            filterChips.add(c)
        }
        paintChips()
        root.addView(chips, marginTop(-1, -2, 12))

        logView = text("", 11, TEXT2, Typeface.MONOSPACE).apply {
            setTextIsSelectable(true)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        logScroll = ScrollView(this).apply {
            setPadding(dp(14), dp(12), dp(14), dp(12))
            clipToPadding = false
            background = round(LOG_BG, 20, BAR_LINE)
            isVerticalScrollBarEnabled = false
            addView(logView, ViewGroup.LayoutParams(-1, -2))
        }
        root.addView(logScroll, LinearLayout.LayoutParams(-1, dp(420)).apply { setMargins(0, dp(14), 0, 0) })
    }

    private fun paintChips() {
        for (i in filterChips.indices) {
            val on = i == logFilter
            val c = filterChips[i]
            c.background = ripple(if (on) round(SELECTED, 99, 0) else round(0, 99, HERO_LINE))
            c.setTextColor(if (on) ACCENT else SUB)
            c.typeface = body(if (on) 600 else 500)
        }
    }

    private fun updateLog(force: Boolean) {
        if (!::logView.isInitialized) return
        val lines = BikeService.logLines(250)
        val sb = SpannableStringBuilder()
        for (l in lines) {
            val filterOk = when (logFilter) {
                1 -> l.contains("FROM BIKE") || l.contains("TO BIKE") || l.contains("   =")
                2 -> l.contains("MAPS") || l.contains("ICON") || l.contains("SEND")
                3 -> l.contains("STATUS") || l.contains("ALERT") || l.contains("Auto-connect")
                else -> true
            }
            if (filterOk) {
                val start = sb.length
                sb.append(l).append("\n")
                val end = sb.length
                val color = when {
                    l.contains("ALERT") || l.contains("warn") -> WARN
                    l.contains("READY") || l.contains("Connected") -> OK
                    l.contains("Disconnected") || l.contains("off") -> BAD
                    l.contains("TO BIKE") || l.contains("SEND") -> ACCENT
                    l.contains("FROM BIKE") -> BLUE
                    l.contains("MAPS") -> PURPLE
                    else -> TEXT2
                }
                sb.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        logView.text = sb
    }

    // =====================================================================================
    // TAB 4: SETTINGS
    // =====================================================================================
    private fun buildSettings(root: LinearLayout) {
        val p = BikeService.prefs(this)
        val head = hbox().apply { gravity = Gravity.BOTTOM }
        head.addView(pageTitle(null, "Settings"), LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(head)

        root.addView(sectionLabel("BIKE PROFILE"))
        val bGroup = group(root)
        val bRow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = ripple(round(0, 22, 0))
            isClickable = true
            setOnClickListener { findBike() }
            setOnLongClickListener {
                editAddress()
                true
            }
        }
        bRow.addView(iconTile(LineIcon.BIKE, 36, 12, SELECTED, ACCENT))
        val bTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
        }
        bikeTitle = text(BIKE_MODEL, 15, TEXT, body(600))
        bTexts.addView(bikeTitle)
        bikeAddr = text(p.getString("mac", "Not connected") ?: "Not connected", 12, MUTED, Typeface.MONOSPACE)
        bTexts.addView(bikeAddr)
        bRow.addView(bTexts, LinearLayout.LayoutParams(0, -2, 1f))

        bikeBtn = text("Find", 13, ACCENT, body(600)).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
            background = ripple(round(PILL, 99, PILL_LINE))
        }
        bRow.addView(bikeBtn, LinearLayout.LayoutParams(-2, dp(34)))
        bGroup.addView(bRow)

        root.addView(sectionLabel("NAVIGATION & BEHAVIOR"))
        val nGroup = group(root)
        switchRow(nGroup, LineIcon.SEND, "Mirror navigation", "Send turn-by-turn arrows to the bike cluster", "mirrorNav", true, null)
        divider(nGroup)
        switchRow(nGroup, LineIcon.BIKE, "Ride mode", "Auto-reconnect upon ignition and restart", "rideMode", true, null)

        root.addView(sectionLabel("SAFETY ALERTS"))
        val aGroup = group(root)
        switchRow(aGroup, LineIcon.OVERVIEW, "Speed limit alert", "Vibrate phone when reaching speed limit", "speedAlert", false, null)
        stepperRow(aGroup, "Limit speed", "speedLimit", 80, 40, 140, 5, " km/h")
        divider(aGroup)
        switchRow(aGroup, LineIcon.FUEL, "Low fuel alert", "Warn when fuel gauge falls below threshold", "fuelAlert", true, null)
        stepperRow(aGroup, "Fuel threshold", "fuelLevel", 1, 0, 4, 1, null)

        root.addView(sectionLabel("APPEARANCE THEME"))
        val tGroup = group(root)
        buildTheme(tGroup)

        root.addView(sectionLabel("PERMISSIONS & SETUP"))
        val sGroup = group(root)
        setupGroup = sGroup
        chkPerms = CheckRow(sGroup, "Bluetooth & Nearby devices", "Lets app talk to TVS cluster") { askPermissions() }
        divider(sGroup)
        chkMaps = CheckRow(sGroup, "Read Google Maps notifications", "Required to detect turn-by-turn arrows") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        divider(sGroup)
        chkBattery = CheckRow(sGroup, "Unrestricted battery mode", "Prevents Android from pausing navigation") { keepRunning() }

        root.addView(sectionLabel("DATA MANAGEMENT"))
        val dGroup = group(root)
        val clearRides = actionRow(dGroup, LineIcon.TRASH, "Clear ride history", "Erase all saved rides from database", true) {
            confirmDelete("Delete all rides?", "The ride history is removed from this phone.", "Delete") {
                RideStore.clear(this)
                refreshRides()
                refreshAnalytics()
                toast("Rides deleted")
            }
        }

        buildDeveloper(root)

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        buildFooter(root)
        updateSettings()
    }

    private fun buildTheme(g: LinearLayout) {
        val p = BikeService.prefs(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val seg = hbox().apply {
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = round(PILL, 99, 0)
        }
        val note = text("", 12, MUTED, body(400))
        val names = arrayOf("Dark", "Light", "System", "Auto")
        val notes = arrayOf(
            "High-contrast cyberpunk dark mode",
            "Crisp daytime light theme",
            "Follows Android system dark mode",
            "Auto dark mode: light 6 AM to 6 PM, dark at night"
        )
        val opts = ArrayList<TextView>()
        val paint = Runnable {
            val cur = p.getString("theme", "dark")
            for (i in opts.indices) {
                val on = THEMES[i] == cur
                opts[i].background = if (on) round(HERO, 99, HERO_LINE) else null
                opts[i].setTextColor(if (on) ACCENT else SUB)
                opts[i].typeface = body(if (on) 600 else 500)
                if (on) note.text = notes[i]
            }
        }
        for (i in names.indices) {
            val idx = i
            val o = text(names[i], 13, SUB, body(500)).apply {
                gravity = Gravity.CENTER
                isClickable = true
                setOnClickListener {
                    p.edit().putString("theme", THEMES[idx]).apply()
                    BikeService.log("Theme set to ${THEMES[idx]}")
                    if (wantDark() != darkNow) recreate()
                    else paint.run()
                }
            }
            seg.addView(o, LinearLayout.LayoutParams(0, dp(36), 1f))
            opts.add(o)
        }
        paint.run()
        box.addView(seg)
        note.setPadding(dp(4), dp(10), 0, 0)
        box.addView(note)
        g.addView(box)
    }

    private fun buildDeveloper(root: LinearLayout) {
        val p = BikeService.prefs(this)
        root.addView(sectionLabel("DEVELOPER OPTIONS"))
        val g = group(root)

        val head = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = ripple(round(0, 22, 0))
            isClickable = true
        }
        head.addView(iconTile(LineIcon.CODE, 36, 12, SELECTED, ACCENT))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
            addView(text("Developer tools", 14, TEXT, body(600)))
            val st = text("Diagnostic logs and debugging exports", 12, MUTED, body(400))
            st.setPadding(0, dp(2), 0, 0)
            addView(st)
        }
        head.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val chev = LineIcon(this, LineIcon.CHEVRON, MUTED)
        head.addView(chev, LinearLayout.LayoutParams(dp(16), dp(16)).apply { setMargins(0, 0, dp(12), 0) })
        val dev = Toggle(p.getBoolean("devOptions", true))
        head.addView(dev, LinearLayout.LayoutParams(dp(50), dp(30)))
        g.addView(head)

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        g.addView(inner)
        divider(inner)

        val save = switchRow(inner, LineIcon.LOG, "Save daily logs", "Rotates one file per day (keeps 7 days)", "saveLogs", true) {
            BikeService.setSaveToFile(it)
        }
        divider(inner)
        val export = actionRow(inner, LineIcon.SHARE, "Export diagnostic logs", "Zip saved log files with privacy redaction", false) {
            shareLog()
        }
        divider(inner)
        val delete = actionRow(inner, LineIcon.TRASH, "Delete all logs", "Remove stored log files from device", true) {
            confirmDelete("Delete logs?", "Deletes all stored log files and resets the live buffer.", "Delete") {
                deleteLogs()
            }
        }

        val fold = Runnable {
            val open = inner.visibility != View.VISIBLE
            inner.visibility = if (open) View.VISIBLE else View.GONE
            chev.rotation = if (open) 90f else 0f
        }
        head.setOnClickListener { fold.run() }
        dev.setOnClickListener {
            dev.set(!dev.on)
            p.edit().putBoolean("devOptions", dev.on).apply()
            if (dev.on && inner.visibility != View.VISIBLE) fold.run()
            BikeService.setSaveToFile(dev.on)
        }
    }

    private fun buildFooter(root: LinearLayout) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, dp(12))
        }
        val devLine = hbox().apply { gravity = Gravity.CENTER }
        devLine.addView(text("Developed by ", 12, MUTED, body(400)))
        val devName = text(DEVELOPER_NAME, 12, ACCENT, body(600)).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setPadding(dp(2), 0, 0, 0)
        }
        devLine.addView(devName)
        row.addView(devLine)

        val projectInfo = text("$APP_TITLE • 100% Kotlin", 11, FAINT, body(500)).apply {
            setPadding(0, dp(4), 0, 0)
        }
        row.addView(projectInfo)
        root.addView(row, marginTop(-1, -2, 20))
    }

    private fun updateSettings() {
        val mac = BikeService.prefs(this).getString("mac", "") ?: ""
        val bikeName = BikeService.prefs(this).getString("bikeName", BIKE_MODEL) ?: BIKE_MODEL
        bikeTitle?.text = if (mac.isEmpty()) "No bike chosen" else bikeName
        bikeAddr?.text = if (mac.isEmpty()) "Tap Find to scan via Bluetooth LE" else mac
        bikeBtn?.text = if (mac.isEmpty()) "Find" else "Change"

        // Update setup check rows
        val hasBt = Build.VERSION.SDK_INT < 31 || checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED
        val hasMaps = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")?.contains(packageName) == true
        val pm = getSystemService(PowerManager::class.java)
        val hasBattery = pm?.isIgnoringBatteryOptimizations(packageName) == true

        chkPerms?.set(hasBt)
        chkMaps?.set(hasMaps)
        chkBattery?.set(hasBattery)

        val allOk = hasBt && hasMaps && hasBattery
        setupCard.visibility = if (allOk) View.GONE else View.VISIBLE
        if (!allOk) {
            setupNote.text = when {
                !hasBt -> "Enable Bluetooth & Nearby permissions"
                !hasMaps -> "Enable Notification Reader for Google Maps"
                else -> "Set Battery to Unrestricted"
            }
        }
    }

    // =====================================================================================
    // BLUETOOTH LE SCAN & CONNECTION
    // =====================================================================================
    private fun bluetoothOn(): Boolean {
        val ad = getSystemService(BluetoothManager::class.java)?.adapter
        return ad != null && ad.isEnabled
    }

    private fun requestBluetooth(cb: Runnable) {
        try {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: Exception) {
            toast("Please turn on Bluetooth")
        }
    }

    private fun connect() {
        val mac = BikeService.prefs(this).getString("mac", "") ?: ""
        if (mac.isEmpty()) {
            findBike()
            return
        }
        startForegroundService(Intent(this, BikeService::class.java).setAction(BikeService.ACTION_CONNECT))
    }

    private fun disconnect() {
        startForegroundService(Intent(this, BikeService::class.java).setAction(BikeService.ACTION_DISCONNECT))
    }

    private fun findBike() {
        if (scanning) return
        val ad = getSystemService(BluetoothManager::class.java)?.adapter
        if (ad == null) {
            toast("Bluetooth LE is not supported on this phone")
            return
        }
        if (!ad.isEnabled) {
            requestBluetooth { findBike() }
            return
        }
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED) {
            askPermissions()
            toast("Allow Nearby devices permission and try again")
            return
        }
        val sc = ad.bluetoothLeScanner
        if (sc == null) {
            toast("Bluetooth scanner not available")
            return
        }

        val found = LinkedHashMap<String, ScanResult>()
        val shown = ArrayList<ScanResult>()
        val sheet = Sheet("Find your TVS Raider", "Ensure bike ignition is ON and TVS Connect is closed.")
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            addView(list)
        }
        sheet.content.addView(listScroll, LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(0, dp(12), 0, 0) })
        val stopRef = arrayOfNulls<Runnable>(1)

        val relist = Runnable {
            shown.clear()
            shown.addAll(found.values)
            shown.sortWith { x, y ->
                val gx = rank(x)
                val gy = rank(y)
                if (gx != gy) gx - gy else y.rssi - x.rssi
            }
            list.removeAllViews()
            for (r in shown) {
                val row = deviceRow(r)
                row.setOnClickListener {
                    sheet.dialog.dismiss()
                    stopRef[0]?.run()
                    useBike(r)
                }
                list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })
            }
            listScroll.layoutParams.height = if (shown.isEmpty()) dp(1) else Math.min(shown.size * dp(66), dp(330))
            listScroll.requestLayout()
        }

        val cb = object : ScanCallback() {
            override fun onScanResult(type: Int, r: ScanResult) {
                h.post {
                    val isNew = !found.containsKey(r.device.address)
                    found[r.device.address] = r
                    if (isNew) relist.run()
                }
            }
        }

        val stop = Runnable {
            if (!scanning) return@Runnable
            scanning = false
            try { sc.stopScan(cb) } catch (ignored: Exception) {}
            relist.run()
            sheet.spinner.visibility = View.GONE
            sheet.title.text = if (found.isEmpty()) "No bikes found" else "Select your TVS Raider"
            sheet.message.text = if (found.isEmpty()) "Turn bike ignition ON, close official TVS Connect, then retry." else "★ = Recognized TVS SmartXonnect bike."
        }
        stopRef[0] = stop

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        disconnect()
        scanning = true
        BikeService.log("Scanning for TVS Raider Bluetooth LE devices...")
        sc.startScan(null, settings, cb)
        h.postDelayed(stop, 15000)

        sheet.spinner.visibility = View.VISIBLE
        sheet.button("Cancel", 0, null)
        sheet.dialog.setOnDismissListener {
            h.removeCallbacks(stop)
            stop.run()
        }
        sheet.show()
    }

    private fun deviceRow(r: ScanResult): View {
        val bike = looksLikeBike(r)
        val name = nameOf(r)
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = ripple(round(if (bike) SELECTED else PILL, 14, if (bike) ACCENT else 0))
            isClickable = true
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val title = (if (bike) "★ " else "") + (name ?: "Unnamed BLE device")
        texts.addView(label(title, 15, if (name == null) MUTED else TEXT, "sans-serif-medium"))
        val addr = label(r.device.address, 12, MUTED, "monospace").apply { typeface = Typeface.MONOSPACE }
        texts.addView(addr)
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        val rssi = r.rssi
        val sig = if (rssi >= -60) "Strong" else if (rssi >= -75) "Good" else "Weak"
        val signal = label(sig, 12, if (rssi >= -60) ACCENT else MUTED, "sans-serif-medium")
        row.addView(signal)
        return row
    }

    private fun rank(r: ScanResult): Int = if (looksLikeBike(r)) 0 else if (nameOf(r) != null) 1 else 2

    private fun nameOf(r: ScanResult): String? {
        var name = r.device.name
        if (name == null && r.scanRecord != null) name = r.scanRecord?.deviceName
        return name
    }

    private fun looksLikeBike(r: ScanResult): Boolean {
        val rec = r.scanRecord
        if (rec != null && rec.serviceUuids != null) {
            for (u in rec.serviceUuids) {
                if (u.uuid == BikeService.SERVICE) return true
            }
        }
        val name = nameOf(r) ?: return false
        val n = name.uppercase(Locale.ROOT)
        return n.contains("TVS") || n.contains("RAIDER") || n.contains("SMARTX") || n.contains("NTORQ")
                || n.contains("APACHE") || n.contains("RONIN") || n.contains("JUPITER")
    }

    private fun useBike(r: ScanResult) {
        val mac = r.device.address
        val name = nameOf(r)
        val editor = BikeService.prefs(this).edit().putString("mac", mac)
        if (name != null) editor.putString("bikeName", name)
        editor.apply()
        updateSettings()
        BikeService.log("Bike address saved: $mac (${name ?: "TVS Raider"})")
        toast("Selected $name. Connecting...")
        startForegroundService(Intent(this, BikeService::class.java).setAction(BikeService.ACTION_CONNECT))
    }

    private fun editAddress() {
        val p = BikeService.prefs(this)
        val sheet = Sheet("Set Bike Bluetooth Address", "Enter Bluetooth MAC manually (e.g. AA:BB:CC:DD:EE:FF)")
        val e = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(p.getString("mac", ""))
            hint = "AA:BB:CC:DD:EE:FF"
            setHintTextColor(FAINT)
            setTextColor(TEXT)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            background = round(PILL, 14, 0)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        sheet.content.addView(e, marginTop(-1, -2, 14))
        sheet.button("Cancel", 0, null).button("Save", 1) {
            p.edit().putString("mac", e.text.toString().trim().uppercase(Locale.ROOT)).apply()
            updateSettings()
            toast("Address saved")
        }.show()
    }

    private fun keepRunning() {
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        toast("Set Battery to 'Unrestricted' so navigation stays active")
    }

    private fun openMaps() {
        val pi = MapsListener.tripIntent
        if (pi != null) {
            try {
                pi.send()
                return
            } catch (ignored: Exception) {}
        }
        val intent = packageManager.getLaunchIntentForPackage(MapsListener.MAPS)
        if (intent != null) {
            startActivity(intent)
        } else {
            toast("Google Maps is not installed")
        }
    }

    // =====================================================================================
    // LOG EXPORT & SHARING
    // =====================================================================================
    private fun shareLog() {
        val dir = LogFileProvider.dir(this)
        deleteLogFiles()
        if (!dir.isDirectory && !dir.mkdirs()) {
            toast("Could not create log export folder")
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
        val days = BikeService.logFiles()
        val zip = File(dir, "TVS-Raider-logs-$stamp.zip")
        try {
            ZipOutputStream(FileOutputStream(zip)).use { z ->
                zipText(z, "device.txt", header())
                for (f in days) zipText(z, f.name, readText(f))
                if (days.isEmpty() || !BikeService.saveLogsWanted(this)) {
                    zipText(z, "live-$stamp.txt", BikeService.liveLog())
                }
            }
        } catch (e: IOException) {
            toast("Failed to export logs: ${e.message}")
            return
        }
        val uri = LogFileProvider.uriFor(zip)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_SUBJECT, "TVS Raider logs $stamp")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(zip.name, uri)
        }
        startActivity(Intent.createChooser(send, "Share TVS Raider logs"))
    }

    private fun header(): String {
        val p = BikeService.prefs(this)
        return "TVS Raider Navigations Log  ${Date()}\n" +
                "Phone: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n" +
                "Bike: ${p.getString("bikeName", BIKE_MODEL)} ${p.getString("mac", "")}\n" +
                "Status: ${BikeService.status}\n" +
                "Developer: $DEVELOPER_NAME\n\n"
    }

    private fun zipText(z: ZipOutputStream, name: String, text: String) {
        z.putNextEntry(ZipEntry(name))
        z.write(LogRedactor.redact(text).toByteArray(StandardCharsets.UTF_8))
        z.closeEntry()
    }

    private fun readText(f: File): String {
        val bytes = ByteArray(f.length().toInt())
        FileInputStream(f).use { it.read(bytes) }
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun deleteLogFiles() {
        val old = LogFileProvider.dir(this).listFiles()
        if (old != null) {
            for (o in old) o.delete()
        }
    }

    private fun deleteLogs() {
        BikeService.clearLog()
        deleteLogFiles()
        updateLog(true)
        toast("Logs deleted")
    }

    private fun askPermissions() {
        val need = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            need.add("android.permission.BLUETOOTH_CONNECT")
            need.add("android.permission.BLUETOOTH_SCAN")
        } else {
            need.add("android.permission.ACCESS_FINE_LOCATION")
            need.add("android.permission.ACCESS_COARSE_LOCATION")
        }
        if (Build.VERSION.SDK_INT >= 33) need.add("android.permission.POST_NOTIFICATIONS")
        val missing = ArrayList<String>()
        for (s in need) {
            if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) missing.add(s)
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)
    }

    // =====================================================================================
    // REFRESH LOOP
    // =====================================================================================
    private val refresh = object : Runnable {
        override fun run() {
            updateStatus()
            updateHero()
            updateHomeTiles()
            if (tab == TAB_OVERVIEW) updateBike()
            if (tab == TAB_LOG) updateLog(false)
            h.postDelayed(this, 1000)
        }
    }

    private fun updateStatus() {
        val s = BikeService.instance
        val ready = s != null && s.isReady()
        val active = s != null && s.isActive()

        pillDot.background = round(if (ready) OK else if (active) WARN else FAINT, 99, 0)
        pillText.text = if (ready) "Connected" else if (active) "Connecting..." else "Standby"
        statusTitle.text = if (ready) "Connected to Raider" else if (active) "Connecting to bike..." else "Ready to ride"

        primaryBtn.text = if (active) "Disconnect" else "Connect"
        primaryBtn.setBackground(ripple(if (active) round(PILL, 99, PILL_LINE) else round(ACCENT, 99, 0)))
        primaryBtn.setTextColor(if (active) TEXT else ON_ACCENT)
    }

    private fun updateHero() {
        val nav = MapsListener.current
        if (nav == null) {
            arrow.setPicto(-1)
            distView.text = "--"
            instrView.text = "Start Google Maps Navigation"
            roadView.text = "Turn instructions will show here & on bike"
            timeLeftView.text = "--"
            distLeftView.text = "--"
            etaView.text = "--"
            return
        }
        arrow.setPicto(nav.picto)
        arrow.setColors(ACCENT, ARROW_DIM)

        val dNext = nav.distNextM
        distView.text = if (dNext >= 1000) String.format(Locale.ROOT, "%.1f km", dNext / 1000f) else "$dNext m"
        instrView.text = nav.text ?: "Continue"
        roadView.text = MapsListener.road.ifEmpty { "Navigating route" }

        val mins = nav.minutesLeft
        timeLeftView.text = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else if (mins > 0) "$mins min" else "--"

        val dLeft = nav.distLeftM
        distLeftView.text = if (dLeft >= 1000) String.format(Locale.ROOT, "%.1f km", dLeft / 1000f) else if (dLeft > 0) "$dLeft m" else "--"
        etaView.text = MapsListener.eta.ifEmpty { "--" }
    }

    private fun updateHomeTiles() {
        val bars = BikeFrames.fuelBars
        paintFuel(homeFuelSegs, bars)
        homeFuelText.text = if (bars < 0) "--" else if (bars == 0) "Empty" else "$bars / 5 bars"
        val ml = BikeFrames.avgKmL
        homeMileage.text = if (ml > 0) ml.toString() else "--"
        val bv = BikeFrames.batteryTenths
        homeBattery.text = if (bv > 0) BikeFrames.tenthsText(bv) else "--"
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }

    // =====================================================================================
    // UI BUILDER HELPERS
    // =====================================================================================
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun hbox(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

    private fun text(s: String, sp: Int, color: Int, tf: Typeface): TextView = TextView(this).apply {
        text = s
        textSize = sp.toFloat()
        setTextColor(color)
        typeface = tf
    }

    private fun label(s: String, sp: Int, color: Int, family: String): TextView {
        val tf = if ("monospace" == family) Typeface.MONOSPACE
        else if (family.contains("medium")) body(600)
        else body(400)
        return text(s, sp, color, tf)
    }

    private fun caps(s: String): TextView = text(s, 11, MUTED, body(600)).apply {
        letterSpacing = 0.12f
    }

    private fun pageTitle(over: String?, title: String): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (over != null) c.addView(text(over, 12, MUTED, body(400)))
        val t = text(title, 26, TEXT, display(600)).apply { setPadding(0, dp(2), 0, 0) }
        c.addView(t)
        return c
    }

    private fun sectionLabel(s: String): TextView = caps(s).apply {
        setPadding(dp(4), dp(20), dp(4), dp(8))
    }

    private fun group(root: LinearLayout): LinearLayout {
        val g = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(TILE, 22, TILE_LINE)
        }
        root.addView(g, LinearLayout.LayoutParams(-1, -2))
        return g
    }

    private fun divider(g: LinearLayout) {
        val d = View(this).apply { setBackgroundColor(TILE_LINE) }
        g.addView(d, LinearLayout.LayoutParams(-1, dp(1)).apply { setMargins(dp(16), 0, dp(16), 0) })
    }

    private fun iconTile(kind: Int, sizeDp: Int, radiusDp: Int, bg: Int, fg: Int): FrameLayout {
        val f = FrameLayout(this).apply {
            background = round(bg, radiusDp, 0)
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        }
        val icon = Math.round(sizeDp * 0.52f)
        f.addView(LineIcon(this, kind, fg), FrameLayout.LayoutParams(dp(icon), dp(icon), Gravity.CENTER))
        return f
    }

    private fun marginTop(w: Int, hgt: Int, topDp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, hgt).apply { setMargins(0, dp(topDp), 0, 0) }

    private fun round(color: Int, radiusDp: Int, strokeColor: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != 0) setStroke(dp(1), strokeColor)
        }

    private fun ripple(content: Drawable): Drawable = RippleDrawable(ColorStateList.valueOf(RIPPLE), content, null)

    private fun pillButton(s: String, primary: Boolean): TextView = text(s, 15, if (primary) ON_ACCENT else TEXT, body(600)).apply {
        gravity = Gravity.CENTER
        background = ripple(if (primary) round(ACCENT, 99, 0) else round(PILL, 99, PILL_LINE))
        isClickable = true
    }

    private fun tile(row: LinearLayout, label: String, leftGap: Int): LinearLayout {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = ripple(round(TILE, 20, TILE_LINE))
            isClickable = true
            addView(caps(label))
        }
        val lp = LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(leftGap, 0, 0, 0) }
        row.addView(t, lp)
        return t
    }

    private fun tileValue(t: LinearLayout, unit: String): TextView {
        val v = text("--", 20, TEXT, display(600)).apply {
            includeFontPadding = false
            setPadding(0, dp(10), 0, 0)
        }
        t.addView(v)
        val u = text(unit, 12, MUTED, body(400)).apply { setPadding(0, dp(6), 0, 0) }
        t.addView(u)
        return v
    }

    private fun stat(row: LinearLayout, title: String, sp: Int): TextView {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(caps(title))
        val v = text("--", sp, TEXT, display(600)).apply { setPadding(0, dp(4), 0, 0) }
        c.addView(v)
        row.addView(c, LinearLayout.LayoutParams(0, -2, 1f))
        return v
    }

    private fun switchRow(
        g: LinearLayout,
        icon: Int,
        title: String,
        sub: String,
        key: String,
        def: Boolean,
        cb: ((Boolean) -> Unit)?
    ): LinearLayout {
        val p = BikeService.prefs(this)
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
        }
        row.addView(iconTile(icon, 36, 12, SELECTED, ACCENT))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
            addView(text(title, 14, TEXT, body(600)))
            val st = text(sub, 12, MUTED, body(400)).apply { setPadding(0, dp(2), 0, 0) }
            addView(st)
        }
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val t = Toggle(p.getBoolean(key, def))
        val flip = View.OnClickListener {
            t.set(!t.on)
            p.edit().putBoolean(key, t.on).apply()
            cb?.invoke(t.on)
        }
        t.setOnClickListener(flip)
        row.setOnClickListener(flip)
        row.addView(t, LinearLayout.LayoutParams(dp(50), dp(30)))
        g.addView(row)
        return row
    }

    private fun stepperRow(
        g: LinearLayout,
        title: String,
        key: String,
        def: Int,
        min: Int,
        max: Int,
        step: Int,
        unit: String?
    ) {
        val p = BikeService.prefs(this)
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(64), 0, dp(16), dp(12))
        }
        row.addView(text(title, 13, SUB, body(500)), LinearLayout.LayoutParams(0, -2, 1f))
        val value = text("", 15, TEXT, display(600)).apply { gravity = Gravity.CENTER }
        var v = Math.max(min, Math.min(max, p.getInt(key, def)))
        val show = Runnable {
            value.text = if (unit != null) "$v$unit" else "$v" + if (v == 1) " bar" else " bars"
        }
        show.run()
        val minus = stepButton("−")
        val plus = stepButton("+")
        val change = View.OnClickListener { btn ->
            val nv = Math.max(min, Math.min(max, v + if (btn == plus) step else -step))
            if (nv != v) {
                v = nv
                p.edit().putInt(key, nv).apply()
                show.run()
            }
        }
        minus.setOnClickListener(change)
        plus.setOnClickListener(change)
        row.addView(minus, LinearLayout.LayoutParams(dp(34), dp(34)))
        row.addView(value, LinearLayout.LayoutParams(dp(72), -2))
        row.addView(plus, LinearLayout.LayoutParams(dp(34), dp(34)))
        g.addView(row)
    }

    private fun stepButton(s: String): TextView = text(s, 18, TEXT, body(600)).apply {
        gravity = Gravity.CENTER
        background = ripple(round(PILL, 99, PILL_LINE))
        isClickable = true
    }

    private fun actionRow(g: LinearLayout, icon: Int, title: String, sub: String, danger: Boolean, action: Runnable): LinearLayout {
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = ripple(round(0, 22, 0))
            isClickable = true
            setOnClickListener { action.run() }
        }
        row.addView(iconTile(icon, 36, 12, if (danger) RED_BG else SELECTED, if (danger) RED_TEXT else ACCENT))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
            addView(text(title, 14, if (danger) RED_TEXT else TEXT, body(600)))
            val st = text(sub, 12, MUTED, body(400)).apply { setPadding(0, dp(2), 0, 0) }
            addView(st)
        }
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(LineIcon(this, LineIcon.CHEVRON, FAINT), LinearLayout.LayoutParams(dp(18), dp(18)))
        g.addView(row)
        return row
    }

    private inner class Toggle(var on: Boolean) : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val r = RectF()

        init { isClickable = true }

        fun set(v: Boolean) {
            on = v
            invalidate()
        }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat()
            val hh = height.toFloat()
            val pad = dp(3).toFloat()
            val knob = hh - 2 * pad
            paint.color = if (on) ACCENT else TOGGLE_OFF
            r.set(0f, 0f, w, hh)
            c.drawRoundRect(r, hh / 2f, hh / 2f, paint)
            paint.color = if (on) ON_ACCENT else KNOB_OFF
            val cx = if (on) w - pad - knob / 2f else pad + knob / 2f
            c.drawCircle(cx, hh / 2f, knob / 2f, paint)
        }
    }

    private inner class CheckRow(parent: LinearLayout, title: String, sub: String, action: Runnable) {
        val badge = FrameLayout(this@MainActivity)
        val tick = LineIcon(this@MainActivity, LineIcon.CHECK, ON_ACCENT)
        val bang = text("!", 14, ON_WARN, body(700)).apply { gravity = Gravity.CENTER }
        val fix: TextView

        init {
            val row = hbox().apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(13), dp(16), dp(13))
            }
            badge.addView(tick, FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER))
            badge.addView(bang, FrameLayout.LayoutParams(-1, -1))
            row.addView(badge, LinearLayout.LayoutParams(dp(26), dp(26)))

            val texts = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(text(title, 14, TEXT, body(600)))
                val st = text(sub, 12, MUTED, body(400)).apply { setPadding(0, dp(2), 0, 0) }
                addView(st)
            }
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
            fix = text("Fix", 13, WARN, body(600)).apply {
                gravity = Gravity.CENTER
                background = ripple(round(WARN_BG, 99, 0))
                isClickable = true
                setOnClickListener { action.run() }
            }
            row.addView(fix, LinearLayout.LayoutParams(dp(56), dp(32)))
            parent.addView(row)
        }

        fun set(ok: Boolean) {
            badge.background = round(if (ok) OK else WARN, 99, 0)
            tick.visibility = if (ok) View.VISIBLE else View.GONE
            bang.visibility = if (ok) View.GONE else View.VISIBLE
            fix.visibility = if (ok) View.GONE else View.VISIBLE
        }
    }

    private inner class Sheet(titleText: String, messageText: String?) {
        val dialog = Dialog(this@MainActivity).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }
        val title: TextView
        val message: TextView
        val content: LinearLayout
        val buttons: LinearLayout
        val spinner: ProgressBar

        init {
            val box = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(22), dp(20), dp(22), dp(18))
                background = round(HERO, 24, HERO_LINE)
            }
            val head = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
            title = text(titleText, 20, TEXT, display(600))
            head.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            spinner = ProgressBar(this@MainActivity).apply {
                indeterminateTintList = ColorStateList.valueOf(ACCENT)
                visibility = View.GONE
            }
            head.addView(spinner, LinearLayout.LayoutParams(dp(22), dp(22)))
            box.addView(head)

            message = text(messageText ?: "", 13, MUTED, body(400)).apply {
                setPadding(0, dp(6), 0, 0)
                visibility = if (messageText == null) View.GONE else View.VISIBLE
            }
            box.addView(message)

            content = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            box.addView(content)

            buttons = hbox().apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(0, dp(16), 0, 0)
            }
            box.addView(buttons)

            dialog.setContentView(box)
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(0))
                setLayout(resources.displayMetrics.widthPixels - dp(36), ViewGroup.LayoutParams.WRAP_CONTENT)
                setDimAmount(0.65f)
            }
        }

        fun button(text: String, kind: Int, action: Runnable?): Sheet {
            val b = text(text, 14, if (kind == 0) TEXT else if (kind == 2) RED_TEXT else ON_ACCENT, body(600)).apply {
                gravity = Gravity.CENTER
                setPadding(dp(20), 0, dp(20), 0)
                background = ripple(
                    if (kind == 1) round(ACCENT, 99, 0)
                    else if (kind == 2) round(RED_BG, 99, RED_LINE)
                    else round(PILL, 99, PILL_LINE)
                )
                isClickable = true
                setOnClickListener {
                    dialog.dismiss()
                    action?.run()
                }
            }
            val lp = LinearLayout.LayoutParams(-2, dp(40))
            if (buttons.childCount > 0) lp.setMargins(dp(10), 0, 0, 0)
            buttons.addView(b, lp)
            return this
        }

        fun show(): Sheet {
            dialog.show()
            return this
        }
    }

    private fun confirmDelete(title: String, message: String, yes: String, action: Runnable) {
        Sheet(title, message).button("Cancel", 0, null).button(yes, 2, action).show()
    }
}
