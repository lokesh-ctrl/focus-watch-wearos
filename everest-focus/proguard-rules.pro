# Keep complication data sources (referenced by name in manifest)
-keep class com.everest.focus.complication.** { *; }
-keep class com.everest.focus.FocusService { *; }
-keep class com.everest.focus.DndReceiver { *; }
-keep class com.everest.focus.BootReceiver { *; }
-keep class com.everest.focus.TimerAlarmReceiver { *; }
-keep class com.everest.focus.ToggleSessionActivity { *; }
-keep class com.everest.focus.NotificationCounterService { *; }
-keep class com.everest.focus.FocusApplication { *; }

# Room database entities and DAOs
-keep class com.everest.focus.data.db.** { *; }
