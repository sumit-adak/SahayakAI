package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.*
import com.example.ui.components.AddLedgerEntryDialog
import com.example.ui.components.OcrConfirmationDialog
import com.example.ui.components.SahayakTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SahayakViewModel

sealed class Screen(
    val route: String,
    val titleEn: String,
    val titleHi: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", "होम", Icons.Filled.Home, Icons.Outlined.Home)
    object Khata : Screen("khata", "Khata", "खाता", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook)
    object MlForecast : Screen("ml_forecast", "Market AI", "बाजार AI", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
    object Advisor : Screen("advisor", "Advisor", "सलाहकार", Icons.Filled.Psychology, Icons.Outlined.Psychology)
    object Schemes : Screen("schemes", "Schemes", "योजनाएं", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    object Report : Screen("report", "Proof", "प्रमाणपत्र", Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium)
    object CommunityKyc : Screen("community_kyc", "Community", "समुदाय", Icons.Filled.Groups, Icons.Outlined.Groups)
    object Profile : Screen("profile", "Profile", "प्रोफ़ाइल", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
    object Feasibility : Screen("feasibility", "Feasibility", "व्यवहार्यता", Icons.Filled.Assessment, Icons.Outlined.Assessment)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Khata,
    Screen.MlForecast,
    Screen.Advisor,
    Screen.Schemes,
    Screen.Report,
    Screen.CommunityKyc
)

@Composable
fun SahayakApp(viewModel: SahayakViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()

    // 1. If user is not logged in, show AuthScreen (Part C2 & C3)
    if (!userProfile.isLoggedIn) {
        AuthScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                // Flow automatically redirects according to profile completion state
            }
        )
        return
    }

    // 2. If user is logged in but profile is not completed (Part C3 & D1), show mandatory ProfileSetupScreen
    if (!userProfile.profileComplete || userProfile.name.isBlank()) {
        ProfileSetupScreen(
            viewModel = viewModel,
            onComplete = {
                // Flow automatically proceeds to main screen
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val ocrReviewItems by viewModel.ocrReviewItems.collectAsState()

    val isHindi = userProfile.preferredLanguage == AppLanguage.HINDI

    var showManualAddDialog by remember { mutableStateOf(false) }
    var showOcrTextPromptDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (currentRoute != Screen.Profile.route) {
                SahayakTopBar(
                    currentLanguage = userProfile.preferredLanguage,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    isSpeaking = isTtsSpeaking,
                    onStopSpeaking = { viewModel.stopSpeaking() },
                    onOpenProfile = {
                        navController.navigate(Screen.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenKyc = {
                        navController.navigate(Screen.CommunityKyc.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhite,
                tonalElevation = 8.dp
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.titleEn
                            )
                        },
                        label = {
                            Text(
                                text = if (isHindi) screen.titleHi else screen.titleEn,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald900,
                            selectedTextColor = Emerald900,
                            indicatorColor = Emerald100,
                            unselectedIconColor = Slate700,
                            unselectedTextColor = Slate700
                        ),
                        modifier = Modifier.testTag("nav_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToKhata = { navController.navigate(Screen.Khata.route) },
                    onNavigateToAdvisor = { navController.navigate(Screen.Advisor.route) },
                    onNavigateToSchemes = { navController.navigate(Screen.Schemes.route) },
                    onNavigateToReport = { navController.navigate(Screen.Report.route) },
                    onOpenOcrScan = { showOcrTextPromptDialog = true },
                    onOpenAddEntry = { showManualAddDialog = true },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToFeasibility = { navController.navigate(Screen.Feasibility.route) }
                )
            }

            composable(Screen.Khata.route) {
                KhataScreen(
                    viewModel = viewModel,
                    onOpenOcrScan = { showOcrTextPromptDialog = true }
                )
            }

            composable(Screen.MlForecast.route) {
                MlForecastingScreen(
                    viewModel = viewModel,
                    onNavigateToAdvisorWithQuery = { query ->
                        viewModel.sendChatMessage(query, autoSpeak = true)
                        navController.navigate(Screen.Advisor.route)
                    }
                )
            }

            composable(Screen.Advisor.route) {
                AdvisorScreen(viewModel = viewModel)
            }

            composable(Screen.Schemes.route) {
                SchemesScreen(
                    viewModel = viewModel,
                    onNavigateToAdvisorWithPrompt = { prompt ->
                        viewModel.sendChatMessage(prompt, autoSpeak = true)
                        navController.navigate(Screen.Advisor.route)
                    }
                )
            }

            composable(Screen.Report.route) {
                FinanceReportScreen(viewModel = viewModel)
            }

            composable(Screen.CommunityKyc.route) {
                CommunityAndKycScreen(viewModel = viewModel)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Feasibility.route) {
                FeasibilityScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onAskAdvisor = { prompt ->
                        viewModel.sendChatMessage(prompt, autoSpeak = true)
                        navController.navigate(Screen.Advisor.route)
                    }
                )
            }
        }
    }

    // Human-in-the-Loop OCR Confirmation Dialog
    ocrReviewItems?.let { items ->
        OcrConfirmationDialog(
            parsedItems = items,
            isHindi = isHindi,
            onDismiss = { viewModel.clearOcrReview() },
            onConfirm = { confirmed ->
                viewModel.confirmOcrEntries(confirmed)
            }
        )
    }

    // Manual / Voice Entry Dialog
    if (showManualAddDialog) {
        AddLedgerEntryDialog(
            isHindi = isHindi,
            onDismiss = { showManualAddDialog = false },
            onSave = { party, desc, amount, type, category, phone ->
                viewModel.addManualLedgerEntry(party, desc, amount, type, category, phone)
                showManualAddDialog = false
            }
        )
    }

    // OCR Scan Text Input / Preset Picker Dialog
    if (showOcrTextPromptDialog) {
        var rawText by remember { mutableStateOf("") }
        val presets = remember { viewModel.getSampleKhataPresets() }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showOcrTextPromptDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                color = PureWhite
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isHindi) "हस्तलिखित बहीखाता स्कैन (OCR)" else "Scan Handwritten Khata / Chit",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "कागजी पर्ची का टेक्स्ट दर्ज करें या नीचे से नमूना खाता चुनें:" else "Paste raw scanned receipt/khata text or pick a realistic preset:",
                        fontSize = 12.sp,
                        color = Slate700
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(if (isHindi) "त्वरित नमूना खाते:" else "Quick Realistic Presets:", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Slate900)
                    Spacer(modifier = Modifier.height(4.dp))

                    for (preset in presets) {
                        AssistChip(
                            onClick = { rawText = preset.second },
                            label = { Text(preset.first, fontSize = 11.sp, color = Slate900) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        placeholder = { Text("e.g. Ramesh Kumar ration ₹850 udhaar\nCounter sales ₹3200 jama", color = Slate500) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showOcrTextPromptDialog = false }, modifier = Modifier.weight(1f)) {
                            Text(if (isHindi) "रद्द करें" else "Cancel", color = Slate900)
                        }
                        Button(
                            onClick = {
                                viewModel.startOcrScan(rawText)
                                showOcrTextPromptDialog = false
                            },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = Amber600, contentColor = PureWhite)
                        ) {
                            Text(if (isHindi) "OCR पार्स करें" else "Parse Entries", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
