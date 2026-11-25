package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodItemEntity::class,
        DiaryEntryEntity::class,
        WaterLogEntity::class,
        WeightLogEntity::class,
        ExerciseLogEntity::class,
        RecipeEntity::class,
        SavedMealPlanEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NutriLifeDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun recipeDao(): RecipeDao
    abstract fun savedMealPlanDao(): SavedMealPlanDao

    companion object {
        @Volatile
        private var INSTANCE: NutriLifeDatabase? = null

        fun getInstance(context: Context): NutriLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriLifeDatabase::class.java,
                    "nutrilife_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
