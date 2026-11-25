package com.example.data.repo

import com.example.data.local.DiaryDao
import com.example.data.local.DiaryEntryEntity
import com.example.data.local.ExerciseDao
import com.example.data.local.ExerciseLogEntity
import com.example.data.local.FoodDao
import com.example.data.local.FoodDatabaseSeed
import com.example.data.local.FoodItemEntity
import com.example.data.local.RecipeDao
import com.example.data.local.RecipeEntity
import com.example.data.local.SavedMealPlanDao
import com.example.data.local.SavedMealPlanEntity
import com.example.data.local.WaterDao
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightDao
import com.example.data.local.WeightLogEntity
import com.example.data.remote.OpenFoodFactsApi
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.ExerciseType
import com.example.domain.model.FoodCategory
import com.example.domain.model.MealType
import com.example.domain.model.NutritionSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NutriLifeRepository(
    private val foodDao: FoodDao,
    private val diaryDao: DiaryDao,
    private val waterDao: WaterDao,
    private val weightDao: WeightDao,
    private val exerciseDao: ExerciseDao,
    private val recipeDao: RecipeDao,
    private val savedMealPlanDao: SavedMealPlanDao,
    private val openFoodFactsApi: OpenFoodFactsApi
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (foodDao.countFoods() == 0) {
            foodDao.insertAll(FoodDatabaseSeed.initialFoods())
        }
    }

    fun searchFoods(
        query: String,
        category: FoodCategory?,
        onlyFavorites: Boolean
    ): Flow<List<FoodItemEntity>> = foodDao.searchFoods(
        query = query.trim(),
        category = category?.name,
        onlyFavorites = onlyFavorites
    )

    suspend fun toggleFoodFavorite(foodId: Long, currentFavorite: Boolean) {
        foodDao.setFavorite(foodId, !currentFavorite)
    }

    suspend fun addCustomFood(food: FoodItemEntity): Long = foodDao.insertFood(food)

    fun observeDiaryByDate(dateIso: String): Flow<List<DiaryEntryEntity>> =
        diaryDao.observeByDate(dateIso)

    fun observeDiarySince(daysBack: Long): Flow<List<DiaryEntryEntity>> {
        val fromDate = LocalDate.now().minusDays(daysBack).format(dateFormatter)
        return diaryDao.observeFromDate(fromDate)
    }

    suspend fun logFoodToDiary(
        dateIso: String,
        mealType: MealType,
        food: FoodItemEntity,
        grams: Double,
        portionLabel: String,
        localizedFoodName: String
    ) {
        val entry = DiaryEntryEntity(
            dateIso = dateIso,
            mealType = mealType.name,
            foodName = localizedFoodName,
            foodCategory = food.category,
            servingGrams = grams,
            portionLabel = portionLabel,
            calories = NutritionCalculator.scalePer100g(food.caloriesPer100g, grams),
            proteinG = NutritionCalculator.scalePer100g(food.proteinPer100g, grams),
            carbsG = NutritionCalculator.scalePer100g(food.carbsPer100g, grams),
            fatG = NutritionCalculator.scalePer100g(food.fatPer100g, grams),
            fiberG = NutritionCalculator.scalePer100g(food.fiberPer100g, grams),
            sugarG = NutritionCalculator.scalePer100g(food.sugarPer100g, grams),
            sodiumMg = NutritionCalculator.scalePer100g(food.sodiumMgPer100g, grams),
            calciumMg = NutritionCalculator.scalePer100g(food.calciumMgPer100g, grams),
            ironMg = NutritionCalculator.scalePer100g(food.ironMgPer100g, grams),
            potassiumMg = NutritionCalculator.scalePer100g(food.potassiumMgPer100g, grams),
            magnesiumMg = NutritionCalculator.scalePer100g(food.magnesiumMgPer100g, grams),
            vitaminAMcg = NutritionCalculator.scalePer100g(food.vitaminAMcgPer100g, grams),
            vitaminB12Mcg = NutritionCalculator.scalePer100g(food.vitaminB12McgPer100g, grams),
            vitaminCMg = NutritionCalculator.scalePer100g(food.vitaminCMgPer100g, grams),
            vitaminDMcg = NutritionCalculator.scalePer100g(food.vitaminDMcgPer100g, grams),
            sourceType = food.originType
        )
        diaryDao.insertEntry(entry)
    }

    suspend fun insertDiaryEntry(entry: DiaryEntryEntity) = diaryDao.insertEntry(entry)

    suspend fun updateDiaryEntry(entry: DiaryEntryEntity) = diaryDao.updateEntry(entry)

    suspend fun deleteDiaryEntry(id: Long) = diaryDao.deleteById(id)

    fun observeWaterByDate(dateIso: String): Flow<List<WaterLogEntity>> =
        waterDao.observeByDate(dateIso)

    fun observeWaterSince(daysBack: Long): Flow<List<WaterLogEntity>> {
        val fromDate = LocalDate.now().minusDays(daysBack).format(dateFormatter)
        return waterDao.observeFromDate(fromDate)
    }

    suspend fun addWater(dateIso: String, amountMl: Int) {
        if (amountMl > 0) {
            waterDao.insertLog(WaterLogEntity(dateIso = dateIso, amountMl = amountMl))
        }
    }

    suspend fun deleteWaterLog(id: Long) = waterDao.deleteById(id)

    fun observeWeightHistory(): Flow<List<WeightLogEntity>> = weightDao.observeRecent()

    suspend fun addWeight(dateIso: String, weightKg: Double, note: String = "") {
        weightDao.insertLog(WeightLogEntity(dateIso = dateIso, weightKg = weightKg, note = note))
    }

    suspend fun updateWeight(log: WeightLogEntity) = weightDao.updateLog(log)

    suspend fun deleteWeight(id: Long) = weightDao.deleteById(id)

    fun observeExerciseByDate(dateIso: String): Flow<List<ExerciseLogEntity>> =
        exerciseDao.observeByDate(dateIso)

    fun observeExerciseSince(daysBack: Long): Flow<List<ExerciseLogEntity>> {
        val fromDate = LocalDate.now().minusDays(daysBack).format(dateFormatter)
        return exerciseDao.observeFromDate(fromDate)
    }

    suspend fun addExercise(
        dateIso: String,
        type: ExerciseType,
        durationMinutes: Int,
        weightKg: Double,
        manualCalories: Int? = null,
        note: String = ""
    ) {
        val estimated = manualCalories?.takeIf { it > 0 }
            ?: NutritionCalculator.estimateExerciseCalories(type, durationMinutes, weightKg)
        exerciseDao.insertLog(
            ExerciseLogEntity(
                dateIso = dateIso,
                activityType = type.name,
                durationMinutes = durationMinutes,
                estimatedCaloriesBurned = estimated,
                note = note
            )
        )
    }

    suspend fun deleteExercise(id: Long) = exerciseDao.deleteById(id)

    fun observeRecipes(): Flow<List<RecipeEntity>> = recipeDao.observeAll()

    suspend fun saveRecipe(recipe: RecipeEntity) = recipeDao.insertRecipe(recipe)

    suspend fun deleteRecipe(id: Long) = recipeDao.deleteById(id)

    fun observeSavedMealPlans(): Flow<List<SavedMealPlanEntity>> = savedMealPlanDao.observeAll()

    suspend fun saveMealPlan(plan: SavedMealPlanEntity) = savedMealPlanDao.insertPlan(plan)

    suspend fun deleteSavedMealPlan(id: Long) = savedMealPlanDao.deleteById(id)

    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        val cleaned = barcode.trim()
        if (cleaned.isEmpty()) return@withContext BarcodeLookupResult.NotFound(cleaned)

        // 1. Check local Room database first (works offline)
        val localMatch = foodDao.findByBarcode(cleaned)
        if (localMatch != null) {
            return@withContext BarcodeLookupResult.Found(localMatch, fromOnline = false)
        }

        // 2. Try OpenFoodFacts online database
        return@withContext try {
            val response = openFoodFactsApi.getProduct(cleaned)
            val product = response.product
            if (response.status == 1 && product != null && !product.productName.isNullOrBlank()) {
                val n = product.nutriments
                val kcal = n?.energyKcal100g ?: 0.0
                val protein = n?.proteins100g ?: 0.0
                val carbs = n?.carbohydrates100g ?: 0.0
                val fat = n?.fat100g ?: 0.0
                val item = FoodItemEntity(
                    nameAz = product.productName,
                    nameEn = product.productName,
                    nameRu = product.productName,
                    category = FoodCategory.PACKAGED.name,
                    originType = NutritionSourceType.BARCODE_ONLINE.name,
                    defaultServingLabelAz = product.servingSize ?: "100 q",
                    defaultServingLabelEn = product.servingSize ?: "100 g",
                    defaultServingLabelRu = product.servingSize ?: "100 г",
                    defaultServingGrams = 100.0,
                    caloriesPer100g = kcal,
                    proteinPer100g = protein,
                    carbsPer100g = carbs,
                    fatPer100g = fat,
                    fiberPer100g = n?.fiber100g ?: 0.0,
                    sugarPer100g = n?.sugars100g ?: 0.0,
                    sodiumMgPer100g = (n?.sodiumG100g ?: 0.0) * 1000.0,
                    calciumMgPer100g = (n?.calciumG100g ?: 0.0) * 1000.0,
                    ironMgPer100g = (n?.ironG100g ?: 0.0) * 1000.0,
                    barcode = cleaned
                )
                val insertedId = foodDao.insertFood(item)
                BarcodeLookupResult.Found(item.copy(id = insertedId), fromOnline = true)
            } else {
                BarcodeLookupResult.NotFound(cleaned)
            }
        } catch (e: Exception) {
            BarcodeLookupResult.NetworkOrOfflineError(cleaned, e.localizedMessage ?: "Network unavailable")
        }
    }

    suspend fun getAllDiaryOnce() = diaryDao.getAllEntriesOnce()
    suspend fun getAllWaterOnce() = waterDao.getAllOnce()
    suspend fun getAllWeightOnce() = weightDao.getAllOnce()
    suspend fun getAllExerciseOnce() = exerciseDao.getAllOnce()

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        diaryDao.clearAll()
        waterDao.clearAll()
        weightDao.clearAll()
        exerciseDao.clearAll()
        recipeDao.clearAll()
        savedMealPlanDao.clearAll()
    }
}

sealed interface BarcodeLookupResult {
    data class Found(val food: FoodItemEntity, val fromOnline: Boolean) : BarcodeLookupResult
    data class NotFound(val barcode: String) : BarcodeLookupResult
    data class NetworkOrOfflineError(val barcode: String, val message: String) : BarcodeLookupResult
}
