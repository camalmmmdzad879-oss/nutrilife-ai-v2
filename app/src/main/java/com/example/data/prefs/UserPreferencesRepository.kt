package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ActivityLevel
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BiologicalSex
import com.example.domain.model.NutritionGoal
import com.example.domain.model.ReminderSettings
import com.example.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrilife_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val AGE = intPreferencesKey("user_age")
        val HEIGHT_CM = intPreferencesKey("user_height_cm")
        val CURRENT_WEIGHT_KG = doublePreferencesKey("current_weight_kg")
        val GOAL_WEIGHT_KG = doublePreferencesKey("goal_weight_kg")
        val SEX = stringPreferencesKey("biological_sex")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val NUTRITION_GOAL = stringPreferencesKey("nutrition_goal")
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WATER_TARGET_ML = intPreferencesKey("water_target_ml")
        val DIETARY_PREFS = stringPreferencesKey("dietary_prefs")
        val ALLERGIES = stringPreferencesKey("allergies")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val AI_CONSENT = booleanPreferencesKey("ai_consent")

        val REMINDER_MEALS = booleanPreferencesKey("reminder_meals")
        val REMINDER_WATER = booleanPreferencesKey("reminder_water")
        val REMINDER_WEIGHT = booleanPreferencesKey("reminder_weight")
        val REMINDER_DIARY = booleanPreferencesKey("reminder_diary")
        val REMINDER_MEAL_HOUR = intPreferencesKey("reminder_meal_hour")
        val REMINDER_WATER_INTERVAL = intPreferencesKey("reminder_water_interval")
        val LAST_VERSION_CODE = intPreferencesKey("last_installed_version_code")
    }

    val lastVersionCodeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_VERSION_CODE] ?: 0
    }

    suspend fun recordInstalledVersionCode(versionCode: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_VERSION_CODE] = versionCode
        }
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            age = prefs[Keys.AGE] ?: 26,
            heightCm = prefs[Keys.HEIGHT_CM] ?: 170,
            currentWeightKg = prefs[Keys.CURRENT_WEIGHT_KG] ?: 70.0,
            goalWeightKg = prefs[Keys.GOAL_WEIGHT_KG] ?: 68.0,
            sex = BiologicalSex.fromName(prefs[Keys.SEX]),
            activityLevel = ActivityLevel.fromName(prefs[Keys.ACTIVITY_LEVEL]),
            nutritionGoal = NutritionGoal.fromName(prefs[Keys.NUTRITION_GOAL]),
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = AppThemeMode.fromName(prefs[Keys.THEME_MODE]),
            dailyWaterTargetMl = prefs[Keys.WATER_TARGET_ML] ?: 2200,
            dietaryPreferences = prefs[Keys.DIETARY_PREFS] ?: "",
            allergies = prefs[Keys.ALLERGIES] ?: "",
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            aiConsentAccepted = prefs[Keys.AI_CONSENT] ?: false
        )
    }

    val reminderSettingsFlow: Flow<ReminderSettings> = context.dataStore.data.map { prefs ->
        ReminderSettings(
            mealRemindersEnabled = prefs[Keys.REMINDER_MEALS] ?: false,
            waterRemindersEnabled = prefs[Keys.REMINDER_WATER] ?: false,
            weightRemindersEnabled = prefs[Keys.REMINDER_WEIGHT] ?: false,
            diaryRemindersEnabled = prefs[Keys.REMINDER_DIARY] ?: false,
            mealHour = prefs[Keys.REMINDER_MEAL_HOUR] ?: 13,
            waterIntervalHours = prefs[Keys.REMINDER_WATER_INTERVAL] ?: 3
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.AGE] = profile.age
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.CURRENT_WEIGHT_KG] = profile.currentWeightKg
            prefs[Keys.GOAL_WEIGHT_KG] = profile.goalWeightKg
            prefs[Keys.SEX] = profile.sex.name
            prefs[Keys.ACTIVITY_LEVEL] = profile.activityLevel.name
            prefs[Keys.NUTRITION_GOAL] = profile.nutritionGoal.name
            prefs[Keys.LANGUAGE] = profile.language.code
            prefs[Keys.THEME_MODE] = profile.themeMode.name
            prefs[Keys.WATER_TARGET_ML] = profile.dailyWaterTargetMl
            prefs[Keys.DIETARY_PREFS] = profile.dietaryPreferences
            prefs[Keys.ALLERGIES] = profile.allergies
            prefs[Keys.ONBOARDING_DONE] = profile.onboardingCompleted
            prefs[Keys.AI_CONSENT] = profile.aiConsentAccepted
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun updateTheme(themeMode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateWaterTarget(targetMl: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WATER_TARGET_ML] = targetMl.coerceIn(800, 6000)
        }
    }

    suspend fun updateAiConsent(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_CONSENT] = accepted
        }
    }

    suspend fun updateReminders(settings: ReminderSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_MEALS] = settings.mealRemindersEnabled
            prefs[Keys.REMINDER_WATER] = settings.waterRemindersEnabled
            prefs[Keys.REMINDER_WEIGHT] = settings.weightRemindersEnabled
            prefs[Keys.REMINDER_DIARY] = settings.diaryRemindersEnabled
            prefs[Keys.REMINDER_MEAL_HOUR] = settings.mealHour
            prefs[Keys.REMINDER_WATER_INTERVAL] = settings.waterIntervalHours
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
