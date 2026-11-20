# ==============================================================================
# TVS RAIDER - R8 / PROGUARD OBFUSCATION AND CODE PROTECTION RULES
# ==============================================================================

# Flatten package hierarchy and obfuscate class names into single unreadable letters
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively

# Strip source file names and line numbers to prevent reverse engineering
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable

# Keep essential AndroidManifest entry points so Android OS can start them
-keep public class com.heshan.tvsraider.MainActivity extends android.app.Activity
-keep public class com.heshan.tvsraider.BikeService extends android.app.Service
-keep public class com.heshan.tvsraider.MapsListener extends android.service.notification.NotificationListenerService
-keep public class com.heshan.tvsraider.BootReceiver extends android.content.BroadcastReceiver
-keep public class com.heshan.tvsraider.LogFileProvider extends androidx.core.content.FileProvider

# All internal protocol, parser, BLE framing, DB, and UI logic will be 100% obfuscated!
