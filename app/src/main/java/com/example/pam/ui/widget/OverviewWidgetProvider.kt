package com.example.pam.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.Toast
import com.example.pam.MainActivity
import com.example.pam.R
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.repository.PAMRepository
import com.example.pam.ui.theme.ThemeManager
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverviewWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_PERIOD_PREFIX + id)
        }
        editor.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SET_PERIOD -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val period = intent.getStringExtra(EXTRA_PERIOD) ?: PERIOD_MONTH
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_PERIOD_PREFIX + appWidgetId, period)
                        .apply()
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_REFRESH_WIDGET -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                } else {
                    updateAllWidgets(context)
                }
                Toast.makeText(context, "Overview updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.pam.ACTION_REFRESH_WIDGET"
        const val ACTION_SET_PERIOD = "com.example.pam.ACTION_SET_PERIOD"
        const val EXTRA_PERIOD = "extra_period"
        const val PERIOD_MONTH = "THIS_MONTH"
        const val PERIOD_WEEK = "THIS_WEEK"

        private const val PREFS_NAME = "crebit_widget_prefs"
        private const val KEY_PERIOD_PREFIX = "period_"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, OverviewWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (id in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, id)
                }
                val intent = Intent(context, OverviewWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            } else {
                updateAppWidget(context, appWidgetManager, AppWidgetManager.INVALID_APPWIDGET_ID)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val periodStr = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    prefs.getString(KEY_PERIOD_PREFIX + appWidgetId, PERIOD_MONTH)
                } else {
                    PERIOD_MONTH
                }
                val dateRangeType = if (periodStr == PERIOD_WEEK) {
                    DateRangeType.THIS_WEEK
                } else {
                    DateRangeType.THIS_MONTH
                }
                val isMonth = dateRangeType == DateRangeType.THIS_MONTH

                val (startDate, endDate) = DateUtils.getDateRange(dateRangeType)
                val repository = PAMRepository.getInstance(context)
                val overview = repository.getFinancialOverview(startDate, endDate)

                // Theme determination (Light, Dark, or System/Device default)
                val isDark = ThemeManager.isDark(context)
                val layoutId = if (isDark) R.layout.widget_overview else R.layout.widget_overview_light

                val rootBg = if (isDark) R.drawable.widget_background else R.drawable.widget_background_light
                val toggleContainerBg = if (isDark) R.drawable.widget_toggle_container else R.drawable.widget_toggle_container_light
                val incomeBoxBg = if (isDark) R.drawable.widget_income_box_bg else R.drawable.widget_income_box_bg_light
                val expenseBoxBg = if (isDark) R.drawable.widget_expense_box_bg else R.drawable.widget_expense_box_bg_light
                val refreshBtnBg = if (isDark) R.drawable.widget_btn_circle else R.drawable.widget_btn_circle_light

                val activeChipBg = if (isDark) R.drawable.widget_chip_active else R.drawable.widget_chip_active_light
                val inactiveChipBg = R.drawable.widget_chip_inactive

                val primaryTextColor = if (isDark) Color.parseColor("#FFFFFFFF") else Color.parseColor("#FF18181B")
                val secondaryTextColor = if (isDark) Color.parseColor("#FFA1A1AA") else Color.parseColor("#FF71717A")
                val incomeColor = if (isDark) Color.parseColor("#FF10B981") else Color.parseColor("#FF059669")
                val activeChipTextColor = if (isDark) Color.parseColor("#FF18181B") else Color.parseColor("#FFFFFFFF")
                val inactiveChipTextColor = secondaryTextColor

                val views = RemoteViews(context.packageName, layoutId)

                // Set container backgrounds and icon tints
                views.setInt(R.id.widget_root, "setBackgroundResource", rootBg)
                views.setInt(R.id.widget_toggle_container, "setBackgroundResource", toggleContainerBg)
                views.setInt(R.id.widget_income_box, "setBackgroundResource", incomeBoxBg)
                views.setInt(R.id.widget_expense_box, "setBackgroundResource", expenseBoxBg)
                views.setInt(R.id.widget_btn_refresh, "setBackgroundResource", refreshBtnBg)
                views.setInt(R.id.widget_btn_refresh, "setColorFilter", secondaryTextColor)
                views.setInt(R.id.widget_arrow_up, "setColorFilter", secondaryTextColor)

                // Header Title with DM Sans
                views.setImageViewBitmap(
                    R.id.widget_title,
                    WidgetBitmapHelper.createTextBitmap(context, "Overview", 14f, primaryTextColor, isBold = true)
                )

                // Month / Week Segmented Switcher with DM Sans
                val monthBg = if (isMonth) activeChipBg else inactiveChipBg
                val monthTextColor = if (isMonth) activeChipTextColor else inactiveChipTextColor
                val weekBg = if (!isMonth) activeChipBg else inactiveChipBg
                val weekTextColor = if (!isMonth) activeChipTextColor else inactiveChipTextColor

                views.setInt(R.id.widget_btn_month, "setBackgroundResource", monthBg)
                views.setImageViewBitmap(
                    R.id.widget_btn_month_img,
                    WidgetBitmapHelper.createTextBitmap(context, "Month", 11f, monthTextColor, isBold = isMonth)
                )

                views.setInt(R.id.widget_btn_week, "setBackgroundResource", weekBg)
                views.setImageViewBitmap(
                    R.id.widget_btn_week_img,
                    WidgetBitmapHelper.createTextBitmap(context, "Week", 11f, weekTextColor, isBold = !isMonth)
                )

                // Month Chip Click Intent
                val monthIntent = Intent(context, OverviewWidgetProvider::class.java).apply {
                    action = ACTION_SET_PERIOD
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_PERIOD, PERIOD_MONTH)
                }
                val monthPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 1,
                    monthIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_month, monthPendingIntent)

                // Week Chip Click Intent
                val weekIntent = Intent(context, OverviewWidgetProvider::class.java).apply {
                    action = ACTION_SET_PERIOD
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_PERIOD, PERIOD_WEEK)
                }
                val weekPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 2,
                    weekIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_week, weekPendingIntent)

                // Net Amount Label & Value with DM Sans
                views.setImageViewBitmap(
                    R.id.widget_net_label,
                    WidgetBitmapHelper.createTextBitmap(context, "Net Amount", 12f, secondaryTextColor)
                )
                views.setImageViewBitmap(
                    R.id.widget_net_amount,
                    WidgetBitmapHelper.createTextBitmap(
                        context,
                        CurrencyFormatter.format(overview.netSavings),
                        22f,
                        primaryTextColor,
                        isBold = true
                    )
                )

                // Net Badge (Positive / Deficit) with DM Sans
                val isPositive = overview.netSavings >= 0
                val badgeBg = if (isPositive) {
                    if (isDark) R.drawable.widget_badge_positive else R.drawable.widget_badge_positive_light
                } else {
                    if (isDark) R.drawable.widget_badge_deficit else R.drawable.widget_badge_deficit_light
                }
                val badgeTextColor = if (isPositive) {
                    if (isDark) Color.parseColor("#FF10B981") else Color.parseColor("#FF059669")
                } else {
                    if (isDark) Color.parseColor("#FFEF4444") else Color.parseColor("#FFDC2626")
                }
                val badgeText = if (isPositive) "Positive" else "Deficit"

                views.setInt(R.id.widget_net_badge, "setBackgroundResource", badgeBg)
                views.setImageViewBitmap(
                    R.id.widget_net_badge_img,
                    WidgetBitmapHelper.createTextBitmap(context, badgeText, 11f, badgeTextColor, isBold = true)
                )

                // Income Label & Value with DM Sans
                views.setImageViewBitmap(
                    R.id.widget_income_label,
                    WidgetBitmapHelper.createTextBitmap(context, "Income", 11f, incomeColor, isBold = true)
                )
                views.setImageViewBitmap(
                    R.id.widget_total_income,
                    WidgetBitmapHelper.createTextBitmap(
                        context,
                        CurrencyFormatter.format(overview.totalIncome),
                        15f,
                        incomeColor,
                        isBold = true
                    )
                )

                // Expense Label & Value with DM Sans
                views.setImageViewBitmap(
                    R.id.widget_expense_label,
                    WidgetBitmapHelper.createTextBitmap(context, "Expense", 11f, secondaryTextColor, isBold = true)
                )
                views.setImageViewBitmap(
                    R.id.widget_total_expense,
                    WidgetBitmapHelper.createTextBitmap(
                        context,
                        CurrencyFormatter.format(overview.totalExpense),
                        15f,
                        primaryTextColor,
                        isBold = true
                    )
                )

                // Root Click -> Open Crebit Main Activity
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val launchPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent)

                // Refresh Button Click -> Refresh Widget Broadcast
                val refreshIntent = Intent(context, OverviewWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 3,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } else {
                    val componentName = ComponentName(context, OverviewWidgetProvider::class.java)
                    appWidgetManager.updateAppWidget(componentName, views)
                }
            }
        }
    }
}
