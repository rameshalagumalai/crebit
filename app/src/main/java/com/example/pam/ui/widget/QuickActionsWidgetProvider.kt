package com.example.pam.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.pam.MainActivity
import com.example.pam.R
import com.example.pam.ui.theme.ThemeManager

class QuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, QuickActionsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
                val intent = Intent(context, QuickActionsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
            updateAppWidget(context, appWidgetManager, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val isDark = ThemeManager.isDark(context)
            val layoutId = if (isDark) R.layout.widget_quick_actions else R.layout.widget_quick_actions_light
            val views = RemoteViews(context.packageName, layoutId)

            // Resolve theme resources
            val rootBg = if (isDark) R.drawable.widget_background else R.drawable.widget_background_light
            val cardBg = if (isDark) R.drawable.widget_action_card_bg_dark else R.drawable.widget_action_card_bg_light

            val scanBoxBg = if (isDark) R.drawable.widget_icon_box_scan_dark else R.drawable.widget_icon_box_scan_light
            val spendBoxBg = if (isDark) R.drawable.widget_icon_box_spend_dark else R.drawable.widget_icon_box_spend_light
            val receiveBoxBg = if (isDark) R.drawable.widget_icon_box_receive_dark else R.drawable.widget_icon_box_receive_light
            val moveBoxBg = if (isDark) R.drawable.widget_icon_box_move_dark else R.drawable.widget_icon_box_move_light

            val scanIconTint = if (isDark) Color.parseColor("#FFE4E4E7") else Color.parseColor("#FF27272A")
            val spendIconTint = if (isDark) Color.parseColor("#EF4444") else Color.parseColor("#DC2626")
            val receiveIconTint = if (isDark) Color.parseColor("#10B981") else Color.parseColor("#059669")
            val moveIconTint = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569")

            // Set Backgrounds
            views.setInt(R.id.widget_qa_root, "setBackgroundResource", rootBg)
            views.setInt(R.id.widget_btn_scan, "setBackgroundResource", cardBg)
            views.setInt(R.id.widget_btn_spend, "setBackgroundResource", cardBg)
            views.setInt(R.id.widget_btn_receive, "setBackgroundResource", cardBg)
            views.setInt(R.id.widget_btn_move, "setBackgroundResource", cardBg)

            views.setInt(R.id.widget_box_scan, "setBackgroundResource", scanBoxBg)
            views.setInt(R.id.widget_box_spend, "setBackgroundResource", spendBoxBg)
            views.setInt(R.id.widget_box_receive, "setBackgroundResource", receiveBoxBg)
            views.setInt(R.id.widget_box_move, "setBackgroundResource", moveBoxBg)

            // Set Icon Tints
            views.setInt(R.id.widget_icon_scan, "setColorFilter", scanIconTint)
            views.setInt(R.id.widget_icon_spend, "setColorFilter", spendIconTint)
            views.setInt(R.id.widget_icon_receive, "setColorFilter", receiveIconTint)
            views.setInt(R.id.widget_icon_move, "setColorFilter", moveIconTint)

            // Setup Click Intents
            fun createActionPendingIntent(action: String, requestCodeOffset: Int): PendingIntent {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_QUICK_ACTION, action)
                }
                return PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + requestCodeOffset,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            views.setOnClickPendingIntent(
                R.id.widget_btn_scan,
                createActionPendingIntent(MainActivity.ACTION_SCAN, 1)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_spend,
                createActionPendingIntent(MainActivity.ACTION_SPEND, 2)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_receive,
                createActionPendingIntent(MainActivity.ACTION_RECEIVE, 3)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_move,
                createActionPendingIntent(MainActivity.ACTION_MOVE, 4)
            )

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } else {
                val componentName = ComponentName(context, QuickActionsWidgetProvider::class.java)
                appWidgetManager.updateAppWidget(componentName, views)
            }
        }
    }
}
