package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ai.AiNutritionService
import com.example.data.ai.FirebaseAiNutritionService
import com.example.data.local.DiaryEntryEntity
import com.example.data.local.ExerciseLogEntity
import com.example.data.local.FoodItemEntity
import com.example.data.local.NutriLifeDatabase
import com.example.data.local.RecipeEntity
import com.example.data.local.SavedMealPlanEntity
import com.example.data.local.WaterLogEntity
import com.example.data.local.WeightLogEntity
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.remote.OpenFoodFactsClient
import com.example.data.repo.BarcodeLookupResult
import com.example.data.repo.NutriLifeRepository
import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.AiMealScanResult
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ExerciseType
import com.example.domain.model.FoodCategory
import com.example.domain.model.GeneratedMealPlan
import com.example.domain.model.MealType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoals
import com.example.domain.model.NutritionSourceType
import com.example.domain.model.PlannedMeal
import com.example.domain.model.ReminderSettings
import com.example.domain.model.ScannedFoodCandidate
import com.example.domain.model.UserProfile
import com.example.export.AppShareManager
import com.example.export.CsvExportManager
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface UiAsyncState<out T> {
    data object Idle : UiAsyncState<Nothing>
    data object Loading : UiAsyncState<Nothing>
    data class Success<T>(val data: T) : UiAsyncState<T>
    data class Error(val message: String) : UiAsyncState<Nothing>
}

