# Keep complication data sources (referenced by name in manifest)
-keep class com.focusdial.app.complication.** { *; }
-keep class com.focusdial.app.FocusService { *; }
-keep class com.focusdial.app.DndReceiver { *; }
-keep class com.focusdial.app.BootReceiver { *; }
-keep class com.focusdial.app.TimerAlarmReceiver { *; }
-keep class com.focusdial.app.ToggleSessionActivity { *; }
-keep class com.focusdial.app.NotificationCounterService { *; }
-keep class com.focusdial.app.FocusApplication { *; }
-keep class com.focusdial.app.LauncherActivity { *; }
-keep class com.focusdial.app.OnboardingActivity { *; }

# Room database entities and DAOs
-keep class com.focusdial.app.data.db.** { *; }
-keepclassmembers class com.focusdial.app.data.db.** { *; }

# Google Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
-keepclassmembers class com.android.billingclient.** { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
