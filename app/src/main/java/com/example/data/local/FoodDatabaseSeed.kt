package com.example.data.local

import com.example.domain.model.FoodCategory
import com.example.domain.model.HouseholdMeasure
import com.example.domain.model.NutritionSourceType
import org.json.JSONArray
import org.json.JSONObject

object FoodDatabaseSeed {

    private fun measuresJson(vararg pairs: Triple<String, String, Double>): String {
        // Triple(az/en/ru packed as "az|en|ru", "", grams) or use explicit helper
        val array = JSONArray()
        pairs.forEach { (az, enRu, grams) ->
            val parts = enRu.split("|")
            val en = parts.getOrElse(0) { az }
            val ru = parts.getOrElse(1) { en }
            array.put(
                JSONObject().apply {
                    put("az", az)
                    put("en", en)
                    put("ru", ru)
                    put("g", grams)
                }
            )
        }
        return array.toString()
    }

    fun parseMeasures(json: String): List<HouseholdMeasure> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HouseholdMeasure(
                    labelAz = obj.getString("az"),
                    labelEn = obj.getString("en"),
                    labelRu = obj.getString("ru"),
                    grams = obj.getDouble("g")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun initialFoods(): List<FoodItemEntity> = listOf(
        // --- AZERBAIJANI CUISINE ---
        FoodItemEntity(
            nameAz = "Şah Plov (düyü, ət, şabalıd)",
            nameEn = "Shah Plov (Saffron Rice Pilaf)",
            nameRu = "Шах-плов (рис с мясом и каштанами)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 boşqab (250 q)",
            defaultServingLabelEn = "1 plate (250 g)",
            defaultServingLabelRu = "1 тарелка (250 г)",
            defaultServingGrams = 250.0,
            householdMeasuresJson = measuresJson(
                Triple("1 boşqab", "1 plate|1 тарелка", 250.0),
                Triple("1/2 boşqab", "1/2 plate|1/2 тарелки", 125.0),
                Triple("1 xörək qaşığı", "1 tbsp|1 ст.л.", 20.0)
            ),
            caloriesPer100g = 198.0, proteinPer100g = 6.2, carbsPer100g = 24.5, fatPer100g = 8.4,
            fiberPer100g = 1.4, sugarPer100g = 2.8, sodiumMgPer100g = 340.0,
            calciumMgPer100g = 22.0, ironMgPer100g = 1.2, potassiumMgPer100g = 145.0,
            magnesiumMgPer100g = 18.0, vitaminAMcgPer100g = 45.0, vitaminB12McgPer100g = 0.6
        ),
        FoodItemEntity(
            nameAz = "Yarpaq Dolması",
            nameEn = "Grape Leaf Dolma (Yarpaq Dolması)",
            nameRu = "Долма в виноградных листьях",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 porsiya (8 ədəd, 220 q)",
            defaultServingLabelEn = "1 serving (8 pcs, 220 g)",
            defaultServingLabelRu = "1 порция (8 шт., 220 г)",
            defaultServingGrams = 220.0,
            householdMeasuresJson = measuresJson(
                Triple("1 porsiya (8 ədəd)", "1 serving (8 pcs)|1 порция (8 шт.)", 220.0),
                Triple("1 ədəd", "1 piece|1 штука", 27.5)
            ),
            caloriesPer100g = 162.0, proteinPer100g = 8.4, carbsPer100g = 11.2, fatPer100g = 9.2,
            fiberPer100g = 1.8, sugarPer100g = 1.1, sodiumMgPer100g = 390.0,
            calciumMgPer100g = 48.0, ironMgPer100g = 1.7, potassiumMgPer100g = 190.0,
            magnesiumMgPer100g = 22.0, vitaminAMcgPer100g = 95.0, vitaminB12McgPer100g = 0.9, vitaminCMgPer100g = 4.5
        ),
        FoodItemEntity(
            nameAz = "Üç Bacı Dolması (badımcan, pomidor, bibər)",
            nameEn = "Three Sisters Dolma (Stuffed Vegetables)",
            nameRu = "Долма Три Сестры (баклажан, томат, перец)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "3 ədəd (300 q)",
            defaultServingLabelEn = "3 pieces (300 g)",
            defaultServingLabelRu = "3 штуки (300 г)",
            defaultServingGrams = 300.0,
            householdMeasuresJson = measuresJson(
                Triple("3 ədəd", "3 pieces|3 штуки", 300.0),
                Triple("1 ədəd", "1 piece|1 штука", 100.0)
            ),
            caloriesPer100g = 128.0, proteinPer100g = 6.9, carbsPer100g = 6.8, fatPer100g = 8.1,
            fiberPer100g = 2.2, sugarPer100g = 2.6, sodiumMgPer100g = 330.0,
            calciumMgPer100g = 24.0, ironMgPer100g = 1.3, potassiumMgPer100g = 240.0,
            magnesiumMgPer100g = 19.0, vitaminAMcgPer100g = 68.0, vitaminCMgPer100g = 18.0
        ),
        FoodItemEntity(
            nameAz = "Düşbərə (bulyonlu)",
            nameEn = "Dushbara (Mini Dumpling Soup)",
            nameRu = "Дюшбара (мини-пельмени в бульоне)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 kasa (320 q)",
            defaultServingLabelEn = "1 bowl (320 g)",
            defaultServingLabelRu = "1 пиала (320 г)",
            defaultServingGrams = 320.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 bowl|1 пиала", 320.0),
                Triple("1/2 kasa", "1/2 bowl|1/2 пиалы", 160.0)
            ),
            caloriesPer100g = 116.0, proteinPer100g = 6.5, carbsPer100g = 12.8, fatPer100g = 4.2,
            fiberPer100g = 0.7, sugarPer100g = 0.6, sodiumMgPer100g = 410.0,
            calciumMgPer100g = 14.0, ironMgPer100g = 1.1, potassiumMgPer100g = 120.0,
            vitaminB12McgPer100g = 0.5
        ),
        FoodItemEntity(
            nameAz = "Göy Qutabı",
            nameEn = "Herb Qutab (Göy Qutabı)",
            nameRu = "Кутаб с зеленью",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "2 ədəd (180 q)",
            defaultServingLabelEn = "2 pieces (180 g)",
            defaultServingLabelRu = "2 штуки (180 г)",
            defaultServingGrams = 180.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 piece|1 штука", 90.0),
                Triple("2 ədəd", "2 pieces|2 штуки", 180.0)
            ),
            caloriesPer100g = 218.0, proteinPer100g = 5.6, carbsPer100g = 29.4, fatPer100g = 8.5,
            fiberPer100g = 2.6, sugarPer100g = 1.2, sodiumMgPer100g = 360.0,
            calciumMgPer100g = 72.0, ironMgPer100g = 1.9, potassiumMgPer100g = 230.0,
            vitaminAMcgPer100g = 180.0, vitaminCMgPer100g = 11.0
        ),
        FoodItemEntity(
            nameAz = "Ət Qutabı",
            nameEn = "Meat Qutab (Ət Qutabı)",
            nameRu = "Кутаб с мясом",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "2 ədəd (210 q)",
            defaultServingLabelEn = "2 pieces (210 g)",
            defaultServingLabelRu = "2 штуки (210 г)",
            defaultServingGrams = 210.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 piece|1 штука", 105.0),
                Triple("2 ədəd", "2 pieces|2 штуки", 210.0)
            ),
            caloriesPer100g = 256.0, proteinPer100g = 10.8, carbsPer100g = 26.0, fatPer100g = 12.1,
            fiberPer100g = 1.3, sugarPer100g = 1.5, sodiumMgPer100g = 420.0,
            ironMgPer100g = 1.8, vitaminB12McgPer100g = 0.8
        ),
        FoodItemEntity(
            nameAz = "Balqabaq Qutabı",
            nameEn = "Pumpkin Qutab",
            nameRu = "Кутаб с тыквой",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "2 ədəd (200 q)",
            defaultServingLabelEn = "2 pieces (200 g)",
            defaultServingLabelRu = "2 штуки (200 г)",
            defaultServingGrams = 200.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 piece|1 штука", 100.0),
                Triple("2 ədəd", "2 pieces|2 штуки", 200.0)
            ),
            caloriesPer100g = 212.0, proteinPer100g = 4.4, carbsPer100g = 32.5, fatPer100g = 7.2,
            fiberPer100g = 2.0, sugarPer100g = 4.8, sodiumMgPer100g = 290.0,
            vitaminAMcgPer100g = 220.0, potassiumMgPer100g = 185.0
        ),
        FoodItemEntity(
            nameAz = "Şəki Piti (quzu əti, noxud)",
            nameEn = "Sheki Piti (Lamb & Chickpea Stew)",
            nameRu = "Шекинский пити (баранина с нутом)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 küpə (350 q)",
            defaultServingLabelEn = "1 clay pot (350 g)",
            defaultServingLabelRu = "1 горшочек (350 г)",
            defaultServingGrams = 350.0,
            householdMeasuresJson = measuresJson(
                Triple("1 küpə", "1 clay pot|1 горшочек", 350.0)
            ),
            caloriesPer100g = 148.0, proteinPer100g = 9.2, carbsPer100g = 8.6, fatPer100g = 8.4,
            fiberPer100g = 2.1, sugarPer100g = 1.0, sodiumMgPer100g = 380.0,
            ironMgPer100g = 1.6, potassiumMgPer100g = 210.0, magnesiumMgPer100g = 24.0, vitaminB12McgPer100g = 1.1
        ),
        FoodItemEntity(
            nameAz = "Küftə Bozbaş",
            nameEn = "Kufta Bozbash (Meatball Soup)",
            nameRu = "Кюфта-бозбаш",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 kasa (350 q)",
            defaultServingLabelEn = "1 bowl (350 g)",
            defaultServingLabelRu = "1 порция (350 г)",
            defaultServingGrams = 350.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 bowl|1 порция", 350.0)
            ),
            caloriesPer100g = 122.0, proteinPer100g = 7.8, carbsPer100g = 8.9, fatPer100g = 6.1,
            fiberPer100g = 1.8, sugarPer100g = 1.1, sodiumMgPer100g = 370.0,
            ironMgPer100g = 1.4, potassiumMgPer100g = 230.0, vitaminB12McgPer100g = 0.8
        ),
        FoodItemEntity(
            nameAz = "Dovğa",
            nameEn = "Dovga (Azerbaijani Yogurt Herb Soup)",
            nameRu = "Довга (кисломолочный суп с зеленью)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 kasa (250 q)",
            defaultServingLabelEn = "1 bowl (250 g)",
            defaultServingLabelRu = "1 пиала (250 г)",
            defaultServingGrams = 250.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 bowl|1 пиала", 250.0),
                Triple("1 stəkan", "1 glass|1 стакан", 200.0)
            ),
            caloriesPer100g = 64.0, proteinPer100g = 3.2, carbsPer100g = 6.4, fatPer100g = 2.8,
            fiberPer100g = 0.8, sugarPer100g = 2.4, sodiumMgPer100g = 220.0,
            calciumMgPer100g = 96.0, potassiumMgPer100g = 155.0, vitaminAMcgPer100g = 70.0, vitaminB12McgPer100g = 0.3
        ),
        FoodItemEntity(
            nameAz = "Xaş",
            nameEn = "Khash (Slow-Simmered Broth)",
            nameRu = "Хаш (наваристый бульон)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 kasa (350 q)",
            defaultServingLabelEn = "1 bowl (350 g)",
            defaultServingLabelRu = "1 тарелка (350 г)",
            defaultServingGrams = 350.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 bowl|1 тарелка", 350.0)
            ),
            caloriesPer100g = 132.0, proteinPer100g = 14.2, carbsPer100g = 0.5, fatPer100g = 8.0,
            fiberPer100g = 0.0, sugarPer100g = 0.0, sodiumMgPer100g = 310.0,
            calciumMgPer100g = 28.0, ironMgPer100g = 0.9, vitaminB12McgPer100g = 0.7
        ),
        FoodItemEntity(
            nameAz = "Göyərti Küküsü",
            nameEn = "Herb Kuku (Azerbaijani Herb Omelette)",
            nameRu = "Кюкю с зеленью",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 dilim (150 q)",
            defaultServingLabelEn = "1 slice (150 g)",
            defaultServingLabelRu = "1 кусочек (150 г)",
            defaultServingGrams = 150.0,
            householdMeasuresJson = measuresJson(
                Triple("1 dilim", "1 slice|1 кусочек", 150.0),
                Triple("2 dilim", "2 slices|2 кусочка", 300.0)
            ),
            caloriesPer100g = 178.0, proteinPer100g = 9.4, carbsPer100g = 3.8, fatPer100g = 13.8,
            fiberPer100g = 1.9, sugarPer100g = 1.1, sodiumMgPer100g = 340.0,
            calciumMgPer100g = 82.0, ironMgPer100g = 2.2, potassiumMgPer100g = 270.0,
            vitaminAMcgPer100g = 240.0, vitaminB12McgPer100g = 0.7, vitaminCMgPer100g = 16.0, vitaminDMcgPer100g = 1.1
        ),
        FoodItemEntity(
            nameAz = "Lülə Kabab",
            nameEn = "Lula Kebab (Minced Meat Skewer)",
            nameRu = "Люля-кебаб",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 şiş (160 q)",
            defaultServingLabelEn = "1 skewer (160 g)",
            defaultServingLabelRu = "1 шампур (160 г)",
            defaultServingGrams = 160.0,
            householdMeasuresJson = measuresJson(
                Triple("1 şiş", "1 skewer|1 шампур", 160.0)
            ),
            caloriesPer100g = 264.0, proteinPer100g = 17.8, carbsPer100g = 2.4, fatPer100g = 20.5,
            fiberPer100g = 0.3, sugarPer100g = 0.7, sodiumMgPer100g = 490.0,
            ironMgPer100g = 2.1, potassiumMgPer100g = 260.0, vitaminB12McgPer100g = 2.0
        ),
        FoodItemEntity(
            nameAz = "Tikə Kabab (quzu əti)",
            nameEn = "Tika Kebab (Lamb Shish Kebab)",
            nameRu = "Тике-кебаб (шашлык из баранины)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 porsiya (180 q)",
            defaultServingLabelEn = "1 serving (180 g)",
            defaultServingLabelRu = "1 порция (180 г)",
            defaultServingGrams = 180.0,
            householdMeasuresJson = measuresJson(
                Triple("1 porsiya", "1 serving|1 порция", 180.0)
            ),
            caloriesPer100g = 242.0, proteinPer100g = 20.5, carbsPer100g = 1.0, fatPer100g = 17.2,
            sodiumMgPer100g = 430.0, ironMgPer100g = 2.0, potassiumMgPer100g = 285.0,
            vitaminB12McgPer100g = 2.2
        ),
        FoodItemEntity(
            nameAz = "Toyuq Kababı",
            nameEn = "Chicken Kebab (Toyuq Kababı)",
            nameRu = "Куриный шашлык",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 porsiya (180 q)",
            defaultServingLabelEn = "1 serving (180 g)",
            defaultServingLabelRu = "1 порция (180 г)",
            defaultServingGrams = 180.0,
            householdMeasuresJson = measuresJson(
                Triple("1 porsiya", "1 serving|1 порция", 180.0)
            ),
            caloriesPer100g = 176.0, proteinPer100g = 23.4, carbsPer100g = 1.5, fatPer100g = 8.4,
            sodiumMgPer100g = 410.0, ironMgPer100g = 1.0, potassiumMgPer100g = 290.0,
            magnesiumMgPer100g = 26.0, vitaminB12McgPer100g = 0.4
        ),
        FoodItemEntity(
            nameAz = "Mərci Şorbası",
            nameEn = "Red Lentil Soup (Mərci Şorbası)",
            nameRu = "Чечевичный суп",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 kasa (300 q)",
            defaultServingLabelEn = "1 bowl (300 g)",
            defaultServingLabelRu = "1 тарелка (300 г)",
            defaultServingGrams = 300.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 bowl|1 тарелка", 300.0)
            ),
            caloriesPer100g = 76.0, proteinPer100g = 4.5, carbsPer100g = 10.6, fatPer100g = 1.8,
            fiberPer100g = 3.1, sugarPer100g = 1.2, sodiumMgPer100g = 320.0,
            ironMgPer100g = 1.5, potassiumMgPer100g = 210.0, magnesiumMgPer100g = 20.0, vitaminAMcgPer100g = 62.0
        ),
        FoodItemEntity(
            nameAz = "Çoban Salatı",
            nameEn = "Choban Salad (Tomato, Cucumber, Herbs)",
            nameRu = "Салат Чобан (огурцы, томаты, зелень)",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 kasa (200 q)",
            defaultServingLabelEn = "1 bowl (200 g)",
            defaultServingLabelRu = "1 порция (200 г)",
            defaultServingGrams = 200.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 bowl|1 порция", 200.0)
            ),
            caloriesPer100g = 42.0, proteinPer100g = 1.2, carbsPer100g = 4.6, fatPer100g = 2.2,
            fiberPer100g = 1.4, sugarPer100g = 2.4, sodiumMgPer100g = 180.0,
            calciumMgPer100g = 22.0, potassiumMgPer100g = 215.0, vitaminAMcgPer100g = 52.0, vitaminCMgPer100g = 17.0
        ),
        FoodItemEntity(
            nameAz = "Paytaxt Salatı",
            nameEn = "Paytakht Salad (Capital Salad)",
            nameRu = "Столичный салат",
            category = FoodCategory.AZERBAIJANI_CUISINE.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 porsiya (180 q)",
            defaultServingLabelEn = "1 serving (180 g)",
            defaultServingLabelRu = "1 порция (180 г)",
            defaultServingGrams = 180.0,
            householdMeasuresJson = measuresJson(
                Triple("1 porsiya", "1 serving|1 порция", 180.0)
            ),
            caloriesPer100g = 165.0, proteinPer100g = 5.4, carbsPer100g = 10.2, fatPer100g = 11.4,
            fiberPer100g = 1.5, sugarPer100g = 1.8, sodiumMgPer100g = 410.0,
            potassiumMgPer100g = 195.0, vitaminAMcgPer100g = 80.0, vitaminCMgPer100g = 8.0
        ),
        FoodItemEntity(
            nameAz = "Təndir Çörəyi",
            nameEn = "Tandoor Bread (Təndir Çörəyi)",
            nameRu = "Тандырный хлеб",
            category = FoodCategory.GRAINS_BREAD.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 dilim (60 q)",
            defaultServingLabelEn = "1 slice (60 g)",
            defaultServingLabelRu = "1 ломтик (60 г)",
            defaultServingGrams = 60.0,
            householdMeasuresJson = measuresJson(
                Triple("1 dilim", "1 slice|1 ломтик", 60.0),
                Triple("1/4 çörək", "1/4 loaf|1/4 лепёшки", 110.0)
            ),
            caloriesPer100g = 268.0, proteinPer100g = 8.5, carbsPer100g = 53.0, fatPer100g = 2.0,
            fiberPer100g = 2.6, sugarPer100g = 2.4, sodiumMgPer100g = 480.0,
            ironMgPer100g = 1.8, magnesiumMgPer100g = 24.0
        ),
        FoodItemEntity(
            nameAz = "Lavaş (nazik)",
            nameEn = "Lavash Flatbread",
            nameRu = "Тонкий лаваш",
            category = FoodCategory.GRAINS_BREAD.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 vərəq (70 q)",
            defaultServingLabelEn = "1 sheet (70 g)",
            defaultServingLabelRu = "1 лист (70 г)",
            defaultServingGrams = 70.0,
            householdMeasuresJson = measuresJson(
                Triple("1 vərəq", "1 sheet|1 лист", 70.0)
            ),
            caloriesPer100g = 275.0, proteinPer100g = 8.9, carbsPer100g = 56.0, fatPer100g = 1.3,
            fiberPer100g = 2.4, sodiumMgPer100g = 510.0, ironMgPer100g = 1.5
        ),
        FoodItemEntity(
            nameAz = "Azərbaycan Paxlavası",
            nameEn = "Azerbaijani Baklava (Paxlava)",
            nameRu = "Азербайджанская пахлава",
            category = FoodCategory.DESSERTS.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 ədəd (55 q)",
            defaultServingLabelEn = "1 piece (55 g)",
            defaultServingLabelRu = "1 кусочек (55 г)",
            defaultServingGrams = 55.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 piece|1 кусочек", 55.0)
            ),
            caloriesPer100g = 438.0, proteinPer100g = 6.8, carbsPer100g = 47.0, fatPer100g = 25.0,
            fiberPer100g = 2.4, sugarPer100g = 29.0, sodiumMgPer100g = 140.0,
            calciumMgPer100g = 44.0, magnesiumMgPer100g = 52.0
        ),
        FoodItemEntity(
            nameAz = "Şəkərbura",
            nameEn = "Shekerbura (Nut Pastry)",
            nameRu = "Шекербура",
            category = FoodCategory.DESSERTS.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 ədəd (65 q)",
            defaultServingLabelEn = "1 piece (65 g)",
            defaultServingLabelRu = "1 штука (65 г)",
            defaultServingGrams = 65.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 piece|1 штука", 65.0)
            ),
            caloriesPer100g = 425.0, proteinPer100g = 7.4, carbsPer100g = 49.0, fatPer100g = 22.0,
            fiberPer100g = 2.6, sugarPer100g = 24.0, sodiumMgPer100g = 110.0,
            calciumMgPer100g = 55.0, magnesiumMgPer100g = 58.0
        ),
        FoodItemEntity(
            nameAz = "Badambura",
            nameEn = "Badambura (Almond Flaky Pastry)",
            nameRu = "Бадамбура",
            category = FoodCategory.DESSERTS.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 ədəd (75 q)",
            defaultServingLabelEn = "1 piece (75 g)",
            defaultServingLabelRu = "1 штука (75 г)",
            defaultServingGrams = 75.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 piece|1 штука", 75.0)
            ),
            caloriesPer100g = 446.0, proteinPer100g = 7.9, carbsPer100g = 46.0, fatPer100g = 25.5,
            fiberPer100g = 2.9, sugarPer100g = 22.0, sodiumMgPer100g = 120.0
        ),

        // --- FRUITS & VEGETABLES ---
        FoodItemEntity(
            nameAz = "Nar (təzə dənə)",
            nameEn = "Pomegranate Seeds",
            nameRu = "Гранат (зёрна)",
            category = FoodCategory.FRUITS.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 kasa (150 q)",
            defaultServingLabelEn = "1 cup (150 g)",
            defaultServingLabelRu = "1 стакан (150 г)",
            defaultServingGrams = 150.0,
            householdMeasuresJson = measuresJson(
                Triple("1 kasa", "1 cup|1 стакан", 150.0),
                Triple("1/2 nar", "1/2 pomegranate|1/2 граната", 90.0)
            ),
            caloriesPer100g = 83.0, proteinPer100g = 1.7, carbsPer100g = 18.7, fatPer100g = 1.2,
            fiberPer100g = 4.0, sugarPer100g = 13.7, sodiumMgPer100g = 3.0,
            calciumMgPer100g = 10.0, ironMgPer100g = 0.3, potassiumMgPer100g = 236.0,
            magnesiumMgPer100g = 12.0, vitaminCMgPer100g = 10.2
        ),
        FoodItemEntity(
            nameAz = "Alma (Quba alması)",
            nameEn = "Apple",
            nameRu = "Яблоко",
            category = FoodCategory.FRUITS.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 orta ədəd (160 q)",
            defaultServingLabelEn = "1 medium (160 g)",
            defaultServingLabelRu = "1 среднее (160 г)",
            defaultServingGrams = 160.0,
            householdMeasuresJson = measuresJson(
                Triple("1 orta ədəd", "1 medium|1 среднее", 160.0)
            ),
            caloriesPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 13.8, fatPer100g = 0.2,
            fiberPer100g = 2.4, sugarPer100g = 10.4, potassiumMgPer100g = 107.0, vitaminCMgPer100g = 4.6
        ),
        FoodItemEntity(
            nameAz = "Banan",
            nameEn = "Banana",
            nameRu = "Банан",
            category = FoodCategory.FRUITS.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 ədəd (120 q)",
            defaultServingLabelEn = "1 medium (120 g)",
            defaultServingLabelRu = "1 средний (120 г)",
            defaultServingGrams = 120.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 medium|1 средний", 120.0)
            ),
            caloriesPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 22.8, fatPer100g = 0.3,
            fiberPer100g = 2.6, sugarPer100g = 12.2, potassiumMgPer100g = 358.0,
            magnesiumMgPer100g = 27.0, vitaminCMgPer100g = 8.7
        ),
        FoodItemEntity(
            nameAz = "Xurma (şərq xurması)",
            nameEn = "Persimmon",
            nameRu = "Хурма",
            category = FoodCategory.FRUITS.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 ədəd (150 q)",
            defaultServingLabelEn = "1 fruit (150 g)",
            defaultServingLabelRu = "1 штука (150 г)",
            defaultServingGrams = 150.0,
            caloriesPer100g = 70.0, proteinPer100g = 0.6, carbsPer100g = 18.6, fatPer100g = 0.2,
            fiberPer100g = 3.6, sugarPer100g = 12.5, potassiumMgPer100g = 161.0,
            vitaminAMcgPer100g = 81.0, vitaminCMgPer100g = 7.5
        ),
        FoodItemEntity(
            nameAz = "Əncir (təzə)",
            nameEn = "Fresh Fig",
            nameRu = "Свежий инжир",
            category = FoodCategory.FRUITS.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "2 ədəd (100 q)",
            defaultServingLabelEn = "2 figs (100 g)",
            defaultServingLabelRu = "2 штуки (100 г)",
            defaultServingGrams = 100.0,
            caloriesPer100g = 74.0, proteinPer100g = 0.8, carbsPer100g = 19.2, fatPer100g = 0.3,
            fiberPer100g = 2.9, sugarPer100g = 16.3, calciumMgPer100g = 35.0, potassiumMgPer100g = 232.0
        ),
        FoodItemEntity(
            nameAz = "Pomidor",
            nameEn = "Tomato",
            nameRu = "Помидор",
            category = FoodCategory.VEGETABLES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 orta ədəd (125 q)",
            defaultServingLabelEn = "1 medium (125 g)",
            defaultServingLabelRu = "1 средний (125 г)",
            defaultServingGrams = 125.0,
            caloriesPer100g = 18.0, proteinPer100g = 0.9, carbsPer100g = 3.9, fatPer100g = 0.2,
            fiberPer100g = 1.2, sugarPer100g = 2.6, potassiumMgPer100g = 237.0,
            vitaminAMcgPer100g = 42.0, vitaminCMgPer100g = 13.7
        ),
        FoodItemEntity(
            nameAz = "Xiyar",
            nameEn = "Cucumber",
            nameRu = "Огурец",
            category = FoodCategory.VEGETABLES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 ədəd (120 q)",
            defaultServingLabelEn = "1 cucumber (120 g)",
            defaultServingLabelRu = "1 огурец (120 г)",
            defaultServingGrams = 120.0,
            caloriesPer100g = 15.0, proteinPer100g = 0.7, carbsPer100g = 3.6, fatPer100g = 0.1,
            fiberPer100g = 0.5, potassiumMgPer100g = 147.0, magnesiumMgPer100g = 13.0
        ),
        FoodItemEntity(
            nameAz = "İspanaq (təzə)",
            nameEn = "Spinach",
            nameRu = "Шпинат",
            category = FoodCategory.VEGETABLES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "100 q",
            defaultServingLabelEn = "100 g",
            defaultServingLabelRu = "100 г",
            defaultServingGrams = 100.0,
            caloriesPer100g = 23.0, proteinPer100g = 2.9, carbsPer100g = 3.6, fatPer100g = 0.4,
            fiberPer100g = 2.2, calciumMgPer100g = 99.0, ironMgPer100g = 2.7, potassiumMgPer100g = 558.0,
            magnesiumMgPer100g = 79.0, vitaminAMcgPer100g = 469.0, vitaminCMgPer100g = 28.0
        ),
        FoodItemEntity(
            nameAz = "Qaynadılmış kartof",
            nameEn = "Boiled Potato",
            nameRu = "Отварной картофель",
            category = FoodCategory.VEGETABLES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 orta ədəd (150 q)",
            defaultServingLabelEn = "1 medium (150 g)",
            defaultServingLabelRu = "1 средний (150 г)",
            defaultServingGrams = 150.0,
            caloriesPer100g = 87.0, proteinPer100g = 1.9, carbsPer100g = 20.1, fatPer100g = 0.1,
            fiberPer100g = 1.8, potassiumMgPer100g = 379.0, magnesiumMgPer100g = 22.0, vitaminCMgPer100g = 13.0
        ),

        // --- DAIRY ---
        FoodItemEntity(
            nameAz = "Təbii Qatıq (3.2%)",
            nameEn = "Plain Yogurt (Qatıq)",
            nameRu = "Натуральный катык / йогурт",
            category = FoodCategory.DAIRY.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 stəkan (200 q)",
            defaultServingLabelEn = "1 cup (200 g)",
            defaultServingLabelRu = "1 стакан (200 г)",
            defaultServingGrams = 200.0,
            householdMeasuresJson = measuresJson(
                Triple("1 stəkan", "1 cup|1 стакан", 200.0),
                Triple("1 xörək qaşığı", "1 tbsp|1 ст.л.", 20.0)
            ),
            caloriesPer100g = 61.0, proteinPer100g = 3.5, carbsPer100g = 4.7, fatPer100g = 3.2,
            sugarPer100g = 4.7, sodiumMgPer100g = 46.0, calciumMgPer100g = 121.0,
            potassiumMgPer100g = 155.0, magnesiumMgPer100g = 12.0, vitaminB12McgPer100g = 0.4
        ),
        FoodItemEntity(
            nameAz = "Ağ Pendir (duzlu)",
            nameEn = "Azerbaijani White Cheese",
            nameRu = "Белый сыр (брынза)",
            category = FoodCategory.DAIRY.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 dilim (40 q)",
            defaultServingLabelEn = "1 slice (40 g)",
            defaultServingLabelRu = "1 ломтик (40 г)",
            defaultServingGrams = 40.0,
            householdMeasuresJson = measuresJson(
                Triple("1 dilim", "1 slice|1 ломтик", 40.0)
            ),
            caloriesPer100g = 264.0, proteinPer100g = 14.2, carbsPer100g = 4.1, fatPer100g = 21.3,
            sodiumMgPer100g = 920.0, calciumMgPer100g = 493.0, vitaminAMcgPer100g = 125.0, vitaminB12McgPer100g = 1.7
        ),
        FoodItemEntity(
            nameAz = "Şor (az yağlı kəsmik)",
            nameEn = "Cottage Cheese (Şor)",
            nameRu = "Творог (шор)",
            category = FoodCategory.DAIRY.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 kasa (150 q)",
            defaultServingLabelEn = "1 bowl (150 g)",
            defaultServingLabelRu = "1 порция (150 г)",
            defaultServingGrams = 150.0,
            caloriesPer100g = 98.0, proteinPer100g = 16.5, carbsPer100g = 3.2, fatPer100g = 2.0,
            sodiumMgPer100g = 180.0, calciumMgPer100g = 110.0, vitaminB12McgPer100g = 0.5
        ),
        FoodItemEntity(
            nameAz = "Süd (2.5%)",
            nameEn = "Milk (2.5%)",
            nameRu = "Молоко (2.5%)",
            category = FoodCategory.DAIRY.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 stəkan (240 ml)",
            defaultServingLabelEn = "1 glass (240 ml)",
            defaultServingLabelRu = "1 стакан (240 мл)",
            defaultServingGrams = 240.0,
            caloriesPer100g = 52.0, proteinPer100g = 3.3, carbsPer100g = 4.8, fatPer100g = 2.5,
            sugarPer100g = 4.8, sodiumMgPer100g = 44.0, calciumMgPer100g = 120.0,
            vitaminB12McgPer100g = 0.45, vitaminDMcgPer100g = 1.0
        ),

        // --- MEAT, EGGS, FISH, LEGUMES ---
        FoodItemEntity(
            nameAz = "Qaynadılmış yumurta",
            nameEn = "Boiled Egg",
            nameRu = "Варёное яйцо",
            category = FoodCategory.MEAT_FISH.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "2 ədəd (100 q)",
            defaultServingLabelEn = "2 eggs (100 g)",
            defaultServingLabelRu = "2 яйца (100 г)",
            defaultServingGrams = 100.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ədəd", "1 egg|1 яйцо", 50.0),
                Triple("2 ədəd", "2 eggs|2 яйца", 100.0)
            ),
            caloriesPer100g = 155.0, proteinPer100g = 12.6, carbsPer100g = 1.1, fatPer100g = 10.6,
            sodiumMgPer100g = 124.0, calciumMgPer100g = 50.0, ironMgPer100g = 1.2,
            vitaminAMcgPer100g = 149.0, vitaminB12McgPer100g = 1.1, vitaminDMcgPer100g = 2.2
        ),
        FoodItemEntity(
            nameAz = "Toyuq sinəsi (qril / bişmiş)",
            nameEn = "Grilled Chicken Breast",
            nameRu = "Куриная грудка на гриле",
            category = FoodCategory.MEAT_FISH.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 porsiya (150 q)",
            defaultServingLabelEn = "1 fillet (150 g)",
            defaultServingLabelRu = "1 филе (150 г)",
            defaultServingGrams = 150.0,
            caloriesPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6,
            sodiumMgPer100g = 74.0, ironMgPer100g = 1.0, potassiumMgPer100g = 256.0,
            magnesiumMgPer100g = 29.0, vitaminB12McgPer100g = 0.3
        ),
        FoodItemEntity(
            nameAz = "Qızılbalıq (qril)",
            nameEn = "Grilled Salmon",
            nameRu = "Лосось на гриле",
            category = FoodCategory.MEAT_FISH.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 porsiya (150 q)",
            defaultServingLabelEn = "1 fillet (150 g)",
            defaultServingLabelRu = "1 филе (150 г)",
            defaultServingGrams = 150.0,
            caloriesPer100g = 206.0, proteinPer100g = 22.1, carbsPer100g = 0.0, fatPer100g = 12.4,
            sodiumMgPer100g = 61.0, potassiumMgPer100g = 384.0, magnesiumMgPer100g = 30.0,
            vitaminB12McgPer100g = 2.8, vitaminDMcgPer100g = 11.0
        ),
        FoodItemEntity(
            nameAz = "Kütüm balığı (bişmiş)",
            nameEn = "Caspian Kutum Fish (Baked)",
            nameRu = "Кутум запечённый",
            category = FoodCategory.MEAT_FISH.name,
            originType = NutritionSourceType.ESTIMATED_RECIPE.name,
            defaultServingLabelAz = "1 porsiya (180 q)",
            defaultServingLabelEn = "1 serving (180 g)",
            defaultServingLabelRu = "1 порция (180 г)",
            defaultServingGrams = 180.0,
            caloriesPer100g = 138.0, proteinPer100g = 20.2, carbsPer100g = 0.0, fatPer100g = 6.0,
            sodiumMgPer100g = 72.0, potassiumMgPer100g = 320.0, magnesiumMgPer100g = 28.0,
            vitaminB12McgPer100g = 2.1, vitaminDMcgPer100g = 4.5
        ),

        // --- GRAINS, CEREALS & NUTS ---
        FoodItemEntity(
            nameAz = "Yulaf sıyığı (suda bişmiş)",
            nameEn = "Cooked Oatmeal",
            nameRu = "Овсяная каша на воде",
            category = FoodCategory.GRAINS_BREAD.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 kasa (220 q)",
            defaultServingLabelEn = "1 bowl (220 g)",
            defaultServingLabelRu = "1 тарелка (220 г)",
            defaultServingGrams = 220.0,
            caloriesPer100g = 71.0, proteinPer100g = 2.5, carbsPer100g = 12.0, fatPer100g = 1.5,
            fiberPer100g = 1.7, ironMgPer100g = 0.9, magnesiumMgPer100g = 27.0
        ),
        FoodItemEntity(
            nameAz = "Qarabaşaq (bişmiş qreçka)",
            nameEn = "Cooked Buckwheat",
            nameRu = "Гречневая каша",
            category = FoodCategory.GRAINS_BREAD.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 boşqab (180 q)",
            defaultServingLabelEn = "1 bowl (180 g)",
            defaultServingLabelRu = "1 порция (180 г)",
            defaultServingGrams = 180.0,
            caloriesPer100g = 92.0, proteinPer100g = 3.4, carbsPer100g = 19.9, fatPer100g = 0.6,
            fiberPer100g = 2.7, ironMgPer100g = 0.8, magnesiumMgPer100g = 51.0, potassiumMgPer100g = 88.0
        ),
        FoodItemEntity(
            nameAz = "Qoz ləpəsi",
            nameEn = "Walnuts",
            nameRu = "Грецкие орехи",
            category = FoodCategory.OTHER.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 ovuc (30 q)",
            defaultServingLabelEn = "1 handful (30 g)",
            defaultServingLabelRu = "1 горсть (30 г)",
            defaultServingGrams = 30.0,
            householdMeasuresJson = measuresJson(
                Triple("1 ovuc", "1 handful|1 горсть", 30.0)
            ),
            caloriesPer100g = 654.0, proteinPer100g = 15.2, carbsPer100g = 13.7, fatPer100g = 65.2,
            fiberPer100g = 6.7, sugarPer100g = 2.6, calciumMgPer100g = 98.0,
            ironMgPer100g = 2.9, potassiumMgPer100g = 441.0, magnesiumMgPer100g = 158.0
        ),
        FoodItemEntity(
            nameAz = "Təbii bal",
            nameEn = "Natural Honey",
            nameRu = "Натуральный мёд",
            category = FoodCategory.OTHER.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 çay qaşığı (7 q)",
            defaultServingLabelEn = "1 tsp (7 g)",
            defaultServingLabelRu = "1 ч.л. (7 г)",
            defaultServingGrams = 7.0,
            caloriesPer100g = 304.0, proteinPer100g = 0.3, carbsPer100g = 82.4, fatPer100g = 0.0,
            sugarPer100g = 82.1
        ),

        // --- BEVERAGES & PACKAGED (WITH SAMPLE BARCODES FOR OFFLINE LOOKUP) ---
        FoodItemEntity(
            nameAz = "Azərbaycan qara çayı (şəkərsiz)",
            nameEn = "Black Tea (Unsweetened)",
            nameRu = "Чёрный чай (без сахара)",
            category = FoodCategory.BEVERAGES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 armudu stəkan (120 ml)",
            defaultServingLabelEn = "1 cup (120 ml)",
            defaultServingLabelRu = "1 стакан (120 мл)",
            defaultServingGrams = 120.0,
            caloriesPer100g = 1.0, proteinPer100g = 0.0, carbsPer100g = 0.2, fatPer100g = 0.0
        ),
        FoodItemEntity(
            nameAz = "Ayran (Azərbaycan süd içkisi)",
            nameEn = "Ayran (Salted Yogurt Drink)",
            nameRu = "Айран",
            category = FoodCategory.BEVERAGES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 stəkan (250 ml)",
            defaultServingLabelEn = "1 bottle/glass (250 ml)",
            defaultServingLabelRu = "1 стакан (250 мл)",
            defaultServingGrams = 250.0,
            caloriesPer100g = 38.0, proteinPer100g = 2.0, carbsPer100g = 2.8, fatPer100g = 2.0,
            sodiumMgPer100g = 280.0, calciumMgPer100g = 75.0,
            barcode = "4760000000012"
        ),
        FoodItemEntity(
            nameAz = "Təbii Nar Şirəsi (100%)",
            nameEn = "100% Pomegranate Juice",
            nameRu = "100% Гранатовый сок",
            category = FoodCategory.BEVERAGES.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "1 stəkan (200 ml)",
            defaultServingLabelEn = "1 glass (200 ml)",
            defaultServingLabelRu = "1 стакан (200 мл)",
            defaultServingGrams = 200.0,
            caloriesPer100g = 54.0, proteinPer100g = 0.2, carbsPer100g = 13.1, fatPer100g = 0.3,
            sugarPer100g = 12.6, potassiumMgPer100g = 214.0, vitaminCMgPer100g = 5.0,
            barcode = "4760000000029"
        ),
        FoodItemEntity(
            nameAz = "Tünd Şokolad 70%",
            nameEn = "Dark Chocolate 70%",
            nameRu = "Тёмный шоколад 70%",
            category = FoodCategory.PACKAGED.name,
            originType = NutritionSourceType.DATABASE.name,
            defaultServingLabelAz = "2 dilim (20 q)",
            defaultServingLabelEn = "2 squares (20 g)",
            defaultServingLabelRu = "2 дольки (20 г)",
            defaultServingGrams = 20.0,
            caloriesPer100g = 598.0, proteinPer100g = 7.8, carbsPer100g = 45.9, fatPer100g = 42.6,
            fiberPer100g = 10.9, sugarPer100g = 24.0, ironMgPer100g = 11.9, magnesiumMgPer100g = 228.0,
            barcode = "4760000000036"
        )
    )
}
