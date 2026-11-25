package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AppLanguage
import com.example.domain.model.FoodCategory
import com.example.domain.model.NutritionSourceType

@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["category"]),
        Index(value = ["barcode"]),
        Index(value = ["isFavorite"])
    ]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nameAz: String,
    val nameEn: String,
    val nameRu: String,
    val category: String = FoodCategory.OTHER.name,
    val originType: String = NutritionSourceType.DATABASE.name,
    val defaultServingLabelAz: String = "1 porsiya",
    val defaultServingLabelEn: String = "1 serving",
    val defaultServingLabelRu: String = "1 порция",
    val defaultServingGrams: Double = 100.0,
    val householdMeasuresJson: String = "",
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double = 0.0,
    val sugarPer100g: Double = 0.0,
    val sodiumMgPer100g: Double = 0.0,
    val calciumMgPer100g: Double = 0.0,
    val ironMgPer100g: Double = 0.0,
    val potassiumMgPer100g: Double = 0.0,
    val magnesiumMgPer100g: Double = 0.0,
    val vitaminAMcgPer100g: Double = 0.0,
    val vitaminB12McgPer100g: Double = 0.0,
    val vitaminCMgPer100g: Double = 0.0,
    val vitaminDMcgPer100g: Double = 0.0,
    val barcode: String? = null,
    val isFavorite: Boolean = false
) {
    fun localizedName(language: AppLanguage): String = when (language) {
        AppLanguage.AZ -> nameAz
        AppLanguage.EN -> nameEn
        AppLanguage.RU -> nameRu
    }

    fun localizedServingLabel(language: AppLanguage): String = when (language) {
        AppLanguage.AZ -> defaultServingLabelAz
        AppLanguage.EN -> defaultServingLabelEn
        AppLanguage.RU -> defaultServingLabelRu
    }
}

@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["dateIso"]), Index(value = ["mealType"])]
)
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateIso: String,
    val mealType: String,
    val foodName: String,
    val foodCategory: String,
    val servingGrams: Double,
    val portionLabel: String,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
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
    val vitaminDMcg: Double = 0.0,
    val sourceType: String = NutritionSourceType.DATABASE.name,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["dateIso"])]
)
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateIso: String,
    val amountMl: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["dateIso"])]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateIso: String,
    val weightKg: Double,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exercise_logs",
    indices = [Index(value = ["dateIso"])]
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateIso: String,
    val activityType: String,
    val durationMinutes: Int,
    val estimatedCaloriesBurned: Int,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val servings: Int,
    val ingredientsSummary: String,
    val totalGrams: Double,
    val totalCalories: Double,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val totalFiberG: Double = 0.0,
    val isFavorite: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_meal_plans")
data class SavedMealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val totalCalories: Int,
    val mealsJson: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
