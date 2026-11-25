package com.example

import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.AppLanguage
import com.example.domain.model.BiologicalSex
import com.example.domain.model.NutritionGoal
import com.example.domain.model.NutritionSourceType
import com.example.domain.model.UserProfile
import com.example.i18n.AzStrings
import com.example.i18n.EnStrings
import com.example.i18n.RuStrings
import com.example.i18n.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun minorGoalProtection_disablesWeightLossDeficit() {
        val goals = NutritionCalculator.calculateDailyGoals(
            UserProfile(
                age = 16,
                heightCm = 168,
                currentWeightKg = 60.0,
                sex = BiologicalSex.MALE,
                activityLevel = ActivityLevel.MODERATE,
                nutritionGoal = NutritionGoal.GRADUAL_LOSS
            )
        )
        assertTrue(goals.isMinorProtectionActive)
        assertEquals(NutritionGoal.IMPROVE_HABITS, goals.effectiveGoal)
        assertTrue(goals.calories >= 1600)
    }

    @Test
    fun allLanguagesAndSourceLabels_areComplete() {
        AppLanguage.entries.forEach { lang ->
            val strings = stringsFor(lang)
            assertTrue(strings.minorSafetyWarning.isNotBlank())
            assertTrue(strings.hiddenIngredientsWarning.isNotBlank())
            NutritionSourceType.entries.forEach { src ->
                assertTrue(strings.sourceName(src).isNotBlank())
            }
        }
        assertEquals("NutriLife AI", AzStrings.appName)
        assertEquals("NutriLife AI", EnStrings.appName)
        assertEquals("NutriLife AI", RuStrings.appName)
    }
}
