package com.example

import com.example.domain.calc.NutritionCalculator
import com.example.domain.model.ActivityLevel
import com.example.domain.model.BiologicalSex
import com.example.domain.model.ExerciseType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoal
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun adultMale_mifflinStJeorAndGoalAdjustment_isAccurate() {
        val profile = UserProfile(
            age = 30,
            heightCm = 180,
            currentWeightKg = 80.0,
            sex = BiologicalSex.MALE,
            activityLevel = ActivityLevel.MODERATE,
            nutritionGoal = NutritionGoal.MAINTAIN
        )
        val goals = NutritionCalculator.calculateDailyGoals(profile)

        // BMR = 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780, goals.bmrKcal)
        // TDEE = 1780 * 1.55 = 2759
        assertEquals(2759, goals.tdeeKcal)
        assertEquals(2759, goals.calories)
        assertFalse(goals.isMinorProtectionActive)
        assertEquals(NutritionGoal.MAINTAIN, goals.effectiveGoal)
    }

    @Test
    fun minorUnder18_weightLossDeficitIsBlockedAndSafeFloorApplied() {
        val minorProfile = UserProfile(
            age = 15,
            heightCm = 162,
            currentWeightKg = 55.0,
            sex = BiologicalSex.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            nutritionGoal = NutritionGoal.GRADUAL_LOSS
        )
        val goals = NutritionCalculator.calculateDailyGoals(minorProfile)

        assertTrue(goals.isMinorProtectionActive)
        assertEquals(NutritionGoal.IMPROVE_HABITS, goals.effectiveGoal)
        assertTrue(goals.calories >= 1600)
    }

    @Test
    fun scalingAndSummingNutrients_handlesMacrosAndMicros() {
        val scaledCalories = NutritionCalculator.scalePer100g(250.0, 200.0)
        assertEquals(500.0, scaledCalories, 0.001)

        val summed = NutritionCalculator.sumNutrients(
            listOf(
                NutrientTotals(calories = 300.0, proteinG = 20.0, ironMg = 2.5, vitaminCMg = 15.0),
                NutrientTotals(calories = 450.0, proteinG = 25.0, ironMg = 3.0, vitaminCMg = 25.0)
            )
        )
        assertEquals(750.0, summed.calories, 0.001)
        assertEquals(45.0, summed.proteinG, 0.001)
        assertEquals(5.5, summed.ironMg, 0.001)
        assertEquals(40.0, summed.vitaminCMg, 0.001)
    }

    @Test
    fun exerciseEstimate_usesMetWithoutReturningZeroForValidDuration() {
        val burned = NutritionCalculator.estimateExerciseCalories(
            exerciseType = ExerciseType.RUNNING,
            durationMinutes = 30,
            weightKg = 70.0
        )
        // 8.5 * 70 * 0.5 = 297.5 -> 298
        assertEquals(298, burned)
        assertEquals(0, NutritionCalculator.estimateExerciseCalories(ExerciseType.WALKING, 0, 70.0))
    }
}
