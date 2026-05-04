package com.focusdial.app

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.focusdial.app.data.FocusPreferences
import com.focusdial.app.data.FocusState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future

class FocusTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        scope.future {
            val manager = FocusSessionManager.getInstance(this@FocusTileService)
            val prefs = FocusPreferences(this@FocusTileService)
            val state = manager.state.value

            val statusText = when (state) {
                is FocusState.Focus -> {
                    val elapsedMin = (manager.elapsedFocusMillis / 60_000).toInt()
                    "FOCUS ${elapsedMin}min"
                }
                is FocusState.Break -> {
                    val remainingMin = (manager.remainingBreakMillis / 60_000).toInt()
                    "BREAK ${remainingMin}min"
                }
                is FocusState.Idle -> "Tap to focus"
            }

            val dailyMillis = prefs.getDailyTotalMillis()
            val dailyMin = (dailyMillis / 60_000).toInt()
            val dailyText = when {
                dailyMin >= 60 -> "${dailyMin / 60}h ${dailyMin % 60}m today"
                dailyMin > 0 -> "${dailyMin}m today"
                else -> "No focus yet"
            }

            val sessions = prefs.getCompletedSessions()
            val goal = prefs.getDailyGoal()

            val accentColor = when (state) {
                is FocusState.Focus -> 0xFF4CAF50.toInt()
                is FocusState.Break -> 0xFFFF9800.toInt()
                is FocusState.Idle -> 0xFF888888.toInt()
            }

            val layout = buildLayout(statusText, dailyText, "$sessions/$goal", accentColor)

            val timeline = TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(layout)
                        .build()
                )
                .build()

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(60_000L)
                .setTileTimeline(timeline)
                .build()
        }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
        )

    private fun buildLayout(
        status: String,
        daily: String,
        sessions: String,
        accentColor: Int
    ): LayoutElementBuilders.Layout {
        val tapAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(ToggleSessionActivity::class.java.name)
                    .build()
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(tapAction)
            .setId("toggle_focus")
            .build()

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(status)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(18f))
                            .setColor(argb(accentColor))
                            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                            .build()
                    )
                    .build()
            )
            .addContent(spacer(8f))
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(daily)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(14f))
                            .setColor(argb(0xFFCCCCCC.toInt()))
                            .build()
                    )
                    .build()
            )
            .addContent(spacer(4f))
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("Sessions: $sessions")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(sp(12f))
                            .setColor(argb(0xFF888888.toInt()))
                            .build()
                    )
                    .build()
            )
            .build()

        val box = LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(column)
            .build()

        return LayoutElementBuilders.Layout.Builder()
            .setRoot(box)
            .build()
    }

    private fun spacer(height: Float): LayoutElementBuilders.Spacer {
        return LayoutElementBuilders.Spacer.Builder()
            .setHeight(dp(height))
            .build()
    }

    companion object {
        fun requestUpdate(context: android.content.Context) {
            TileService.getUpdater(context)
                .requestUpdate(FocusTileService::class.java)
        }
    }
}
