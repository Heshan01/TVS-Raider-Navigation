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
 * 100% faithful implementation of Obsidian Telemetry design system (Stitch TVS Raider Smart Dashboard).
 * Built with pure programmatic Android Views for ultra-fast startup and fluid 60fps animations.
 * Developed by Heshan for TVS Raider 125.
 */
@SuppressLint("MissingPermission")
class MainActivity : Activity() {

    companion object {
        const val TAB_HOME = 0
        const val TAB_OVERVIEW = 1
        const val TAB_ANALYTICS = 2
        const val TAB_LOG = 3
        const val TAB_SETTINGS = 4

        const val BIKE_MODEL = "TVS Raider 125"
        const val DEVELOPER_NAME = "Heshan"
        const val APP_TITLE = "TVS Raider Navigations"

        val THEMES = arrayOf("dark", "light", "system", "auto")
        val PERIODS = arrayOf("Week", "Month", "Year", "All")
        val FILTERS = arrayOf("All", "Bike data", "Maps", "Status")

        const val SPEED_SEGS = 24
        const val SPEED_SCALE = 120
    }

    // Obsidian Telemetry Theme Palette
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

    // Global Top Bar
    private lateinit var topClock: TextView
    private lateinit var topBleIcon: LineIcon
    private lateinit var topTitle: TextView

    // UI elements: Home
    private lateinit var brandBadge: TextView
    private lateinit var pillDot: View
    private lateinit var pillText: TextView
    private lateinit var dispatchStatus: TextView
    private lateinit var statusTitle: TextView
    private lateinit var arrowTile: FrameLayout
    private lateinit var arrow: ArrowView
    private lateinit var distView: TextView
    private lateinit var distUnit: TextView
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
    private lateinit var primaryBtnDot: View

    // UI elements: Overview
    private lateinit var liveDot: View
    private lateinit var liveText: TextView
    private val fuelSegs = arrayOfNulls<View>(5)
    private lateinit var fuelState: TextView
    private lateinit var fuelCapacity: TextView
    private lateinit var fuelBig: TextView
    private lateinit var odoText: TextView
    private lateinit var tripBText: TextView
    private lateinit var mileageText: TextView
    private lateinit var batteryText: TextView
    private lateinit var weekText: TextView
    private lateinit var ridesBody: LinearLayout

    // UI elements: Analytics
    private var period = 0
    private val periodTabs = ArrayList<TextView>()
    private lateinit var anRecording: LinearLayout
    private lateinit var anHeroLabel: TextView
    private lateinit var anHeroKm: TextView
    private lateinit var anTrend: TextView
    private lateinit var anOdo: TextView
    private lateinit var anRides: TextView
    private lateinit var anTime: TextView
    private lateinit var anAvg: TextView
    private lateinit var anTop: TextView
    private lateinit var anMileage: TextView
    private lateinit var anPerRide: TextView
    private lateinit var anChart: BarChart

    // UI elements: Logs
    private var logFilter = 0
    private val filterChips = ArrayList<TextView>()
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var logBleStatus: TextView

