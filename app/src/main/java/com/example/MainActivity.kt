package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.configureAppCheck
import com.example.i18n.LocalStrings
import com.example.i18n.stringsFor
import com.example.ui.screens.AboutAppPage
import com.example.ui.screens.DiaryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlannerAndRecipesScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.NutriLifeTheme
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

enum class PrimaryTab(val testTag: String, val icon: ImageVector) {
    HOME("nav_home", Icons.Outlined.Home),
    SCANNER("nav_scanner", Icons.Outlined.CameraAlt),
    DIARY("nav_diary", Icons.Outlined.Book),
    STATS("nav_stats", Icons.Outlined.BarChart),
    PROFILE("nav_profile", Icons.Outlined.Person)
}

enum class SubScreen {
    NONE,
    PLANNER_RECIPES,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val pendingSharedImage = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before setContent, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        extractSharedImageUri(intent)?.let { pendingSharedImage.value = it }

        setContent {
            NutriLifeAppRoot(
                viewModel = viewModel,
                incomingSharedImage = pendingSharedImage.value,
                onConsumeSharedImage = { pendingSharedImage.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedImageUri(intent)?.let { pendingSharedImage.value = it }
    }

    @Suppress("DEPRECATION")
    private fun extractSharedImageUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val mime = intent.type ?: return null
        if (!mime.startsWith("image/")) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}

@Composable
fun NutriLifeAppRoot(
    viewModel: AppViewModel,
    incomingSharedImage: Uri? = null,
    onConsumeSharedImage: () -> Unit = {}
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val goals by viewModel.nutritionGoals.collectAsStateWithLifecycle()
    val totals by viewModel.dailyTotals.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntriesForSelectedDate.collectAsStateWithLifecycle()
    val diaryLast30Days by viewModel.diaryLast30Days.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
    val foodResults by viewModel.foodResults.collectAsStateWithLifecycle()
    val waterTodayLogs by viewModel.waterForSelectedDate.collectAsStateWithLifecycle()
    val waterLast7Days by viewModel.waterLast7Days.collectAsStateWithLifecycle()
    val weightHistory by viewModel.weightHistory.collectAsStateWithLifecycle()
    val exerciseTodayLogs by viewModel.exerciseForSelectedDate.collectAsStateWithLifecycle()
    val exerciseLast30Days by viewModel.exerciseLast30Days.collectAsStateWithLifecycle()
    val scanImageUri by viewModel.scanImageUri.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val barcodeState by viewModel.barcodeState.collectAsStateWithLifecycle()
    val mealPlanState by viewModel.mealPlanState.collectAsStateWithLifecycle()
    val insightState by viewModel.insightState.collectAsStateWithLifecycle()
    val savedPlans by viewModel.savedMealPlans.collectAsStateWithLifecycle()
    val recipeIngredients by viewModel.recipeDraftIngredients.collectAsStateWithLifecycle()
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
    val showUpdatedBanner by viewModel.wasAppUpdatedBanner.collectAsStateWithLifecycle()

    val strings = stringsFor(profile.language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableStateOf(PrimaryTab.HOME) }
    var activeSubScreen by rememberSaveable { mutableStateOf(SubScreen.NONE) }
    var scannerInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var plannerInitialTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(incomingSharedImage) {
        if (incomingSharedImage != null) {
            viewModel.setSharedImageUri(incomingSharedImage)
            if (profile.aiConsentAccepted) {
                viewModel.analyzeFoodPhoto(incomingSharedImage)
            }
            scannerInitialTab = 0
            activeSubScreen = SubScreen.NONE
            activeTab = PrimaryTab.SCANNER
            snackbarHostState.showSnackbar(strings.sharedImageReceivedMsg)
            onConsumeSharedImage()
        }
    }

    val exportAndShareCsv: () -> Unit = {
        scope.launch {
            runCatching {
                val file = viewModel.exportCsv()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(sendIntent, strings.exportCsvBtn))
                snackbarHostState.showSnackbar(strings.exportSuccessMsg)
            }.onFailure {
                snackbarHostState.showSnackbar(it.localizedMessage ?: "Export error")
            }
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        NutriLifeTheme(themeMode = profile.themeMode) {
            if (!profile.onboardingCompleted) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        OnboardingScreen(
                            initialProfile = profile,
                            onLanguageSelected = { viewModel.updateLanguage(it) },
                            onComplete = { completed ->
                                viewModel.saveUserProfile(completed, alsoLogWeight = true)
                            }
                        )
                    }
                }
            } else {
                when (activeSubScreen) {
                    SubScreen.PLANNER_RECIPES -> {
                        PlannerAndRecipesScreen(
                            initialTab = plannerInitialTab,
                            language = profile.language,
                            isLiveGeminiEnabled = viewModel.aiService.isLiveGeminiEnabled,
                            mealPlanState = mealPlanState,
                            savedPlans = savedPlans,
                            foodResults = foodResults,
                            searchQuery = searchQuery,
                            recipeIngredients = recipeIngredients,
                            savedRecipes = savedRecipes,
                            onBack = { activeSubScreen = SubScreen.NONE },
                            onGeneratePlan = { viewModel.generateMealPlan() },
                            onReplaceMeal = { viewModel.replacePlannedMeal(it) },
                            onSavePlan = { viewModel.saveCurrentMealPlan() },
                            onDeletePlan = { viewModel.deleteSavedPlan(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onAddRecipeIngredient = { food, grams -> viewModel.addRecipeIngredient(food, grams) },
                            onRemoveRecipeIngredient = { viewModel.removeRecipeIngredient(it) },
                            onSaveRecipe = { name, servings -> viewModel.saveRecipe(name, servings) },
                            onLogRecipeServing = { recipe, meal ->
                                viewModel.logRecipeServing(recipe, meal)
                                scope.launch { snackbarHostState.showSnackbar(strings.entryAddedMessage) }
                            },
                            onDeleteRecipe = { viewModel.deleteRecipe(it) }
                        )
                    }

                    SubScreen.ABOUT -> {
                        AboutAppPage(
                            onBack = { activeSubScreen = SubScreen.NONE },
                            onPrepareApkShareUri = { viewModel.prepareShareableApkUri() },
                            onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }

                    SubScreen.NONE -> {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val useRail = maxWidth >= 600.dp
                            val navLabel: (PrimaryTab) -> String = { tab ->
                                when (tab) {
                                    PrimaryTab.HOME -> strings.navHome
                                    PrimaryTab.SCANNER -> strings.navScanner
                                    PrimaryTab.DIARY -> strings.navDiary
                                    PrimaryTab.STATS -> strings.navStats
                                    PrimaryTab.PROFILE -> strings.navProfile
                                }
                            }

                            Scaffold(
                                contentWindowInsets = WindowInsets.safeDrawing,
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                bottomBar = {
                                    if (!useRail) {
                                        NavigationBar {
                                            PrimaryTab.entries.forEach { tab ->
                                                val label = navLabel(tab)
                                                NavigationBarItem(
                                                    selected = activeTab == tab,
                                                    onClick = { activeTab = tab },
                                                    icon = { Icon(tab.icon, contentDescription = label) },
                                                    label = { Text(label, maxLines = 1) },
                                                    modifier = Modifier.testTag(tab.testTag)
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (useRail) {
                                        NavigationRail {
                                            PrimaryTab.entries.forEach { tab ->
                                                val label = navLabel(tab)
                                                NavigationRailItem(
                                                    selected = activeTab == tab,
                                                    onClick = { activeTab = tab },
                                                    icon = { Icon(tab.icon, contentDescription = label) },
                                                    label = { Text(label, maxLines = 1) },
                                                    modifier = Modifier.testTag(tab.testTag)
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                        when (activeTab) {
                                            PrimaryTab.HOME -> {
                                                HomeScreen(
                                                    profile = profile,
                                                    goals = goals,
                                                    totals = totals,
                                                    waterTodayMl = waterTodayLogs.sumOf { it.amountMl },
                                                    diaryEntries = diaryEntries,
                                                    showUpdatedBanner = showUpdatedBanner,
                                                    onDismissUpdatedBanner = { viewModel.dismissUpdatedBanner() },
                                                    onQuickAddWater = { ml -> viewModel.addWater(ml) },
                                                    onOpenScannerPhoto = {
                                                        scannerInitialTab = 0
                                                        activeTab = PrimaryTab.SCANNER
                                                    },
                                                    onOpenScannerBarcode = {
                                                        scannerInitialTab = 1
                                                        activeTab = PrimaryTab.SCANNER
                                                    },
                                                    onOpenDiary = { activeTab = PrimaryTab.DIARY },
                                                    onOpenPlanner = {
                                                        plannerInitialTab = 0
                                                        activeSubScreen = SubScreen.PLANNER_RECIPES
                                                    },
                                                    onOpenRecipes = {
                                                        plannerInitialTab = 1
                                                        activeSubScreen = SubScreen.PLANNER_RECIPES
                                                    },
                                                    onOpenSettings = { activeTab = PrimaryTab.PROFILE },
                                                    insightState = insightState,
                                                    isLiveGeminiEnabled = viewModel.aiService.isLiveGeminiEnabled,
                                                    onRequestDailyInsight = { viewModel.requestDailyNutritionInsight() }
                                                )
                                            }

                                            PrimaryTab.SCANNER -> {
                                                ScannerScreen(
                                                    initialTab = scannerInitialTab,
                                                    language = profile.language,
                                                    aiConsentAccepted = profile.aiConsentAccepted,
                                                    isLiveGeminiEnabled = viewModel.aiService.isLiveGeminiEnabled,
                                                    selectedImageUri = scanImageUri,
                                                    scanState = scanState,
                                                    barcodeState = barcodeState,
                                                    onAcceptConsent = { viewModel.acceptAiConsent() },
                                                    onAnalyzePhoto = { uri -> viewModel.analyzeFoodPhoto(uri) },
                                                    onUpdateCandidate = { viewModel.updateScannedCandidate(it) },
                                                    onRemoveCandidate = { viewModel.removeScannedCandidate(it) },
                                                    onSaveAnalyzedMeal = { mealType ->
                                                        viewModel.saveScannedMealToDiary(mealType)
                                                        scope.launch { snackbarHostState.showSnackbar(strings.entryAddedMessage) }
                                                    },
                                                    onLookupBarcode = { viewModel.lookupBarcode(it) },
                                                    onLogFood = { food, mealType, grams, label ->
                                                        viewModel.logFood(food, mealType, grams, label)
                                                        scope.launch { snackbarHostState.showSnackbar(strings.entryAddedMessage) }
                                                    },
                                                    onAddManualBarcodeFood = { barcode, name, kcal, p, c, f, grams, mealType ->
                                                        viewModel.addCustomFoodAndLog(
                                                            name = name,
                                                            category = com.example.domain.model.FoodCategory.PACKAGED,
                                                            kcal100 = kcal,
                                                            protein100 = p,
                                                            carbs100 = c,
                                                            fat100 = f,
                                                            fiber100 = 0.0,
                                                            sugar100 = 0.0,
                                                            grams = grams,
                                                            mealType = mealType,
                                                            barcode = barcode
                                                        )
                                                        scope.launch { snackbarHostState.showSnackbar(strings.entryAddedMessage) }
                                                    }
                                                )
                                            }

                                            PrimaryTab.DIARY -> {
                                                DiaryScreen(
                                                    selectedDateIso = selectedDate,
                                                    language = profile.language,
                                                    goals = goals,
                                                    totals = totals,
                                                    entries = diaryEntries,
                                                    searchQuery = searchQuery,
                                                    selectedCategory = selectedCategory,
                                                    favoritesOnly = favoritesOnly,
                                                    foodResults = foodResults,
                                                    onShiftDate = { viewModel.shiftDate(it) },
                                                    onSelectToday = {
                                                        viewModel.selectDate(java.time.LocalDate.now().toString())
                                                    },
                                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                                    onCategoryChange = { viewModel.setSelectedCategory(it) },
                                                    onToggleFavoritesFilter = { viewModel.toggleFavoritesFilter() },
                                                    onToggleFoodFavorite = { viewModel.toggleFoodFavorite(it) },
                                                    onLogFood = { food, mealType, grams, label ->
                                                        viewModel.logFood(food, mealType, grams, label)
                                                        scope.launch { snackbarHostState.showSnackbar(strings.entryAddedMessage) }
                                                    },
                                                    onAddCustomFood = { name, cat, kcal, p, c, f, fib, sug, grams, mealType ->
                                                        viewModel.addCustomFoodAndLog(name, cat, kcal, p, c, f, fib, sug, grams, mealType)
                                                        scope.launch { snackbarHostState.showSnackbar(strings.entryAddedMessage) }
                                                    },
                                                    onUpdateEntry = { entry, newGrams, newMeal ->
                                                        viewModel.updateDiaryEntry(entry, newGrams, newMeal)
                                                    },
                                                    onDeleteEntry = { id ->
                                                        viewModel.deleteDiaryEntry(id)
                                                        scope.launch { snackbarHostState.showSnackbar(strings.entryDeletedMessage) }
                                                    }
                                                )
                                            }

                                            PrimaryTab.STATS -> {
                                                StatsScreen(
                                                    language = profile.language,
                                                    profile = profile,
                                                    goals = goals,
                                                    dailyTotals = totals,
                                                    diaryLast30Days = diaryLast30Days,
                                                    waterTodayLogs = waterTodayLogs,
                                                    waterLast7Days = waterLast7Days,
                                                    weightHistory = weightHistory,
                                                    exerciseTodayLogs = exerciseTodayLogs,
                                                    exerciseLast30Days = exerciseLast30Days,
                                                    onAddWater = { viewModel.addWater(it) },
                                                    onDeleteWater = { viewModel.deleteWaterLog(it) },
                                                    onUpdateWaterTarget = { viewModel.updateWaterTarget(it) },
                                                    onAddWeight = { w, note -> viewModel.addWeight(w, note) },
                                                    onUpdateWeight = { viewModel.updateWeight(it) },
                                                    onDeleteWeight = { viewModel.deleteWeight(it) },
                                                    onAddExercise = { type, min, manual, note ->
                                                        viewModel.addExercise(type, min, manual, note)
                                                    },
                                                    onDeleteExercise = { viewModel.deleteExercise(it) },
                                                    onExportCsv = exportAndShareCsv
                                                )
                                            }

                                            PrimaryTab.PROFILE -> {
                                                ProfileAndSettingsScreen(
                                                    profile = profile,
                                                    reminderSettings = reminderSettings,
                                                    onSaveProfile = { updated -> viewModel.saveUserProfile(updated) },
                                                    onLanguageSelected = { viewModel.updateLanguage(it) },
                                                    onThemeSelected = { viewModel.updateTheme(it) },
                                                    onRemindersChanged = { viewModel.updateReminders(it) },
                                                    onExportCsv = exportAndShareCsv,
                                                    onPrepareApkShareUri = { viewModel.prepareShareableApkUri() },
                                                    onOpenAboutPage = { activeSubScreen = SubScreen.ABOUT },
                                                    onDeleteAllData = { viewModel.deleteAllPersonalData() },
                                                    onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