data class RecipeIngredientDraft(
    val food: FoodItemEntity,
    val grams: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NutriLifeDatabase.getInstance(application)
    val prefs = UserPreferencesRepository(application)
    val repository = NutriLifeRepository(
        foodDao = db.foodDao(),
        diaryDao = db.diaryDao(),
        waterDao = db.waterDao(),
        weightDao = db.weightDao(),
        exerciseDao = db.exerciseDao(),
        recipeDao = db.recipeDao(),
        savedMealPlanDao = db.savedMealPlanDao(),
        openFoodFactsApi = OpenFoodFactsClient.api
    )
    val aiService: AiNutritionService = FirebaseAiNutritionService(application)

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FoodCategory?>(null)
    val selectedCategory: StateFlow<FoodCategory?> = _selectedCategory.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _wasAppUpdatedBanner = MutableStateFlow(false)
    val wasAppUpdatedBanner: StateFlow<Boolean> = _wasAppUpdatedBanner.asStateFlow()

    private val _scanImageUri = MutableStateFlow<Uri?>(null)
    val scanImageUri: StateFlow<Uri?> = _scanImageUri.asStateFlow()

    private val _scanState = MutableStateFlow<UiAsyncState<AiMealScanResult>>(UiAsyncState.Idle)
    val scanState: StateFlow<UiAsyncState<AiMealScanResult>> = _scanState.asStateFlow()

    private val _barcodeState = MutableStateFlow<UiAsyncState<BarcodeLookupResult>>(UiAsyncState.Idle)
    val barcodeState: StateFlow<UiAsyncState<BarcodeLookupResult>> = _barcodeState.asStateFlow()

    private val _mealPlanState = MutableStateFlow<UiAsyncState<GeneratedMealPlan>>(UiAsyncState.Idle)
    val mealPlanState: StateFlow<UiAsyncState<GeneratedMealPlan>> = _mealPlanState.asStateFlow()

    private val _insightState = MutableStateFlow<UiAsyncState<String>>(UiAsyncState.Idle)
    val insightState: StateFlow<UiAsyncState<String>> = _insightState.asStateFlow()

    private val _recipeDraftIngredients = MutableStateFlow<List<RecipeIngredientDraft>>(emptyList())
    val recipeDraftIngredients: StateFlow<List<RecipeIngredientDraft>> = _recipeDraftIngredients.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var planVariantCounter = 0

    val userProfile: StateFlow<UserProfile> = prefs.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserProfile())

    val reminderSettings: StateFlow<ReminderSettings> = prefs.reminderSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ReminderSettings())

    val nutritionGoals: StateFlow<NutritionGoals> = userProfile
        .map { NutritionCalculator.calculateDailyGoals(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            NutritionCalculator.calculateDailyGoals(UserProfile())
        )

    val foodResults: StateFlow<List<FoodItemEntity>> = combine(
        _searchQuery,
        _selectedCategory,
        _favoritesOnly
    ) { query, category, favOnly -> Triple(query, category, favOnly) }
        .flatMapLatest { (q, c, f) -> repository.searchFoods(q, c, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val diaryEntriesForSelectedDate: StateFlow<List<DiaryEntryEntity>> = _selectedDate
        .flatMapLatest { date -> repository.observeDiaryByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dailyTotals: StateFlow<NutrientTotals> = diaryEntriesForSelectedDate
        .map { entries ->
            NutritionCalculator.sumNutrients(
                entries.map {
                    NutrientTotals(
                        calories = it.calories,
                        proteinG = it.proteinG,
                        carbsG = it.carbsG,
                        fatG = it.fatG,
                        fiberG = it.fiberG,
                        sugarG = it.sugarG,
                        sodiumMg = it.sodiumMg,
                        calciumMg = it.calciumMg,
                        ironMg = it.ironMg,
                        potassiumMg = it.potassiumMg,
                        magnesiumMg = it.magnesiumMg,
                        vitaminAMcg = it.vitaminAMcg,
                        vitaminB12Mcg = it.vitaminB12Mcg,
                        vitaminCMg = it.vitaminCMg,
                        vitaminDMcg = it.vitaminDMcg
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), NutrientTotals())

    val diaryLast30Days: StateFlow<List<DiaryEntryEntity>> = repository.observeDiarySince(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val waterForSelectedDate: StateFlow<List<WaterLogEntity>> = _selectedDate
        .flatMapLatest { date -> repository.observeWaterByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val waterLast7Days: StateFlow<List<WaterLogEntity>> = repository.observeWaterSince(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val weightHistory: StateFlow<List<WeightLogEntity>> = repository.observeWeightHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val exerciseForSelectedDate: StateFlow<List<ExerciseLogEntity>> = _selectedDate
        .flatMapLatest { date -> repository.observeExerciseByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val exerciseLast30Days: StateFlow<List<ExerciseLogEntity>> = repository.observeExerciseSince(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedRecipes: StateFlow<List<RecipeEntity>> = repository.observeRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedMealPlans: StateFlow<List<SavedMealPlanEntity>> = repository.observeSavedMealPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            checkAppUpdateRecognition()
        }
    }

    private suspend fun checkAppUpdateRecognition() {
        val lastCode = prefs.lastVersionCodeFlow.first()
        val currentCode = BuildConfig.VERSION_CODE
        if (lastCode in 1 until currentCode) {
            _wasAppUpdatedBanner.value = true
        }
        if (lastCode != currentCode) {
            prefs.recordInstalledVersionCode(currentCode)
        }
    }

    fun dismissUpdatedBanner() {
        _wasAppUpdatedBanner.value = false
    }

    fun selectDate(dateIso: String) {
        _selectedDate.value = dateIso
    }

    fun shiftDate(days: Long) {
        val current = runCatching { LocalDate.parse(_selectedDate.value, dateFormatter) }.getOrElse { LocalDate.now() }
        _selectedDate.value = current.plusDays(days).format(dateFormatter)
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setSelectedCategory(cat: FoodCategory?) {
        _selectedCategory.value = cat
    }

    fun toggleFavoritesFilter() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun toggleFoodFavorite(food: FoodItemEntity) {
        viewModelScope.launch {
            repository.toggleFoodFavorite(food.id, food.isFavorite)
        }
    }

    fun saveUserProfile(profile: UserProfile, alsoLogWeight: Boolean = false) {
        viewModelScope.launch {
            prefs.saveUserProfile(profile)
            if (alsoLogWeight) {
                repository.addWeight(LocalDate.now().format(dateFormatter), profile.currentWeightKg, "Profile")
            }
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefs.updateLanguage(language)
            val currentReminders = reminderSettings.value
            ReminderScheduler.syncReminders(getApplication(), currentReminders, language)
        }
    }

    fun updateTheme(themeMode: AppThemeMode) {
        viewModelScope.launch {
            prefs.updateTheme(themeMode)
        }
    }

    fun updateWaterTarget(targetMl: Int) {
        viewModelScope.launch {
            prefs.updateWaterTarget(targetMl)
        }
    }

    fun acceptAiConsent() {
        viewModelScope.launch {
            prefs.updateAiConsent(true)
        }
    }

    fun updateReminders(settings: ReminderSettings) {
        viewModelScope.launch {
            prefs.updateReminders(settings)
            ReminderScheduler.syncReminders(getApplication(), settings, userProfile.value.language)
        }
    }

    fun logFood(food: FoodItemEntity, mealType: MealType, grams: Double, portionLabel: String) {
        viewModelScope.launch {
            val lang = userProfile.value.language
            repository.logFoodToDiary(
                dateIso = _selectedDate.value,
                mealType = mealType,
                food = food,
                grams = grams,
                portionLabel = portionLabel,
                localizedFoodName = food.localizedName(lang)
            )
        }
    }

    fun addCustomFoodAndLog(
        name: String,
        category: FoodCategory,
        kcal100: Double,
        protein100: Double,
        carbs100: Double,
        fat100: Double,
        fiber100: Double,
        sugar100: Double,
        grams: Double,
        mealType: MealType,
        barcode: String? = null
    ) {
        viewModelScope.launch {
            val entity = FoodItemEntity(
                nameAz = name,
                nameEn = name,
                nameRu = name,
                category = category.name,
                originType = NutritionSourceType.USER_ENTERED.name,
                defaultServingLabelAz = "${grams.toInt()} q",
                defaultServingLabelEn = "${grams.toInt()} g",
                defaultServingLabelRu = "${grams.toInt()} г",
                defaultServingGrams = grams,
                caloriesPer100g = kcal100,
                proteinPer100g = protein100,
                carbsPer100g = carbs100,
                fatPer100g = fat100,
                fiberPer100g = fiber100,
                sugarPer100g = sugar100,
                barcode = barcode
            )
            val id = repository.addCustomFood(entity)
            repository.logFoodToDiary(
                dateIso = _selectedDate.value,
                mealType = mealType,
                food = entity.copy(id = id),
                grams = grams,
                portionLabel = "${grams.toInt()} g",
                localizedFoodName = name
            )
        }
    }

    fun updateDiaryEntry(entry: DiaryEntryEntity, newGrams: Double, newMealType: MealType) {
        viewModelScope.launch {
            val factor = if (entry.servingGrams > 0.0) newGrams / entry.servingGrams else 1.0
            repository.updateDiaryEntry(
                entry.copy(
                    mealType = newMealType.name,
                    servingGrams = newGrams,
                    portionLabel = "${newGrams.toInt()} g",
                    calories = entry.calories * factor,
                    proteinG = entry.proteinG * factor,
                    carbsG = entry.carbsG * factor,
                    fatG = entry.fatG * factor,
                    fiberG = entry.fiberG * factor,
                    sugarG = entry.sugarG * factor,
                    sodiumMg = entry.sodiumMg * factor,
                    calciumMg = entry.calciumMg * factor,
                    ironMg = entry.ironMg * factor,
                    potassiumMg = entry.potassiumMg * factor,
                    magnesiumMg = entry.magnesiumMg * factor,
                    vitaminAMcg = entry.vitaminAMcg * factor,
                    vitaminB12Mcg = entry.vitaminB12Mcg * factor,
                    vitaminCMg = entry.vitaminCMg * factor,
                    vitaminDMcg = entry.vitaminDMcg * factor
                )
            )
        }
    }

    fun deleteDiaryEntry(id: Long) {
        viewModelScope.launch { repository.deleteDiaryEntry(id) }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch { repository.addWater(_selectedDate.value, amountMl) }
    }

    fun deleteWaterLog(id: Long) {
        viewModelScope.launch { repository.deleteWaterLog(id) }
    }

    fun addWeight(weightKg: Double, note: String) {
        viewModelScope.launch {
            repository.addWeight(_selectedDate.value, weightKg, note)
            val current = userProfile.value
            prefs.saveUserProfile(current.copy(currentWeightKg = weightKg))
        }
    }

    fun updateWeight(log: WeightLogEntity) {
        viewModelScope.launch { repository.updateWeight(log) }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch { repository.deleteWeight(id) }
    }

    fun addExercise(type: ExerciseType, durationMin: Int, manualCalories: Int?, note: String) {
        viewModelScope.launch {
            repository.addExercise(
                dateIso = _selectedDate.value,
                type = type,
                durationMinutes = durationMin,
                weightKg = userProfile.value.currentWeightKg,
                manualCalories = manualCalories,
                note = note
            )
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch { repository.deleteExercise(id) }
    }

    fun setSharedImageUri(uri: Uri) {
        _scanImageUri.value = uri
    }

    fun analyzeFoodPhoto(uri: Uri) {
        _scanImageUri.value = uri
        viewModelScope.launch {
            _scanState.value = UiAsyncState.Loading
            val lang = userProfile.value.language
            val res = aiService.analyzeFoodImage(uri, lang)
            _scanState.value = res.fold(
                onSuccess = { UiAsyncState.Success(it) },
                onFailure = {
                    UiAsyncState.Error(
                        it.localizedMessage ?: FirebaseAiNutritionService.toUserFriendlyAiErrorMessage(it, lang)
                    )
                }
            )
        }
    }

    fun updateScannedCandidate(updated: ScannedFoodCandidate) {
        val current = (_scanState.value as? UiAsyncState.Success)?.data ?: return
        val replaced = current.items.map { if (it.id == updated.id) updated else it }
        _scanState.value = UiAsyncState.Success(current.copy(items = replaced))
    }

    fun removeScannedCandidate(candidateId: String) {
        val current = (_scanState.value as? UiAsyncState.Success)?.data ?: return
        _scanState.value = UiAsyncState.Success(current.copy(items = current.items.filterNot { it.id == candidateId }))
    }

    fun saveScannedMealToDiary(mealType: MealType) {
        val current = (_scanState.value as? UiAsyncState.Success)?.data ?: return
        viewModelScope.launch {
            current.items.forEach { item ->
                repository.insertDiaryEntry(
                    DiaryEntryEntity(
                        dateIso = _selectedDate.value,
                        mealType = mealType.name,
                        foodName = item.name,
                        foodCategory = item.category.name,
                        servingGrams = item.estimatedGrams,
                        portionLabel = item.portionLabel,
                        calories = item.scaledCalories,
                        proteinG = item.scaledProtein,
                        carbsG = item.scaledCarbs,
                        fatG = item.scaledFat,
                        fiberG = item.scaledFiber,
                        sugarG = item.scaledSugar,
                        sodiumMg = NutritionCalculator.scalePer100g(item.sodiumMgPer100g, item.estimatedGrams),
                        calciumMg = NutritionCalculator.scalePer100g(item.calciumMgPer100g, item.estimatedGrams),
                        ironMg = NutritionCalculator.scalePer100g(item.ironMgPer100g, item.estimatedGrams),
                        potassiumMg = NutritionCalculator.scalePer100g(item.potassiumMgPer100g, item.estimatedGrams),
                        magnesiumMg = NutritionCalculator.scalePer100g(item.magnesiumMgPer100g, item.estimatedGrams),
                        vitaminAMcg = NutritionCalculator.scalePer100g(item.vitaminAMcgPer100g, item.estimatedGrams),
                        vitaminB12Mcg = NutritionCalculator.scalePer100g(item.vitaminB12McgPer100g, item.estimatedGrams),
                        vitaminCMg = NutritionCalculator.scalePer100g(item.vitaminCMgPer100g, item.estimatedGrams),
                        vitaminDMcg = NutritionCalculator.scalePer100g(item.vitaminDMcgPer100g, item.estimatedGrams),
                        sourceType = NutritionSourceType.AI_ESTIMATED.name
                    )
                )
            }
            _scanState.value = UiAsyncState.Idle
        }
    }

    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            _barcodeState.value = UiAsyncState.Loading
            val res = repository.lookupBarcode(code)
            _barcodeState.value = UiAsyncState.Success(res)
        }
    }

    fun resetBarcodeLookup() {
        _barcodeState.value = UiAsyncState.Idle
    }

    fun generateMealPlan() {
        viewModelScope.launch {
            _mealPlanState.value = UiAsyncState.Loading
            val lang = userProfile.value.language
            val res = aiService.generateMealPlan(
                profile = userProfile.value,
                goals = nutritionGoals.value,
                language = lang,
                variantIndex = planVariantCounter++
            )
            _mealPlanState.value = res.fold(
                onSuccess = { UiAsyncState.Success(it) },
                onFailure = {
                    UiAsyncState.Error(
                        it.localizedMessage ?: FirebaseAiNutritionService.toUserFriendlyAiErrorMessage(it, lang)
                    )
                }
            )
        }
    }

    fun requestDailyNutritionInsight() {
        viewModelScope.launch {
            _insightState.value = UiAsyncState.Loading
            val lang = userProfile.value.language
            val mealsSummary = diaryEntriesForSelectedDate.value
                .joinToString(", ") { "${it.foodName} (${it.calories.toInt()} kcal)" }
            val waterMl = waterForSelectedDate.value.sumOf { it.amountMl }
            try {
                aiService.generateDailyNutritionInsightStream(
                    profile = userProfile.value,
                    goals = nutritionGoals.value,
                    totals = dailyTotals.value,
                    waterTodayMl = waterMl,
                    mealsSummary = mealsSummary,
                    language = lang
                ).collect { text ->
                    _insightState.value = UiAsyncState.Success(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _insightState.value = UiAsyncState.Error(
                    e.localizedMessage ?: FirebaseAiNutritionService.toUserFriendlyAiErrorMessage(e, lang)
                )
            }
        }
    }

    fun replacePlannedMeal(mealType: MealType) {
        val current = (_mealPlanState.value as? UiAsyncState.Success)?.data ?: return
        viewModelScope.launch {
            val fresh = aiService.generateMealPlan(
                profile = userProfile.value,
                goals = nutritionGoals.value,
                language = userProfile.value.language,
                variantIndex = planVariantCounter++
            ).getOrNull() ?: return@launch
            val replacement = fresh.meals.firstOrNull { it.mealType == mealType } ?: return@launch
            _mealPlanState.value = UiAsyncState.Success(
                current.copy(
                    meals = current.meals.map { if (it.mealType == mealType) replacement else it }
                )
            )
        }
    }

    fun saveCurrentMealPlan() {
        val current = (_mealPlanState.value as? UiAsyncState.Success)?.data ?: return
        viewModelScope.launch {
            val arr = JSONArray()
            current.meals.forEach { m ->
                arr.put(
                    JSONObject().apply {
                        put("mealType", m.mealType.name)
                        put("title", m.title)
                        put("grams", m.portionGrams)
                        put("calories", m.calories)
                    }
                )
            }
            repository.saveMealPlan(
                SavedMealPlanEntity(
                    title = current.title,
                    totalCalories = current.totalCalories,
                    mealsJson = arr.toString()
                )
            )
        }
    }

    fun deleteSavedPlan(id: Long) {
        viewModelScope.launch { repository.deleteSavedMealPlan(id) }
    }

    fun addRecipeIngredient(food: FoodItemEntity, grams: Double) {
        _recipeDraftIngredients.value = _recipeDraftIngredients.value + RecipeIngredientDraft(food, grams)
    }

    fun removeRecipeIngredient(index: Int) {
        _recipeDraftIngredients.value = _recipeDraftIngredients.value.filterIndexed { i, _ -> i != index }
    }

    fun saveRecipe(name: String, servings: Int) {
        val list = _recipeDraftIngredients.value
        if (name.isBlank() || list.isEmpty()) return
        val lang = userProfile.value.language
        val totalGrams = list.sumOf { it.grams }
        val totalKcal = list.sumOf { NutritionCalculator.scalePer100g(it.food.caloriesPer100g, it.grams) }
        val totalProtein = list.sumOf { NutritionCalculator.scalePer100g(it.food.proteinPer100g, it.grams) }
        val totalCarbs = list.sumOf { NutritionCalculator.scalePer100g(it.food.carbsPer100g, it.grams) }
        val totalFat = list.sumOf { NutritionCalculator.scalePer100g(it.food.fatPer100g, it.grams) }
        val totalFiber = list.sumOf { NutritionCalculator.scalePer100g(it.food.fiberPer100g, it.grams) }
        val summary = list.joinToString(", ") { "${it.food.localizedName(lang)} (${it.grams.toInt()}g)" }

        viewModelScope.launch {
            repository.saveRecipe(
                RecipeEntity(
                    name = name.trim(),
                    servings = servings.coerceAtLeast(1),
                    ingredientsSummary = summary,
                    totalGrams = totalGrams,
                    totalCalories = totalKcal,
                    totalProteinG = totalProtein,
                    totalCarbsG = totalCarbs,
                    totalFatG = totalFat,
                    totalFiberG = totalFiber
                )
            )
            _recipeDraftIngredients.value = emptyList()
        }
    }

    fun logRecipeServing(recipe: RecipeEntity, mealType: MealType) {
        val servings = recipe.servings.coerceAtLeast(1)
        viewModelScope.launch {
            repository.insertDiaryEntry(
                DiaryEntryEntity(
                    dateIso = _selectedDate.value,
                    mealType = mealType.name,
                    foodName = recipe.name,
                    foodCategory = FoodCategory.OTHER.name,
                    servingGrams = recipe.totalGrams / servings,
                    portionLabel = "1/$servings",
                    calories = recipe.totalCalories / servings,
                    proteinG = recipe.totalProteinG / servings,
                    carbsG = recipe.totalCarbsG / servings,
                    fatG = recipe.totalFatG / servings,
                    fiberG = recipe.totalFiberG / servings,
                    sourceType = NutritionSourceType.ESTIMATED_RECIPE.name
                )
            )
        }
    }

    fun deleteRecipe(id: Long) {
        viewModelScope.launch { repository.deleteRecipe(id) }
    }

    suspend fun exportCsv(): File {
        return CsvExportManager.exportNutritionCsv(
            context = getApplication(),
            diary = repository.getAllDiaryOnce(),
            water = repository.getAllWaterOnce(),
            weight = repository.getAllWeightOnce(),
            exercise = repository.getAllExerciseOnce()
        )
    }

    suspend fun prepareShareableApkUri(): Uri? {
        return AppShareManager.prepareShareableApkUri(getApplication())
    }

    fun deleteAllPersonalData() {
        viewModelScope.launch {
            repository.clearAllUserData()
            ReminderScheduler.cancelAll(getApplication())
            prefs.clearAllPreferences()
        }
    }
}