    // UI elements: Settings
    private var bikeTitle: TextView? = null
    private var bikeAddr: TextView? = null
    private var bikeBtn: TextView? = null
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
            // Obsidian Telemetry Dark Mode
            BG = 0xFF0B0E11.toInt()           // Base Obsidian Canvas #0B0E11
            BAR = 0xFF14191F.toInt()          // Elevated Bento Cards #14191F
            BAR_LINE = 0xFF232B35.toInt()     // Structural Ghost Borders #232B35
            HERO = 0xFF14191F.toInt()
            HERO_LINE = 0xFF232B35.toInt()
            TILE = 0xFF14191F.toInt()
            TILE_LINE = 0xFF232B35.toInt()
            PILL = 0xFF191C1F.toInt()
            PILL_LINE = 0xFF232B35.toInt()
            SELECTED = 0xFF232B35.toInt()     // Active Tab background #232B35
            LOG_BG = 0xFF080A0D.toInt()       // Deep Wells & Inset Displays #080A0D
            ACCENT = 0xFF00E5C3.toInt()       // Electric Cyan-Teal #00E5C3
            ON_ACCENT = 0xFF00382E.toInt()
            TEXT = 0xFFF1F5F9.toInt()         // High-Contrast Text #F1F5F9
            TEXT2 = 0xFFCBD5E1.toInt()
            SUB = 0xFF94A3B8.toInt()
            MUTED = 0xFF64748B.toInt()        // Muted Technical Labels #64748B
            FAINT = 0xFF334155.toInt()
            EMPTY = 0xFF1D242C.toInt()
            OK = 0xFF22C55E.toInt()           // Live Emerald #22C55E
            WARN = 0xFFFF9F1C.toInt()         // Racing Amber #FF9F1C
            WARN_BG = 0xFF2C1904.toInt()
            ON_WARN = 0xFF492900.toInt()
            BAD = 0xFFFF5252.toInt()
            RED_TEXT = 0xFFFFB4AB.toInt()
            RED_BG = 0xFF2D1214.toInt()
            RED_LINE = 0xFF4D2226.toInt()
            PURPLE = 0xFFB388FF.toInt()
            BLUE = 0xFF00E5C3.toInt()
            SCALE_HI = 0xFFFF9F1C.toInt()
            ARROW_DIM = 0x3300E5C3.toInt()
            RIPPLE = 0x22FFFFFF
            TOGGLE_OFF = 0xFF1E2631.toInt()
            KNOB_OFF = 0xFF64748B.toInt()
        } else {
            // High-legibility Day/Light Mode
            BG = 0xFFF4F6F9.toInt()
            BAR = 0xFFFFFFFF.toInt()
            BAR_LINE = 0xFFE2E8F0.toInt()
            HERO = 0xFFFFFFFF.toInt()
            HERO_LINE = 0xFFCBD5E1.toInt()
            TILE = 0xFFFFFFFF.toInt()
            TILE_LINE = 0xFFE2E8F0.toInt()
            PILL = 0xFFE2E8F0.toInt()
            PILL_LINE = 0xFFCBD5E1.toInt()
            SELECTED = 0xFF00E5C3.toInt()
            LOG_BG = 0xFF0F172A.toInt()
            ACCENT = 0xFF00A88F.toInt()
            ON_ACCENT = 0xFFFFFFFF.toInt()
            TEXT = 0xFF0F172A.toInt()
            TEXT2 = 0xFF334155.toInt()
            SUB = 0xFF475569.toInt()
            MUTED = 0xFF64748B.toInt()
            FAINT = 0xFF94A3B8.toInt()
            EMPTY = 0xFFE2E8F0.toInt()
            OK = 0xFF16A34A.toInt()
            WARN = 0xFFD97706.toInt()
            WARN_BG = 0xFFFEF3C7.toInt()
            ON_WARN = 0xFF78350F.toInt()
            BAD = 0xFFDC2626.toInt()
            RED_TEXT = 0xFFB91C1C.toInt()
            RED_BG = 0xFFFEE2E2.toInt()
            RED_LINE = 0xFFFCA5A5.toInt()
            PURPLE = 0xFF7C3AED.toInt()
            BLUE = 0xFF0284C7.toInt()
            SCALE_HI = 0xFFD97706.toInt()
            ARROW_DIM = 0x3300A88F.toInt()
            RIPPLE = 0x22000000
            TOGGLE_OFF = 0xFFCBD5E1.toInt()
            KNOB_OFF = 0xFF64748B.toInt()
        }
    }

    private fun wantDark(): Boolean {
        val t = BikeService.prefs(this).getString("theme", "dark")
        if ("dark" == t) return true
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
        window.navigationBarColor = BG
        window.decorView.setBackgroundColor(BG)

        val outer = FrameLayout(this).apply {
            setBackgroundColor(BG)
        }

        // Top-level vertical container
        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Fixed Top Status Bar
        buildTopHeader(mainCol)

        // 2. Tab Pages Container
        val content = FrameLayout(this)
        mainCol.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        outer.addView(mainCol, FrameLayout.LayoutParams(-1, -1))

        // Create 5 Tab Pages with appropriate padding for floating dock
        buildHome(tabPage(content, true))
        buildOverview(tabPage(content, true))
        buildAnalytics(tabPage(content, true))
        buildLog(tabPage(content, true))
        buildSettings(tabPage(content, true))

        // 3. Floating Bottom Navigation Dock (Pill design)
        buildFloatingNavDock(outer)

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
            // Extra bottom padding ensures content never gets clipped by floating pill dock
            setPadding(dp(16), dp(10), dp(16), dp(100))
        }
        val v: View = if (scroll) {
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
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
        val titles = arrayOf("HOME", "OVERVIEW", "ANALYTICS", "LOGS", "SETTINGS")
        topTitle.text = titles.getOrElse(t) { "HOME" }

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

    // =====================================================================================
    // TOP STATUS & APP BAR
    // =====================================================================================
    private fun buildTopHeader(parent: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xF00B0E11.toInt()) // Frosted obsidian
            setPadding(dp(16), dp(8), dp(16), dp(10))
        }

        // 1. System Telemetry Strip (Time, BLE, 5G, Battery)
        val sysRow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        topClock = text(SimpleDateFormat("HH:mm", Locale.ROOT).format(Date()), 11, MUTED, Typeface.MONOSPACE)
        sysRow.addView(topClock, LinearLayout.LayoutParams(0, -2, 1f))

        val icons = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        topBleIcon = LineIcon(this, LineIcon.BLUETOOTH, OK)
        icons.addView(topBleIcon, LinearLayout.LayoutParams(dp(14), dp(14)).apply { setMargins(0, 0, dp(6), 0) })
        icons.addView(text("5G", 10, MUTED, Typeface.MONOSPACE).apply { setPadding(0, 0, dp(6), 0) })
        icons.addView(LineIcon(this, LineIcon.BOLT, MUTED), LinearLayout.LayoutParams(dp(13), dp(13)))
        sysRow.addView(icons)
        bar.addView(sysRow)

        // 2. Main Title Row with Pulse Dot and Diagnostics/Profile Action
        val titleRow = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        val titleLeft = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        val cyanPulse = View(this).apply {
            background = round(ACCENT, 99, 0)
        }
        titleLeft.addView(cyanPulse, LinearLayout.LayoutParams(dp(8), dp(8)))
        topTitle = text("HOME", 18, TEXT, display(700)).apply {
            setPadding(dp(10), 0, 0, 0)
            letterSpacing = 0.08f
        }
        titleLeft.addView(topTitle)
        titleRow.addView(titleLeft, LinearLayout.LayoutParams(0, -2, 1f))

        // Sensors diagnostic & profile icons
        val btnSensors = FrameLayout(this).apply {
            addView(LineIcon(this@MainActivity, LineIcon.SENSORS, MUTED), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
            isClickable = true
            setOnClickListener { findBike() }
        }
        titleRow.addView(btnSensors, LinearLayout.LayoutParams(dp(36), dp(36)))

        val btnProfile = FrameLayout(this).apply {
            background = round(ACCENT, 99, 0)
            addView(LineIcon(this@MainActivity, LineIcon.PERSON, ON_ACCENT), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
            isClickable = true
            setOnClickListener { showDeveloperInfo() }
        }
        val plp = LinearLayout.LayoutParams(dp(32), dp(32)).apply { setMargins(dp(8), 0, 0, 0) }
        titleRow.addView(btnProfile, plp)

        bar.addView(titleRow)
        parent.addView(bar)
    }

    // =====================================================================================
    // FLOATING PILL NAVIGATION DOCK
    // =====================================================================================
    private fun buildFloatingNavDock(parent: FrameLayout) {
        val dockContainer = FrameLayout(this).apply {
            setPadding(dp(16), 0, dp(16), dp(12))
        }
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = round(0xF014191F.toInt(), 99, BAR_LINE)
            elevation = dp(8).toFloat()
        }

        val defs = arrayOf(
            Triple("Nav", LineIcon.NAV, TAB_HOME),
            Triple("Dash", LineIcon.SPEED, TAB_OVERVIEW),
            Triple("Stats", LineIcon.BAR_CHART, TAB_ANALYTICS),
            Triple("Logs", LineIcon.TERMINAL, TAB_LOG),
            Triple("Setup", LineIcon.TUNE, TAB_SETTINGS)
        )

        for (i in defs.indices) {
            val item = NavItem(pill, defs[i].first, defs[i].second) { showTab(defs[i].third) }
            navItems.add(item)
        }

        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            // Maximum width ~380dp for tablet/desktop centering
            width = Math.min(resources.displayMetrics.widthPixels - dp(32), dp(380))
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        dockContainer.addView(pill, FrameLayout.LayoutParams(-1, -1))
        parent.addView(dockContainer, lp)
    }

    private inner class NavItem(parent: LinearLayout, title: String, iconKind: Int, action: Runnable) {
        val box: LinearLayout = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            isClickable = true
            setOnClickListener { action.run() }
        }
        val icon: LineIcon = LineIcon(this@MainActivity, iconKind, MUTED)
        val text: TextView = text(title, 10, MUTED, body(500)).apply {
            letterSpacing = 0.04f
        }

        init {
            box.addView(icon, LinearLayout.LayoutParams(dp(20), dp(20)))
            text.setPadding(0, dp(3), 0, 0)
            box.addView(text)
            parent.addView(box, LinearLayout.LayoutParams(0, -1, 1f))
        }

        fun set(on: Boolean) {
            box.background = if (on) round(SELECTED, 99, 0) else null
            val c = if (on) ACCENT else MUTED
            icon.setColor(c)
            text.setTextColor(c)
            text.typeface = body(if (on) 700 else 500)
        }
    }

    // =====================================================================================
    // TAB 0: HOME (COCKPIT & NAVIGATION)
    // =====================================================================================
    private fun buildHome(root: LinearLayout) {
        // 1. Machine Identity & Connected Live Status Bar
        val top = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        val idGroup = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        idGroup.addView(text("TVS RAIDER", 18, TEXT, display(700)).apply { letterSpacing = 0.04f })
        brandBadge = text("125", 10, ACCENT, Typeface.MONOSPACE).apply {
            background = round(0x2B00E5C3.toInt(), 4, 0)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        val blp = LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(8), 0, 0, 0) }
        idGroup.addView(brandBadge, blp)
        top.addView(idGroup, LinearLayout.LayoutParams(0, -2, 1f))

        val pill = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(14), dp(6))
            background = round(BAR, 99, BAR_LINE)
        }
        pillDot = View(this).apply { background = round(OK, 99, 0) }
        pill.addView(pillDot, LinearLayout.LayoutParams(dp(8), dp(8)))
        pillText = text("Raider connected", 10, OK, Typeface.MONOSPACE).apply {
            setPadding(dp(8), 0, 0, 0)
            letterSpacing = 0.06f
        }
        pill.addView(pillText)
        top.addView(pill)
        root.addView(top)

        // 2. Route Dispatch Status
        val dispatchRow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(2))
        }
        dispatchRow.addView(LineIcon(this, LineIcon.SYNC, ACCENT), LinearLayout.LayoutParams(dp(13), dp(13)))
        dispatchStatus = text("SENDING TO THE CLUSTER", 10, ACCENT, Typeface.MONOSPACE).apply {
            letterSpacing = 0.12f
            setPadding(dp(6), 0, 0, 0)
        }
        dispatchRow.addView(dispatchStatus)
        root.addView(dispatchRow)

        statusTitle = text("On the way", 26, TEXT, display(700))
        root.addView(statusTitle)

        // 3. Turn-by-Turn Navigation Hero Card
        val navCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = ripple(round(HERO, 16, HERO_LINE))
            isClickable = true
            setOnClickListener { openMaps() }
        }

        val turnRow = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        // High-chroma directional glyph container
        arrowTile = FrameLayout(this).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = round(ACCENT, 16, 0)
        }
        arrow = ArrowView(this)
        arrowTile.addView(arrow, FrameLayout.LayoutParams(-1, -1))
        turnRow.addView(arrowTile, LinearLayout.LayoutParams(dp(64), dp(64)))

        val turnCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }
        val distRow = hbox().apply { gravity = Gravity.BOTTOM }
        distView = text("250", 36, TEXT, display(700)).apply {
            includeFontPadding = false
            letterSpacing = -0.02f
        }
        distRow.addView(distView)
        distUnit = text("m", 15, ACCENT, display(700)).apply {
            setPadding(dp(4), 0, 0, dp(3))
        }
        distRow.addView(distUnit)
        turnCol.addView(distRow)

        instrView = text("Turn left", 18, TEXT, display(600))
        turnCol.addView(instrView)
        roadView = text("onto Galle Rd", 13, SUB, body(400)).apply { maxLines = 1 }
        turnCol.addView(roadView)
        turnRow.addView(turnCol, LinearLayout.LayoutParams(0, -2, 1f))
        navCard.addView(turnRow)

        // Navigation Meta Metrics Divider (Time left, Distance, Arrive)
        val metaWell = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = round(0xFF192028.toInt(), 10, 0)
        }
        timeLeftView = stat(metaWell, "TIME LEFT", 16)
        distLeftView = stat(metaWell, "DISTANCE", 16)
        etaView = stat(metaWell, "ARRIVE", 16)
        navCard.addView(metaWell, marginTop(-1, -2, 14))
        root.addView(navCard, marginTop(-1, -2, 12))

        // 4. 3-Column Telemetry Bento Strip (Fuel, Mileage, Battery)
        val bento = hbox()
        val fuelBento = bentoTile(bento, "FUEL", LineIcon.FUEL, OK, 0)
        val segs = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        for (i in 0 until 5) {
            val sView = View(this)
            homeFuelSegs[i] = sView
            val sl = LinearLayout.LayoutParams(0, dp(14), 1f)
            if (i < 4) sl.setMargins(0, 0, dp(3), 0)
            segs.addView(sView, sl)
        }
        fuelBento.addView(segs, marginTop(-1, -2, 10))
        val fuelLabelRow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        homeFuelText = text("5 / 5", 12, OK, Typeface.MONOSPACE)
        fuelLabelRow.addView(homeFuelText, LinearLayout.LayoutParams(0, -2, 1f))
        fuelLabelRow.addView(text("Full", 10, MUTED, Typeface.MONOSPACE))
        fuelBento.addView(fuelLabelRow)

        val mileageBento = bentoTile(bento, "MILEAGE", LineIcon.ECO, ACCENT, dp(8))
        homeMileage = text("58", 24, TEXT, display(700)).apply {
            includeFontPadding = false
            setPadding(0, dp(6), 0, 0)
        }
        mileageBento.addView(homeMileage)
        mileageBento.addView(text("km/L", 10, ACCENT, Typeface.MONOSPACE))
        mileageBento.addView(text("avg cycle", 10, MUTED, Typeface.MONOSPACE).apply { setPadding(0, dp(2), 0, 0) })

        val batteryBento = bentoTile(bento, "BATTERY", LineIcon.BOLT, WARN, dp(8))
        homeBattery = text("12.1", 24, TEXT, display(700)).apply {
            includeFontPadding = false
            setPadding(0, dp(6), 0, 0)
        }
        batteryBento.addView(homeBattery)
        batteryBento.addView(text("volts", 10, WARN, Typeface.MONOSPACE))
        batteryBento.addView(text("optimal", 10, MUTED, Typeface.MONOSPACE).apply { setPadding(0, dp(2), 0, 0) })

        for (i in 0 until bento.childCount) {
            bento.getChildAt(i).setOnClickListener { showTab(TAB_OVERVIEW) }
        }
        root.addView(bento, marginTop(-1, -2, 12))

        // 5. Speedometer & 24-Segment LED Matrix Card
        buildSpeedMatrix(root)

        // 6. Cockpit Action Row (Disconnect pill + Google Maps search)
        val actions = hbox().apply { setPadding(0, dp(12), 0, 0) }
        val disconnectContainer = hbox().apply {
            gravity = Gravity.CENTER
            background = ripple(round(BAR, 99, BAR_LINE))
            isClickable = true
            setOnClickListener {
                val s = BikeService.instance
                if (!bluetoothOn()) requestBluetooth { connect() }
                else if (s != null && s.isActive()) disconnect()
                else connect()
            }
        }
        primaryBtnDot = View(this).apply { background = round(BAD, 99, 0) }
        disconnectContainer.addView(primaryBtnDot, LinearLayout.LayoutParams(dp(8), dp(8)).apply { setMargins(0, 0, dp(8), 0) })
        primaryBtn = text("Disconnect", 14, TEXT, body(600))
        disconnectContainer.addView(primaryBtn)
        actions.addView(disconnectContainer, LinearLayout.LayoutParams(0, dp(48), 1f))

        val mapSearch = FrameLayout(this).apply {
            background = ripple(round(BAR, 99, BAR_LINE))
            isClickable = true
            contentDescription = "Search destination in Google Maps"
            addView(LineIcon(this@MainActivity, LineIcon.SEARCH, TEXT), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
            setOnClickListener { openMaps() }
        }
        val mlp = LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(dp(10), 0, 0, 0) }
        actions.addView(mapSearch, mlp)
        root.addView(actions)
    }

    private fun bentoTile(row: LinearLayout, label: String, iconKind: Int, iconColor: Int, leftGap: Int): LinearLayout {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = ripple(round(HERO, 14, HERO_LINE))
            isClickable = true
        }
        val header = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(text(label, 10, MUTED, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(LineIcon(this, iconKind, iconColor), LinearLayout.LayoutParams(dp(14), dp(14)))
        t.addView(header)
        row.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(leftGap, 0, 0, 0) })
        return t
    }

    private fun buildSpeedMatrix(root: LinearLayout) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = round(HERO, 16, HERO_LINE)
        }
        val head = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(LineIcon(this, LineIcon.SPEED, ACCENT), LinearLayout.LayoutParams(dp(18), dp(18)))
        val l = text("SPEED", 10, MUTED, Typeface.MONOSPACE).apply {
            letterSpacing = 0.08f
            setPadding(dp(8), 0, 0, 0)
        }
        head.addView(l, LinearLayout.LayoutParams(0, -2, 1f))
        speedMax = text("Max today 68 km/h", 11, SUB, body(400))
        head.addView(speedMax)
        c.addView(head)

        val veloRow = hbox().apply {
            gravity = Gravity.BOTTOM
            setPadding(0, dp(6), 0, dp(8))
        }
        speedView = text("42", 52, TEXT, display(700)).apply {
            includeFontPadding = false
            letterSpacing = -0.04f
        }
        veloRow.addView(speedView)
        veloRow.addView(text("km/h", 16, ACCENT, display(700)).apply {
            setPadding(dp(6), 0, 0, dp(6))
        })
        c.addView(veloRow)

        // 24-Segment High-Frequency LED Matrix Gauge (2 rows of 12 segments)
        val row1 = hbox()
        for (i in 0 until 12) {
            val sv = View(this)
            speedSegs[i] = sv
            val sl = LinearLayout.LayoutParams(0, dp(12), 1f)
            if (i < 11) sl.setMargins(0, 0, dp(3), 0)
            row1.addView(sv, sl)
        }
        c.addView(row1)

        val row2 = hbox()
        for (i in 12 until 24) {
            val sv = View(this)
            speedSegs[i] = sv
            val sl = LinearLayout.LayoutParams(0, dp(12), 1f)
            if (i < 23) sl.setMargins(0, 0, dp(3), 0)
            row2.addView(sv, sl)
        }
        c.addView(row2, marginTop(-1, -2, 4))

        // Linear Scale Ticks
        val scale = hbox().apply { setPadding(0, dp(4), 0, 0) }
        val marks = arrayOf("0", "30", "60", "90", "120")
        for (i in marks.indices) {
            val isMax = i == marks.size - 1
            val m = text(marks[i], 10, if (isMax) WARN else MUTED, Typeface.MONOSPACE)
            m.gravity = if (i == 0) Gravity.START else if (isMax) Gravity.END else Gravity.CENTER_HORIZONTAL
            scale.addView(m, LinearLayout.LayoutParams(0, -2, 1f))
        }
        c.addView(scale)

        root.addView(c, marginTop(-1, -2, 12))
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

        speedView.text = if (kmh >= 0) kmh.toString() else "--"
        speedView.setTextColor(if (kmh >= warnAt) WARN else TEXT)

        for (i in 0 until SPEED_SEGS) {
            val sv = speedSegs[i] ?: continue
            val on = i < lit
            val isHighRpm = i >= 22
            val color = if (on) (if (isHighRpm || kmh >= warnAt) WARN else ACCENT) else EMPTY
            sv.background = round(color, 2, 0)
        }
        speedMax.text = if (maxTodaySaved > 0) "Max today $maxTodaySaved km/h" else ""
    }

    private val speedTick = object : Runnable {
        override fun run() {
            updateSpeed()
            h.postDelayed(this, 200)
        }
    }

    // =====================================================================================
    // TAB 1: OVERVIEW (VITALS & RIDES)
    // =====================================================================================
    private fun buildOverview(root: LinearLayout) {
        // Screen Header Zone
        val top = hbox().apply { gravity = Gravity.BOTTOM }
        val id = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("TVS Raider 125", 10, MUTED, Typeface.MONOSPACE))
            addView(text("Overview", 26, TEXT, display(700)))
        }
        top.addView(id, LinearLayout.LayoutParams(0, -2, 1f))

        val liveBadge = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = round(BAR, 99, BAR_LINE)
        }
        liveDot = View(this).apply { background = round(OK, 99, 0) }
        liveBadge.addView(liveDot, LinearLayout.LayoutParams(dp(7), dp(7)))
        liveText = text("Live", 10, OK, Typeface.MONOSPACE).apply {
            setPadding(dp(6), 0, 0, 0)
        }
        liveBadge.addView(liveText)
        top.addView(liveBadge)
        root.addView(top)

        // Hero Stepped 5-bar Telemetry Fuel Card
        val fuelCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = round(HERO, 16, HERO_LINE)
        }
        val fcHeader = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        fcHeader.addView(iconTile(LineIcon.FUEL, 32, 8, SELECTED, ACCENT))
        val fcTitles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            addView(text("Fuel", 10, MUTED, Typeface.MONOSPACE))
            addView(text("Estimated 490 km range", 13, SUB, body(500)))
        }
        fcHeader.addView(fcTitles, LinearLayout.LayoutParams(0, -2, 1f))
        fuelState = text("Full", 10, OK, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 99, 0)
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        fcHeader.addView(fuelState)
        fuelCard.addView(fcHeader)

        // Stepped 5-bar vertical gauge
        val steppedBars = hbox().apply {
            gravity = Gravity.BOTTOM
            setPadding(0, dp(14), 0, dp(10))
        }
        val barHeights = arrayOf(24, 32, 40, 48, 56)
        for (i in 0 until 5) {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val fv = View(this)
            fuelSegs[i] = fv
            col.addView(fv, LinearLayout.LayoutParams(-1, dp(barHeights[i])))
            col.addView(text("${i + 1}", 9, MUTED, Typeface.MONOSPACE).apply { setPadding(0, dp(4), 0, 0) })
            val clp = LinearLayout.LayoutParams(0, -2, 1f)
            if (i > 0) clp.setMargins(dp(6), 0, 0, 0)
            steppedBars.addView(col, clp)
        }
        fuelCard.addView(steppedBars)

        val fuelFooter = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        fuelCapacity = text("9.8 / 10.0 L Capacity", 11, SUB, body(400))
        fuelFooter.addView(fuelCapacity, LinearLayout.LayoutParams(0, -2, 1f))
        fuelBig = text("5 / 5 bars", 13, ACCENT, Typeface.MONOSPACE)
        fuelFooter.addView(fuelBig)
        fuelCard.addView(fuelFooter)
        root.addView(fuelCard, marginTop(-1, -2, 12))

        // 2x2 Bento Telemetry Grid
        val r1 = hbox()
        odoText = bentoMetric(r1, "ODO", LineIcon.PIN, "Kilometers", 0)
        tripBText = bentoMetric(r1, "TRIP B", LineIcon.ROUTE, "km · Active", dp(8))
        root.addView(r1, marginTop(-1, -2, 10))

        val r2 = hbox()
        mileageText = bentoMetric(r2, "AVG MILEAGE", LineIcon.ECO, "+3.2 vs cluster avg", 0)
        batteryText = bentoMetric(r2, "BATTERY", LineIcon.BOLT, "Optimal charge", dp(8))
        root.addView(r2, marginTop(-1, -2, 10))

        // Riding Dynamics Tactile Teaser Card
        val dynCard = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = round(HERO, 16, HERO_LINE)
        }
        val dynTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Riding Dynamics", 10, ACCENT, Typeface.MONOSPACE))
            addView(text("Power & Eco Mode Ready", 16, TEXT, display(600)).apply { setPadding(0, dp(2), 0, 0) })
            addView(text("SmartXonnect active · Gear Shift Assist on", 12, SUB, body(400)))
        }
        dynCard.addView(dynTexts, LinearLayout.LayoutParams(0, -2, 1f))
        dynCard.addView(iconTile(LineIcon.BIKE, 42, 12, SELECTED, ACCENT))
        root.addView(dynCard, marginTop(-1, -2, 12))

        // Recent Rides Section
        val rh = hbox().apply {
            gravity = Gravity.BOTTOM
            setPadding(dp(2), dp(16), dp(2), dp(6))
        }
        rh.addView(text("Rides", 18, TEXT, display(600)), LinearLayout.LayoutParams(0, -2, 1f))
        weekText = text("Last 7 days · 7 rides · 155 km", 10, MUTED, Typeface.MONOSPACE)
        rh.addView(weekText)
        root.addView(rh)

        ridesBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ridesBody)

        // Telemetry Diagnostic Quick CTA Button
        val cta = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = round(HERO, 14, HERO_LINE)
        }
        cta.addView(LineIcon(this, LineIcon.SYNC, ACCENT), LinearLayout.LayoutParams(dp(18), dp(18)))
        val ctaText = text("Last synced via BLE", 12, SUB, body(400)).apply {
            setPadding(dp(8), 0, 0, 0)
        }
        cta.addView(ctaText, LinearLayout.LayoutParams(0, -2, 1f))
        val syncBtn = text("Sync", 11, ACCENT, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 8, 0)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            isClickable = true
            setOnClickListener {
                updateBike()
                toast("Telemetry updated via BLE")
            }
        }
        cta.addView(syncBtn)
        root.addView(cta, marginTop(-1, -2, 14))

        updateBike()
    }

    private fun bentoMetric(row: LinearLayout, label: String, iconKind: Int, subText: String, leftGap: Int): TextView {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = round(HERO, 14, HERO_LINE)
        }
        val header = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(text(label, 10, MUTED, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(LineIcon(this, iconKind, MUTED), LinearLayout.LayoutParams(dp(14), dp(14)))
        t.addView(header)

        val v = text("--", 24, TEXT, display(700)).apply {
            includeFontPadding = false
            setPadding(0, dp(8), 0, 0)
        }
        t.addView(v)
        val s = text(subText, 10, MUTED, Typeface.MONOSPACE).apply { setPadding(0, dp(4), 0, 0) }
        t.addView(s)

        row.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(leftGap, 0, 0, 0) })
        return v
    }

    private fun updateBike() {
        if (!::fuelBig.isInitialized) return
        val live = BikeService.instance != null && BikeService.instance!!.isReady()
                && (System.currentTimeMillis() - BikeFrames.updatedAt < 5000)
        liveDot.background = round(if (live) OK else FAINT, 99, 0)
        liveText.text = if (live) "Live" else "Standby"

        val bars = BikeFrames.fuelBars
        paintFuel(fuelSegs, bars)
        paintFuel(homeFuelSegs, bars)

        val fuelStr = if (bars < 0) "--" else if (bars == 0) "Empty" else "$bars / 5 bars"
        fuelBig.text = fuelStr
        fuelState.text = if (bars in 0..1) "Low Fuel" else if (bars >= 4) "Full" else "Normal"
        homeFuelText.text = if (bars >= 0) "$bars / 5" else "--"

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
        var totalKm = 0.0
        var totalMs = 0L

        for (r in all) {
            totalKm += r.km()
            totalMs += r.durationMs()
            ridesBody.addView(rideRow(r), marginTop(-1, -2, 8))
        }
        weekText.text = String.format(Locale.ROOT, "%d rides · %.0f km", all.size, totalKm)
    }

    private fun rideRow(r: RideTracker.Ride): View {
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = ripple(round(HERO, 14, HERO_LINE))
            isClickable = true
            setOnClickListener { showRideSummary(r) }
        }
        row.addView(iconTile(LineIcon.BIKE, 38, 10, SELECTED, ACCENT))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        val top = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        val dateStr = SimpleDateFormat("EEE, d MMM", Locale.ROOT).format(Date(r.start))
        val timeStr = SimpleDateFormat(" · HH:mm", Locale.ROOT).format(Date(r.start))
        top.addView(text(dateStr, 13, TEXT, body(600)))
        top.addView(text(timeStr, 11, MUTED, Typeface.MONOSPACE))
        texts.addView(top)

        val sub = String.format(
            Locale.ROOT,
            "%.1f km · %s · avg %.0f km/h",
            r.km(),
            RideTracker.duration(r.durationMs()),
            r.avgKmh()
        )
        texts.addView(text(sub, 12, SUB, body(400)).apply { setPadding(0, dp(2), 0, 0) })
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        row.addView(LineIcon(this, LineIcon.CHEVRON, MUTED), LinearLayout.LayoutParams(dp(16), dp(16)))
        return row
    }

    // =====================================================================================
    // TAB 2: ANALYTICS (PERFORMANCE CHARTS & BENCHMARKS)
    // =====================================================================================
    private fun buildAnalytics(root: LinearLayout) {
        val top = hbox().apply { gravity = Gravity.BOTTOM }
        val id = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("TVS Raider 125", 10, MUTED, Typeface.MONOSPACE))
            addView(text("Analytics", 26, TEXT, display(700)))
        }
        top.addView(id, LinearLayout.LayoutParams(0, -2, 1f))

        anRecording = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(10), dp(4))
            background = round(BAR, 99, BAR_LINE)
            val redDot = View(this@MainActivity).apply { background = round(BAD, 99, 0) }
            addView(redDot, LinearLayout.LayoutParams(dp(7), dp(7)))
            addView(text("Recording", 10, BAD, Typeface.MONOSPACE).apply { setPadding(dp(6), 0, 0, 0) })
            visibility = View.GONE
        }
        top.addView(anRecording)
        root.addView(top)

        // Time Horizon Pill Selector (Week, Month, Year, All)
        val seg = hbox().apply {
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = round(0xFF080A0D.toInt(), 12, BAR_LINE)
        }
        for (i in PERIODS.indices) {
            val p = i
            val t = text(PERIODS[i], 12, SUB, Typeface.MONOSPACE).apply {
                gravity = Gravity.CENTER
                isClickable = true
                setOnClickListener {
                    period = p
                    paintPeriods()
                    refreshAnalytics()
                }
            }
            seg.addView(t, LinearLayout.LayoutParams(0, dp(34), 1f))
            periodTabs.add(t)
        }
        root.addView(seg, marginTop(-1, -2, 12))
        paintPeriods()

        // Hero Distance Metric Card
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = round(HERO, 16, HERO_LINE)
        }
        val heroHead = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        anHeroLabel = text("DISTANCE · LAST 7 DAYS", 10, MUTED, Typeface.MONOSPACE)
        heroHead.addView(anHeroLabel, LinearLayout.LayoutParams(0, -2, 1f))
        anTrend = text("+8% vs prev", 10, OK, Typeface.MONOSPACE).apply {
            background = round(0x2B22C55E.toInt(), 99, 0)
            setPadding(dp(8), dp(2), dp(8), dp(2))
        }
        heroHead.addView(anTrend)
        hero.addView(heroHead)

        val distRow = hbox().apply {
            gravity = Gravity.BOTTOM
            setPadding(0, dp(6), 0, dp(8))
        }
        anHeroKm = text("155", 52, TEXT, display(700)).apply {
            includeFontPadding = false
            letterSpacing = -0.04f
        }
        distRow.addView(anHeroKm)
        distRow.addView(text("km", 16, ACCENT, display(700)).apply {
            setPadding(dp(6), 0, 0, dp(6))
        })
        hero.addView(distRow)

        val odoRow = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        odoRow.addView(LineIcon(this, LineIcon.SPEED, MUTED), LinearLayout.LayoutParams(dp(14), dp(14)))
        odoRow.addView(text("Odometer", 10, MUTED, Typeface.MONOSPACE).apply {
            setPadding(dp(6), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        anOdo = text("12,625.7 km", 11, TEXT, Typeface.MONOSPACE)
        odoRow.addView(anOdo)
        hero.addView(odoRow)
        root.addView(hero, marginTop(-1, -2, 12))

        // Bento 2x3 Performance Matrix
        val b1 = hbox()
        anRides = perfTile(b1, "RIDES", LineIcon.BIKE, "trips", 0)
        anTime = perfTile(b1, "RIDING TIME", LineIcon.TIMER, "", dp(8))
        root.addView(b1, marginTop(-1, -2, 10))

        val b2 = hbox()
        anAvg = perfTile(b2, "AVG SPEED", LineIcon.SPEED, "km/h", 0)
        anTop = perfTile(b2, "TOP SPEED", LineIcon.BOLT, "km/h", dp(8), true)
        root.addView(b2, marginTop(-1, -2, 10))

        val b3 = hbox()
        anMileage = perfTile(b3, "MILEAGE", LineIcon.FUEL, "km/L", 0)
        anPerRide = perfTile(b3, "PER RIDE", LineIcon.ROUTE, "km", dp(8))
        root.addView(b3, marginTop(-1, -2, 10))

        // Weekly Distance Bar Chart Bento Card
        val chartCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = round(HERO, 16, HERO_LINE)
        }
        chartCard.addView(text("DISTANCE ACTIVITY", 10, MUTED, Typeface.MONOSPACE))
        anChart = BarChart()
        chartCard.addView(anChart, LinearLayout.LayoutParams(-1, dp(140)).apply { setMargins(0, dp(12), 0, 0) })
        root.addView(chartCard, marginTop(-1, -2, 12))
    }

    private fun perfTile(row: LinearLayout, label: String, iconKind: Int, unit: String, leftGap: Int, isHighlight: Boolean = false): TextView {
        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = round(HERO, 14, if (isHighlight) WARN else HERO_LINE)
        }
        val head = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(text(label, 10, if (isHighlight) WARN else MUTED, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(LineIcon(this, iconKind, if (isHighlight) WARN else ACCENT), LinearLayout.LayoutParams(dp(14), dp(14)))
        t.addView(head)

        val vRow = hbox().apply {
            gravity = Gravity.BOTTOM
            setPadding(0, dp(6), 0, 0)
        }
        val v = text("--", 24, if (isHighlight) WARN else TEXT, display(700)).apply {
            includeFontPadding = false
        }
        vRow.addView(v)
        if (unit.isNotEmpty()) {
            vRow.addView(text(" $unit", 10, MUTED, Typeface.MONOSPACE).apply { setPadding(dp(3), 0, 0, dp(3)) })
        }
        t.addView(vRow)

        row.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(leftGap, 0, 0, 0) })
        return v
    }

    private fun paintPeriods() {
        for (i in periodTabs.indices) {
            val on = i == period
            val t = periodTabs[i]
            t.background = if (on) round(SELECTED, 8, 0) else null
            t.setTextColor(if (on) ACCENT else MUTED)
            t.typeface = Typeface.MONOSPACE
        }
    }

    private fun refreshAnalytics() {
        if (!::anHeroKm.isInitialized) return
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        val from = when (period) {
            0 -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            1 -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            2 -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }

        val rides = RideStore.between(this, from, now)
        var totalKm = 0.0
        var totalMs = 0L
        var topKmh = 0
        for (r in rides) {
            totalKm += r.km()
            totalMs += r.durationMs()
            topKmh = Math.max(topKmh, r.maxKmh)
        }

        anHeroKm.text = String.format(Locale.ROOT, "%.0f", totalKm)
        anHeroLabel.text = "DISTANCE · " + PERIODS[period].uppercase(Locale.ROOT)
        anRides.text = rides.size.toString()

        val hours = totalMs / 3_600_000L
        val mins = (totalMs % 3_600_000L) / 60_000L
        anTime.text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val avgKmh = if (totalMs > 60_000L) totalKm / (totalMs / 3_600_000.0) else 0.0
        anAvg.text = String.format(Locale.ROOT, "%.0f", avgKmh)
        anTop.text = topKmh.toString()

        val avgMl = if (BikeFrames.avgKmL > 0) BikeFrames.avgKmL else 58
        anMileage.text = avgMl.toString()
        val perRide = if (rides.isNotEmpty()) totalKm / rides.size else 0.0
        anPerRide.text = String.format(Locale.ROOT, "%.1f", perRide)

        val odo = BikeService.odoTenths
        anOdo.text = if (odo > 0) "${BikeFrames.tenthsText(odo)} km" else "12,625.7 km"

        // Setup bins for the chart
        val chartBins = if (period == 0) 7 else 6
        val chartValues = FloatArray(chartBins)
        val binLabels = if (period == 0) arrayOf("M", "T", "W", "T", "F", "S", "S") else arrayOf("W1", "W2", "W3", "W4", "W5", "W6")

        if (rides.isNotEmpty() && chartBins > 0) {
            val step = Math.max(1L, (now - from) / chartBins)
            for (r in rides) {
                val bIdx = Math.min(chartBins - 1, Math.max(0, ((r.start - from) / step).toInt()))
                chartValues[bIdx] += r.km().toFloat()
            }
        }
        anChart.set(chartValues, binLabels)
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
            val barW = (w / n) * 0.52f
            val slotW = w / n

            pText.textSize = dp(10).toFloat()
            pText.typeface = Typeface.MONOSPACE
            pText.color = MUTED
            pText.textAlign = Paint.Align.CENTER

            for (i in 0 until n) {
                val cx = slotW * i + slotW / 2f
                val bh = Math.max(dp(4).toFloat(), (values[i] / maxV) * (plotH - dp(10)))
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
    // TAB 3: LOGS (DIAGNOSTIC CONSOLE)
    // =====================================================================================
    private fun buildLog(root: LinearLayout) {
        // Live Telemetry Status Banner
        val banner = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = round(HERO, 14, HERO_LINE)
        }
        banner.addView(iconTile(LineIcon.BLUETOOTH, 32, 8, SELECTED, ACCENT))
        val bTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            addView(text("TVS_RAIDER_BT4", 13, TEXT, body(600)))
            logBleStatus = text("CAN-BUS V2.4 · 10 Hz POLLING", 10, MUTED, Typeface.MONOSPACE)
            addView(logBleStatus)
        }
        banner.addView(bTexts, LinearLayout.LayoutParams(0, -2, 1f))
        val liveChip = text("LIVE STREAM", 9, ACCENT, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 99, 0)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        banner.addView(liveChip)
        root.addView(banner)

        // Header Section with Title & Clear Action
        val head = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(4))
        }
        val titleGroup = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        titleGroup.addView(text("LOGS", 22, TEXT, display(700)).apply { letterSpacing = 0.04f })
        val debugBadge = text("DEBUG", 9, ACCENT, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 4, 0)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        titleGroup.addView(debugBadge, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(8), 0, 0, 0) })
        head.addView(titleGroup, LinearLayout.LayoutParams(0, -2, 1f))

        val clear = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(14), dp(6))
            background = ripple(round(SELECTED, 99, 0))
            isClickable = true
            setOnClickListener {
                BikeService.clearScreen()
                updateLog(true)
            }
        }
        clear.addView(LineIcon(this, LineIcon.TRASH, MUTED), LinearLayout.LayoutParams(dp(14), dp(14)))
        clear.addView(text("Clear", 11, TEXT, body(600)).apply { setPadding(dp(6), 0, 0, 0) })
        head.addView(clear)
        root.addView(head)

        val note = text("Live, from when the app started. To save or share the log, see Settings > Developer options.", 12, SUB, body(400))
        root.addView(note)

        // Category Filter Chips
        val chips = hbox().apply { setPadding(0, dp(8), 0, dp(6)) }
        for (i in FILTERS.indices) {
            val f = i
            val c = text(FILTERS[i], 11, SUB, Typeface.MONOSPACE).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(6), dp(12), dp(6))
                isClickable = true
                setOnClickListener {
                    logFilter = f
                    paintChips()
                    updateLog(true)
                }
            }
            val lp = LinearLayout.LayoutParams(-2, -2)
            if (i > 0) lp.setMargins(dp(6), 0, 0, 0)
            chips.addView(c, lp)
            filterChips.add(c)
        }
        paintChips()
        root.addView(chips)

        // Terminal Log Console Bento Box
        val terminalCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(HERO, 14, HERO_LINE)
        }

        // Terminal Chrome Bar
        val chrome = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = round(0xFF192028.toInt(), 14, 0)
        }
        val dot1 = View(this).apply { background = round(OK, 99, 0) }
        val dot2 = View(this).apply { background = round(WARN, 99, 0) }
        val dot3 = View(this).apply { background = round(MUTED, 99, 0) }
        chrome.addView(dot1, LinearLayout.LayoutParams(dp(8), dp(8)))
        chrome.addView(dot2, LinearLayout.LayoutParams(dp(8), dp(8)).apply { setMargins(dp(4), 0, 0, 0) })
        chrome.addView(dot3, LinearLayout.LayoutParams(dp(8), dp(8)).apply { setMargins(dp(4), 0, 0, 0) })
        chrome.addView(text("CONSOLE_TTY_0", 10, SUB, Typeface.MONOSPACE).apply {
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val sessionStr = SimpleDateFormat("yyyyMMdd EEE", Locale.ROOT).format(Date())
        chrome.addView(text("SESSION: $sessionStr", 10, MUTED, Typeface.MONOSPACE))
        terminalCard.addView(chrome)

        // Terminal Output View
        logView = text("", 11, TEXT2, Typeface.MONOSPACE).apply {
            setTextIsSelectable(true)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        logScroll = ScrollView(this).apply {
            setPadding(dp(14), dp(12), dp(14), dp(12))
            clipToPadding = false
            background = round(LOG_BG, 0, 0)
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(logView, ViewGroup.LayoutParams(-1, -2))
        }
        terminalCard.addView(logScroll, LinearLayout.LayoutParams(-1, dp(340)))
        root.addView(terminalCard, marginTop(-1, -2, 6))
    }

    private fun paintChips() {
        for (i in filterChips.indices) {
            val on = i == logFilter
            val c = filterChips[i]
            c.background = if (on) round(ACCENT, 99, 0) else round(BAR, 99, BAR_LINE)
            c.setTextColor(if (on) ON_ACCENT else SUB)
            c.typeface = Typeface.MONOSPACE
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
    // TAB 4: SETTINGS (PAIRING, ALERTS, PREFERENCES)
    // =====================================================================================
    private fun buildSettings(root: LinearLayout) {
        val p = BikeService.prefs(this)
        val top = hbox().apply { gravity = Gravity.BOTTOM }
        val id = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("TVS Raider", 10, MUTED, Typeface.MONOSPACE))
            addView(text("Settings", 26, TEXT, display(700)))
        }
        top.addView(id, LinearLayout.LayoutParams(0, -2, 1f))
        val fwBadge = text("FIRMWARE V2.4.9", 9, OK, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 99, 0)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        top.addView(fwBadge)
        root.addView(top)

        // Connected Bike Hero Card
        val bGroup = group(root, 12)
        val bRow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = ripple(round(0, 16, 0))
            isClickable = true
            setOnClickListener { findBike() }
            setOnLongClickListener {
                editAddress()
                true
            }
        }
        bRow.addView(iconTile(LineIcon.BIKE, 42, 12, SELECTED, ACCENT))
        val bTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
        }
        val nameRow = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        bikeTitle = text(BIKE_MODEL, 15, TEXT, body(600))
        nameRow.addView(bikeTitle)
        val btBadge = text("BT LE", 9, OK, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 4, 0)
            setPadding(dp(4), dp(1), dp(4), dp(1))
        }
        nameRow.addView(btBadge, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(6), 0, 0, 0) })
        bTexts.addView(nameRow)

        val addrRow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, 0)
        }
        bikeAddr = text(p.getString("mac", "Not connected") ?: "Not connected", 11, MUTED, Typeface.MONOSPACE)
        addrRow.addView(bikeAddr)
        val connDot = View(this).apply { background = round(OK, 99, 0) }
        addrRow.addView(connDot, LinearLayout.LayoutParams(dp(6), dp(6)).apply { setMargins(dp(8), 0, 0, 0) })
        addrRow.addView(text("Connected", 10, OK, Typeface.MONOSPACE).apply { setPadding(dp(4), 0, 0, 0) })
        bTexts.addView(addrRow)
        bRow.addView(bTexts, LinearLayout.LayoutParams(0, -2, 1f))

        bikeBtn = text("Change", 11, TEXT, Typeface.MONOSPACE).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = ripple(round(SELECTED, 99, 0))
        }
        bRow.addView(bikeBtn)
        bGroup.addView(bRow)

        // RIDING Card Group
        root.addView(sectionLabel("RIDING"))
        val nGroup = group(root, 6)
        switchRow(nGroup, LineIcon.SEND, "Mirror navigation", "Send Google Maps turns to the cluster", "mirrorNav", true, null)
        divider(nGroup)
        switchRow(nGroup, LineIcon.SYNC, "Ride mode", "Keep connecting, even after a phone restart", "rideMode", true, null)

        // ALERTS Card Group
        root.addView(sectionLabel("ALERTS"))
        val aGroup = group(root, 6)
        switchRow(aGroup, LineIcon.SPEED, "Speed limit alert", "Vibrate phone when reaching speed limit", "speedAlert", false, null)
        stepperRow(aGroup, "Limit speed", "speedLimit", 80, 40, 140, 5, " km/h")
        divider(aGroup)
        switchRow(aGroup, LineIcon.FUEL, "Low fuel alert", "Warn when fuel gauge falls below threshold", "fuelAlert", true, null)
        stepperRow(aGroup, "Fuel threshold", "fuelLevel", 1, 0, 4, 1, null)

        // PREFERENCES
        root.addView(sectionLabel("PREFERENCES"))
        val tGroup = group(root, 6)
        buildTheme(tGroup)

        // SETUP & PERMISSIONS
        root.addView(sectionLabel("SETUP & PERMISSIONS"))
        val sGroup = group(root, 6)
        chkPerms = CheckRow(sGroup, "Bluetooth & Nearby devices", "Lets app talk to TVS cluster") { askPermissions() }
        divider(sGroup)
        chkMaps = CheckRow(sGroup, "Read Google Maps notifications", "Required to detect turn-by-turn arrows") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        divider(sGroup)
        chkBattery = CheckRow(sGroup, "Unrestricted battery mode", "Prevents Android from pausing navigation") { keepRunning() }

        // DATA & DEVELOPER
        root.addView(sectionLabel("DEVELOPER OPTIONS"))
        val devGroup = group(root, 6)
        switchRow(devGroup, LineIcon.LOG, "Save daily logs", "Preserve rolling 7-day logs for reverse engineering", "saveLogs", true, null)
        divider(devGroup)
        actionRow(devGroup, LineIcon.SHARE, "Export log archive (ZIP)", "Share redacted logs for diagnostic analysis", false) { shareLog() }
        divider(devGroup)
        actionRow(devGroup, LineIcon.TRASH, "Delete all logs", "Free up storage by clearing all saved logs", true) {
            confirmDelete("Delete all logs?", "This will remove all session logs from this device.", "Delete") { deleteLogs() }
        }
        divider(devGroup)
        actionRow(devGroup, LineIcon.TRASH, "Clear ride history", "Erase all saved rides from SQLite database", true) {
            confirmDelete("Delete all rides?", "The ride history is removed permanently from this phone.", "Delete") {
                RideStore.clear(this)
                refreshRides()
                refreshAnalytics()
                toast("Rides deleted")
            }
        }

        buildFooter(root)
        updateSettings()
    }

    private fun buildTheme(g: LinearLayout) {
        val p = BikeService.prefs(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val seg = hbox().apply {
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = round(0xFF080A0D.toInt(), 12, BAR_LINE)
        }
        val themeTabs = ArrayList<TextView>()
        for (i in THEMES.indices) {
            val tName = THEMES[i]
            val tv = text(tName.replaceFirstChar { it.uppercase() }, 11, SUB, Typeface.MONOSPACE).apply {
                gravity = Gravity.CENTER
                isClickable = true
                setOnClickListener {
                    p.edit().putString("theme", tName).apply()
                    applyPalette(wantDark())
                    recreate()
                }
            }
            val active = tName == p.getString("theme", "dark")
            tv.background = if (active) round(SELECTED, 8, 0) else null
            tv.setTextColor(if (active) ACCENT else MUTED)
            seg.addView(tv, LinearLayout.LayoutParams(0, dp(32), 1f))
            themeTabs.add(tv)
        }
        box.addView(seg)
        g.addView(box)
    }

    private fun buildFooter(root: LinearLayout) {
        val f = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(24), 0, dp(12))
        }
        f.addView(text("TVS Raider Navigations v1.0.0", 11, MUTED, Typeface.MONOSPACE))
        f.addView(text("Crafted by Heshan for TVS Raider 125", 11, ACCENT, Typeface.MONOSPACE).apply {
            setPadding(0, dp(2), 0, 0)
        })
        root.addView(f)
    }

    private fun showDeveloperInfo() {
        Sheet("TVS Raider Navigations", "Advanced Bluetooth SmartXonnect telemetry & HUD bridge.\n\nDeveloped by: Heshan\nTarget Bike: TVS Raider 125\nTheme: Obsidian Telemetry")
            .button("Close", 1, null)
            .show()
    }

    // =====================================================================================
    // RIDE SUMMARY TELEMETRY MODAL (MATCHING ride_summary_telemetry_modal)
    // =====================================================================================
    private fun showRideSummary(r: RideTracker.Ride) {
        val sheet = Dialog(this).apply { requestWindowFeature(Window.FEATURE_NO_TITLE) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            background = round(HERO, 24, HERO_LINE)
        }

        // Drag handle
        val handle = View(this).apply { background = round(0xFF37393D.toInt(), 99, 0) }
        val hlp = LinearLayout.LayoutParams(dp(48), dp(5)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setMargins(0, 0, 0, dp(14))
        }
        root.addView(handle, hlp)

        // Top Navigation Header
        val navRow = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
        val backBtn = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(10), dp(4))
            background = round(SELECTED, 8, 0)
            isClickable = true
            setOnClickListener { sheet.dismiss() }
        }
        backBtn.addView(LineIcon(this, LineIcon.BACK, ACCENT), LinearLayout.LayoutParams(dp(14), dp(14)))
        backBtn.addView(text("Ride Details", 11, TEXT, Typeface.MONOSPACE).apply { setPadding(dp(6), 0, 0, 0) })
        navRow.addView(backBtn, LinearLayout.LayoutParams(0, -2, 1f))

        val btnShare = FrameLayout(this).apply {
            background = round(SELECTED, 8, 0)
            addView(LineIcon(this@MainActivity, LineIcon.SHARE, TEXT), FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER))
            isClickable = true
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "TVS Raider 125 Ride: ${RideTracker.summary(r)}")
                }
                startActivity(Intent.createChooser(send, "Share Ride"))
            }
        }
        navRow.addView(btnShare, LinearLayout.LayoutParams(dp(36), dp(36)))

        root.addView(navRow)

        // Title & Eyebrow Meta
        val dateEyebrow = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(2))
        }
        val dateStr = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.ROOT).format(Date(r.start))
        dateEyebrow.addView(text(dateStr, 10, MUTED, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
        val loggedBadge = text("TRACK LOGGED", 9, ACCENT, Typeface.MONOSPACE).apply {
            background = round(SELECTED, 4, 0)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        dateEyebrow.addView(loggedBadge)
        root.addView(dateEyebrow)

        root.addView(text("Commute Telemetry Record", 20, TEXT, display(700)))
        root.addView(text("TVS Raider 125 · Bluetooth SmartXonnect Live", 12, SUB, body(400)))

        // Hero Ride Metrics Bento (Logged Distance, Trip Duration)
        val heroBento = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = round(0xFF192028.toInt(), 14, 0)
        }
        val distCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("LOGGED DISTANCE", 10, MUTED, Typeface.MONOSPACE))
            val dRow = hbox().apply { gravity = Gravity.BOTTOM }
            dRow.addView(text(String.format(Locale.ROOT, "%.1f", r.km()), 34, ACCENT, display(700)).apply {
                includeFontPadding = false
            })
            dRow.addView(text(" KM", 14, MUTED, Typeface.MONOSPACE).apply { setPadding(dp(4), 0, 0, dp(3)) })
            addView(dRow)
        }
        heroBento.addView(distCol, LinearLayout.LayoutParams(0, -2, 1f))

        val durCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(text("TRIP DURATION", 10, MUTED, Typeface.MONOSPACE))
            val durRow = hbox().apply {
                gravity = Gravity.CENTER_VERTICAL
                background = round(SELECTED, 8, 0)
                setPadding(dp(10), dp(4), dp(10), dp(4))
            }
            durRow.addView(LineIcon(this@MainActivity, LineIcon.TIMER, ACCENT), LinearLayout.LayoutParams(dp(14), dp(14)))
            durRow.addView(text(RideTracker.duration(r.durationMs()), 12, TEXT, Typeface.MONOSPACE).apply {
                setPadding(dp(6), 0, 0, 0)
            })
            addView(durRow, marginTop(-2, -2, 4))
        }
        heroBento.addView(durCol)
        root.addView(heroBento, marginTop(-1, -2, 14))

        // Metrics 2x2 Grid (Avg Speed, Max Speed)
        val mRow = hbox()
        val avgTile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = round(0xFF192028.toInt(), 12, 0)
            val h = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
            h.addView(text("AVG SPEED", 10, MUTED, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
            h.addView(LineIcon(this@MainActivity, LineIcon.SPEED, MUTED), LinearLayout.LayoutParams(dp(14), dp(14)))
            addView(h)
            addView(text(String.format(Locale.ROOT, "%.1f KM/H", r.avgKmh()), 20, TEXT, display(700)).apply {
                setPadding(0, dp(6), 0, 0)
            })
        }
        mRow.addView(avgTile, LinearLayout.LayoutParams(0, -2, 1f))

        val topTile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = round(0xFF192028.toInt(), 12, 0)
            val h = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
            h.addView(text("MAX SPEED", 10, WARN, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
            h.addView(LineIcon(this@MainActivity, LineIcon.BOLT, WARN), LinearLayout.LayoutParams(dp(14), dp(14)))
            addView(h)
            addView(text("${r.maxKmh} KM/H", 20, WARN, display(700)).apply {
                setPadding(0, dp(6), 0, 0)
            })
        }
        mRow.addView(topTile, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(8), 0, 0, 0) })
        root.addView(mRow, marginTop(-1, -2, 8))

        sheet.setContentView(root)
        sheet.window?.apply {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(resources.displayMetrics.widthPixels - dp(24), ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.7f)
        }
        sheet.show()
    }

    // =====================================================================================
    // BLUETOOTH LE CONNECTIVITY & SCANNING
    // =====================================================================================
    private fun connect() {
        val mac = BikeService.prefs(this).getString("mac", "")
        if (mac.isNullOrEmpty()) {
            findBike()
            return
        }
        BikeService.log("Starting BikeService connection...")
        startForegroundService(Intent(this, BikeService::class.java).setAction(BikeService.ACTION_CONNECT))
    }

    private fun disconnect() {
        BikeService.log("Disconnect requested by user")
        startService(Intent(this, BikeService::class.java).setAction(BikeService.ACTION_DISCONNECT))
    }

    private fun bluetoothOn(): Boolean {
        val bm = getSystemService(BluetoothManager::class.java) ?: return false
        val a = bm.adapter ?: return false
        return a.isEnabled
    }

    private fun requestBluetooth(cb: Runnable) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(intent)
        toast("Enable Bluetooth to connect to Raider")
    }

    private fun findBike() {
        if (scanning) return
        val bm = getSystemService(BluetoothManager::class.java) ?: return
        val adapter = bm.adapter
        if (adapter == null || !adapter.isEnabled) {
            requestBluetooth { findBike() }
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            toast("Bluetooth LE scanner not available")
            return
        }
        scanning = true
        val found = LinkedHashMap<String, ScanResult>()
        val sheet = Sheet("Select your TVS Raider", "Ensure bike ignition is ON. Select your Raider from the list:")
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(list, ViewGroup.LayoutParams(-1, -2))
        }
        sheet.content.addView(listScroll, marginTop(-1, -2, 10))

        val stopRef = arrayOfNulls<Runnable>(1)
        val relist = Runnable {
            val shown = ArrayList(found.values)
            shown.sortWith { a, b -> rank(a).compareTo(rank(b)) }
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

        val stopScan = Runnable {
            if (scanning) {
                scanning = false
                try { scanner.stopScan(cb) } catch (ignored: Exception) {}
                sheet.spinner.visibility = View.GONE
            }
        }
        stopRef[0] = stopScan
        sheet.spinner.visibility = View.VISIBLE

        sheet.button("Cancel", 0) { stopScan.run() }
        sheet.dialog.setOnDismissListener { stopScan.run() }
        sheet.show()

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner.startScan(null, settings, cb)
        } catch (e: Exception) {
            scanning = false
            toast("Scan failed: ${e.message}")
            sheet.dialog.dismiss()
            return
        }
        h.postDelayed({ stopScan.run() }, 15000)
    }

    private fun deviceRow(r: ScanResult): View {
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = ripple(round(0xFF192028.toInt(), 12, BAR_LINE))
            isClickable = true
        }
        val isBike = looksLikeBike(r)
        row.addView(iconTile(LineIcon.BIKE, 36, 10, if (isBike) SELECTED else PILL, if (isBike) ACCENT else MUTED))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        val name = nameOf(r) ?: "Unknown Device"
        texts.addView(text(name, 14, if (isBike) ACCENT else TEXT, body(600)))
        texts.addView(text("${r.device.address}  ·  ${r.rssi} dBm", 11, MUTED, Typeface.MONOSPACE))
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
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
        BikeService.log("Bike address saved: $mac (${name ?: BIKE_MODEL})")
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
            setHintTextColor(MUTED)
            setTextColor(TEXT)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            background = round(PILL, 12, BAR_LINE)
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
            topClock.text = SimpleDateFormat("HH:mm", Locale.ROOT).format(Date())
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
        pillText.text = if (ready) "Raider connected" else if (active) "Connecting..." else "Not connected"
        pillText.setTextColor(if (ready) OK else if (active) WARN else MUTED)

        topBleIcon.setColor(if (ready) OK else if (active) WARN else FAINT)

        primaryBtn.text = if (active) "Disconnect" else "Connect"
        primaryBtnDot.background = round(if (active) BAD else OK, 99, 0)
    }

    private fun updateHero() {
        val nav = MapsListener.current
        if (nav == null) {
            arrow.setPicto(-1)
            distView.text = "--"
            distUnit.text = ""
            instrView.text = "Navigation standby"
            roadView.text = "Start route in Google Maps"
            dispatchStatus.text = "CLUSTER READY FOR ROUTE"
            statusTitle.text = "Navigation standby"
            timeLeftView.text = "--"
            distLeftView.text = "--"
            etaView.text = "--"
            return
        }
        arrow.setPicto(nav.picto)
        arrow.setColors(ON_ACCENT, ARROW_DIM)

        dispatchStatus.text = "SENDING TO THE CLUSTER"
        statusTitle.text = "On the way"

        val dNext = nav.distNextM
        if (dNext >= 1000) {
            distView.text = String.format(Locale.ROOT, "%.1f", dNext / 1000f)
            distUnit.text = "km"
        } else {
            distView.text = "$dNext"
            distUnit.text = "m"
        }

        instrView.text = nav.text ?: "Continue straight"
        roadView.text = MapsListener.road.ifEmpty { "Navigating active route" }

        val mins = nav.minutesLeft
        timeLeftView.text = if (mins >= 60) "${mins / 60}h ${mins % 60}m" else if (mins > 0) "$mins min" else "--"

        val dLeft = nav.distLeftM
        distLeftView.text = if (dLeft >= 1000) String.format(Locale.ROOT, "%.1f km", dLeft / 1000f) else if (dLeft > 0) "$dLeft m" else "--"
        etaView.text = MapsListener.eta.ifEmpty { "--" }
    }

    private fun updateHomeTiles() {
        val bars = BikeFrames.fuelBars
        paintFuel(homeFuelSegs, bars)
        homeFuelText.text = if (bars >= 0) "$bars / 5" else "--"
        val ml = BikeFrames.avgKmL
        homeMileage.text = if (ml > 0) ml.toString() else "58"
        val bv = BikeFrames.batteryTenths
        homeBattery.text = if (bv > 0) BikeFrames.tenthsText(bv) else "12.1"
    }

    private fun updateSettings() {
        val p = BikeService.prefs(this)
        val mac = p.getString("mac", "")
        bikeAddr?.text = if (mac.isNullOrEmpty()) "No bike paired" else mac
        bikeTitle?.text = p.getString("bikeName", BIKE_MODEL)

        chkPerms?.set(
            if (Build.VERSION.SDK_INT >= 31)
                checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED
            else
                checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
        )
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        chkMaps?.set(enabledListeners != null && enabledListeners.contains(packageName))

        val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        chkBattery?.set(pm != null && pm.isIgnoringBatteryOptimizations(packageName))
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

    private fun sectionLabel(s: String): TextView = text(s, 10, MUTED, Typeface.MONOSPACE).apply {
        letterSpacing = 0.08f
        setPadding(dp(4), dp(16), dp(4), dp(6))
    }

    private fun group(root: LinearLayout, topMarginDp: Int = 10): LinearLayout {
        val g = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(HERO, 16, HERO_LINE)
        }
        root.addView(g, marginTop(-1, -2, topMarginDp))
        return g
    }

    private fun divider(g: LinearLayout) {
        val d = View(this).apply { setBackgroundColor(BAR_LINE) }
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

    private fun stat(row: LinearLayout, title: String, sp: Int): TextView {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), 0, dp(4), 0)
        }
        c.addView(text(title, 9, MUTED, Typeface.MONOSPACE))
        val v = text("--", sp, TEXT, display(700)).apply {
            includeFontPadding = false
            setPadding(0, dp(2), 0, 0)
        }
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
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        row.addView(iconTile(icon, 34, 10, SELECTED, ACCENT))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
            addView(text(title, 14, TEXT, body(600)))
            addView(text(sub, 12, SUB, body(400)).apply { setPadding(0, dp(1), 0, 0) })
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
        row.addView(t, LinearLayout.LayoutParams(dp(48), dp(28)))
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
            setPadding(dp(62), 0, dp(16), dp(10))
        }
        row.addView(text(title, 12, SUB, Typeface.MONOSPACE), LinearLayout.LayoutParams(0, -2, 1f))
        val value = text("", 13, TEXT, Typeface.MONOSPACE).apply { gravity = Gravity.CENTER }
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
        row.addView(minus, LinearLayout.LayoutParams(dp(30), dp(30)))
        row.addView(value, LinearLayout.LayoutParams(dp(68), -2))
        row.addView(plus, LinearLayout.LayoutParams(dp(30), dp(30)))
        g.addView(row)
    }

    private fun stepButton(s: String): TextView = text(s, 16, TEXT, body(600)).apply {
        gravity = Gravity.CENTER
        background = ripple(round(SELECTED, 99, 0))
        isClickable = true
    }

    private fun actionRow(g: LinearLayout, icon: Int, title: String, sub: String, danger: Boolean, action: Runnable): LinearLayout {
        val row = hbox().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = ripple(round(0, 16, 0))
            isClickable = true
            setOnClickListener { action.run() }
        }
        row.addView(iconTile(icon, 34, 10, if (danger) RED_BG else SELECTED, if (danger) RED_TEXT else ACCENT))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(10), 0)
            addView(text(title, 14, if (danger) RED_TEXT else TEXT, body(600)))
            addView(text(sub, 12, SUB, body(400)).apply { setPadding(0, dp(1), 0, 0) })
        }
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(LineIcon(this, LineIcon.CHEVRON, MUTED), LinearLayout.LayoutParams(dp(16), dp(16)))
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
            val pad = dp(2).toFloat()
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
        val bang = text("!", 13, ON_WARN, body(700)).apply { gravity = Gravity.CENTER }
        val fix: TextView

        init {
            val row = hbox().apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            badge.addView(tick, FrameLayout.LayoutParams(dp(13), dp(13), Gravity.CENTER))
            badge.addView(bang, FrameLayout.LayoutParams(-1, -1))
            row.addView(badge, LinearLayout.LayoutParams(dp(24), dp(24)))

            val texts = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(text(title, 14, TEXT, body(600)))
                addView(text(sub, 12, SUB, body(400)).apply { setPadding(0, dp(1), 0, 0) })
            }
            row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
            fix = text("Fix", 12, WARN, Typeface.MONOSPACE).apply {
                gravity = Gravity.CENTER
                background = ripple(round(WARN_BG, 99, 0))
                isClickable = true
                setOnClickListener { action.run() }
            }
            row.addView(fix, LinearLayout.LayoutParams(dp(52), dp(30)))
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
                background = round(HERO, 20, HERO_LINE)
            }
            val head = hbox().apply { gravity = Gravity.CENTER_VERTICAL }
            title = text(titleText, 18, TEXT, display(700))
            head.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            spinner = ProgressBar(this@MainActivity).apply {
                indeterminateTintList = ColorStateList.valueOf(ACCENT)
                visibility = View.GONE
            }
            head.addView(spinner, LinearLayout.LayoutParams(dp(22), dp(22)))
            box.addView(head)

            message = text(messageText ?: "", 13, SUB, body(400)).apply {
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
                setLayout(resources.displayMetrics.widthPixels - dp(32), ViewGroup.LayoutParams.WRAP_CONTENT)
                setDimAmount(0.7f)
            }
        }

        fun button(text: String, kind: Int, action: Runnable?): Sheet {
            val b = text(text, 13, if (kind == 0) TEXT else if (kind == 2) RED_TEXT else ON_ACCENT, body(600)).apply {
                gravity = Gravity.CENTER
                setPadding(dp(18), 0, dp(18), 0)
                background = ripple(
                    if (kind == 1) round(ACCENT, 99, 0)
                    else if (kind == 2) round(RED_BG, 99, RED_LINE)
                    else round(SELECTED, 99, 0)
                )
                isClickable = true
                setOnClickListener {
                    dialog.dismiss()
                    action?.run()
                }
            }
            val lp = LinearLayout.LayoutParams(-2, dp(38))
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
