package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun countFoods(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(item: FoodItemEntity): Long

    @Query(
        """
        SELECT * FROM foods
        WHERE (:category IS NULL OR category = :category)
          AND (:onlyFavorites = 0 OR isFavorite = 1)
          AND (
              :query = '' OR
              nameAz LIKE '%' || :query || '%' OR
              nameEn LIKE '%' || :query || '%' OR
              nameRu LIKE '%' || :query || '%'
          )
        ORDER BY isFavorite DESC, id ASC
        LIMIT :limit
        """
    )
    fun searchFoods(
        query: String,
        category: String?,
        onlyFavorites: Boolean,
        limit: Int = 50
    ): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): FoodItemEntity?

    @Query("UPDATE foods SET isFavorite = :favorite WHERE id = :foodId")
    suspend fun setFavorite(foodId: Long, favorite: Boolean)

    @Query("SELECT * FROM foods ORDER BY id ASC LIMIT :limit")
    suspend fun getTopFoods(limit: Int = 60): List<FoodItemEntity>
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE dateIso = :dateIso ORDER BY createdAtMillis DESC")
    fun observeByDate(dateIso: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE dateIso >= :fromDateIso ORDER BY dateIso ASC, createdAtMillis ASC")
    fun observeFromDate(fromDateIso: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries ORDER BY dateIso DESC, createdAtMillis DESC")
    suspend fun getAllEntriesOnce(): List<DiaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM diary_entries")
    suspend fun clearAll()
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE dateIso = :dateIso ORDER BY timestampMillis DESC")
    fun observeByDate(dateIso: String): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE dateIso >= :fromDateIso ORDER BY dateIso ASC")
    fun observeFromDate(fromDateIso: String): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs ORDER BY dateIso DESC")
    suspend fun getAllOnce(): List<WaterLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLogEntity): Long

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM water_logs")
    suspend fun clearAll()
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY dateIso DESC, timestampMillis DESC LIMIT 90")
    fun observeRecent(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY dateIso DESC, timestampMillis DESC")
    suspend fun getAllOnce(): List<WeightLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WeightLogEntity): Long

    @Update
    suspend fun updateLog(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM weight_logs")
    suspend fun clearAll()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_logs WHERE dateIso = :dateIso ORDER BY timestampMillis DESC")
    fun observeByDate(dateIso: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs WHERE dateIso >= :fromDateIso ORDER BY dateIso DESC")
    fun observeFromDate(fromDateIso: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs ORDER BY dateIso DESC")
    suspend fun getAllOnce(): List<ExerciseLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExerciseLogEntity): Long

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exercise_logs")
    suspend fun clearAll()
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recipes")
    suspend fun clearAll()
}

@Dao
interface SavedMealPlanDao {
    @Query("SELECT * FROM saved_meal_plans ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SavedMealPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SavedMealPlanEntity): Long

    @Query("DELETE FROM saved_meal_plans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_meal_plans")
    suspend fun clearAll()
}
