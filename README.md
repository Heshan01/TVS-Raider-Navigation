# TVS Raider Navigations

**TVS Raider Navigations** is a 100% Kotlin Android application engineered specifically for the **TVS Raider (SmartXonnect)** motorcycle. It mirrors Google Maps turn-by-turn navigation directly to the bike's LCD cluster and reads real-time telemetry (speed, fuel gauge, odometer, trip meters, battery voltage, mileage) over Bluetooth Low Energy.

- **App Label on Phone:** `TVS Raider`
- **Project Name:** `TVS Raider Navigations`
- **Developer / Author:** **Heshan**
- **Language:** **100% Pure Kotlin**
- **Architecture:** Android Modern Architecture, Kotlin Coroutines & Handlers, SQLite OpenHelper, Bluetooth Low Energy GATT Client, Android Notification Listener Service.

---

## 🚀 Key Features (100% Match with Full Hardware Protocol)

1. **Google Maps Turn-by-Turn Mirroring**
   - Automatically detects active Google Maps navigation notifications.
   - Extracts real turn arrows from the notification icon using topological BFS shape analysis (turns, roundabouts, U-turns, highway exits, arrival flag).
   - Transmits standard TVS 20-byte navigation frames (`0x5A 0x4E` control frame and `0x5B 0x4F` text frame).
   - Repeats frames every **700 ms** so the bike cluster keeps the navigation panel open continuously without dropping.

2. **Full Bluetooth LE Connectivity**
   - Direct connection to TVS SmartXonnect service (`5456534d-5647-5341-5342-454e544f5251`).
   - Negotiates 137-byte MTU for high-throughput packet delivery.
   - Phone Status heartbeat (`0x5B 0x4A`) sent every **2 seconds** keeping cluster clock accurate and unlocking telemetry from the motorcycle.
   - Background standby auto-reconnect (`autoConnect=true`) when bike ignition is switched on.

3. **Live Bike Telemetry & Cluster Synchronization**
   - **Speedometer:** Live speed in km/h received about 3 times a second (`0x10` frame).
   - **Odometer & Trip B:** High precision odometer in 0.1 km steps.
   - **Fuel Gauge:** 5-segment live fuel bar monitor with threshold alerts.
   - **Battery Voltage & Mileage:** Real-time electrical voltage and average fuel consumption.

4. **Safety Alerts**
   - **Speed Limit Alert:** Vibrates the phone (using `USAGE_ALARM` so it vibrates even when phone is on silent) when user-defined speed limit is exceeded.
   - **Low Fuel Alert:** High-priority Android notification when fuel falls to warning threshold.

5. **Automatic Ride Recording & Analytics**
   - Detects rides starting at $\ge 3$ km/h.
   - Auto-ends rides after 2 minutes of silence or 15 minutes idle.
   - Stores all rides in persistent SQLite database (`rides.db`).
   - Analytics tab displays total distance, duration, top speed, average speed, fuel economy, and dynamic bar charts for Week, Month, Year, and All Time.

6. **Privacy First & Diagnostics**
   - **Zero Internet Permissions:** No network access requested; completely offline.
   - Diagnostic logging with automatic 7-day file retention.
   - Zip export tool with privacy name redaction (`[name]`) for safe debugging.

7. **Sleek Sports Dashboard UI**
   - High-contrast neon cyan (`#00E5FF`) and amber cyberpunk styling.
   - Glowing real-time connection status pills.
   - Interactive live speedometer gauge with rev-meter segments.
   - Dark, Light, System, and Auto (6 AM – 6 PM) themes.
   - Prominently showcases **"Developed by Heshan"**.

---

## 📱 Permissions Declared

| Permission | Purpose |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | To read Google Maps navigation notifications. |
| `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` | To discover and connect to the TVS Raider over Bluetooth LE (Android 12+). |
| `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Bluetooth connectivity on Android 9–11. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Maintains Bluetooth link alive while riding with screen off. |
| `POST_NOTIFICATIONS` | Foreground service status and low-fuel alerts. |
| `VIBRATE` | Speed warning vibration. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents Android aggressive task killers from killing navigation. |
| `RECEIVE_BOOT_COMPLETED` | Automatically reconnects when phone restarts (Ride mode). |

---

## 🛠️ Project Structure

```
Raider/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/fonts/
│       │   ├── Inter.ttf
│       │   └── SpaceGrotesk.ttf
│       ├── res/
│       │   ├── drawable/
│       │   ├── mipmap-anydpi-v26/
│       │   └── values/
│       │       ├── strings.xml   (app_name = "TVS Raider", developer_name = "Heshan")
│       │       ├── colors.xml
│       │       └── styles.xml
│       └── kotlin/com/heshan/tvsraider/
│           ├── TvsProtocol.kt       # TVS SmartXonnect 20-byte BLE frame encoder
│           ├── BikeFrames.kt        # Raider telemetry frame decoder (0x10, 0x11, 0x18, 0x19)
│           ├── MapsParser.kt        # Google Maps notification parser
│           ├── IconReader.kt        # 32x32 icon mask reader
│           ├── IconTable.kt         # Known arrow masks & roundabout patterns
│           ├── ArrowShape.kt        # BFS arrow shape topology analyzer
│           ├── ArrowView.kt         # Vector arrow drawer for all turn pictograms
│           ├── LineIcon.kt          # 2px vector line icons (24x24 grid)
│           ├── RideTracker.kt       # Automated ride session tracking engine
│           ├── RideDb.kt            # SQLite database helper for rides.db
│           ├── RideStore.kt         # Ride repository & active session cache
│           ├── BikeService.kt       # Foreground BLE communication & keep-alive service
│           ├── MapsListener.kt      # Google Maps notification listener
│           ├── BootReceiver.kt      # Phone boot & update auto-start receiver
│           ├── LogRedactor.kt       # Sensitive street/place name redactor for exports
│           ├── LogFileProvider.kt   # Secure ContentProvider for log zip exports
│           └── MainActivity.kt      # Premium 5-tab sports dashboard UI
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

---

## 🔨 Building the Project

### Option 1: Android Studio
1. Open Android Studio.
2. Choose **Open** and select the `Raider` folder.
3. Let Gradle sync dependencies.
4. Select your connected Android phone or emulator and click **Run (Shift + F10)**.

### Option 2: Command Line (Gradle)
```bash
cd Raider
./gradlew assembleDebug
```
The APK will be generated at:
`Raider/app/build/outputs/apk/debug/app-debug.apk`

---

## 🏍️ Connecting to TVS Raider

1. Turn your bike's ignition **ON**.
2. Ensure the official TVS Connect app is **closed**.
3. Open **TVS Raider** on your phone.
4. Tap **Find bike** (Search icon).
5. Select your bike (marked with a star `★ TVS RAIDER...`).
6. Start navigation in **Google Maps** — turns will immediately appear on your bike's cluster!

---

**Built with ❤️ by Heshan**
"# TVS-Raider-Navigation" 
