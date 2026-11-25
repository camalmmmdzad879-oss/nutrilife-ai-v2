package com.example.domain.model

enum class AppLanguage(val code: String, val nativeName: String) {
    AZ("az", "Azərbaycan dili"),
    EN("en", "English"),
    RU("ru", "Русский");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: AZ
    }
}

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(name: String?): AppThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

enum class BiologicalSex {
    FEMALE, MALE;

    companion object {
        fun fromName(name: String?): BiologicalSex =
            entries.firstOrNull { it.name == name } ?: FEMALE
    }
}

enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9);

    companion object {
        fun fromName(name: String?): ActivityLevel =
            entries.firstOrNull { it.name == name } ?: LIGHT
    }
}

enum class NutritionGoal {
    MAINTAIN,
    GRADUAL_LOSS,
    GRADUAL_GAIN,
    IMPROVE_HABITS;

    companion object {
        fun fromName(name: String?): NutritionGoal =
            entries.firstOrNull { it.name == name } ?: IMPROVE_HABITS
    }
}

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK;

    companion object {
        fun fromName(name: String?): MealType =
            entries.firstOrNull { it.name == name } ?: BREAKFAST
    }
}

enum class FoodCategory {
    AZERBAIJANI_CUISINE,
    FRUITS,
    VEGETABLES,
    DAIRY,
    MEAT_FISH,
    GRAINS_BREAD,
    DESSERTS,
    PACKAGED,
    BEVERAGES,
    OTHER;

    companion object {
        fun fromName(name: String?): FoodCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class NutritionSourceType {
    DATABASE,
    ESTIMATED_RECIPE,
    AI_ESTIMATED,
    BARCODE_ONLINE,
    USER_ENTERED;

    companion object {
        fun fromName(name: String?): NutritionSourceType =
            entries.firstOrNull { it.name == name } ?: DATABASE
    }
}

enum class ExerciseType(val metValue: Double) {
    WALKING(3.5),
    RUNNING(8.5),
    CYCLING(6.8),
    GYM(5.5),
    HOME_WORKOUT(4.5),
    OTHER(4.0);

    companion object {
        fun fromName(name: String?): ExerciseType =
            entries.firstOrNull { it.name == name } ?: WALKING
    }
}

data class UserProfile(
    val name: String = "",
    val age: Int = 25,
    val heightCm: Int = 170,
    val currentWeightKg: Double = 70.0,
    val goalWeightKg: Double = 68.0,
    val sex: BiologicalSex = BiologicalSex.FEMALE,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val nutritionGoal: NutritionGoal = NutritionGoal.MAINTAIN,
    val language: AppLanguage = AppLanguage.AZ,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dailyWaterTargetMl: Int = 2200,
    val dietaryPreferences: String = "",
    val allergies: String = "",
    val onboardingCompleted: Boolean = false,
    val aiConsentAccepted: Boolean = false
)

data class NutritionGoals(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int,
    val sugarLimitG: Int,
    val sodiumLimitMg: Int,
    val waterMl: Int,
    val isMinorProtectionActive: Boolean,
    val effectiveGoal: NutritionGoal,
    val bmrKcal: Int,
    val tdeeKcal: Int
)

data class NutrientTotals(
    val calories: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sugarG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val magnesiumMg: Double = 0.0,
    val vitaminAMcg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    val vitaminCMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0
)

data class HouseholdMeasure(
    val labelAz: String,
    val labelEn: String,
    val labelRu: String,
    val grams: Double
) {
    fun localizedLabel(language: AppLanguage): String = when (language) {
        AppLanguage.AZ -> labelAz
        AppLanguage.EN -> labelEn
        AppLanguage.RU -> labelRu
    }
}

data class ScannedFoodCandidate(
    val id: String,
    val name: String,
    val category: FoodCategory,
    val portionLabel: String,
    val estimatedGrams: Double,
    val minGrams: Double,
    val maxGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    val sugarPer100g: Double,
    val sodiumMgPer100g: Double,
    val calciumMgPer100g: Double = 0.0,
    val ironMgPer100g: Double = 0.0,
    val potassiumMgPer100g: Double = 0.0,
    val magnesiumMgPer100g: Double = 0.0,
    val vitaminAMcgPer100g: Double = 0.0,
    val vitaminB12McgPer100g: Double = 0.0,
    val vitaminCMgPer100g: Double = 0.0,
    val vitaminDMcgPer100g: Double = 0.0,
    val confidenceNote: String = ""
) {
    val scaledCalories: Double get() = caloriesPer100g * estimatedGrams / 100.0
    val scaledProtein: Double get() = proteinPer100g * estimatedGrams / 100.0
    val scaledCarbs: Double get() = carbsPer100g * estimatedGrams / 100.0
    val scaledFat: Double get() = fatPer100g * estimatedGrams / 100.0
    val scaledFiber: Double get() = fiberPer100g * estimatedGrams / 100.0
    val scaledSugar: Double get() = sugarPer100g * estimatedGrams / 100.0
    val calorieRangeMin: Int get() = (caloriesPer100g * minGrams / 100.0 * 0.88).toInt().coerceAtLeast(0)
    val calorieRangeMax: Int get() = (caloriesPer100g * maxGrams / 100.0 * 1.15).toInt().coerceAtLeast(calorieRangeMin)
}

data class AiMealScanResult(
    val items: List<ScannedFoodCandidate>,
    val isPreviewFallback: Boolean,
    val uncertaintyNoticeKey: String,
    val hiddenIngredientsNote: String
)

data class ReminderSettings(
    val mealRemindersEnabled: Boolean = false,
    val waterRemindersEnabled: Boolean = false,
    val weightRemindersEnabled: Boolean = false,
    val diaryRemindersEnabled: Boolean = false,
    val mealHour: Int = 13,
    val waterIntervalHours: Int = 3
)

data class PlannedMeal(
    val mealType: MealType,
    val title: String,
    val ingredientsText: String,
    val portionGrams: Double,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val isAzerbaijani: Boolean
)

data class GeneratedMealPlan(
    val id: String,
    val title: String,
    val meals: List<PlannedMeal>,
    val isPreviewFallback: Boolean,
    val note: String
) {
    val totalCalories: Int get() = meals.sumOf { it.calories }
    val totalProtein: Double get() = meals.sumOf { it.proteinG }
    val totalCarbs: Double get() = meals.sumOf { it.carbsG }
    val totalFat: Double get() = meals.sumOf { it.fatG }
}
