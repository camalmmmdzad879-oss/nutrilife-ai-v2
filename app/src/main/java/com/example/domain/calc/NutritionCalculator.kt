package com.example.domain.calc

import com.example.domain.model.ActivityLevel
import com.example.domain.model.BiologicalSex
import com.example.domain.model.ExerciseType
import com.example.domain.model.NutrientTotals
import com.example.domain.model.NutritionGoal
import com.example.domain.model.NutritionGoals
import com.example.domain.model.UserProfile
import kotlin.math.roundToInt

object NutritionCalculator {

    private const val MIN_FEMALE_KCAL = 1200
    private const val MIN_MALE_KCAL = 1500
    private const val MAX_SAFE_KCAL = 4500

    /**
     * Computes daily energy & macronutrient targets using the Mifflin-St Jeor equation.
     * Enforces minor safety protection (< 18 years): weight-loss calorie deficits are disabled
     * and replaced with balanced growth-maintenance targets.
     */
    fun calculateDailyGoals(profile: UserProfile): NutritionGoals {
        val safeAge = profile.age.coerceIn(10, 110)
        val safeHeight = profile.heightCm.coerceIn(100, 240)
        val safeWeight = profile.currentWeightKg.coerceIn(25.0, 300.0)

        val sexOffset = if (profile.sex == BiologicalSex.MALE) 5.0 else -161.0
        val bmr = (10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge) + sexOffset
        val tdee = bmr * profile.activityLevel.factor

        val isMinor = safeAge < 18
        val effectiveGoal = if (isMinor && profile.nutritionGoal == NutritionGoal.GRADUAL_LOSS) {
            NutritionGoal.IMPROVE_HABITS
        } else {
            profile.nutritionGoal
        }

        val rawTarget = when (effectiveGoal) {
            NutritionGoal.MAINTAIN -> tdee
            NutritionGoal.IMPROVE_HABITS -> tdee
            NutritionGoal.GRADUAL_LOSS -> (tdee - 350.0).coerceAtLeast(
                if (profile.sex == BiologicalSex.MALE) MIN_MALE_KCAL.toDouble() else MIN_FEMALE_KCAL.toDouble()
            )
            NutritionGoal.GRADUAL_GAIN -> tdee + 300.0
        }

        val minFloor = when {
            isMinor -> 1600
            profile.sex == BiologicalSex.MALE -> MIN_MALE_KCAL
            else -> MIN_FEMALE_KCAL
        }
        val targetCalories = rawTarget.roundToInt().coerceIn(minFloor, MAX_SAFE_KCAL)

        val proteinPerKg = when (effectiveGoal) {
            NutritionGoal.GRADUAL_LOSS -> 1.6
            NutritionGoal.GRADUAL_GAIN -> 1.6
            NutritionGoal.MAINTAIN, NutritionGoal.IMPROVE_HABITS -> 1.3
        }
        val proteinG = (safeWeight * proteinPerKg).roundToInt().coerceIn(45, 220)
        val fatG = ((targetCalories * 0.28) / 9.0).roundToInt().coerceIn(35, 140)
        val remainingCarbsKcal = (targetCalories - (proteinG * 4) - (fatG * 9)).coerceAtLeast(targetCalories / 4)
        val carbsG = (remainingCarbsKcal / 4.0).roundToInt().coerceAtLeast(80)
        val fiberG = ((targetCalories / 1000.0) * 14.0).roundToInt().coerceIn(20, 45)
        val sugarLimitG = ((targetCalories * 0.10) / 4.0).roundToInt().coerceIn(25, 70)

        val recommendedWater = (safeWeight * 32.0).roundToInt().coerceIn(1600, 3500)
        val waterTarget = if (profile.dailyWaterTargetMl in 800..6000) {
            profile.dailyWaterTargetMl
        } else {
            recommendedWater
        }

        return NutritionGoals(
            calories = targetCalories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            fiberG = fiberG,
            sugarLimitG = sugarLimitG,
            sodiumLimitMg = 2300,
            waterMl = waterTarget,
            isMinorProtectionActive = isMinor,
            effectiveGoal = effectiveGoal,
            bmrKcal = bmr.roundToInt(),
            tdeeKcal = tdee.roundToInt()
        )
    }

    fun recommendedWaterMl(weightKg: Double): Int {
        return (weightKg.coerceIn(30.0, 250.0) * 32.0).roundToInt().coerceIn(1600, 3500)
    }

    fun scalePer100g(per100gValue: Double, grams: Double): Double {
        if (grams <= 0.0 || per100gValue <= 0.0) return 0.0
        return (per100gValue * grams) / 100.0
    }

    fun estimateExerciseCalories(
        exerciseType: ExerciseType,
        durationMinutes: Int,
        weightKg: Double
    ): Int {
        if (durationMinutes <= 0) return 0
        val safeWeight = weightKg.coerceIn(30.0, 250.0)
        val hours = durationMinutes.coerceIn(1, 720) / 60.0
        return (exerciseType.metValue * safeWeight * hours).roundToInt().coerceAtLeast(1)
    }

    fun sumNutrients(items: List<NutrientTotals>): NutrientTotals {
        if (items.isEmpty()) return NutrientTotals()
        return items.fold(NutrientTotals()) { acc, item ->
            NutrientTotals(
                calories = acc.calories + item.calories,
                proteinG = acc.proteinG + item.proteinG,
                carbsG = acc.carbsG + item.carbsG,
                fatG = acc.fatG + item.fatG,
                fiberG = acc.fiberG + item.fiberG,
                sugarG = acc.sugarG + item.sugarG,
                sodiumMg = acc.sodiumMg + item.sodiumMg,
                calciumMg = acc.calciumMg + item.calciumMg,
                ironMg = acc.ironMg + item.ironMg,
                potassiumMg = acc.potassiumMg + item.potassiumMg,
                magnesiumMg = acc.magnesiumMg + item.magnesiumMg,
                vitaminAMcg = acc.vitaminAMcg + item.vitaminAMcg,
                vitaminB12Mcg = acc.vitaminB12Mcg + item.vitaminB12Mcg,
                vitaminCMg = acc.vitaminCMg + item.vitaminCMg,
                vitaminDMcg = acc.vitaminDMcg + item.vitaminDMcg
            )
        }
    }
}
