package com.tradepilot.app.navigation

import android.webkit.WebView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import com.tradepilot.app.webview.AppRootViewModel
import com.tradepilot.feature.ai.AnalysisScreen
import com.tradepilot.feature.browser.BrowserScreen
import com.tradepilot.feature.journal.AddTradeScreen
import com.tradepilot.feature.journal.JournalScreen
import com.tradepilot.feature.notification.NotificationScreen
import com.tradepilot.feature.analytics.StatisticScreen
import com.tradepilot.feature.settings.SettingsScreen
import com.tradepilot.feature.trading.MoneyManagementScreen
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Root layout: ActivityBar (kiri, persistent) + area konten yang berganti
 * sesuai route (Blueprint bagian 7: Navigation Diagram). Single Activity +
 * Compose Navigation, tanpa Activity terpisah per screen.
 */
@Composable
fun TradePilotNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    rootViewModel: AppRootViewModel = hiltViewModel()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TradePilotDestination.Browser.route

    Row(modifier = modifier.fillMaxSize()) {
        ActivityBar(
            currentRoute = currentRoute,
            onNavigate = { dest ->
                navController.navigate(dest.route) {
                    launchSingleTop = true
                    popUpTo(TradePilotDestination.Browser.route) { saveState = true }
                    restoreState = true
                }
            }
        )

        NavHost(
            navController = navController,
            startDestination = TradePilotDestination.Browser.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(TradePilotDestination.Browser.route) {
                BrowserScreen(
                    onAnalyzeRequested = { webView: WebView ->
                        rootViewModel.onAnalyzeRequested(webView)
                        navController.navigate(TradePilotDestination.Analysis.route) { launchSingleTop = true }
                    },
                    onWebViewReady = { webView: WebView -> rootViewModel.registerWebView(webView) }
                )
            }
            composable(TradePilotDestination.Analysis.route) { AnalysisScreen() }
            composable(TradePilotDestination.MoneyManagement.route) { MoneyManagementScreen() }
            composable(TradePilotDestination.Journal.route) {
                JournalScreen(onAddTradeClick = { navController.navigate(TradePilotDestination.AddTrade.route) })
            }
            composable(TradePilotDestination.AddTrade.route) {
                AddTradeScreen(onSaved = { navController.popBackStack() })
            }
            composable(TradePilotDestination.Statistic.route) { StatisticScreen() }
            composable(TradePilotDestination.Notification.route) { NotificationScreen() }
            composable(TradePilotDestination.Settings.route) { SettingsScreen() }
        }
    }
}
