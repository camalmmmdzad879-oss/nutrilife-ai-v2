package com.example.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.domain.model.ActivityLevel
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BiologicalSex
import com.example.domain.model.ExerciseType
import com.example.domain.model.FoodCategory
import com.example.domain.model.MealType
import com.example.domain.model.NutritionGoal
import com.example.domain.model.NutritionSourceType

data class AppStrings(
    val appName: String,
    // Navigation
    val navHome: String,
    val navScanner: String,
    val navDiary: String,
    val navStats: String,
    val navProfile: String,
    val navPlanner: String,
    val navSettings: String,
    // Common
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val add: String,
    val close: String,
    val back: String,
    val next: String,
    val finish: String,
    val retry: String,
    val search: String,
    val kcal: String,
    val gramsUnit: String,
    val mlUnit: String,
    val kgUnit: String,
    val cmUnit: String,
    val minUnit: String,
    val yearsUnit: String,
    val loading: String,
    val noDataYet: String,
    val offlineBadge: String,
    val onlineBadge: String,
    val today: String,
    val yesterday: String,
    // Onboarding
    val onboardingWelcomeTitle: String,
    val onboardingWelcomeSubtitle: String,
    val onboardingStepLanguage: String,
    val onboardingStepMetrics: String,
    val onboardingStepGoal: String,
    val onboardingStepSummary: String,
    val selectLanguage: String,
    val fullNameLabel: String,
    val ageLabel: String,
    val heightLabel: String,
    val currentWeightLabel: String,
    val goalWeightLabel: String,
    val sexLabel: String,
    val sexFemale: String,
    val sexMale: String,
    val activityLevelLabel: String,
    val nutritionGoalLabel: String,
    val minorSafetyWarning: String,
    val medicalDisclaimerShort: String,
    val calculatedDailyTarget: String,
    val bmrLabel: String,
    val tdeeLabel: String,
    val validationInvalidAge: String,
    val validationInvalidHeight: String,
    val validationInvalidWeight: String,
    // Activity levels
    val activitySedentary: String,
    val activityLight: String,
    val activityModerate: String,
    val activityActive: String,
    val activityVeryActive: String,
    // Goals
    val goalMaintain: String,
    val goalGradualLoss: String,
    val goalGradualGain: String,
    val goalImproveHabits: String,
    // Home
    val homeGreeting: String,
    val caloriesRemaining: String,
    val caloriesOver: String,
    val consumedLabel: String,
    val targetLabel: String,
    val proteinLabel: String,
    val carbsLabel: String,
    val fatLabel: String,
    val fiberLabel: String,
    val sugarLabel: String,
    val waterIntakeTitle: String,
    val quickActionsTitle: String,
    val scanMealPhotoCta: String,
    val scanBarcodeCta: String,
    val mealPlannerCta: String,
    val recipeCalcCta: String,
    val todaysMealsTitle: String,
    val aiEstimationBanner: String,
    // Meals
    val mealBreakfast: String,
    val mealLunch: String,
    val mealDinner: String,
    val mealSnack: String,
    // Food Database & Diary
    val diaryTitle: String,
    val addFoodToMeal: String,
    val searchFoodsHint: String,
    val allCategories: String,
    val favoritesOnly: String,
    val azerbaijaniCuisineFilter: String,
    val createCustomFood: String,
    val foodNameLabel: String,
    val caloriesPer100gLabel: String,
    val proteinPer100gLabel: String,
    val carbsPer100gLabel: String,
    val fatPer100gLabel: String,
    val fiberPer100gLabel: String,
    val sugarPer100gLabel: String,
    val portionGramsLabel: String,
    val householdMeasuresLabel: String,
    val selectMealCategory: String,
    val entryAddedMessage: String,
    val entryDeletedMessage: String,
    val emptyDiaryMessage: String,
    val sourceDatabase: String,
    val sourceEstimatedRecipe: String,
    val sourceAiEstimated: String,
    val sourceBarcodeOnline: String,
    val sourceUserEntered: String,
    // Scanner
    val scannerTitle: String,
    val tabPhotoScanner: String,
    val tabBarcodeScanner: String,
    val takePhotoBtn: String,
    val pickFromGalleryBtn: String,
    val aiConsentTitle: String,
    val aiConsentBody: String,
    val aiConsentAccept: String,
    val aiServiceStatusTitle: String,
    val aiPreviewFallbackNotice: String,
    val aiAccuracyNoticeTitle: String,
    val aiAccuracyNoticeBody: String,
    val hiddenIngredientsWarning: String,
    val estimatedWeightRangeFormat: String,
    val estimatedCaloriesRangeFormat: String,
    val saveAnalyzedMealBtn: String,
    val editDetectedItemTitle: String,
    val barcodePrompt: String,
    val manualBarcodeHint: String,
    val lookupBarcodeBtn: String,
    val cameraPermissionRequired: String,
    val grantCameraPermissionBtn: String,
    val barcodeNotFoundTitle: String,
    val barcodeNotFoundBody: String,
    val addBarcodeManuallyBtn: String,
    val sharedImageReceivedMsg: String,
    // Stats & Micros & Water & Weight & Exercise & Reports
    val statsTitle: String,
    val tabOverview: String,
    val tabMicronutrients: String,
    val tabWater: String,
    val tabWeight: String,
    val tabExercise: String,
    val tabReports: String,
    val weeklyCaloriesTitle: String,
    val micronutrientsDisclaimer: String,
    val sodiumLabel: String,
    val calciumLabel: String,
    val ironLabel: String,
    val potassiumLabel: String,
    val magnesiumLabel: String,
    val vitaminALabel: String,
    val vitaminB12Label: String,
    val vitaminCLabel: String,
    val vitaminDLabel: String,
    val addWaterCustomLabel: String,
    val adjustWaterTargetLabel: String,
    val waterHistoryTitle: String,
    val logWeightTitle: String,
    val weightKgInputLabel: String,
    val weightHistoryTitle: String,
    val weightDeltaWeekly: String,
    val logExerciseTitle: String,
    val exerciseDurationLabel: String,
    val exerciseEstimatedBurnNote: String,
    val exerciseNotSubtractedNote: String,
    val weeklySummaryTitle: String,
    val monthlySummaryTitle: String,
    val exportCsvBtn: String,
    val exportSuccessMsg: String,
    // Planner & Recipes
    val plannerTitle: String,
    val tabMealPlanner: String,
    val tabRecipeCalculator: String,
    val generateMealPlanBtn: String,
    val saveFavoritePlanBtn: String,
    val replaceMealBtn: String,
    val mealPlanPreviewNotice: String,
    val savedPlansTitle: String,
    val recipeNameLabel: String,
    val servingsCountLabel: String,
    val addIngredientBtn: String,
    val totalNutritionLabel: String,
    val perServingNutritionLabel: String,
    val saveRecipeBtn: String,
    val savedRecipesTitle: String,
    val logServingToDiaryBtn: String,
    // Profile & Settings
    val profileTitle: String,
    val editProfileSection: String,
    val allergiesLabel: String,
    val dietaryPrefsLabel: String,
    val profileSavedMsg: String,
    val settingsTitle: String,
    val themeLabel: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val remindersTitle: String,
    val reminderMeals: String,
    val reminderWater: String,
    val reminderWeight: String,
    val reminderDiary: String,
    val notificationPermissionNotice: String,
    val grantNotificationPermissionBtn: String,
    val reminderNotificationTitle: String,
    val reminderNotificationBody: String,
    val privacyPolicyTitle: String,
    val privacyPolicyBody: String,
    val termsOfUseTitle: String,
    val termsOfUseBody: String,
    val aiSettingsTitle: String,
    val aiSettingsBody: String,
    val deleteAllDataTitle: String,
    val deleteAllDataConfirm: String,
    val dataDeletedMsg: String,
    val aboutTitle: String,
    val aboutBody: String,
    val appVersionLabel: String,
    val shareAppTitle: String,
    val shareAppSubtitle: String,
    val shareAppLinkBtn: String,
    val shareAppApkBtn: String,
    val shareAppInviteText: String,
    val shareApkUnavailableMsg: String,
    val developerLabel: String,
    val copyrightText: String,
    val openAboutPageBtn: String,
    val updateRecognizedBanner: String,
    val updateStatusTitle: String,
    val updateStatusBody: String,
    val packageIdLabel: String,
    val aiInsightTitle: String,
    val aiInsightCta: String,
    val aiInsightPromptHint: String,
    val aiLiveBadge: String
) {
    fun mealName(mealType: MealType): String = when (mealType) {
        MealType.BREAKFAST -> mealBreakfast
        MealType.LUNCH -> mealLunch
        MealType.DINNER -> mealDinner
        MealType.SNACK -> mealSnack
    }

    fun activityName(level: ActivityLevel): String = when (level) {
        ActivityLevel.SEDENTARY -> activitySedentary
        ActivityLevel.LIGHT -> activityLight
        ActivityLevel.MODERATE -> activityModerate
        ActivityLevel.ACTIVE -> activityActive
        ActivityLevel.VERY_ACTIVE -> activityVeryActive
    }

    fun goalName(goal: NutritionGoal): String = when (goal) {
        NutritionGoal.MAINTAIN -> goalMaintain
        NutritionGoal.GRADUAL_LOSS -> goalGradualLoss
        NutritionGoal.GRADUAL_GAIN -> goalGradualGain
        NutritionGoal.IMPROVE_HABITS -> goalImproveHabits
    }

    fun sourceName(source: NutritionSourceType): String = when (source) {
        NutritionSourceType.DATABASE -> sourceDatabase
        NutritionSourceType.ESTIMATED_RECIPE -> sourceEstimatedRecipe
        NutritionSourceType.AI_ESTIMATED -> sourceAiEstimated
        NutritionSourceType.BARCODE_ONLINE -> sourceBarcodeOnline
        NutritionSourceType.USER_ENTERED -> sourceUserEntered
    }

    fun themeName(theme: AppThemeMode): String = when (theme) {
        AppThemeMode.SYSTEM -> themeSystem
        AppThemeMode.LIGHT -> themeLight
        AppThemeMode.DARK -> themeDark
    }

    fun sexName(sex: BiologicalSex): String = when (sex) {
        BiologicalSex.FEMALE -> sexFemale
        BiologicalSex.MALE -> sexMale
    }

    fun categoryName(category: FoodCategory, lang: AppLanguage): String = when (lang) {
        AppLanguage.AZ -> when (category) {
            FoodCategory.AZERBAIJANI_CUISINE -> "Azərbaycan mətbəxi"
            FoodCategory.FRUITS -> "Meyvələr"
            FoodCategory.VEGETABLES -> "Tərəvəzlər"
            FoodCategory.DAIRY -> "Süd məhsulları"
            FoodCategory.MEAT_FISH -> "Ət və balıq"
            FoodCategory.GRAINS_BREAD -> "Taxıl və çörək"
            FoodCategory.DESSERTS -> "Şirniyyat"
            FoodCategory.PACKAGED -> "Qablaşdırılmış"
            FoodCategory.BEVERAGES -> "İçkilər"
            FoodCategory.OTHER -> "Digər"
        }
        AppLanguage.EN -> when (category) {
            FoodCategory.AZERBAIJANI_CUISINE -> "Azerbaijani Cuisine"
            FoodCategory.FRUITS -> "Fruits"
            FoodCategory.VEGETABLES -> "Vegetables"
            FoodCategory.DAIRY -> "Dairy"
            FoodCategory.MEAT_FISH -> "Meat & Fish"
            FoodCategory.GRAINS_BREAD -> "Grains & Bread"
            FoodCategory.DESSERTS -> "Desserts"
            FoodCategory.PACKAGED -> "Packaged Foods"
            FoodCategory.BEVERAGES -> "Beverages"
            FoodCategory.OTHER -> "Other"
        }
        AppLanguage.RU -> when (category) {
            FoodCategory.AZERBAIJANI_CUISINE -> "Азербайджанская кухня"
            FoodCategory.FRUITS -> "Фрукты"
            FoodCategory.VEGETABLES -> "Овощи"
            FoodCategory.DAIRY -> "Молочные продукты"
            FoodCategory.MEAT_FISH -> "Мясо и рыба"
            FoodCategory.GRAINS_BREAD -> "Крупы и хлеб"
            FoodCategory.DESSERTS -> "Десерты"
            FoodCategory.PACKAGED -> "Упакованные продукты"
            FoodCategory.BEVERAGES -> "Напитки"
            FoodCategory.OTHER -> "Другое"
        }
    }

    fun exerciseName(type: ExerciseType, lang: AppLanguage): String = when (lang) {
        AppLanguage.AZ -> when (type) {
            ExerciseType.WALKING -> "Gəzinti"
            ExerciseType.RUNNING -> "Qaçış"
            ExerciseType.CYCLING -> "Velosiped"
            ExerciseType.GYM -> "İdman zalı"
            ExerciseType.HOME_WORKOUT -> "Ev məşqi"
            ExerciseType.OTHER -> "Digər fəaliyyət"
        }
        AppLanguage.EN -> when (type) {
            ExerciseType.WALKING -> "Walking"
            ExerciseType.RUNNING -> "Running"
            ExerciseType.CYCLING -> "Cycling"
            ExerciseType.GYM -> "Gym workout"
            ExerciseType.HOME_WORKOUT -> "Home workout"
            ExerciseType.OTHER -> "Other activity"
        }
        AppLanguage.RU -> when (type) {
            ExerciseType.WALKING -> "Ходьба"
            ExerciseType.RUNNING -> "Бег"
            ExerciseType.CYCLING -> "Велосипед"
            ExerciseType.GYM -> "Тренировка в зале"
            ExerciseType.HOME_WORKOUT -> "Домашняя тренировка"
            ExerciseType.OTHER -> "Другая активность"
        }
    }
}

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.AZ -> AzStrings
    AppLanguage.EN -> EnStrings
    AppLanguage.RU -> RuStrings
}

val LocalStrings = staticCompositionLocalOf { AzStrings }
